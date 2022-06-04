package pers.lenwind.args;

import pers.lenwind.args.exception.MultiArgsException;

import java.util.List;

public class ParseStringOption extends ParseOption<String> {
    @Override
    public String parse(List<String> args, Option option) {
        List<String> values = values(args, option);
        if (values.size() > 1) {
            throw new MultiArgsException(option.value(), String.class);
        }
        return values.size() == 0 ? "" : values.get(0);
    }
}
