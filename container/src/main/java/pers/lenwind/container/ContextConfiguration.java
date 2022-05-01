package pers.lenwind.container;

import java.util.HashMap;
import java.util.Map;

public class ContextConfiguration {
    private final Map<Class<?>, Provider<?>> componentProviders = new HashMap<>();

    public <T> void bind(Class<T> componentType, T instance) {
        componentProviders.put(componentType, context -> instance);
    }

    public <ComponentType, InstanceType extends ComponentType>
    void bind(Class<ComponentType> componentType, Class<InstanceType> instanceType) {
        componentProviders.put(componentType, new ComponentProvider<>(instanceType));
    }

    public Map<Class<?>, Provider<?>> getComponentProviders() {
        return componentProviders;
    }

    public interface Provider<T> {
        T get(Context context);
    }
}
