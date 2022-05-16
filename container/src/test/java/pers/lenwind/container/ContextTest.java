package pers.lenwind.container;

import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ArgumentsSource;
import pers.lenwind.container.exception.CyclicDependencyException;
import pers.lenwind.container.exception.DependencyNotFoundException;

import java.lang.reflect.ParameterizedType;
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

        Component component = new Context(contextConfiguration.getComponentProviders()).get(Component.class).get();
        assertSame(instance, component);
    }

    static class Instance implements Component {
    }

    @Test
    void should_bind_provider_type_to_context() {
        ParameterizedType type = new Literal<ComponentProvider<Component>>() {
        }.getType();

        ComponentProvider<Instance> provider = new ComponentProvider<>(Instance.class);
        contextConfiguration.bind(type, provider);

        ContextConfiguration.Provider<?> componentProvider = new Context(contextConfiguration.getComponentProviders()).get(type).get();
        assertSame(provider, componentProvider);
    }



    @Test
    void should_return_empty_if_not_bind_to_context() {
        Optional<Component> component = new Context(contextConfiguration.getComponentProviders()).get(Component.class);

        assertTrue(component.isEmpty());
    }

    @Nested
    class DependencyCheck {
        @ParameterizedTest(name = "Not find the dependency in {0}")
        @ArgumentsSource(ComponentTypeProvider.class)
        void should_throw_exception_if_dependency_not_found(Class<? extends Component> type) {
            contextConfiguration.bind(Component.class, type);

            DependencyNotFoundException exception = assertThrows(DependencyNotFoundException.class, () -> new Context(contextConfiguration.getComponentProviders()));
            assertEquals(type, exception.getInstanceType());
            assertEquals(Dependency.class, exception.getDependencyType());
        }

        @ParameterizedTest(name = "Cyclic dependency in {0}")
        @ArgumentsSource(ComponentTypeProvider.class)
        void should_throw_exception_if_cyclic_dependency(Class<? extends Component> type) {
            contextConfiguration.bind(Component.class, type);
            contextConfiguration.bind(Dependency.class, DependencyWithAnotherDependency.class);
            contextConfiguration.bind(AnotherDependency.class, DependencyWithBean.class);

            CyclicDependencyException exception = assertThrows(CyclicDependencyException.class, () -> new Context(contextConfiguration.getComponentProviders()));
            Set<Class<?>> dependencies = Set.of(type, DependencyWithAnotherDependency.class, DependencyWithBean.class);
            assertTrue(exception.getDependencies().containsAll(dependencies));
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
