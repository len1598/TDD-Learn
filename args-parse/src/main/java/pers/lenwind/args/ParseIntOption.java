package pers.lenwind.args;

import pers.lenwind.args.exception.ParseException;

import java.util.List;

public class ParseIntOption extends ParseOption<Integer> {
    @Override
    public Integer parse(List<String> args, Option option) {
        return values(args, option)
            .map(l -> {
                if (l.size() > 1 || l.size() == 1 && !l.get(0).matches("\\d+")) {
                    throw new ParseException(option.value(), int.class);
                }
                return l.size() == 0 ? 0 : Integer.parseInt(l.get(0));
            })
            .orElseThrow(() -> new ParseException(option.value(), int.class));
    }
}
