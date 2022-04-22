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
import java.util.stream.Collectors;

public class ContextConfiguration {
    private final Map<Class<?>, Builder> contextBuilder = new HashMap<>();

    public <T> void bind(Class<T> clazz, T instance) {
        contextBuilder.put(clazz, () -> instance);
    }

    public <ComponentType, InstanceType extends ComponentType>
    void bind(Class<ComponentType> type, Class<InstanceType> implement) {
        Constructor<?> constructor = getConstructor(implement);
        contextBuilder.put(type, new InstanceBuilder(constructor));
    }

    public Map<Class<?>, Object> initContainer() {
        return contextBuilder.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, entry -> entry.getValue().build()));
    }

    private interface Builder {
        Object build();
    }

    private class InstanceBuilder implements Builder {
        boolean cyclicFlag;

        Constructor<?> constructor;

        InstanceBuilder(Constructor<?> constructor) {
            this.constructor = constructor;
        }

        @Override
        public Object build() {
            if (cyclicFlag) {
                throw new CyclicDependencyException(constructor.getDeclaringClass());
            }
            cyclicFlag = true;
            try {
                Object[] parameters = Arrays.stream(constructor.getParameterTypes())
                        .map(dependency -> {
                            Builder builder = contextBuilder.get(dependency);
                            if (builder == null) {
                                throw new DependencyNotFoundException(constructor.getDeclaringClass(), dependency);
                            }
                            return builder.build();
                        })
                        .toArray();
                return constructor.newInstance(parameters);
            } catch (CyclicDependencyException e) {
                throw new CyclicDependencyException(constructor.getDeclaringClass(), e);
            } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
                throw new RuntimeException(e);
            } finally {
                cyclicFlag = false;
            }
        }
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
}
