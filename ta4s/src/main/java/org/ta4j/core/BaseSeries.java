package org.ta4j.core;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.ta4j.core.num.Num;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class BaseSeries<T> implements Series<T> {

    private static final long serialVersionUID = -1878027009695790126L;
    /**
     * The logger
     */
    private static final Logger log = LoggerFactory.getLogger(BaseSeries.class);
    /**
     * Name for unnamed series
     */
    private static final String UNNAMED_SERIES_NAME = "unnamed_series";
    /**
     * Name of the series
     */
    private final String name;
    /**
     * List of bars
     */
    private final List<T> bars;
    /**
     * Begin index of the bar series
     */
    private int seriesBeginIndex;
    /**
     * End index of the bar series
     */
    private int seriesEndIndex;
    /**
     * Maximum number of bars for the bar series
     */
    private int maximumBarCount = Integer.MAX_VALUE;
    /**
     * Number of removed bars
     */
    private int removedBarsCount = 0;
    /**
     * True if the current series is constrained (i.e. its indexes cannot change),
     * false otherwise
     */
    private boolean constrained;

    /**
     * Constructor of an unnamed series.
     */
    public BaseSeries() {
        this(UNNAMED_SERIES_NAME);
    }

    /**
     * Constructor.
     *
     * @param name the name of the series
     */
    public BaseSeries(String name) {
        this(name, new ArrayList<>());
    }

    /**
     * Constructor of an unnamed series.
     *
     * @param bars the list of bars of the series
     */
    public BaseSeries(List<T> bars) {
        this(UNNAMED_SERIES_NAME, bars);
    }

    /**
     * Constructor.
     *
     * @param name the name of the series
     * @param bars the list of bars of the series
     */
    public BaseSeries(String name, List<T> bars) {
        this(name, bars, 0, bars.size() - 1, false);
    }


    /**
     * Constructor.
     *
     * @param name             the name of the series
     * @param bars             the list of bars of the series
     * @param seriesBeginIndex the begin index (inclusive) of the bar series
     * @param seriesEndIndex   the end index (inclusive) of the bar series
     * @param constrained      true to constrain the bar series (i.e. indexes cannot
     *                         change), false otherwise
     *                         {@link Num Num implementation}
     */
    BaseSeries(String name, List<T> bars, int seriesBeginIndex, int seriesEndIndex, boolean constrained) {
        this.name = name;

        this.bars = bars;
        if (bars.isEmpty()) {
            // Bar list empty
            this.seriesBeginIndex = -1;
            this.seriesEndIndex = -1;
            this.constrained = false;
            return;
        }
        // Bar list not empty: checking indexes
        if (seriesEndIndex < seriesBeginIndex - 1) {
            throw new IllegalArgumentException("End index must be >= to begin index - 1");
        }
        if (seriesEndIndex >= bars.size()) {
            throw new IllegalArgumentException("End index must be < to the bar list size");
        }
        this.seriesBeginIndex = seriesBeginIndex;
        this.seriesEndIndex = seriesEndIndex;
        this.constrained = constrained;
    }


    /**
     * @param series a bar series
     * @param index  an out of bounds bar index
     * @return a message for an OutOfBoundsException
     */
    private static String buildOutOfBoundsMessage(BaseSeries series, int index) {
        return String.format("Size of series: %s bars, %s bars removed, index = %s", series.bars.size(),
                series.removedBarsCount, index);
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public T getBar(int i) {
        int innerIndex = i - removedBarsCount;
        if (innerIndex < 0) {
            if (i < 0) {
                // Cannot return the i-th bar if i < 0
                throw new IndexOutOfBoundsException(buildOutOfBoundsMessage(this, i));
            }
            log.trace("Bar series `{}` ({} bars): bar {} already removed, use {}-th instead", name, bars.size(), i,
                    removedBarsCount);
            if (bars.isEmpty()) {
                throw new IndexOutOfBoundsException(buildOutOfBoundsMessage(this, removedBarsCount));
            }
            innerIndex = 0;
        } else if (innerIndex >= bars.size()) {
            // Cannot return the n-th bar if n >= bars.size()
            throw new IndexOutOfBoundsException(buildOutOfBoundsMessage(this, i));
        }
        return bars.get(innerIndex);
    }

    @Override
    public int getBarCount() {
        if (seriesEndIndex < 0) {
            return 0;
        }
        final int startIndex = Math.max(removedBarsCount, seriesBeginIndex);
        return seriesEndIndex - startIndex + 1;
    }

    @Override
    public int getBeginIndex() {
        return seriesBeginIndex;
    }

    @Override
    public int getEndIndex() {
        return seriesEndIndex;
    }

    @Override
    public int getMaximumBarCount() {
        return maximumBarCount;
    }

    @Override
    public void setMaximumBarCount(int maximumBarCount) {
        if (constrained) {
            throw new IllegalStateException("Cannot set a maximum bar count on a constrained bar series");
        }
        if (maximumBarCount <= 0) {
            throw new IllegalArgumentException("Maximum bar count must be strictly positive");
        }
        this.maximumBarCount = maximumBarCount;
        removeExceedingBars();
    }

    @Override
    public int getRemovedBarsCount() {
        return removedBarsCount;
    }

    /**
     * @param bar the <code>Bar</code> to be added
     */
    @Override
    public void addBar(T bar, boolean replace) {
        Objects.requireNonNull(bar);

        if (!bars.isEmpty()) {
            if (replace) {
                bars.set(bars.size() - 1, bar);
                return;
            }
        }

        bars.add(bar);
        if (seriesBeginIndex == -1) {
            // Begin index set to 0 only if it wasn't initialized
            seriesBeginIndex = 0;
        }
        seriesEndIndex++;
        removeExceedingBars();
    }

    /**
     * Removes the N first bars which exceed the maximum bar count.
     */
    private void removeExceedingBars() {
        int barCount = bars.size();
        if (barCount > maximumBarCount) {
            // Removing old bars
            int nbBarsToRemove = barCount - maximumBarCount;
            for (int i = 0; i < nbBarsToRemove; i++) {
                bars.remove(0);
            }
            // Updating removed bars count
            removedBarsCount += nbBarsToRemove;
        }
    }

}

