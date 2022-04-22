package pers.linwind.args.exception;

public class ParseException extends RuntimeException {
    private String flag;
    private Class<?> type;

    public ParseException(String flag, Class<?> type) {
        this.flag = flag;
        this.type = type;
    }
}
