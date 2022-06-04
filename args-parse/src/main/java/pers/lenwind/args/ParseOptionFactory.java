package pers.lenwind.args;

import java.util.Map;

class ParseOptionFactory {
    static private final Map<Class<?>, ParseOption<?>> PARSE_OPTIONS = Map.of(boolean.class, new ParseBooleanOption(),
        int.class, new ParseIntOption(), String.class, new ParseStringOption());

    public static ParseOption<?> createOption(Class<?> type) {
        return PARSE_OPTIONS.get(type);
    }
}
