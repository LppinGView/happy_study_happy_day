package org.example.callback;

@FunctionalInterface
public interface Callback<T> {
    void doAction(T t);
}
