package com.mgr.arapp.zoodigitalassistant.xmlparser;

/**
 * Created by Marcin on 31.07.2018.
 */

public class Animal {

    public String model;
    public String marker;
    public String videoUrl;

    public Animal(String model, String marker, String videoUrl) {
        this.model = model;
        this.marker = marker;
        this.videoUrl = videoUrl;
    }

    @Override
    public String toString() {
        return "Animal{" +
                "model='" + model + '\'' +
                ", marker='" + marker + '\'' +
                '}';
    }
}
