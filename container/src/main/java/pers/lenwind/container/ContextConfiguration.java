package pers.lenwind.container;

import pers.lenwind.container.exception.UnsupportedBindException;

import java.lang.annotation.Annotation;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;

public class ContextConfiguration {
    private final Map<Type, Provider<?>> componentProviders = new HashMap<>();

    public <Type> void component(Class<Type> componentType, Type instance) {
        componentProviders.put(componentType, context -> instance);
    }

    public <Type> void component(Class<Type> componentType, Type instance, Annotation... qualifiers) {
    }

    public <ComponentType>
    void bind(Class<ComponentType> componentType, Class<? extends ComponentType> instanceType) {
        componentProviders.put(componentType, new ComponentProvider<>(instanceType));
    }

    public <ComponentType> void bind(Class<ComponentType> type, ParameterizedType providerType) {
        if (providerType.getRawType() != Provider.class) {
            throw new UnsupportedBindException(providerType);
        }
        Provider<?> provider = new ComponentProvider<>(providerType);
        componentProviders.put(type, provider);
        componentProviders.put(providerType, provider);
    }

    public Context toContext() {
        return new Context(componentProviders);
    }
}
