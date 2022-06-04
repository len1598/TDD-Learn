package pers.lenwind.args;

import java.util.List;

public interface ParseOption<T> {
    T parse(List<String> args, Option option);
}
