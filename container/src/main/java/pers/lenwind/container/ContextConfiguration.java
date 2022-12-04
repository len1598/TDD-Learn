package pers.lenwind.container;

import java.lang.annotation.Annotation;
import java.util.HashMap;
import java.util.Map;

public class ContextConfiguration {
    private final Map<Ref, Provider<?>> initialCache = new HashMap<>();

    public <Type> void component(Class<Type> componentType, Type instance) {
        initialCache.put(Ref.of(componentType), context -> instance);
    }

    public <Type> void component(Class<Type> componentType, Type instance, Annotation... qualifiers) {
    }

    public <ComponentType>
    void bind(Class<ComponentType> componentType, Class<? extends ComponentType> instanceType) {
        initialCache.put(Ref.of(componentType), new ComponentProvider<>(instanceType));
    }

    public Context toContext() {
        return new Context(initialCache);
    }
}
