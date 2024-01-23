package org.ta4j.core.trading.rules;


import org.ta4j.core.Indicator;
import org.ta4j.core.TradingRecord;
import org.ta4j.core.indicators.helpers.ConstantIndicator;
import org.ta4j.core.num.Num;
import org.ta4j.core.trading.rules.AbstractRule;

/**
 * Indicator-under-indicator rule.
 *
 * Satisfied when the value of the first indicator is strictly
 * lesser than the value of the second one.
 */
public class UnderOrEqualIndicatorRule extends AbstractRule {

    /**
     * The first indicator
     */
    private final Indicator<Num> first;
    /**
     * The second indicator
     */
    private final Indicator<Num> second;

    /**
     * Constructor.
     *
     * @param indicator the indicator
     * @param threshold a threshold
     */
    public UnderOrEqualIndicatorRule(Indicator<Num> indicator, Number threshold) {
        this(indicator, new ConstantIndicator<>(indicator.getBarSeries(), indicator.numOf(threshold)));
    }

    /**
     * Constructor.
     *
     * @param indicator the indicator
     * @param threshold a threshold
     */
    public UnderOrEqualIndicatorRule(Indicator<Num> indicator, Num threshold) {
        this(indicator, new ConstantIndicator<>(indicator.getBarSeries(), threshold));
    }

    /**
     * Constructor.
     *
     * @param first  the first indicator
     * @param second the second indicator
     */
    public UnderOrEqualIndicatorRule(Indicator<Num> first, Indicator<Num> second) {
        this.first = first;
        this.second = second;
    }

    @Override
    public boolean isSatisfied(int index, TradingRecord tradingRecord) {
        final boolean satisfied = first.getValue(index).isLessThanOrEqual(second.getValue(index));
        traceIsSatisfied(index, satisfied);
        return satisfied;
    }
}

