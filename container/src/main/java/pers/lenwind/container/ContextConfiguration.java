package pers.lenwind.container;

import jakarta.inject.Inject;
import pers.lenwind.container.exception.CyclicDependencyException;
import pers.lenwind.container.exception.DependencyNotFoundException;
import pers.lenwind.container.exception.MultiInjectException;
import pers.lenwind.container.exception.NoAvailableConstructionException;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ContextConfiguration {
    private final Map<Class<?>, Provider> componentProviders = new HashMap<>();

    public <T> void bind(Class<T> componentType, T instance) {
        componentProviders.put(componentType, () -> instance);
    }

    public <ComponentType, InstanceType extends ComponentType>
    void bind(Class<ComponentType> componentType, Class<InstanceType> instanceType) {
        componentProviders.put(componentType, new ComponentProvider(instanceType));
    }

    public Map<Class<?>, Provider> initContainer() {
        componentProviders.values().forEach(provider -> {
            if (provider instanceof ComponentProvider componentProvider) {
                componentProvider.checkDependencies();
            }
        });
        return componentProviders;
    }

    public interface Provider {
        Object get();
    }

    private class ComponentProvider implements Provider {
        boolean cyclicFlag;

        Class<?> componentType;

        List<Class<?>> dependencies;

        ComponentProvider(Class<?> componentType) {
            this.componentType = componentType;
        }

        @Override
        public Object get() {
            if (cyclicFlag) {
                throw new CyclicDependencyException(componentType);
            }
            cyclicFlag = true;
            Constructor<?> constructor = getConstructor(componentType);
            try {
                Object[] parameters = Arrays.stream(constructor.getParameterTypes())
                        .map(dependency -> {
                            Provider provider = componentProviders.get(dependency);
                            if (provider == null) {
                                throw new DependencyNotFoundException(constructor.getDeclaringClass(), dependency);
                            }
                            return provider.get();
                        })
                        .toArray();
                return constructor.newInstance(parameters);
            } catch (CyclicDependencyException e) {
                throw new CyclicDependencyException(componentType, e);
            } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
                throw new RuntimeException(e);
            } finally {
                cyclicFlag = false;
            }
        }

        public List<Class<?>> getDependencies() {
            return dependencies;
        }

        private <T> Constructor<?> getConstructor(Class<T> implementation) {
            List<Constructor<?>> constructors = Arrays.stream(implementation.getConstructors())
                    .filter(constructor -> constructor.isAnnotationPresent(Inject.class)).toList();
            if (constructors.size() > 1) {
                throw new MultiInjectException(implementation);
            }
            return constructors.isEmpty() ? getDefaultConstructor(implementation) : constructors.get(0);
        }

        private <T> Constructor<T> getDefaultConstructor(Class<T> implementation) {
            try {
                return implementation.getConstructor();
            } catch (NoSuchMethodException e) {
                throw new NoAvailableConstructionException(implementation);
            }
        }

        public void checkDependencies() {

        }
    }
}
