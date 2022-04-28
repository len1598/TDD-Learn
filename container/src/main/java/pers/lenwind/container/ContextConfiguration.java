package pers.lenwind.container;

import jakarta.inject.Inject;
import pers.lenwind.container.exception.CyclicDependencyException;
import pers.lenwind.container.exception.DependencyNotFoundException;
import pers.lenwind.container.exception.MultiInjectException;
import pers.lenwind.container.exception.NoAvailableConstructionException;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.*;

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
        initDependencies();
        checkDependencies();
        return componentProviders;
    }

    private void checkDependencies() {
        componentProviders.values().forEach(provider -> {
            if (provider instanceof ComponentProvider componentProvider) {
                componentProvider.checkCyclicDependencies(componentProvider, new Stack<>());
            }
        });
    }

    private void initDependencies() {
        componentProviders.values().forEach(provider -> {
            if (provider instanceof ComponentProvider componentProvider) {
                componentProvider.initDependencies();
            }
        });
    }

    public interface Provider {
        Object get();
    }

    class ComponentProvider implements Provider {
        Class<?> componentType;

        List<Class<?>> dependencies;
        private Constructor<?> constructor;

        ComponentProvider(Class<?> componentType) {
            this.componentType = componentType;
        }

        @Override
        public Object get() {
            try {
                Object[] parameters = Arrays.stream(constructor.getParameterTypes())
                    .map(dependency -> componentProviders.get(dependency).get())
                    .toArray();
                Object instance = constructor.newInstance(parameters);
                for (Field field : ComponentUtils.getFieldDependencies(componentType)) {
                    field.setAccessible(true);
                    field.set(instance, componentProviders.get(field.getType()).get());
                }
                return instance;
            } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
                throw new RuntimeException(e);
            }
        }

        public void initDependencies() {
            constructor = ComponentUtils.getConstructor(componentType);
            dependencies = new ArrayList<>();
            dependencies.addAll(List.of(constructor.getParameterTypes()));
            dependencies.addAll(ComponentUtils.getFieldDependencies(componentType).stream().map(Field::getType).toList());
        }

        private void checkCyclicDependencies(ComponentProvider provider, Stack<Class<?>> dependencyStack) {
            if (dependencyStack.contains(provider.componentType)) {
                throw new CyclicDependencyException(dependencyStack.stream().toList());
            }
            dependencyStack.push(provider.componentType);
            provider.dependencies.forEach(dependencyType -> {
                Provider dependency = componentProviders.get(dependencyType);
                if (dependency == null) {
                    throw new DependencyNotFoundException(provider.componentType, dependencyType);
                }
                if (dependency instanceof ComponentProvider componentProvider) {
                    checkCyclicDependencies(componentProvider, dependencyStack);
                }
            });
            dependencyStack.pop();
        }
    }

    private static class ComponentUtils {
        static <T> Constructor<?> getConstructor(Class<T> implementation) {
            List<Constructor<?>> constructors = Arrays.stream(implementation.getConstructors())
                .filter(constructor -> constructor.isAnnotationPresent(Inject.class)).toList();
            if (constructors.size() > 1) {
                throw new MultiInjectException(implementation);
            }
            return constructors.isEmpty() ? getDefaultConstructor(implementation) : constructors.get(0);
        }

        static <T> Constructor<T> getDefaultConstructor(Class<T> implementation) {
            try {
                return implementation.getDeclaredConstructor();
            } catch (NoSuchMethodException e) {
                throw new NoAvailableConstructionException(implementation);
            }
        }

        static List<Field> getFieldDependencies(Class<?> componentType) {
            List<Field> fields = new ArrayList<>();
            Class<?> currentClass = componentType;
            while (currentClass != Object.class) {
                fields.addAll(Arrays.stream(currentClass.getDeclaredFields())
                    .filter(field -> field.isAnnotationPresent(Inject.class)).toList());
                currentClass = currentClass.getSuperclass();
            }
            return fields;
        }
    }
}
