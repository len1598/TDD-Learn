package pers.lenwind.args;

import java.util.List;

sealed interface ParseOption<T> permits ParseBooleanOption, ParseIntOption, ParseStringOption {
    default int getOptionLength(int index, List<String> args) {
        if (index == -1) {
            return 0;
        }
        for (int i = index + 1; i < args.size(); i++) {
            if (args.get(i).startsWith("-")) {
                return i - index;
            }
        }
        return args.size() - index;
    }

    T parse(List<String> args, Option option);
}
