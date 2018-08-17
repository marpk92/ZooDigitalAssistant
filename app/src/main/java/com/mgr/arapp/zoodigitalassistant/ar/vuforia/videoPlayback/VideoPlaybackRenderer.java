package com.mgr.arapp.zoodigitalassistant.ar.vuforia.videoPlayback;

import android.app.Activity;
import android.opengl.GLES11Ext;
import android.opengl.GLES20;
import android.opengl.Matrix;
import android.os.SystemClock;
import android.util.DisplayMetrics;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.mgr.arapp.zoodigitalassistant.ar.DigitalAssistantActivity;
import com.mgr.arapp.zoodigitalassistant.ar.libgdx.Display;
import com.mgr.arapp.zoodigitalassistant.ar.vuforia.SampleMath;
import com.mgr.arapp.zoodigitalassistant.ar.vuforia.VuforiaRenderer;
import com.mgr.arapp.zoodigitalassistant.ar.vuforia.utils.SampleUtils;
import com.mgr.arapp.zoodigitalassistant.ar.vuforia.utils.Texture;
import com.mgr.arapp.zoodigitalassistant.xmlparser.Animal;
import com.vuforia.COORDINATE_SYSTEM_TYPE;
import com.vuforia.ImageTarget;
import com.vuforia.Matrix34F;
import com.vuforia.Matrix44F;
import com.vuforia.Tool;
import com.vuforia.TrackableResult;
import com.vuforia.Vec2F;
import com.vuforia.Vec3F;
import com.vuforia.ViewList;

import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.microedition.khronos.opengles.GL10;

/**
 * Created by Marcin on 07.08.2018.
 */

public class VideoPlaybackRenderer {

    public static final String BUTTON_STOP = "BUTTON_STOP";
    public static final String BUTTON_BUSY = "BUTTON_BUSY";
    public static final String BUTTON_PLAY = "BUTTON_PLAY";
    public static final String DEFAULT_TEXTURE = "DEFAULT_TEXTURE";
    public int numTargets;

    //todo wielkosc = liczba par model-marker, ustawic w konstruktorze, po zaladowaniu mapy Animals
    Map<String, Boolean> isTracking = new HashMap<>();
    Map<String, Matrix44F> modelViewMatrix  = new HashMap<>();
    Set<String> modelNames;
    Map<String, VideoPlayerHelper.MEDIA_STATE> currentStatus  = new HashMap<>();
    Map<String, Texture> mTextures  = new HashMap<>();
    Map<String, Float> videoQuadAspectRatio  = new HashMap<>();
    Map<String, Float> keyframeQuadAspectRatio = new HashMap<>();
    Map<String, Integer> videoPlaybackTextureIDMap = new HashMap<>();
    int[] videoPlaybackTextureID;

    private Map<String, float[]>  videoQuadTextureCoordsTransformed = new HashMap<>();

    Map<String, Vec3F> targetPositiveDimensions = new HashMap<>();

    private int keyframeShaderID = 0;
    private int keyframeVertexHandle = 0;
    private int keyframeTexCoordHandle = 0;
    private int keyframeMVPMatrixHandle = 0;
    private int keyframeTexSampler2DHandle = 0;

    Buffer quadVertices, quadTexCoords, quadIndices, quadNormals;

    static int NUM_QUAD_INDEX = 6;

    private int videoPlaybackShaderID = 0;
    private int videoPlaybackVertexHandle = 0;
    private int videoPlaybackTexCoordHandle = 0;
    private int videoPlaybackMVPMatrixHandle = 0;
    private int videoPlaybackTexSamplerOESHandle = 0;
    private boolean mIsActive = false;

    double quadVerticesArray[] = { -1.0f, -1.0f, 0.0f, 1.0f, -1.0f, 0.0f, 1.0f,
            1.0f, 0.0f, -1.0f, 1.0f, 0.0f };

    double quadTexCoordsArray[] = { 0.0f, 0.0f, 1.0f, 0.0f, 1.0f, 1.0f, 0.0f,
            1.0f };

    double quadNormalsArray[] = { 0, 0, 1, 0, 0, 1, 0, 0, 1, 0, 0, 1, };

    short quadIndicesArray[] = { 0, 1, 2, 2, 3, 0 };

