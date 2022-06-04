package pers.lenwind.args;

import pers.lenwind.args.exception.ParseException;

import java.util.List;

non-sealed class ParseBooleanOption implements ParseOption<Boolean> {
    @Override
    public Boolean parse(List<String> args, Option option) {
        int index = args.indexOf(option.value());
        int optionLength = getOptionLength(index, args);
        if (optionLength > 1) {
            throw new ParseException(option.value(), boolean.class);
        }
        return optionLength == 1;
    }
}
