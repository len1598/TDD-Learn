package pers.lenwind.container.exception;

import lombok.Getter;
import lombok.ToString;
import pers.lenwind.container.CommonUtils;

@ToString
@Getter
public class IllegalInjectionException extends BaseException {
    private final String msg;

    public IllegalInjectionException(Class<?> instanceType, String key) {
        super(instanceType);
        this.msg = CommonUtils.getErrorMsg(key);
    }
}
