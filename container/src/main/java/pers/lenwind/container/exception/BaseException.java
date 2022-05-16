package pers.lenwind.container.exception;

import lombok.ToString;
import pers.lenwind.container.CommonUtils;

import java.lang.reflect.Type;

@ToString
public class BaseException extends RuntimeException {
    protected Type instanceType;

    public BaseException(Type instanceType) {
        this.instanceType = instanceType;
    }

    public <T> BaseException(Class<?> instanceType, String key) {
        this(instanceType, key, null);
    }

    public BaseException(Class<?> instanceType, String key, Throwable cause) {
        super(CommonUtils.getErrorMsg(key), cause);
        this.instanceType = instanceType;
    }

    public Type getInstanceType() {
        return instanceType;
    }
}
