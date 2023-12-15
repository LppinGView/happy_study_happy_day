package org.example.api;

import org.example.callback.Callback;

public interface MScenter {
    void push(String topic, String msg, Integer partition, Callback<String> function);
    void pull(String topic);
}
