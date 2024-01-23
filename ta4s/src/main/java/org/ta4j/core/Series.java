package org.ta4j.core;

import java.io.Serializable;

public interface Series<T> extends Serializable {

    String getName();

    T getBar(int i);

    default T getFirstBar() {
        return getBar(getBeginIndex());
    }

    default T getLastBar() {
        return getBar(getEndIndex());
    }

    int getBarCount();

    default boolean isEmpty() {
        return getBarCount() == 0;
    }

    int getBeginIndex();

    int getEndIndex();

    int getMaximumBarCount();

    void setMaximumBarCount(int maximumBarCount);

    int getRemovedBarsCount();

    default void addBar(T bar) {
        addBar(bar, false);
    }

    void addBar(T bar, boolean replace);

}

