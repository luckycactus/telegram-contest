package ru.luckycactus.telegramcontest.chartview.common;

import java.lang.ref.WeakReference;

public abstract class WeakSingletonHolder<T> {

    private WeakReference<T> instance;

    public final synchronized T getInstance() {
        T classInstance;
        if (instance != null) {
            classInstance = instance.get();
            if (classInstance != null) {
                return classInstance;
            }
        }

        classInstance = create();
        instance = new WeakReference<>(classInstance);

        return classInstance;
    }

    public abstract T create();
}
