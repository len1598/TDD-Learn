package pers.lenwind.container.exception;

import lombok.ToString;

@ToString(callSuper = true)
public class MultiInjectException extends BaseException {
    public MultiInjectException(Class<?> instanceType) {
        super(instanceType);
    }
}
