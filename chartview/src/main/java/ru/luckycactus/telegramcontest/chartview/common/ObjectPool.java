package ru.luckycactus.telegramcontest.chartview.common;

import java.util.LinkedList;

public abstract class ObjectPool<T> {
    private LinkedList<T> free = new LinkedList<>();

    public T acquire() {
        if (free.isEmpty()) {
            return create();
        } else {
            return free.pop();
        }
    }

    public void release(T object) {
        free.push(object);
    }

    protected abstract T create();
}