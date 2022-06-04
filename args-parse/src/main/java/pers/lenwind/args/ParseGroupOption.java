package pers.lenwind.args;

import pers.lenwind.args.exception.ParseException;

import java.util.List;

public class ParseGroupOption extends ParseOption<String[]> {
    @Override
    public String[] parse(List<String> args, Option option) {
        return values(args, option)
            .map(l -> l.toArray(String[]::new))
            .orElseThrow(() -> new ParseException(option.value(), String[].class));
    }
}
