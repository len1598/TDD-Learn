package pers.lenwind.args;

import java.util.List;

public class ParseGroupOption extends ParseOption<String[]> {
    @Override
    public String[] parse(List<String> args, Option option) {
        return values(args, option).toArray(String[]::new);
    }
}
