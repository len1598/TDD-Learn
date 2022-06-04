package pers.lenwind.args;

import pers.lenwind.args.exception.ParseException;

import java.util.List;

non-sealed class ParseIntOption implements ParseOption<Integer> {
    @Override
    public Integer parse(List<String> args, Option option) {
        int index = args.indexOf(option.value());
        int optionLength = getOptionLength(index, args);
        if (optionLength > 2 || optionLength == 2 && !args.get(index + 1).matches("\\d+")) {
            throw new ParseException(option.value(), int.class);
        }
        return optionLength == 1 ? 0 : Integer.parseInt(args.get(index + 1));
    }
}
