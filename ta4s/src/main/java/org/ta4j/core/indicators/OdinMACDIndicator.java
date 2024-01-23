package org.ta4j.core.indicators;

import org.ta4j.core.Indicator;
import org.ta4j.core.indicators.helpers.DifferenceIndicator;
import org.ta4j.core.num.Num;

import java.time.LocalDateTime;

public class OdinMACDIndicator extends CachedIndicator<Num> {

    private static final long serialVersionUID = -6899062131435971404L;

    private final EMAIndicator shortTermEma;
    private final EMAIndicator longTermEma;

    private final DifferenceIndicator macdLineIndicator;
    private final EMAIndicator signalLineIndicator;

    private final CrossDownIndicator crossDownIndicator;
    private final CrossUpIndicator crossUpIndicator;

    /**
     * Constructor with shortBarCount "12" and longBarCount "26".
     *
     * @param indicator the indicator
     */
    public OdinMACDIndicator(Indicator<Num> indicator) {
        this(indicator, 12, 26, 9);
    }

    /**
     * Constructor.
     *
     * @param indicator     the indicator
     * @param shortBarCount the short time frame (normally 12)
     * @param longBarCount  the long time frame (normally 26)
     */
    public OdinMACDIndicator(Indicator<Num> indicator, int shortBarCount, int longBarCount, int signalBarCount) {
        super(indicator);
        if (shortBarCount > longBarCount) {
            throw new IllegalArgumentException("Long term period count must be greater than short term period count");
        }
        shortTermEma = new EMAIndicator(indicator, shortBarCount);
        longTermEma = new EMAIndicator(indicator, longBarCount);

        macdLineIndicator = new DifferenceIndicator(shortTermEma, longTermEma);
        signalLineIndicator = new EMAIndicator(macdLineIndicator, signalBarCount);

        crossDownIndicator = new CrossDownIndicator(macdLineIndicator, signalLineIndicator);
        crossUpIndicator = new CrossUpIndicator(macdLineIndicator, signalLineIndicator);

    }


    /**
     * MACD Line: (12-day EMA - 26-day EMA)
     * <p>
     * Signal Line: 9-day EMA of MACD Line
     * <p>
     * MACD Histogram: MACD Line - Signal Line
     */
    @Override
    protected Num calculate(int index) {
        Num macdLineValue = getMACDLine(index);
        Num signalValue = getSignal(index);

        //MACD Histogram
        return macdLineValue.minus(signalValue);
    }

    public EMAIndicator getSignalIndicator() {
        return signalLineIndicator;
    }

    public DifferenceIndicator getMACDLineIndicator() {
        return macdLineIndicator;
    }

    public Num getSignal(int index) {
        return signalLineIndicator.getValue(index);
    }

    public Num getMACDLine(int index) {
        return macdLineIndicator.getValue(index);
    }

    public boolean isMACDLineCrossDownSignal() {
        return crossDownIndicator.getValue(getBarSeries().getEndIndex());
    }

    public boolean isMACDLineCrossUpSignal() {
        return crossUpIndicator.getValue(getBarSeries().getEndIndex());
    }

    public LocalDateTime getLastBarDateTime() {
        if (!getBarSeries().isEmpty())
            return getBarSeries().getLastBar().getBeginTime().toLocalDateTime();
        else
            return null;
    }

    public void printDetails() {
        System.out.println("MACDLine : " + getMACDLine(macdLineIndicator.getBarSeries().getEndIndex()));
        System.out.println("Signal : " + getSignal(signalLineIndicator.getBarSeries().getEndIndex()));
    }
}
