package pers.lenwind.container;

import jakarta.inject.Named;

import java.lang.annotation.Annotation;

@Named
public class AnnotationContainer {
    public static Annotation getNamed() {
        return AnnotationContainer.class.getAnnotation(Named.class);
    }
}
