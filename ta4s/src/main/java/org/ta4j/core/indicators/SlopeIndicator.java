package org.ta4j.core.indicators;

import org.ta4j.core.Indicator;
import org.ta4j.core.indicators.helpers.DifferenceIndicator;
import org.ta4j.core.indicators.helpers.PreviousValueIndicator;
import org.ta4j.core.num.Num;

public class SlopeIndicator  extends CachedIndicator<Num> {

    private Indicator<Num> ref;
    /** The previous n-th value of ref */
    private PreviousValueIndicator prev;

    private DifferenceIndicator diff;

    private Integer nthPrevious = 1;

    public SlopeIndicator(Indicator<Num> indicator) {
        super(indicator);
        ref = indicator;
        prev =  new PreviousValueIndicator(ref, nthPrevious);
        diff = new DifferenceIndicator(ref, prev);
    }

    @Override
    protected Num calculate(int index) {
        return diff.getValue(index);
    }
}