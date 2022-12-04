package pers.lenwind.container;

import lombok.Builder;
import lombok.EqualsAndHashCode;

import java.lang.annotation.Annotation;

@Builder
@EqualsAndHashCode
public class Ref {
    public static Ref of(Class<?> componentType) {
        return builder().type(componentType).build();
    }

    public static Ref of(Class<?> componentType, Annotation qualifier) {
        return builder().type(componentType).qualifier(qualifier).build();
    }

    private Class<?> type;

    private Annotation qualifier;
}
