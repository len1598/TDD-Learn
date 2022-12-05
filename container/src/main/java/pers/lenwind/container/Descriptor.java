package pers.lenwind.container;

import pers.lenwind.container.exception.InternalException;

import java.lang.annotation.Annotation;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Locale;


public record Descriptor(Class<?> type, boolean isProvider, Annotation qualifier) {
    static Descriptor of(Type type, Annotation qualifier) {
        if (type instanceof ParameterizedType parameterizedType) {
            if (parameterizedType.getRawType() == Provider.class) {
                return new Descriptor((Class<?>) parameterizedType.getActualTypeArguments()[0], true, qualifier);
            }
            throw new InternalException(String.format(Locale.ENGLISH, "Not support type: %s", type));
        }
        return new Descriptor((Class<?>) type, false, qualifier);
    }

    Ref toRef() {
        return Ref.of(type, qualifier);
    }
}
