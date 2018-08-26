package com.mgr.arapp.zoodigitalassistant.ar;

import android.app.Activity;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.badlogic.gdx.backends.android.AndroidApplication;
import com.badlogic.gdx.backends.android.AndroidApplicationConfiguration;
import com.mgr.arapp.zoodigitalassistant.R;
import com.mgr.arapp.zoodigitalassistant.ar.libgdx.Engine;
import com.mgr.arapp.zoodigitalassistant.ar.vuforia.AppSession;
import com.mgr.arapp.zoodigitalassistant.ar.vuforia.SessionControl;
import com.mgr.arapp.zoodigitalassistant.ar.vuforia.VuforiaException;
import com.mgr.arapp.zoodigitalassistant.ar.vuforia.VuforiaRenderer;
import com.mgr.arapp.zoodigitalassistant.ar.vuforia.utils.Texture;
import com.mgr.arapp.zoodigitalassistant.ar.vuforia.videoPlayback.VideoPlaybackRenderer;
import com.mgr.arapp.zoodigitalassistant.ar.vuforia.videoPlayback.VideoPlayerHelper;
import com.mgr.arapp.zoodigitalassistant.utils.LoadingDialogHandler;
import com.mgr.arapp.zoodigitalassistant.xmlparser.Animal;
import com.vuforia.CameraDevice;
import com.vuforia.DataSet;
import com.vuforia.HINT;
import com.vuforia.ObjectTracker;
import com.vuforia.STORAGE_TYPE;
import com.vuforia.State;
import com.vuforia.Tracker;
import com.vuforia.TrackerManager;
import com.vuforia.Vuforia;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by Marcin on 14.07.2018.
 */

public class DigitalAssistantActivity extends AndroidApplication implements SessionControl {

    private static final String LOGTAG = "DigitalAssistant";

    private AppSession session;

    private DataSet posterDataSet;
    private Engine mEngine;
    private boolean mPlayFullscreenVideo = false;
    private boolean renderVideo = false;
    VuforiaRenderer mRenderer;

    Map<String, Texture> mTextures  = new HashMap<>();

    private GestureDetector mGestureDetector = null;
    private GestureDetector.SimpleOnGestureListener mSimpleListener = null;

    LoadingDialogHandler loadingDialogHandler = new LoadingDialogHandler(this);
    private RelativeLayout mUILayout;
    @Override
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
//        setContentView(R.layout.camera_overlay);
        startLoadingAnimation();

