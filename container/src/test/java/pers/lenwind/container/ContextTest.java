package pers.lenwind.container;

import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import pers.lenwind.container.exception.CyclicDependencyException;
import pers.lenwind.container.exception.DependencyNotFoundException;
import pers.lenwind.container.exception.UnsupportedBindException;

import java.lang.reflect.ParameterizedType;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

public class ContextTest {
    private ContextConfiguration contextConfiguration;

    @BeforeEach
    void setUp() {
        contextConfiguration = new ContextConfiguration();
    }

    @Test
    void should_bind_specific_instance_to_context() {
        Component instance = new Component() {
        };
        contextConfiguration.bind(Component.class, instance);

        Component component = (Component) new Context(contextConfiguration.getComponentProviders()).get(Component.class).get();
        assertSame(instance, component);
    }

    static class Instance implements Component {
    }

    @Test
    void should_bind_provider_type_to_context() {
        ParameterizedType type = new Literal<Provider<Instance>>() {
        }.getType();
        contextConfiguration.bind(Component.class, type);

        ComponentProvider<?> componentProvider = (ComponentProvider<?>) new Context(contextConfiguration.getComponentProviders()).get(type).get();
        assertEquals(type, componentProvider.getComponentType());
    }

    @Test
    void should_throw_exception_if_bind_type_by_unsupported_container() {
        ParameterizedType type = new Literal<List<Instance>>() {
        }.getType();
        assertThrows(UnsupportedBindException.class, () -> contextConfiguration.bind(Component.class, type));
    }

    @Test
    void should_return_empty_if_not_bind_to_context() {
        Optional<Component> component = new Context(contextConfiguration.getComponentProviders()).get(Component.class);

        assertTrue(component.isEmpty());
    }

    @Nested
    class DependencyCheck {
        @ParameterizedTest(name = "Not found {0}")
        @MethodSource("pers.lenwind.container.ComponentTypeProvider#dependencies")
        void should_throw_exception_if_dependency_not_found(Class<? extends Component> type) {
            contextConfiguration.bind(Component.class, type);

            DependencyNotFoundException exception = assertThrows(DependencyNotFoundException.class, () -> new Context(contextConfiguration.getComponentProviders()));
            assertEquals(type, exception.getInstanceType());
            assertEquals(Dependency.class, exception.getDependencyType());
        }

        @ParameterizedTest(name = "Cyclic {0}")
        @MethodSource("pers.lenwind.container.ComponentTypeProvider#instanceDependencies")
        void should_throw_exception_if_cyclic_dependency(Class<? extends Component> type) {
            contextConfiguration.bind(Component.class, type);
            contextConfiguration.bind(Dependency.class, DependencyWithAnotherDependency.class);
            contextConfiguration.bind(AnotherDependency.class, DependencyWithBean.class);

            CyclicDependencyException exception = assertThrows(CyclicDependencyException.class, () -> new Context(contextConfiguration.getComponentProviders()));
            Set<Class<?>> dependencies = Set.of(type, DependencyWithAnotherDependency.class, DependencyWithBean.class);
            assertTrue(exception.getDependencies().containsAll(dependencies));
        }

        @Test
        void should_not_throw_exception_if_cyclic_dependency_in_provider_type() {
            contextConfiguration.bind(Component.class, ComponentTypeProvider.ConstructionDependency.class);
            contextConfiguration.bind(Dependency.class, DependencyWithComponentProvider.class);

            assertDoesNotThrow(() -> new Context(contextConfiguration.getComponentProviders()));
        }

        static class DependencyWithComponentProvider implements Dependency {
            @Inject
            private Provider<Component> componentProvider;
        }

    }

    interface AnotherDependency {
    }

    static class DependencyWithAnotherDependency implements Dependency {
        AnotherDependency anotherDependency;

        @Inject
        public DependencyWithAnotherDependency(AnotherDependency anotherDependency) {
            this.anotherDependency = anotherDependency;
        }
    }

    static class DependencyWithBean implements AnotherDependency {
        Component component;

        @Inject
        public DependencyWithBean(Component component) {
            this.component = component;
        }
    }

}
