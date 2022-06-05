package pers.lenwind.container.exception;

import lombok.ToString;

import java.lang.reflect.Type;

@ToString(callSuper = true)
public class MultiInjectException extends BaseException {
    public MultiInjectException(Type instanceType) {
        super(instanceType);
    }
}
