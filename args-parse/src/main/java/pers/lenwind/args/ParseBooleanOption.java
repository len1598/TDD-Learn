package pers.lenwind.args;

import pers.lenwind.args.exception.MultiArgsException;
import pers.lenwind.args.exception.ParseException;

import java.util.List;

public class ParseBooleanOption extends ParseOption<Boolean> {
    @Override
    public Boolean parse(List<String> args, Option option) {
        try {
            if (values(args, option).size() > 0) {
                throw new MultiArgsException(option.value(), boolean.class);
            }
            return true;
        } catch (ParseException e) {
            return false;
        }
    }
}
