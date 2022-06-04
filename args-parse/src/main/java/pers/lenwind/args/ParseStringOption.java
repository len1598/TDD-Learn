package pers.lenwind.args;

import pers.lenwind.args.exception.ParseException;

import java.util.List;

public class ParseStringOption extends ParseOption<String> {
    @Override
    public String parse(List<String> args, Option option) {
        return values(args, option)
            .map(l -> {
                if (l.size() > 1) {
                    throw new ParseException(option.value(), String.class);
                }
                return l.size() == 0 ? "" : l.get(0);
            })
            .orElseThrow(() -> new ParseException(option.value(), String.class));
    }
}
