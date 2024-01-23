package org.ta4j.core.indicators;

import org.ta4j.core.Indicator;
import org.ta4j.core.num.Num;

public class ValueDifferenceIndicator extends CachedIndicator<Num> {

    private final int n;
    private Indicator<Num> indicator;

    public ValueDifferenceIndicator(Indicator<Num> indicator) {
        this(indicator, 1);
    }


    public ValueDifferenceIndicator(Indicator<Num> indicator, int n) {
        super(indicator);
        this.n = n;
        this.indicator = indicator;
    }

    protected Num calculate(int index) {
        int previousValueIndex = Math.max(0, (index - n));
        return this.indicator.getValue(index).minus(this.indicator.getValue(previousValueIndex));
    }
}
