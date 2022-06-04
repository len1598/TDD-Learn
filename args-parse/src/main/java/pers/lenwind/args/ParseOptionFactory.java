package pers.lenwind.args;

import pers.lenwind.args.exception.MultiArgsException;
import pers.lenwind.args.exception.ParseException;

import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

class ParseOptionFactory {
    private static final Map<Class<?>, ParseOption<?>> PARSE_OPTIONS = Map.of(
        boolean.class, bool(),
        int.class, integer(),
        String.class, string(),
        String[].class, group(),
        int[].class, decimals());

    public static ParseOption<?> createOption(Class<?> type) {
        return PARSE_OPTIONS.get(type);
    }

    private static ParseOption<String> string() {
        return (args, option) -> {
            List<String> values = values(args, option);
            if (values.size() > 1) {
                throw new MultiArgsException(option.value(), String.class);
            }
            return values.size() == 0 ? "" : values.get(0);
        };
    }

    private static ParseOption<Boolean> bool() {
        return (args, option) -> {
            try {
                if (values(args, option).size() > 0) {
                    throw new MultiArgsException(option.value(), boolean.class);
                }
                return true;
            } catch (ParseException e) {
                return false;
            }
        };
    }

    private static ParseOption<Integer> integer() {
        return (args, option) -> {
            List<String> values = values(args, option);
            if (values.size() > 1) {
                throw new MultiArgsException(option.value(), int.class);
            }
            if (values.size() == 0) {
                return 0;
            }
            if (!values.get(0).matches("\\d+")) {
                throw new ParseException(option.value());
            }
            return Integer.parseInt(values.get(0));
        };
    }

    private static ParseOption<String[]> group() {
        return (args, option) -> values(args, option).toArray(String[]::new);
    }

    private static ParseOption<int[]> decimals() {
        return (args, option) -> values(args, option).stream().mapToInt(Integer::parseInt).toArray();
    }

    protected static List<String> values(List<String> args, Option option) {
        int index = args.indexOf(option.value());
        if (index == -1) {
            throw new ParseException(option.value());
        }
        return args.subList(index + 1, getFlowingFlagIndex(index, args));
    }

    private static int getFlowingFlagIndex(int index, List<String> args) {
        return IntStream.range(index + 1, args.size())
            .filter(i -> args.get(i).matches("-[dglp]"))
            .findFirst().orElse(args.size());
    }
}
