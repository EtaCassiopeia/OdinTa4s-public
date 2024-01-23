package org.ta4j.core.indicators;


import org.ta4j.core.Indicator;
import org.ta4j.core.num.Num;

/**
 * Simple boolean transform indicator.
 *
 * Transforms any decimal indicator to a boolean indicator by using common
 * logical operators.
 */
public class OdinBooleanTransformIndicator extends CachedIndicator<Boolean> {

    private static final long serialVersionUID = -6196778606319962794L;

    public enum BooleanTransformType {
        equals,
        isGreaterThan,
        isGreaterThanOrEqual,
        isLessThan,
        isLessThanOrEqual
    }

    private Indicator<Num> firstIndicator;
    private Indicator<Num> secondIndicator;
    private BooleanTransformType type;

    public OdinBooleanTransformIndicator(Indicator<Num> firstIndicator, Indicator<Num> secondIndicator, BooleanTransformType type) {
        super(firstIndicator);
        this.firstIndicator = firstIndicator;
        this.secondIndicator = secondIndicator;
        this.type = type;
    }


    @Override
    protected Boolean calculate(int index) {

        Num firstVal = firstIndicator.getValue(index);
        Num secondVal = secondIndicator.getValue(index);

        if (type != null) {
            switch (type) {
                case equals:
                    return firstVal.equals(secondVal);
                case isGreaterThan:
                    return firstVal.isGreaterThan(secondVal);
                case isGreaterThanOrEqual:
                    return firstVal.isGreaterThanOrEqual(secondVal);
                case isLessThan:
                    return firstVal.isLessThan(secondVal);
                case isLessThanOrEqual:
                    return firstVal.isLessThanOrEqual(secondVal);
                default:
                    break;
            }
        }

        return false;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + " Transform(" + type.name() + ")";
    }
}
