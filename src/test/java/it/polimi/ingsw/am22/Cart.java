package it.polimi.ingsw.am22;

import java.util.ArrayList;
import java.util.List;

public class Cart {
    private List<Double> storage;
    public cart(){
        storage = new ArrayList<>();
    }

    public void addElement(String el, double price){
        if
        storage.add(price);
    }

    public double getTotal(){
        var total = storage
                .stream()
                .reduce(0.0, Double::sum);

    }
}
