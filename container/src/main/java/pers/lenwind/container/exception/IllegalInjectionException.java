package pers.lenwind.container.exception;

import lombok.Getter;
import lombok.ToString;
import pers.lenwind.container.CommonUtils;

import java.lang.reflect.Type;

@ToString
@Getter
public class IllegalInjectionException extends BaseException {
    private final String msg;

    public IllegalInjectionException(Type instanceType, String key) {
        super(instanceType);
        this.msg = CommonUtils.getErrorMsg(key);
    }
}
