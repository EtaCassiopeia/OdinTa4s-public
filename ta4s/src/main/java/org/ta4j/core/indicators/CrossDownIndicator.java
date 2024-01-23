package org.ta4j.core.indicators;

import org.ta4j.core.Indicator;
import org.ta4j.core.num.Num;

public class CrossDownIndicator extends CachedIndicator<Boolean> {

    /** Upper indicator */
    private final Indicator<Num> up;
    /** Lower indicator */
    private final Indicator<Num> low;

    /**
     * Constructor.
     *
     * @param up  the upper indicator
     * @param low the lower indicator
     */
    public CrossDownIndicator(Indicator<Num> up, Indicator<Num> low) {
        // TODO: check if up series is equal to low series
        super(up);
        this.up = up;
        this.low = low;
    }

    @Override
    protected Boolean calculate(int index) {

        if (index == 0 || up.getValue(index).isGreaterThan(low.getValue(index))) {
            return false;
        }

        return up.getValue(index).isLessThanOrEqual(low.getValue(index)) && up.getValue(index-1).isGreaterThan(low.getValue(index-1));

    }

    /**
     * @return the initial lower indicator
     */
    public Indicator<Num> getLow() {
        return low;
    }

    /**
     * @return the initial upper indicator
     */
    public Indicator<Num> getUp() {
        return up;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + " " + low + " " + up;
    }

}
