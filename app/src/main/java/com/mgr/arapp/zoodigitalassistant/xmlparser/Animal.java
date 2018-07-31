package com.mgr.arapp.zoodigitalassistant.xmlparser;

/**
 * Created by Marcin on 31.07.2018.
 */

public class Animal {

    private String model;
    private String marker;

    public Animal(String model, String marker) {
        this.model = model;
        this.marker = marker;
    }

    @Override
    public String toString() {
        return "Animal{" +
                "model='" + model + '\'' +
                ", marker='" + marker + '\'' +
                '}';
    }
}
