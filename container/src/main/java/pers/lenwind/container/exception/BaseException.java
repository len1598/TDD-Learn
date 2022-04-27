package pers.lenwind.container.exception;

import lombok.ToString;

@ToString
public class BaseException extends RuntimeException {
    protected Class<?> instanceType;

    public BaseException(Class<?> instanceType) {
        this.instanceType = instanceType;
    }

    public Class<?> getInstanceType() {
        return instanceType;
    }
}