    private float videoQuadTextureCoords[] = { 0.0f, 0.0f, 1.0f, 0.0f, 1.0f,
            1.0f, 0.0f, 1.0f, };

    private Map<String, float[]> mTexCoordTransformationMatrix = new HashMap<>();
    public Map<String, VideoPlayerHelper> mVideoPlayerHelper = new HashMap<>();
    private Map<String, String> mMovieName = new HashMap<>();
    private Map<String, VideoPlayerHelper.MEDIA_TYPE> mCanRequestType = new HashMap<>();
    public Map<String, Integer> mSeekPosition = new HashMap<>();
    private Map<String, Boolean> mShouldPlayImmediately = new HashMap<>();
    private Map<String, Long> mLostTrackingSince = new HashMap<>();
    private Map<String, Boolean> mLoadRequested = new HashMap<>();

    private List<Animal> animalList;
    private String currentMarkerVideo;
    private Matrix44F tappingProjectionMatrix = null;
    Activity mActivity;
    private VuforiaRenderer vuforiaRenderer;


    public VideoPlaybackRenderer(List<Animal> animalList, VuforiaRenderer arRenderer, Activity activity) {
        this.vuforiaRenderer = arRenderer;
        this.mActivity = activity;
        this.currentMarkerVideo = "";
        loadTextures();
        float[] defaultQuadTextCoord = { 0.0f, 0.0f,
                1.0f, 0.0f, 1.0f, 1.0f, 0.0f, 1.0f,};
        float[] temp = { 0f, 0f, 0f };
        this.animalList = animalList;
        modelNames = new HashSet<>();
        for(Animal animal : animalList){
            modelNames.add(animal.marker);
            modelViewMatrix.put(animal.marker, new Matrix44F());
            keyframeQuadAspectRatio.put(animal.marker, (float) mTextures.get(DEFAULT_TEXTURE).mHeight / (float) mTextures.get(DEFAULT_TEXTURE).mWidth);

            videoQuadTextureCoordsTransformed.put(animal.marker, defaultQuadTextCoord);
            Vec3F tempTarDim = new Vec3F();
            targetPositiveDimensions.put(animal.marker, tempTarDim);
            mVideoPlayerHelper.put(animal.marker, null);
            mMovieName.put(animal.marker, animal.videoUrl);
            mCanRequestType.put(animal.marker, VideoPlayerHelper.MEDIA_TYPE.ON_TEXTURE_FULLSCREEN);
            mSeekPosition.put(animal.marker, 0);
            mShouldPlayImmediately.put(animal.marker, false);
            mLostTrackingSince.put(animal.marker, -1l);
            mLoadRequested.put(animal.marker, true);

            targetPositiveDimensions.put(animal.marker, new Vec3F());
            modelViewMatrix.put(animal.marker, new Matrix44F());

            VideoPlayerHelper playerHelper = new VideoPlayerHelper();
            playerHelper.init();
            playerHelper.setActivity(mActivity);
            mVideoPlayerHelper.put(animal.marker, playerHelper);

        }
        numTargets = modelNames.size();
        videoPlaybackTextureID = new int[numTargets];
        for (int i = 0; i < numTargets; i++)
        {
            videoPlaybackTextureID[i] = -1;
        }
    }

    public boolean isTapOnScreenInsideTarget(String target, float x, float y)
    {
        // Here we calculate that the touch event is inside the target
        Vec3F intersection;
        // Vec3F lineStart = new Vec3F();
        // Vec3F lineEnd = new Vec3F();

        DisplayMetrics metrics = new DisplayMetrics();
        mActivity.getWindowManager().getDefaultDisplay().getMetrics(metrics);
        intersection = SampleMath.getPointToPlaneIntersection(SampleMath
                        .Matrix44FInverse(tappingProjectionMatrix),
                modelViewMatrix.get(target), metrics.widthPixels, metrics.heightPixels,
                new Vec2F(x, y), new Vec3F(0, 0, 0), new Vec3F(0, 0, 1));

        // The target returns as pose the center of the trackable. The following
        // if-statement simply checks that the tap is within this range
        if ((intersection.getData()[0] >= -(targetPositiveDimensions.get(target)
                .getData()[0]))
                && (intersection.getData()[0] <= (targetPositiveDimensions.get(target)
                .getData()[0]))
                && (intersection.getData()[1] >= -(targetPositiveDimensions.get(target)
                .getData()[1]))
                && (intersection.getData()[1] <= (targetPositiveDimensions.get(target)
                .getData()[1])))
            return true;
        else
            return false;
    }

