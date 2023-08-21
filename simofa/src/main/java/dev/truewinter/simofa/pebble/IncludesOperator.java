package dev.truewinter.simofa.pebble;

import com.mitchellbosecke.pebble.node.expression.BinaryExpression;
import com.mitchellbosecke.pebble.operator.Associativity;
import com.mitchellbosecke.pebble.operator.BinaryOperator;
import com.mitchellbosecke.pebble.operator.BinaryOperatorType;

public class IncludesOperator implements BinaryOperator {
    @Override
    public int getPrecedence() {
        return 1000;
    }

    @Override
    public String getSymbol() {
        return "includes";
    }

    @Override
    public BinaryExpression<?> getInstance() {
        return new IncludesExpression();
    }

    @Override
    public BinaryOperatorType getType() {
        return BinaryOperatorType.NORMAL;
    }

    @Override
    public Associativity getAssociativity() {
        return Associativity.LEFT;
    }
}
