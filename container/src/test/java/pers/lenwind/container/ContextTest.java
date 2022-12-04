package pers.lenwind.container;

import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import pers.lenwind.container.exception.CyclicDependencyException;
import pers.lenwind.container.exception.DependencyNotFoundException;

import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

public class ContextTest {
    private ContextConfiguration contextConfiguration;

    @BeforeEach
    void setUp() {
        contextConfiguration = new ContextConfiguration();
    }

    @Nested
    class ComponentBind {
        @Test
        void should_bind_specific_instance_to_context() {
            Component instance = new Component() {
            };
            contextConfiguration.component(Component.class, instance);

            Component component = contextConfiguration.toContext().getInstance(Component.class).get();
            assertSame(instance, component);
        }

        @Test
        void should_bind_specific_instance_with_qualifier() {
            Instance instance = new Instance();
            contextConfiguration.component(Component.class, instance, AnnotationContainer.getNamed());

            Component component = contextConfiguration.toContext().getInstance(Component.class, AnnotationContainer.getNamed()).get();
            assertSame(instance, component);
        }


        @Test
        void should_bind_component_with_qualifier() {
            contextConfiguration.bind(Component.class, Instance.class, AnnotationContainer.getNamed());

            Optional<Component> component = contextConfiguration.toContext().getInstance(Component.class, AnnotationContainer.getNamed());
            assertTrue(component.isPresent());
        }

        // TODO should throw exception if not a qualifier annotation
        // TODO should return empty if qualifier not equal


        static class Instance implements Component {
        }

        @Test
        void should_return_empty_if_not_bind_to_context() {
            Optional<Component> component = contextConfiguration.toContext().getInstance(Component.class);

            assertTrue(component.isEmpty());
        }

        @Test
        void should_return_provider_type_if_want() {
            contextConfiguration.bind(Component.class, Instance.class);

            Optional<Provider<Component>> optional = contextConfiguration.toContext().getProvider(Component.class);
            assertTrue(optional.isPresent());
        }
    }

    @Nested
    class DependencyCheck {
        @ParameterizedTest(name = "Not found {0}")
        @MethodSource("pers.lenwind.container.ComponentTypeProvider#dependencies")
        void should_throw_exception_if_dependency_not_found(Class<? extends Component> type) {
            contextConfiguration.bind(Component.class, type);

            DependencyNotFoundException exception = assertThrows(DependencyNotFoundException.class, () -> contextConfiguration.toContext());
            assertEquals(type, exception.getInstanceType());
            assertEquals(Dependency.class, exception.getDependencyType());
        }

        @ParameterizedTest(name = "Cyclic {0}")
        @MethodSource("pers.lenwind.container.ComponentTypeProvider#instanceDependencies")
        void should_throw_exception_if_cyclic_dependency(Class<? extends Component> type) {
            contextConfiguration.bind(Component.class, type);
            contextConfiguration.bind(Dependency.class, DependencyWithAnotherDependency.class);
            contextConfiguration.bind(AnotherDependency.class, DependencyWithBean.class);

            CyclicDependencyException exception = assertThrows(CyclicDependencyException.class, () -> contextConfiguration.toContext());
            Set<Class<?>> dependencies = Set.of(type, DependencyWithAnotherDependency.class, DependencyWithBean.class);
            assertTrue(exception.getDependencies().containsAll(dependencies));
        }

        @ParameterizedTest(name = "Cyclic {0}")
        @MethodSource("pers.lenwind.container.ComponentTypeProvider#providerDependencies")
        void should_not_throw_exception_if_cyclic_dependency_in_provider_type(Class<? extends Component> type) {
            contextConfiguration.bind(Component.class, type);
            contextConfiguration.bind(Dependency.class, DependencyWithComponentProvider.class);

            assertDoesNotThrow(() -> contextConfiguration.toContext());
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
