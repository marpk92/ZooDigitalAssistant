package com.mgr.arapp.zoodigitalassistant.ar;

import android.app.Activity;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
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
import com.mgr.arapp.zoodigitalassistant.utils.LoadingDialogHandler;
import com.vuforia.CameraDevice;
import com.vuforia.DataSet;
import com.vuforia.HINT;
import com.vuforia.ObjectTracker;
import com.vuforia.STORAGE_TYPE;
import com.vuforia.State;
import com.vuforia.Tracker;
import com.vuforia.TrackerManager;
import com.vuforia.Vuforia;

/**
 * Created by Marcin on 14.07.2018.
 */

public class DigitalAssistantActivity extends AndroidApplication implements SessionControl {

    private static final String LOGTAG = "DigitalAssistant";

    private AppSession session;

    private DataSet posterDataSet;
    private Engine mEngine;

    VuforiaRenderer mRenderer;

    LoadingDialogHandler loadingDialogHandler = new LoadingDialogHandler(this);
    private RelativeLayout mUILayout;
    @Override
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.camera_overlay);
        //startLoadingAnimation();

        session = new AppSession(this);
        session.initAR(this, ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        mRenderer = new VuforiaRenderer(session);

        FrameLayout container = (FrameLayout) findViewById(R.id.ar_container);

        AndroidApplicationConfiguration config = new AndroidApplicationConfiguration();
        config.useAccelerometer = false;
        config.useCompass = false;

        mEngine = new Engine(mRenderer, this);
        View glView = initializeForView(mEngine);

        container.addView(glView);

    }

    @Override
    protected void onResume() {
        super.onResume();

        try {
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
        mUILayout = (RelativeLayout) View.inflate(this, R.layout.camera_overlay, null);

        mUILayout.setVisibility(View.VISIBLE);
        mUILayout.setBackgroundColor(Color.BLACK);

        loadingDialogHandler.mLoadingDialogContainer = mUILayout.findViewById(R.id.loading_indicator);

        loadingDialogHandler.sendEmptyMessage(LoadingDialogHandler.SHOW_LOADING_DIALOG);

        addContentView(mUILayout, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));
    }
}
