package pers.lenwind.container;

import java.lang.annotation.Annotation;


public record Descriptor(Class<?> type, boolean isProvider, Annotation qualifier) {
    Ref toRef() {
        return Ref.of(type, qualifier);
    }
}
