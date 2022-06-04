package pers.lenwind.args;

import pers.lenwind.args.exception.ParseException;

import java.util.List;

public class ParseBooleanOption extends ParseOption<Boolean> {
    @Override
    public Boolean parse(List<String> args, Option option) {
        return values(args, option).map(l -> {
            if (l.size() > 0) {
                throw new ParseException(option.value(), boolean.class);
            }
            return true;
        }).orElse(false);
    }
}
