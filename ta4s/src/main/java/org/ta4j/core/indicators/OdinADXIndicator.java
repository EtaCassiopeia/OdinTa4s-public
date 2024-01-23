package org.ta4j.core.indicators;


import org.ta4j.core.BarSeries;
import org.ta4j.core.indicators.CachedIndicator;
import org.ta4j.core.indicators.MMAIndicator;
import org.ta4j.core.indicators.helpers.DXIndicator;
import org.ta4j.core.num.Num;

/**
 * ADX indicator. Part of the Directional Movement System.
 *
 * @see <a
 *      href="https://www.investopedia.com/terms/a/adx.asp>https://www.investopedia.com/terms/a/adx.asp</a>
 */
public class OdinADXIndicator extends CachedIndicator<Num> {

    private final SMAIndicator averageDXIndicator;
    private final int diBarCount;
    private final int adxBarCount;

    public OdinADXIndicator(BarSeries series, int diBarCount, int adxBarCount) {
        super(series);
        this.diBarCount = diBarCount;
        this.adxBarCount = adxBarCount;
        this.averageDXIndicator = new SMAIndicator(new DXIndicator(series, diBarCount), adxBarCount);
    }

    public OdinADXIndicator(BarSeries series, int barCount) {
        this(series, barCount, barCount);
    }

    @Override
    protected Num calculate(int index) {
        return averageDXIndicator.getValue(index);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + " diBarCount: " + diBarCount + " adxBarCount: " + adxBarCount;
    }
}

