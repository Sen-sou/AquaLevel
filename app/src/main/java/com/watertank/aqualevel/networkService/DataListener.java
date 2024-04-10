package com.watertank.aqualevel.networkService;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Objects;

public class DataListener {

    public interface Listener {
        void listen(String received);
    }

    private HashMap<String, ArrayList<Listener>> dataListener;

    public DataListener() {
        this.dataListener = new HashMap<>();
    }

    public DataListener(DataListener dataListener) {
        this.dataListener = dataListener.getHashMap();
    }

    public void add(String key, Listener listener) {
        if (!dataListener.containsKey(key)) {
            ArrayList<Listener> list = new ArrayList<>();
            list.add(listener);
            dataListener.put(key, list);
        } else {
            Objects.requireNonNull(dataListener.get(key)).add(listener);
        }
    }

    public void addAll(DataListener dataListener) {
        for (String key: dataListener.getHashMap().keySet()) {
            if (!dataListener.containsKey(key)) {
                this.dataListener.put(key, dataListener.getListeners(key));
            } else {
                this.getListeners(key).addAll(dataListener.getListeners(key));
            }
        }
    }

    public HashMap<String, ArrayList<Listener>> getHashMap() {
        return this.dataListener;
    }

    public boolean containsKey(String key) {
        return dataListener.containsKey(key);
    }

    public ArrayList<Listener> getListeners(String key) {
        return dataListener.getOrDefault(key, null);
    }

    public int size() {
        return dataListener.size();
    }

    public void clear() {
        dataListener.clear();
    }

}