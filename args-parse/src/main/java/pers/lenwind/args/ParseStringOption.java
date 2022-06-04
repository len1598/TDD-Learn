package pers.lenwind.args;

import pers.lenwind.args.exception.ParseException;

import java.util.List;

non-sealed class ParseStringOption implements ParseOption<String> {
    @Override
    public String parse(List<String> args, Option option) {
        int index = args.indexOf(option.value());
        int optionLength = getOptionLength(index, args);
        if (optionLength > 2) {
            throw new ParseException(option.value(), String.class);
        }
        return optionLength == 1 ? "" : args.get(index + 1);
    }
}