        session = new AppSession(this);
        session.initAR(this, ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        mRenderer = new VuforiaRenderer(session);

        FrameLayout container = (FrameLayout) findViewById(R.id.ar_container);

        AndroidApplicationConfiguration config = new AndroidApplicationConfiguration();
        config.useAccelerometer = false;
        config.useCompass = false;

        mSimpleListener = new GestureDetector.SimpleOnGestureListener();
        mGestureDetector = new GestureDetector(getApplicationContext(),
                mSimpleListener);


        mGestureDetector.setOnDoubleTapListener(new GestureDetector.OnDoubleTapListener()
        {
            public boolean onDoubleTap(MotionEvent e)
            {
                // We do not react to this event
                return false;
            }


            public boolean onDoubleTapEvent(MotionEvent e)
            {
                // We do not react to this event
                return false;
            }


            // Handle the single tap
            public boolean onSingleTapConfirmed(MotionEvent e)
            {
                final Handler autofocusHandler = new Handler();
                // Do not react if the StartupScreen is being displayed
                for (Animal animal : mEngine.animalModels)
                {
                    // Verify that the tap happened inside the target
                    // todo dorobic weryfikacje czy klikniecie bylo w przestrzen video
                    if (mEngine.videoPlaybackRenderer!= null && renderVideo)
                    {
                        VideoPlayerHelper mVideoPlayerHelper = mEngine.videoPlaybackRenderer.mVideoPlayerHelper.get(animal.marker);
                        // Check if it is playable on texture
                        if (mEngine.videoPlaybackRenderer.mVideoPlayerHelper.get(animal.marker).isPlayableOnTexture())
                        {
                            // We can play only if the movie was paused, ready
                            // or stopped

                            if ((mVideoPlayerHelper.getStatus() == VideoPlayerHelper.MEDIA_STATE.PAUSED)
                                    || (mVideoPlayerHelper.getStatus() == VideoPlayerHelper.MEDIA_STATE.READY)
                                    || (mVideoPlayerHelper.getStatus() == VideoPlayerHelper.MEDIA_STATE.STOPPED)
                                    || (mVideoPlayerHelper.getStatus() == VideoPlayerHelper.MEDIA_STATE.REACHED_END))
                            {
                                // Pause all other media
                                pauseAll(animal.marker);

                                // If it has reached the end then rewind
                                if ((mVideoPlayerHelper.getStatus() == VideoPlayerHelper.MEDIA_STATE.REACHED_END))
                                    mEngine.videoPlaybackRenderer.mSeekPosition.put(animal.marker, 0);

                                mVideoPlayerHelper.play(mPlayFullscreenVideo,
                                        mEngine.videoPlaybackRenderer.mSeekPosition.get(animal.marker));
                                mEngine.videoPlaybackRenderer.mSeekPosition.put(animal.marker, VideoPlayerHelper.CURRENT_POSITION);
                            } else if (mVideoPlayerHelper.getStatus() == VideoPlayerHelper.MEDIA_STATE.PLAYING)
                            {
                                // If it is playing then we pause it
                                mVideoPlayerHelper.pause();
                            }
                        } else if (mVideoPlayerHelper.isPlayableFullscreen())
                        {
                            // If it isn't playable on texture
                            // Either because it wasn't requested or because it
                            // isn't supported then request playback fullscreen.
                            mVideoPlayerHelper.play(true,
                                    VideoPlayerHelper.CURRENT_POSITION);
                        }

                        // Even though multiple videos can be loaded only one
                        // can be playing at any point in time. This break
                        // prevents that, say, overlapping videos trigger
                        // simultaneously playback.
                        break;
                    }
                    else
                    {
                        boolean result = CameraDevice.getInstance().setFocusMode(
                                CameraDevice.FOCUS_MODE.FOCUS_MODE_TRIGGERAUTO);
                        if (!result)
                            Log.e("SingleTapConfirmed", "Unable to trigger focus");

                        // Generates a Handler to trigger continuous auto-focus
                        // after 1 second
                        autofocusHandler.postDelayed(new Runnable()
                        {
                            public void run()
                            {
                                final boolean autofocusResult = CameraDevice.getInstance().setFocusMode(
                                        CameraDevice.FOCUS_MODE.FOCUS_MODE_CONTINUOUSAUTO);

                                if (!autofocusResult)
                                    Log.e("SingleTapConfirmed", "Unable to re-enable continuous auto-focus");
                            }
                        }, 1000L);
                    }
                }

                return true;
            }
        });

        mEngine = new Engine(mRenderer, this);
        View glView = initializeForView(mEngine);
        glView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View arg0, MotionEvent arg1) {
                //gesture detector to detect swipe.
                mGestureDetector.onTouchEvent(arg1);
                return true;//always return true to consume event
            }
        });
        container.addView(glView);
        final Button changeRenderContent = findViewById(R.id.change_render_content);
        changeRenderContent.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                renderVideo = !renderVideo;
                if(!renderVideo){
                    for (Animal animal : mEngine.animalModels) {
                        pauseAll(animal.marker);
                    }
                    changeRenderContent.setText("Video");
                } else {
                    changeRenderContent.setText("Model");

                }
                mEngine.setRenderMode(renderVideo);
            }
        });
    }

    private void pauseAll(String except)
    {
        for (Animal animal : mEngine.animalModels)
        {
            VideoPlayerHelper mVideoPlayerHelper = mEngine.videoPlaybackRenderer.mVideoPlayerHelper.get(animal.marker);
            if (animal.marker.equals(except))
            {
                if (mVideoPlayerHelper.isPlayableOnTexture())
                {
                    mVideoPlayerHelper.pause();
                }
            }
        }
    }
    @Override
    public boolean onTouchEvent(MotionEvent event)
    {
        boolean result = false;

        // Process the Gestures
        if (!result)
            mGestureDetector.onTouchEvent(event);

        return result;
    }

    @Override
    protected void onResume() {
        super.onResume();

        try {
            showProgressIndicator(true);
            session.resumeAR();
        } catch (VuforiaException e) {
            Toast.makeText(this, "Unable to start augmented reality.", Toast.LENGTH_LONG).show();
            Log.e(LOGTAG, e.getString());
        }
    }

    @Override
    protected void onPause() {
        super.onPause();

        try {
            session.pauseAR();
        } catch (VuforiaException e){
            Toast.makeText(this, "Unable to stop augmented reality.", Toast.LENGTH_LONG).show();
            Log.e(LOGTAG, e.getString());
        }
    }

    @Override
    public void onConfigurationChanged(Configuration config) {
        super.onConfigurationChanged(config);
        session.onConfigurationChanged();
        mRenderer.updateRenderingPrimitives();
    }

    @Override
    public boolean doInitTrackers() {

        boolean result = true;

        TrackerManager trackerManager = TrackerManager.getInstance();
        Tracker tracker = trackerManager.initTracker(ObjectTracker.getClassType());

        if(tracker == null) {
            result = false;
        }
        return result;
    }

    @Override
    public boolean doLoadTrackersData() {
        TrackerManager trackerManager = TrackerManager.getInstance();
        ObjectTracker imageTracker = (ObjectTracker) trackerManager.getTracker(ObjectTracker.getClassType());

        if (imageTracker == null) {
            Log.d(LOGTAG, "Failed to load tracking data set because the ImageTracker has not been initialized.");
            return false;
        }

        // Create the data sets:
        posterDataSet = imageTracker.createDataSet();
        if (posterDataSet == null) {
            Log.d(LOGTAG, "Failed to create a new tracking data.");
            return false;
        }

        // Load the data sets:
        if (!posterDataSet.load("StonesAndChips.xml", STORAGE_TYPE.STORAGE_APPRESOURCE)) {
            Log.d(LOGTAG, "Failed to load data set.");
            return false;
        }

        // Activate the data set:
        if (!imageTracker.activateDataSet(posterDataSet)) {
            Log.d(LOGTAG, "Failed to activate data set.");
            return false;
        }

        Log.d(LOGTAG, "Successfully loaded and activated data set.");
        return true;
    }

    @Override
    public boolean doStartTrackers() {
        boolean result = true;

        Tracker imageTracker = TrackerManager.getInstance().getTracker(
                ObjectTracker.getClassType());
        if (imageTracker != null) {
            imageTracker.start();
            Vuforia.setHint(HINT.HINT_MAX_SIMULTANEOUS_IMAGE_TARGETS, 1);
        } else
            result = false;

        return result;
    }

    @Override
    public boolean doStopTrackers() {
        // Indicate if the trackers were stopped correctly
        boolean result = true;

        Tracker imageTracker = TrackerManager.getInstance().getTracker(
                ObjectTracker.getClassType());
        if (imageTracker != null)
            imageTracker.stop();
        else
            result = false;

        return result;
    }

    @Override
    public boolean doUnloadTrackersData() {
        boolean result = true;

        // Get the image tracker:
        TrackerManager trackerManager = TrackerManager.getInstance();
        ObjectTracker imageTracker = (ObjectTracker) trackerManager
                .getTracker(ObjectTracker.getClassType());
        if (imageTracker == null) {
            Log.d(LOGTAG, "Failed to destroy the tracking data set because the ImageTracker has not been initialized.");
            return false;
        }

        if (posterDataSet != null) {
            if (imageTracker.getActiveDataSet(0) == posterDataSet && !imageTracker.deactivateDataSet(posterDataSet)) {
                Log.d(LOGTAG, "Failed to destroy the tracking data set StonesAndChips because the data set could not be deactivated.");
                result = false;
            } else if (!imageTracker.destroyDataSet(posterDataSet)) {
                Log.d(LOGTAG, "Failed to destroy the tracking data set StonesAndChips.");
                result = false;
            }

            posterDataSet = null;
        }

        return result;
    }

    @Override
    public boolean doDeinitTrackers() {
        TrackerManager trackerManager = TrackerManager.getInstance();
        trackerManager.deinitTracker(ObjectTracker.getClassType());

        return true;
    }

    @Override
    public void onInitARDone(VuforiaException e) {
        if (e == null){
            mRenderer.mIsActive = true;

            try {
                session.startAR(CameraDevice.CAMERA_DIRECTION.CAMERA_DIRECTION_DEFAULT);
            } catch (VuforiaException ex) {
                Log.e(LOGTAG, ex.getString());
            }
            boolean result = CameraDevice.getInstance().setFocusMode(CameraDevice.FOCUS_MODE.FOCUS_MODE_CONTINUOUSAUTO);

            if (!result) Log.e(LOGTAG, "Unable to enable continuous autofocus");

            try {
                mEngine.resume();
            } catch (Exception ex) {
                ex.printStackTrace();
            }
            showProgressIndicator(false);
        } else {
            Toast.makeText(this, "Unable to start augmented reality.", Toast.LENGTH_LONG).show();
            Log.e(LOGTAG, e.getString());
            finish();
        }

    }

    @Override
    public void onQCARUpdate(State state) {

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        try {
            session.stopAR();
        } catch (VuforiaException e){
            Log.e(LOGTAG, e.getString());
        }
        System.gc();
    }

    private void startLoadingAnimation(){
        Log.d(LOGTAG, "Start loadin anitmation");
        mUILayout = (RelativeLayout) View.inflate(this, R.layout.camera_overlay, null);

        mUILayout.setVisibility(View.VISIBLE);
        mUILayout.setBackgroundColor(Color.BLACK);

        loadingDialogHandler.mLoadingDialogContainer = mUILayout.findViewById(R.id.loading_indicator);

        loadingDialogHandler.sendEmptyMessage(LoadingDialogHandler.SHOW_LOADING_DIALOG);

        addContentView(mUILayout, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));
    }

    public void showProgressIndicator(boolean show)
    {
        if (loadingDialogHandler != null)
        {
            if (show)
            {
                loadingDialogHandler
                        .sendEmptyMessage(LoadingDialogHandler.SHOW_LOADING_DIALOG);
            }
            else
            {
                loadingDialogHandler
                        .sendEmptyMessage(LoadingDialogHandler.HIDE_LOADING_DIALOG);
            }
        }
    }

    private void loadTextures()
    {
        mTextures.put(VideoPlaybackRenderer.DEFAULT_TEXTURE, Texture.loadTextureFromApk(
                "VideoPlayback/VuforiaSizzleReel_1.png", getAssets()));

        mTextures.put(VideoPlaybackRenderer.BUTTON_PLAY, Texture.loadTextureFromApk("VideoPlayback/play.png",
                getAssets()));
        mTextures.put(VideoPlaybackRenderer.BUTTON_BUSY, Texture.loadTextureFromApk("VideoPlayback/busy.png",
                getAssets()));
        mTextures.put(VideoPlaybackRenderer.BUTTON_STOP, Texture.loadTextureFromApk("VideoPlayback/error.png",
                getAssets()));
    }
}
