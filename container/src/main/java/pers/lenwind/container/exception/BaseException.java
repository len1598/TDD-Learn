package pers.lenwind.container.exception;

import lombok.ToString;
import pers.lenwind.container.CommonUtils;

@ToString
public class BaseException extends RuntimeException {
    protected Class<?> instanceType;

    public BaseException(Class<?> instanceType) {
        this.instanceType = instanceType;
    }

    public BaseException(Class<?> instanceType, String key, Throwable cause) {
        super(CommonUtils.getErrorMsg(key), cause);
        this.instanceType = instanceType;
    }

    public Class<?> getInstanceType() {
        return instanceType;
    }
}
