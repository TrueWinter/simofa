package dev.truewinter.simofa.pebble;

import com.mitchellbosecke.pebble.extension.AbstractExtension;
import com.mitchellbosecke.pebble.extension.Function;
import com.mitchellbosecke.pebble.operator.BinaryOperator;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SimofaPebbleExtension extends AbstractExtension {
    @Override
    public Map<String, Function> getFunctions() {
        HashMap<String, Function> functions = new HashMap<>();
        functions.put(JsonStringifyFunction.FUNCTION_NAME, new JsonStringifyFunction());
        functions.put(JsonParseFunction.FUNCTION_NAME, new JsonParseFunction());
        return functions;
    }

    @Override
    public List<BinaryOperator> getBinaryOperators() {
        List<BinaryOperator> operators = new ArrayList<>();
        operators.add(new IncludesOperator());
        return operators;
    }
}
