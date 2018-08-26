package com.mgr.arapp.zoodigitalassistant.ar.libgdx;

import android.app.Activity;
import android.util.Log;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.graphics.FPSLogger;
import com.mgr.arapp.zoodigitalassistant.ar.vuforia.VuforiaRenderer;
import com.mgr.arapp.zoodigitalassistant.ar.vuforia.videoPlayback.VideoPlaybackRenderer;
import com.mgr.arapp.zoodigitalassistant.xmlparser.Animal;
import com.mgr.arapp.zoodigitalassistant.xmlparser.AnimalXmlParser;

import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Instance of libgdx Game class responsible for rendering 3D content over augmented reality.
 */
public class Engine extends Game {

    private FPSLogger fps;
    private VuforiaRenderer vuforiaRenderer;
    public VideoPlaybackRenderer videoPlaybackRenderer;
    private Activity mActivity;
    public List<Animal> animalModels;
    private Display mDisplay;

    public Engine(VuforiaRenderer vuforiaRenderer, Activity activity) {
        this.vuforiaRenderer = vuforiaRenderer;
        this.mActivity = activity;
    }

    private void loadAnimals(Activity activity){
        try {
            InputStream in = activity.getAssets().open("animals.xml");
            AnimalXmlParser parser = new AnimalXmlParser();
            animalModels = parser.parse(in);
        } catch (IOException e){
            animalModels = new ArrayList<>();
        } catch (XmlPullParserException e) {
            e.printStackTrace();
        }
    }

    public void setRenderMode(boolean renderVideo){
        mDisplay.setRenderVideo(renderVideo);
    }

    @Override
    public void create () {
        this.loadAnimals(mActivity);

        videoPlaybackRenderer = new VideoPlaybackRenderer(animalModels,  vuforiaRenderer, mActivity);
        mDisplay = new Display(vuforiaRenderer, mActivity, animalModels, videoPlaybackRenderer);
        setScreen(mDisplay);
        vuforiaRenderer.initRendering(mActivity);
        vuforiaRenderer.updateRenderingPrimitives();
        videoPlaybackRenderer.initRendering();

        fps = new FPSLogger();
    }

    @Override
    public void resize(int width, int height) {
        super.resize(width, height);
        Log.d("ENGINE", "Resize: "+width+"x"+height);
        vuforiaRenderer.onSurfaceChanged(width, height);
    }

    @Override
    public void render () {
        super.render();
        fps.log();
    }

}
