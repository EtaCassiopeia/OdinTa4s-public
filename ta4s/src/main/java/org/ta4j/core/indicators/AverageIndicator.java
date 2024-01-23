package org.ta4j.core.indicators;

import org.ta4j.core.Indicator;
import org.ta4j.core.num.Num;

/**
 * Simple average indicator.
 */
public class AverageIndicator extends CachedIndicator<Num> {

    private final Indicator<Num> indicator;
    private final Indicator<Num> otherIndicator;

    public AverageIndicator(Indicator<Num> indicator, Indicator<Num> otherIndicator) {
        super(indicator);
        this.indicator = indicator;
        this.otherIndicator = otherIndicator;
    }

    @Override
    protected Num calculate(int index) {
        return indicator.getValue(index).plus(otherIndicator.getValue(index)).dividedBy(numOf(2));
    }

    @Override
    public String toString() {
        return getClass().getSimpleName();
    }
}