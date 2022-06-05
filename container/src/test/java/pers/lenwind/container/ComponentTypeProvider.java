package pers.lenwind.container;

import jakarta.inject.Inject;
import org.junit.jupiter.api.Named;
import org.junit.jupiter.params.provider.Arguments;

import java.lang.reflect.ParameterizedType;
import java.util.stream.Stream;

import static org.junit.jupiter.params.provider.Arguments.arguments;

public class ComponentTypeProvider {
    public static Stream<Arguments> dependencies() {
        return Stream.concat(instanceDependencies(), providerDependencies());
    }

    public static Stream<Arguments> providerDependencies() {
        return Stream.of(
            Arguments.of(Named.of("provider dependency in construction type", ConstructionDependencyProvider.class)),
            Arguments.of(Named.of("provider dependency in field type", FieldDependencyProvider.class)),
            Arguments.of(Named.of("provider dependency in method type", MethodDependencyProvider.class)));
    }

    public static Stream<Arguments> instanceDependencies() {
        return Stream.of(
            arguments(Named.of("dependency in construction type", ConstructionDependency.class)),
            arguments(Named.of("dependency in method type", MethodDependency.class)),
            arguments(Named.of("dependency in field type", FieldDependency.class)));
    }

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

    static class ConstructionDependencyProvider implements Component {
        private Provider<Dependency> dependencyProvider;

        @Inject
        public ConstructionDependencyProvider(Provider<Dependency> dependencyProvider) {
            this.dependencyProvider = dependencyProvider;
        }

        @Override
        public Provider<Dependency> getDependencyProvider() {
            return dependencyProvider;
        }
    }

    static class FieldDependencyProvider implements Component {
        @Inject
        private Provider<Dependency> dependencyProvider;

        @Override
        public Provider<Dependency> getDependencyProvider() {
            return dependencyProvider;
        }
    }

    static class MethodDependencyProvider implements Component {
        private Provider<Dependency> dependencyProvider;

        @Inject
        public void setDependencyProvider(Provider<Dependency> dependencyProvider) {
            this.dependencyProvider = dependencyProvider;
        }

        @Override
        public Provider<Dependency> getDependencyProvider() {
            return dependencyProvider;
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

    default Provider<Dependency> getDependencyProvider() {
        return null;
    }
}

interface Dependency {
}
