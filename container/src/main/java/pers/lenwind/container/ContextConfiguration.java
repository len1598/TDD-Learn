package pers.lenwind.container;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class ContextConfiguration {
    private final Map<Ref, Provider<?>> initialCache = new HashMap<>();

    public <Type> void component(Class<Type> componentType, Type instance) {
        initialCache.put(Ref.of(componentType), context -> instance);
    }

    public <Type> void component(Class<Type> componentType, Type instance, Annotation... qualifiers) {
        innerComponent(componentType, instance, qualifiers);
    }

    private void innerComponent(Class<?> componentType, Object instance, Annotation[] qualifiers) {
        for (Annotation qualifier : qualifiers) {
            initialCache.put(Ref.of(componentType, qualifier), context -> instance);
        }
    }

    public <ComponentType>
    void bind(Class<ComponentType> componentType, Class<? extends ComponentType> instanceType) {
        initialCache.put(Ref.of(componentType), new ComponentProvider<>(instanceType));
    }

    public <ComponentType>
    void bind(Class<ComponentType> componentType, Class<? extends ComponentType> instanceType, Annotation... qualifiers) {
        innerBind(componentType, instanceType, qualifiers);
    }

    private void innerBind(Class<?> componentType, Class<?> instanceType, Annotation[] qualifiers) {
        for (Annotation qualifier : qualifiers) {
            initialCache.put(Ref.of(componentType, qualifier), new ComponentProvider<>(instanceType));
        }
    }

    public void from(Config config) {
        new DSL(config).init();
    }

    public Context toContext() {
        return new Context(initialCache);
    }

    class DSL {
        private Config config;

        public DSL(Config config) {
            this.config = config;
        }

        public void init() {
            Arrays.stream(config.getClass().getFields())
                .filter(f -> !f.isSynthetic())
                .map(Declaration::new)
                .forEach(Declaration::bind);
        }

        class Declaration {
            Field field;

            public Declaration(Field field) {
                this.field = field;
            }

            public void bind() {
                value().ifPresentOrElse(this::bindInstance, this::bindComponent);
            }

            private void bindComponent() {
                ContextConfiguration.this.innerBind(type(), field.getType(), annotations());
            }

            private void bindInstance(Object v) {
                ContextConfiguration.this.innerComponent(type(), v, annotations());
            }

            private Annotation[] annotations() {
                return Arrays.stream(field.getAnnotations()).filter(a -> a.annotationType() != Config.Export.class).toArray(Annotation[]::new);
            }

            private Class<?> type() {
                Config.Export export = field.getAnnotation(Config.Export.class);
                return export == null ? field.getType() : export.value();
            }

            private Optional<Object> value() {
                try {
                    field.setAccessible(true);
                    return Optional.ofNullable(field.get(config));
                } catch (IllegalAccessException e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }
}
