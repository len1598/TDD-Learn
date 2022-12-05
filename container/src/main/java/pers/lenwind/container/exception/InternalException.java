package pers.lenwind.container.exception;

import lombok.ToString;

@ToString
public class InternalException extends RuntimeException {
    public InternalException(String message) {
        super(message);
    }
}
