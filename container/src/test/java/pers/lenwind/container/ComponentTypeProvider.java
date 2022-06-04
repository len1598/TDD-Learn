package pers.lenwind.container;

import jakarta.inject.Inject;
import org.junit.jupiter.api.Named;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.ArgumentsProvider;

import java.lang.reflect.ParameterizedType;
import java.util.stream.Stream;

import static org.junit.jupiter.params.provider.Arguments.arguments;

public class ComponentTypeProvider implements ArgumentsProvider {
    @Override
    public Stream<? extends Arguments> provideArguments(ExtensionContext extensionContext) {
        return Stream.of(
            arguments(Named.of("construction type", ConstructionDependency.class)),
            arguments(Named.of("method type", MethodDependency.class)),
            arguments(Named.of("field type", FieldDependency.class)));
    }

    public static Stream<Arguments> typeSource = Stream.of(
        arguments(Named.of("construction type", ConstructionDependency.class)),
        arguments(Named.of("method type", MethodDependency.class)),
        arguments(Named.of("field type", FieldDependency.class)));

    static class ConstructionDependency implements Component {

        Dependency dependency;

        @Inject
        public ConstructionDependency(Dependency dependency) {
            this.dependency = dependency;
        }

        @Override
        public Dependency getDependency() {
            return dependency;
        }
    }

    static class MethodDependency implements Component {

        Dependency dependency;

        @Inject
        public void setDependency(Dependency dependency) {
            this.dependency = dependency;
        }

        @Override
        public Dependency getDependency() {
            return dependency;
        }

    }

    static class FieldDependency implements Component {

        @Inject
        Dependency dependency;

        @Override
        public Dependency getDependency() {
            return dependency;
        }

    }
}

abstract class Literal<T> {
    ParameterizedType getType() {
        return (ParameterizedType) ((ParameterizedType) getClass().getGenericSuperclass()).getActualTypeArguments()[0];
    }
}

interface Component {
    default Dependency getDependency() {
        return null;
    }

    default ContextConfiguration.Provider<Dependency> getDependencyProvider() {
        return null;
    }
}

interface Dependency {
}