    public void requestLoad(String target, int seekPosition,
                            boolean playImmediately)
    {
        mSeekPosition.put(target, seekPosition);
        mShouldPlayImmediately.put(target, playImmediately);
        mLoadRequested.put(target, true);
    }

    public void setVideoPlayerHelper(String target,
                                     VideoPlayerHelper newVideoPlayerHelper)
    {
        mVideoPlayerHelper.put(target, newVideoPlayerHelper);
    }

    public void initRendering(){
        for (String keyTexture : mTextures.keySet())
        {
            Texture t = mTextures.get(keyTexture);

            GLES20.glGenTextures(1, t.mTextureID, 0);
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, t.mTextureID[0]);
            GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D,
                    GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
            GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D,
                    GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
            GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D,
                    GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
            GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D,
                    GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);
            GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_RGBA,
                    t.mWidth, t.mHeight, 0, GLES20.GL_RGBA,
                    GLES20.GL_UNSIGNED_BYTE, t.mData);
        }
        int textureId = 0;
        for (String modelName : modelNames)
        {
            GLES20.glGenTextures(1, videoPlaybackTextureID, textureId);
            GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
                    videoPlaybackTextureID[textureId]);
            GLES20.glTexParameterf(GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
                    GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
            GLES20.glTexParameterf(GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
                    GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
            GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, 0);
            videoPlaybackTextureIDMap.put(modelName, textureId);
            textureId++;
        }
        //todo dokonczyc
        videoPlaybackShaderID = SampleUtils.createProgramFromShaderSrc(
                VideoPlaybackShaders.VIDEO_PLAYBACK_VERTEX_SHADER,
                VideoPlaybackShaders.VIDEO_PLAYBACK_FRAGMENT_SHADER);
        videoPlaybackVertexHandle = GLES20.glGetAttribLocation(
                videoPlaybackShaderID, "vertexPosition");
        videoPlaybackTexCoordHandle = GLES20.glGetAttribLocation(
                videoPlaybackShaderID, "vertexTexCoord");
        videoPlaybackMVPMatrixHandle = GLES20.glGetUniformLocation(
                videoPlaybackShaderID, "modelViewProjectionMatrix");
        videoPlaybackTexSamplerOESHandle = GLES20.glGetUniformLocation(
                videoPlaybackShaderID, "texSamplerOES");

        // This is a simpler shader with regular 2D textures
        keyframeShaderID = SampleUtils.createProgramFromShaderSrc(
                KeyFrameShaders.KEY_FRAME_VERTEX_SHADER,
                KeyFrameShaders.KEY_FRAME_FRAGMENT_SHADER);
        keyframeVertexHandle = GLES20.glGetAttribLocation(keyframeShaderID,
                "vertexPosition");
        keyframeTexCoordHandle = GLES20.glGetAttribLocation(keyframeShaderID,
                "vertexTexCoord");
        keyframeMVPMatrixHandle = GLES20.glGetUniformLocation(keyframeShaderID,
                "modelViewProjectionMatrix");
        keyframeTexSampler2DHandle = GLES20.glGetUniformLocation(
                keyframeShaderID, "texSampler2D");

        for (String modelName : modelNames){
            keyframeQuadAspectRatio.put(modelName, (float) mTextures
                    .get(DEFAULT_TEXTURE).mHeight / (float) mTextures.get(DEFAULT_TEXTURE).mWidth);
        }
        quadVertices = fillBuffer(quadVerticesArray);
        quadTexCoords = fillBuffer(quadTexCoordsArray);
        quadIndices = fillBuffer(quadIndicesArray);
        quadNormals = fillBuffer(quadNormalsArray);
    }

    public void render(Display display){
        GL20 gl = Gdx.gl;

        gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT);

        TrackableResult[] results = null;

        if (vuforiaRenderer.mIsActive) {
            //render camera background and find targets
            results = vuforiaRenderer.processFrame();
        }
        ViewList viewList = vuforiaRenderer.mRenderingPrimitives.getRenderingViews();

        if(viewList != null && viewList.getNumViews() > 0) {
            Matrix34F projMatrix = vuforiaRenderer.mRenderingPrimitives.getProjectionMatrix(0,
                    COORDINATE_SYSTEM_TYPE.COORDINATE_SYSTEM_CAMERA);

            float rawProjectionMatrixGL[] = Tool.convertPerspectiveProjection2GLMatrix(
                    projMatrix,
                    0.01f,
                    5f)
                    .getData();

            float eyeAdjustmentGL[] = Tool.convert2GLMatrix(vuforiaRenderer.mRenderingPrimitives
                    .getEyeDisplayAdjustmentMatrix(0)).getData();

            float projectionMatrix[] = new float[16];
            Matrix.multiplyMM(projectionMatrix, 0, rawProjectionMatrixGL, 0, eyeAdjustmentGL, 0);
            if (results != null) {
                renderFrame(results, projectionMatrix);
            }
        }
    }

    public void renderFrame(TrackableResult[] results, float[] projectionMatrix){

        float temp[] = { 0.0f, 0.0f, 0.0f };

        if(tappingProjectionMatrix == null)
        {
            tappingProjectionMatrix = new Matrix44F();
            tappingProjectionMatrix.setData(projectionMatrix);
        }

        for (String name : modelNames){
            isTracking.put(name, Boolean.FALSE);
            targetPositiveDimensions.get(name).setData(temp);
        }

        for (TrackableResult result : results) {


            ImageTarget imageTarget = (ImageTarget) result.getTrackable();

            String currentTarget = imageTarget.getName();
            if (currentMarkerVideo.equals(currentTarget)) {
                modelViewMatrix.put(currentTarget, Tool.convertPose2GLMatrix(result.getPose()));

                isTracking.put(currentTarget, Boolean.TRUE);

                targetPositiveDimensions.put(currentTarget, imageTarget.getSize());

                temp[0] = targetPositiveDimensions.get(currentTarget).getData()[0] / 2.0f;
                temp[1] = targetPositiveDimensions.get(currentTarget).getData()[1] / 2.0f;

                if ((currentStatus.get(currentTarget) == VideoPlayerHelper.MEDIA_STATE.READY)
                        || (currentStatus.get(currentTarget) == VideoPlayerHelper.MEDIA_STATE.REACHED_END)
                        || (currentStatus.get(currentTarget) == VideoPlayerHelper.MEDIA_STATE.NOT_READY)
                        || (currentStatus.get(currentTarget) == VideoPlayerHelper.MEDIA_STATE.ERROR)) {

                    float[] modelViewMatrixKeyframe = Tool.convertPose2GLMatrix(
                            result.getPose()).getData();
                    float[] modelViewProjectionKeyframe = new float[16];

                    float ratio = 1.0f;
                    if (mTextures.get(DEFAULT_TEXTURE).mSuccess)
                        ratio = keyframeQuadAspectRatio.get(currentTarget);
                    else {
                        ratio = targetPositiveDimensions.get(currentTarget).getData()[1]
                                / targetPositiveDimensions.get(currentTarget).getData()[0];
                    }

                    Matrix.scaleM(modelViewMatrixKeyframe, 0, targetPositiveDimensions.get(currentTarget).getData()[0],
                            targetPositiveDimensions.get(currentTarget).getData()[0] * ratio,
                            targetPositiveDimensions.get(currentTarget).getData()[0]);
                    Matrix.multiplyMM(modelViewMatrixKeyframe, 0, projectionMatrix, 0,
                            modelViewMatrixKeyframe, 0);

                    GLES20.glUseProgram(keyframeShaderID);

                    // Prepare for rendering the keyframe
                    GLES20.glVertexAttribPointer(keyframeVertexHandle, 3,
                            GLES20.GL_FLOAT, false, 0, quadVertices);
                    GLES20.glVertexAttribPointer(keyframeTexCoordHandle, 2,
                            GLES20.GL_FLOAT, false, 0, quadTexCoords);

                    GLES20.glEnableVertexAttribArray(keyframeVertexHandle);
                    GLES20.glEnableVertexAttribArray(keyframeTexCoordHandle);

                    GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
                    GLES20.glBindTexture(GLES20.GL_TEXTURE_2D,
                            mTextures.get(DEFAULT_TEXTURE).mTextureID[0]);
                    GLES20.glUniformMatrix4fv(keyframeMVPMatrixHandle, 1, false,
                            modelViewProjectionKeyframe, 0);
                    GLES20.glUniform1i(keyframeTexSampler2DHandle, 0);
                    // Render
                    GLES20.glDrawElements(GLES20.GL_TRIANGLES, NUM_QUAD_INDEX,
                            GLES20.GL_UNSIGNED_SHORT, quadIndices);

                    GLES20.glDisableVertexAttribArray(keyframeVertexHandle);
                    GLES20.glDisableVertexAttribArray(keyframeTexCoordHandle);

                    GLES20.glUseProgram(0);
                } else
                //wyswietlenie obrazu podczas odtwarzania lub pauzy
                {
                    float[] modelViewMatrixVideo = Tool.convertPose2GLMatrix(
                            result.getPose()).getData();
                    float[] modelViewProjectionVideo = new float[16];

                    Matrix.scaleM(modelViewMatrixVideo, 0, targetPositiveDimensions.get(currentTarget).getData()[0],
                            targetPositiveDimensions.get(currentTarget).getData()[0]
                                    * videoQuadAspectRatio.get(currentTarget),
                            targetPositiveDimensions.get(currentTarget).getData()[0]);

                    GLES20.glUseProgram(videoPlaybackShaderID);

                    // Prepare for rendering the keyframe
                    GLES20.glVertexAttribPointer(videoPlaybackVertexHandle, 3,
                            GLES20.GL_FLOAT, false, 0, quadVertices);

                    GLES20.glVertexAttribPointer(videoPlaybackTexCoordHandle,
                            2, GLES20.GL_FLOAT, false, 0,
                            fillBuffer(videoQuadTextureCoordsTransformed.get(currentTarget)));

                    GLES20.glEnableVertexAttribArray(videoPlaybackVertexHandle);
                    GLES20.glEnableVertexAttribArray(videoPlaybackTexCoordHandle);

                    GLES20.glActiveTexture(GLES20.GL_TEXTURE0);

                    GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
                            videoPlaybackTextureID[videoPlaybackTextureIDMap.get(currentTarget)]);
                    GLES20.glUniformMatrix4fv(videoPlaybackMVPMatrixHandle, 1,
                            false, modelViewProjectionVideo, 0);
                    GLES20.glUniform1i(videoPlaybackTexSamplerOESHandle, 0);

                    // Render
                    GLES20.glDrawElements(GLES20.GL_TRIANGLES, NUM_QUAD_INDEX,
                            GLES20.GL_UNSIGNED_SHORT, quadIndices);

                    GLES20.glDisableVertexAttribArray(videoPlaybackVertexHandle);
                    GLES20.glDisableVertexAttribArray(videoPlaybackTexCoordHandle);

                    GLES20.glUseProgram(0);
                }

                //render icons
                if ((currentStatus.get(currentTarget) == VideoPlayerHelper.MEDIA_STATE.READY)
                        || (currentStatus.get(currentTarget) == VideoPlayerHelper.MEDIA_STATE.REACHED_END)
                        || (currentStatus.get(currentTarget) == VideoPlayerHelper.MEDIA_STATE.PAUSED)
                        || (currentStatus.get(currentTarget) == VideoPlayerHelper.MEDIA_STATE.NOT_READY)
                        || (currentStatus.get(currentTarget) == VideoPlayerHelper.MEDIA_STATE.ERROR)) {
                    float[] modelViewMatrixButton = Tool.convertPose2GLMatrix(
                            result.getPose()).getData();
                    float[] modelViewProjectionButton = new float[16];

                    GLES20.glDepthFunc(GLES20.GL_LEQUAL);

                    GLES20.glEnable(GLES20.GL_BLEND);
                    GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA,
                            GLES20.GL_ONE_MINUS_SRC_ALPHA);

                    Matrix
                            .translateM(
                                    modelViewMatrixButton,
                                    0,
                                    0.0f,
                                    0.0f,
                                    targetPositiveDimensions.get(currentTarget).getData()[1] / 10.98f);
                    Matrix
                            .scaleM(
                                    modelViewMatrixButton,
                                    0,
                                    (targetPositiveDimensions.get(currentTarget).getData()[1] / 2.0f),
                                    (targetPositiveDimensions.get(currentTarget).getData()[1] / 2.0f),
                                    (targetPositiveDimensions.get(currentTarget).getData()[1] / 2.0f));
                    Matrix.multiplyMM(modelViewProjectionButton, 0,
                            projectionMatrix, 0, modelViewMatrixButton, 0);

                    GLES20.glUseProgram(keyframeShaderID);

                    GLES20.glVertexAttribPointer(keyframeVertexHandle, 3,
                            GLES20.GL_FLOAT, false, 0, quadVertices);
                    GLES20.glVertexAttribPointer(keyframeTexCoordHandle, 2,
                            GLES20.GL_FLOAT, false, 0, quadTexCoords);

                    GLES20.glEnableVertexAttribArray(keyframeVertexHandle);
                    GLES20.glEnableVertexAttribArray(keyframeTexCoordHandle);

                    GLES20.glActiveTexture(GLES20.GL_TEXTURE0);

                    switch (currentStatus.get(currentTarget)) {
                        case READY:
                            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D,
                                    mTextures.get(BUTTON_PLAY).mTextureID[0]);
                            break;
                        case REACHED_END:
                            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D,
                                    mTextures.get(BUTTON_PLAY).mTextureID[0]);
                            break;
                        case PAUSED:
                            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D,
                                    mTextures.get(BUTTON_PLAY).mTextureID[0]);
                            break;
                        case NOT_READY:
                            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D,
                                    mTextures.get(BUTTON_BUSY).mTextureID[0]);
                            break;
                        case ERROR:
                            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D,
                                    mTextures.get(BUTTON_STOP).mTextureID[0]);
                            break;
                        default:
                            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D,
                                    mTextures.get(BUTTON_STOP).mTextureID[0]);
                            break;
                    }

                    GLES20.glUniformMatrix4fv(keyframeMVPMatrixHandle, 1, false,
                            modelViewProjectionButton, 0);
                    GLES20.glUniform1i(keyframeTexSampler2DHandle, 0);

                    // Render
                    GLES20.glDrawElements(GLES20.GL_TRIANGLES, NUM_QUAD_INDEX,
                            GLES20.GL_UNSIGNED_SHORT, quadIndices);

                    GLES20.glDisableVertexAttribArray(keyframeVertexHandle);
                    GLES20.glDisableVertexAttribArray(keyframeTexCoordHandle);

                    GLES20.glUseProgram(0);

                    // Finally we return the depth func to its original state
                    GLES20.glDepthFunc(GLES20.GL_LESS);
                    GLES20.glDisable(GLES20.GL_BLEND);
                }

                SampleUtils.checkGLError("VideoPlayback renderFrame");

            }else {
                loadVideo(currentTarget);
            }
        }

        GLES20.glDisable(GLES20.GL_DEPTH_TEST);
    }

    public void onDrawFrame(Display display)
    {
        if (!vuforiaRenderer.mIsActive)
            return;

        for (String modelName : modelNames)
        {
            if (mVideoPlayerHelper.get(modelName) != null)
            {
                if (mVideoPlayerHelper.get(modelName).isPlayableOnTexture())
                {
                    // First we need to update the video data. This is a built
                    // in Android call
                    // Here, the decoded data is uploaded to the OES texture
                    // We only need to do this if the movie is playing
                    if (mVideoPlayerHelper.get(modelName).getStatus() == VideoPlayerHelper.MEDIA_STATE.PLAYING)
                    {
                        mVideoPlayerHelper.get(modelName).updateVideoData();
                    }

                    // According to the Android API
                    // (http://developer.android.com/reference/android/graphics/SurfaceTexture.html)
                    // transforming the texture coordinates needs to happen
                    // every frame.
                    mVideoPlayerHelper.get(modelName)
                            .getSurfaceTextureTransformMatrix(mTexCoordTransformationMatrix.get(modelName));
                    setVideoDimensions(modelName,
                            mVideoPlayerHelper.get(modelName).getVideoWidth(),
                            mVideoPlayerHelper.get(modelName).getVideoHeight(),
                            mTexCoordTransformationMatrix.get(modelName));
                }

                setStatus(modelName, mVideoPlayerHelper.get(modelName).getStatus().getNumericType());
            }
        }
        render(display);

        // Call our function to render content from SampleAppRenderer class

        for (String modelName : modelNames)
        {
            // Ask whether the target is currently being tracked and if so react
            // to it
            if (isTracking(modelName))
            {
                // If it is tracking reset the timestamp for lost tracking
                mLostTrackingSince.put(modelName, -1l);
            } else
            {
                // If it isn't tracking
                // check whether it just lost it or if it's been a while
                if (mLostTrackingSince.get(modelName) < 0)
                    mLostTrackingSince.put(modelName,SystemClock.uptimeMillis());
                else
                {
                    // If it's been more than 2 seconds then pause the player
                    if ((SystemClock.uptimeMillis() - mLostTrackingSince.get(modelName)) > 2000)
                    {
                        if (mVideoPlayerHelper.get(modelName) != null)
                            mVideoPlayerHelper.get(modelName).pause();
                    }
                }
            }
        }

        // If you would like the video to start playing as soon as it starts
        // tracking
        // and pause as soon as tracking is lost you can do that here by
        // commenting
        // the for-loop above and instead checking whether the isTracking()
        // value has
        // changed since the last frame. Notice that you need to be careful not
        // to
        // trigger automatic playback for fullscreen since that will be
        // inconvenient
        // for your users.

    }

    boolean isTracking(String target)
    {
        return isTracking.get(target);
    }

    void setStatus(String target, int value)
    {
        // Transform the value passed from java to our own values
        switch (value)
        {
            case 0:
                currentStatus.put(target, VideoPlayerHelper.MEDIA_STATE.REACHED_END);
                break;
            case 1:
                currentStatus.put(target, VideoPlayerHelper.MEDIA_STATE.PAUSED);
                break;
            case 2:
                currentStatus.put(target, VideoPlayerHelper.MEDIA_STATE.STOPPED);
                break;
            case 3:
                currentStatus.put(target, VideoPlayerHelper.MEDIA_STATE.PLAYING);
                break;
            case 4:
                currentStatus.put(target, VideoPlayerHelper.MEDIA_STATE.READY);
                break;
            case 5:
                currentStatus.put(target, VideoPlayerHelper.MEDIA_STATE.NOT_READY);
                break;
            case 6:
                currentStatus.put(target, VideoPlayerHelper.MEDIA_STATE.ERROR);
                break;
            default:
                currentStatus.put(target, VideoPlayerHelper.MEDIA_STATE.NOT_READY);
                break;
        }
    }

    void setVideoDimensions(String target, float videoWidth, float videoHeight,
                            float[] textureCoordMatrix)
    {
        // The quad originaly comes as a perfect square, however, the video
        // often has a different aspect ration such as 4:3 or 16:9,
        // To mitigate this we have two options:
        // 1) We can either scale the width (typically up)
        // 2) We can scale the height (typically down)
        // Which one to use is just a matter of preference. This example scales
        // the height down.
        // (see the render call in renderFrame)
        videoQuadAspectRatio.put(target, videoHeight / videoWidth);

        float mtx[] = textureCoordMatrix;
        float tempUVMultRes[] = new float[2];


            tempUVMultRes = uvMultMat4f(
                    videoQuadTextureCoordsTransformed.get(target)[0],
                    videoQuadTextureCoordsTransformed.get(target)[1],
                    videoQuadTextureCoords[0], videoQuadTextureCoords[1], mtx);
            videoQuadTextureCoordsTransformed.get(target)[0] = tempUVMultRes[0];
            videoQuadTextureCoordsTransformed.get(target)[1] = tempUVMultRes[1];
            tempUVMultRes = uvMultMat4f(
                    videoQuadTextureCoordsTransformed.get(target)[2],
                    videoQuadTextureCoordsTransformed.get(target)[3],
                    videoQuadTextureCoords[2], videoQuadTextureCoords[3], mtx);
            videoQuadTextureCoordsTransformed.get(target)[2] = tempUVMultRes[0];
            videoQuadTextureCoordsTransformed.get(target)[3] = tempUVMultRes[1];
            tempUVMultRes = uvMultMat4f(
                    videoQuadTextureCoordsTransformed.get(target)[4],
                    videoQuadTextureCoordsTransformed.get(target)[5],
                    videoQuadTextureCoords[4], videoQuadTextureCoords[5], mtx);
            videoQuadTextureCoordsTransformed.get(target)[4] = tempUVMultRes[0];
            videoQuadTextureCoordsTransformed.get(target)[5] = tempUVMultRes[1];
            tempUVMultRes = uvMultMat4f(
                    videoQuadTextureCoordsTransformed.get(target)[6],
                    videoQuadTextureCoordsTransformed.get(target)[7],
                    videoQuadTextureCoords[6], videoQuadTextureCoords[7], mtx);
            videoQuadTextureCoordsTransformed.get(target)[6] = tempUVMultRes[0];
            videoQuadTextureCoordsTransformed.get(target)[7] = tempUVMultRes[1];

        // textureCoordMatrix = mtx;
    }

    float[] uvMultMat4f(float transformedU, float transformedV, float u,
                        float v, float[] pMat)
    {
        float x = pMat[0] * u + pMat[4] * v /* + pMat[ 8]*0.f */+ pMat[12]
                * 1.f;
        float y = pMat[1] * u + pMat[5] * v /* + pMat[ 9]*0.f */+ pMat[13]
                * 1.f;
        // float z = pMat[2]*u + pMat[6]*v + pMat[10]*0.f + pMat[14]*1.f; // We
        // dont need z and w so we comment them out
        // float w = pMat[3]*u + pMat[7]*v + pMat[11]*0.f + pMat[15]*1.f;

        float result[] = new float[2];
        // transformedU = x;
        // transformedV = y;
        result[0] = x;
        result[1] = y;
        return result;
    }

    private Buffer fillBuffer(float[] array)
    {
        // Convert to floats because OpenGL doesnt work on doubles, and manually
        // casting each input value would take too much time.
        ByteBuffer bb = ByteBuffer.allocateDirect(4 * array.length); // each float takes 4 bytes
        bb.order(ByteOrder.LITTLE_ENDIAN);
        for (float d : array)
            bb.putFloat(d);
        bb.rewind();

        return bb;

    }

    private Buffer fillBuffer(double[] array)
    {
        // Convert to floats because OpenGL doesnt work on doubles, and manually
        // casting each input value would take too much time.
        ByteBuffer bb = ByteBuffer.allocateDirect(4 * array.length); // each float takes 4 bytes
        bb.order(ByteOrder.LITTLE_ENDIAN);
        for (double d : array)
            bb.putFloat((float)d);
        bb.rewind();

        return bb;

    }

    private Buffer fillBuffer(short[] array)
    {
        // Convert to floats because OpenGL doesnt work on doubles, and manually
        // casting each input value would take too much time.
        ByteBuffer bb = ByteBuffer.allocateDirect(2 * array.length); // each float takes 4 bytes
        bb.order(ByteOrder.LITTLE_ENDIAN);
        for (short d : array)
            bb.putShort(d);
        bb.rewind();

        return bb;

    }

    private void loadVideo(String markerName) {
        if (currentMarkerVideo == null || !markerName.equals(currentMarkerVideo)){
            if(mVideoPlayerHelper.get(markerName).setupSurfaceTexture(videoPlaybackTextureID[videoPlaybackTextureIDMap.get(markerName)])){
                mCanRequestType.put(markerName, VideoPlayerHelper.MEDIA_TYPE.FULLSCREEN);
            } else {
                mCanRequestType.put(markerName, VideoPlayerHelper.MEDIA_TYPE.ON_TEXTURE_FULLSCREEN);
            }
            if (mLoadRequested.get(markerName)){
                mVideoPlayerHelper.get(markerName).load(mMovieName.get(markerName), mCanRequestType.get(markerName),
                        mShouldPlayImmediately.get(markerName), mSeekPosition.get(markerName));
                mLoadRequested.put(markerName, false);
            }
            currentMarkerVideo = markerName;
        }
    }

    private void loadTextures()
    {
        mTextures.put(DEFAULT_TEXTURE,Texture.loadTextureFromApk(
                "VideoPlayback/VuforiaSizzleReel_1.png", mActivity.getAssets()));

        mTextures.put(BUTTON_PLAY, Texture.loadTextureFromApk("VideoPlayback/play.png",
                mActivity.getAssets()));
        mTextures.put(BUTTON_BUSY, Texture.loadTextureFromApk("VideoPlayback/busy.png",
                mActivity.getAssets()));
        mTextures.put(BUTTON_STOP, Texture.loadTextureFromApk("VideoPlayback/error.png",
                mActivity.getAssets()));
    }

}
