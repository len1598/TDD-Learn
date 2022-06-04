package pers.lenwind.args;

import pers.lenwind.args.exception.MultiArgsException;
import pers.lenwind.args.exception.ParseException;

import java.util.List;

public class ParseIntOption extends ParseOption<Integer> {
    @Override
    public Integer parse(List<String> args, Option option) {
        List<String> values = values(args, option);
        if (values.size() > 1) {
            throw new MultiArgsException(option.value(), int.class);
        }
        if (values.size() == 0) {
            return 0;
        }
        if (!values.get(0).matches("\\d+")) {
            throw new ParseException(option.value(), int.class);
        }
        return Integer.parseInt(values.get(0));
    }
}
