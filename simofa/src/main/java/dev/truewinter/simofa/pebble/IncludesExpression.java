package dev.truewinter.simofa.pebble;

import com.mitchellbosecke.pebble.node.expression.BinaryExpression;
import com.mitchellbosecke.pebble.template.EvaluationContextImpl;
import com.mitchellbosecke.pebble.template.PebbleTemplateImpl;

public class IncludesExpression extends BinaryExpression<Boolean> {
    @Override
    public Boolean evaluate(PebbleTemplateImpl self, EvaluationContextImpl context) {
        String left = (String) getLeftExpression().evaluate(self, context);
        String right = (String) getRightExpression().evaluate(self, context);
        return left.contains(right);
    }
}
