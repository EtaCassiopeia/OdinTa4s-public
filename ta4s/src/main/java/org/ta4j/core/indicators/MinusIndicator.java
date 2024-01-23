package org.ta4j.core.indicators;

import org.ta4j.core.Indicator;
import org.ta4j.core.num.Num;

/**
 * Simple plus indicator.
 */
public class MinusIndicator extends CachedIndicator<Num> {

    private final Indicator<Num> indicator;
    private final Indicator<Num> otherIndicator;
    private final Num coefficient;

    public MinusIndicator(Indicator<Num> indicator, double coefficient) {
        super(indicator);
        this.indicator = indicator;
        this.coefficient = numOf(coefficient);
        this.otherIndicator = null;
    }

    public MinusIndicator(Indicator<Num> indicator, Indicator<Num> otherIndicator) {
        super(indicator);
        this.indicator = indicator;
        this.otherIndicator = otherIndicator;
        this.coefficient = numOf(0);
    }

    @Override
    protected Num calculate(int index) {
        if (otherIndicator != null)
            return indicator.getValue(index).minus(otherIndicator.getValue(index));
        else
            return indicator.getValue(index).minus(coefficient);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + " Coefficient: " + coefficient;
    }
}