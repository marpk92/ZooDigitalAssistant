package com.mgr.arapp.zoodigitalassistant.ar.libgdx;

import android.os.AsyncTask;
import android.util.Log;

import com.badlogic.gdx.Screen;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.mgr.arapp.zoodigitalassistant.ar.vuforia.VuforiaRenderer;

/**
 * Screen implementation responsible for model loading and calling renderer properly.
 */
public class Display implements Screen {

    public ModelInstance modelInstance;
    public Model model;

    private Renderer mRenderer;
    private boolean loading = false;
    public static AssetManager assets = new AssetManager();
    private String name;

    public Display(VuforiaRenderer vuforiaRenderer) {

        mRenderer = new Renderer(vuforiaRenderer);

//        AssetManager assets = new AssetManager();
//        assets.load("jet.g3db", Model.class);
//        assets.finishLoading();
//
//        model = assets.get("jet.g3db", Model.class);
//        modelInstance = new ModelInstance(model);

    }

    public void loadModel(String name){
        Log.d("Diplay", "*************LOAD MODEL: " + name + " *****************");
        loading = true;
        this.name = name;
        assets.clear();
        assets.load(name, Model.class);
    }

    @Override
    public void render(float delta) {


        if (loading && name != null && assets.update()) {
            Log.d("Diplay", "*************NAME: " + name + " *****************");
            model = assets.get(name, Model.class);
            modelInstance = new ModelInstance(model);
            loading = false;
        }
        mRenderer.render(this, delta);
    }

    @Override
    public void dispose() {
        mRenderer.dispose();
    }


    @Override
    public void resize(int i, int i2) {

    }

    @Override
    public void show() {

    }

    @Override
    public void hide() {

    }

    @Override
    public void pause() {

    }

    @Override
    public void resume() {

    }

    public boolean isLoading() {
        return loading;
    }
}
