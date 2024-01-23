package org.ta4j.core.trading.rules;

import org.ta4j.core.Indicator;
import org.ta4j.core.TradingRecord;
import org.ta4j.core.indicators.helpers.BooleanTransformIndicator;
import org.ta4j.core.num.Num;

public class OdinBooleanTransformRule extends AbstractRule {

    final private Indicator<Num> first;
    final private Indicator<Num> second;
    final private BooleanTransformIndicator.BooleanTransformType type;
    final private Num coefficient;

    public OdinBooleanTransformRule(Indicator<Num> first, Indicator<Num> second, BooleanTransformIndicator.BooleanTransformType type) {
        this.first = first;
        this.second = second;
        this.type = type;
        coefficient = null;
    }

    public OdinBooleanTransformRule(Indicator<Num> first, Num coefficient, BooleanTransformIndicator.BooleanTransformType type) {
        this.first = first;
        this.type = type;
        this.coefficient = coefficient;
        this.second = null;
    }

    @Override
    public boolean isSatisfied(int index, TradingRecord tradingRecord) {

        if (second != null) {
            Num firstVal = first.getValue(index);
            Num secondVal = second.getValue(index);

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
                        return false;
                }
            }
        } else {
            Num firstVal = first.getValue(index);

            if (type != null) {
                switch (type) {
                    case equals:
                        return firstVal.equals(coefficient);
                    case isGreaterThan:
                        return firstVal.isGreaterThan(coefficient);
                    case isGreaterThanOrEqual:
                        return firstVal.isGreaterThanOrEqual(coefficient);
                    case isLessThan:
                        return firstVal.isLessThan(coefficient);
                    case isLessThanOrEqual:
                        return firstVal.isLessThanOrEqual(coefficient);
                    default:
                        return false;
                }
            }
        }

        return false;
    }
}
