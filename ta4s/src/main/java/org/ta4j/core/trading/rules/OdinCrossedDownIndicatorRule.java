package org.ta4j.core.trading.rules;


import org.ta4j.core.Indicator;
import org.ta4j.core.TradingRecord;
import org.ta4j.core.indicators.CrossDownIndicator;
import org.ta4j.core.indicators.CrossUpIndicator;
import org.ta4j.core.indicators.helpers.ConstantIndicator;
import org.ta4j.core.num.Num;

/**
 * Crossed-up indicator rule.
 *
 * Satisfied when the value of the first indicator crosses-down
 * the value of the second one.
 */
public class OdinCrossedDownIndicatorRule extends AbstractRule {

    /** The cross indicator */
    private CrossDownIndicator cross;

    /**
     * Constructor.
     *
     * @param indicator the indicator
     * @param threshold a threshold
     */
    public OdinCrossedDownIndicatorRule(Indicator<Num> indicator, Number threshold) {
        this(indicator, indicator.numOf(threshold));
    }

    /**
     * Constructor.
     *
     * @param indicator the indicator
     * @param threshold a threshold
     */
    public OdinCrossedDownIndicatorRule(Indicator<Num> indicator, Num threshold) {
        this(indicator, new ConstantIndicator<Num>(indicator.getBarSeries(), threshold));
    }

    /**
     * Constructor.
     *
     * @param first  the first indicator
     * @param second the second indicator
     */
    public OdinCrossedDownIndicatorRule(Indicator<Num> first, Indicator<Num> second) {
        this.cross = new CrossDownIndicator(first, second);
    }

    @Override
    public boolean isSatisfied(int index, TradingRecord tradingRecord) {
        final boolean satisfied = cross.getValue(index);
        traceIsSatisfied(index, satisfied);
        return satisfied;
    }

    /**
     * @return the initial lower indicator
     */
    public Indicator<Num> getLow() {
        return cross.getLow();
    }

    /**
     * @return the initial upper indicator
     */
    public Indicator<Num> getUp() {
        return cross.getUp();
    }
}

