package pers.lenwind.container.exception;

import lombok.Getter;
import lombok.ToString;

@ToString
@Getter
public class IllegalInjectionException extends BaseException {
    private final String msg;

    public IllegalInjectionException(Class<?> instanceType, String msg) {
        super(instanceType);
        this.msg = msg;
    }
}
