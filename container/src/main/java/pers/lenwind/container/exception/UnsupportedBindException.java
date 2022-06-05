package pers.lenwind.container.exception;

import java.lang.reflect.Type;

public class UnsupportedBindException extends BaseException{
    public UnsupportedBindException(Type instanceType) {
        super(instanceType);
    }
}
