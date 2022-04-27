package pers.lenwind.container.exception;

import lombok.ToString;

@ToString(callSuper = true)
public class NoAvailableConstructionException extends BaseException {
    public NoAvailableConstructionException(Class<?> instanceType) {
        super(instanceType);
    }
}
