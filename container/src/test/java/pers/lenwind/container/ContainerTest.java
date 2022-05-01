package pers.lenwind.container;

import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import pers.lenwind.container.exception.BaseException;
import pers.lenwind.container.exception.CyclicDependencyException;
import pers.lenwind.container.exception.DependencyNotFoundException;

import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

public class ContainerTest {
    @Nested
    class ComponentConstruction {
        private ContextConfiguration contextConfiguration;

        @BeforeEach
        void setUp() {
            contextConfiguration = new ContextConfiguration();
        }

        @Test
        void should_bind_instance_to_context() {
            Component instance = new Component() {
            };
            contextConfiguration.bind(Component.class, instance);

            Component component = new Context(contextConfiguration.getComponentProviders()).get(Component.class).get();
            assertSame(instance, component);
        }

        @Test
        void should_throw_exception_while_not_instantiable() {
            contextConfiguration.bind(Component.class, AbstractComponent.class);


            assertThrows(BaseException.class,
                () -> new Context(contextConfiguration.getComponentProviders()).get(Component.class));
        }

        static abstract class AbstractComponent implements Component {
        }


        @Test
        void should_return_empty_if_not_bind_to_context() {
            Optional<Component> component = new Context(contextConfiguration.getComponentProviders()).get(Component.class);

            assertTrue(component.isEmpty());
        }

        @Test
        void should_throw_exception_if_dependency_not_found() {
            contextConfiguration.bind(Component.class, InstanceWithInject.class);

            DependencyNotFoundException exception = assertThrows(DependencyNotFoundException.class, () -> new Context(contextConfiguration.getComponentProviders()));
            assertEquals(InstanceWithInject.class, exception.getInstanceType());
            assertEquals(Dependency.class, exception.getDependencyType());
        }

        @Test
        void should_throw_exception_if_cyclic_transitive_dependency() {
            contextConfiguration.bind(Component.class, InstanceWithInject.class);
            contextConfiguration.bind(Dependency.class, DependencyWithAnotherDependency.class);
            contextConfiguration.bind(AnotherDependency.class, DependencyWithBean.class);

            CyclicDependencyException exception = assertThrows(CyclicDependencyException.class, () -> new Context(contextConfiguration.getComponentProviders()));
            Set<Class<?>> dependencies = Set.of(InstanceWithInject.class, DependencyWithAnotherDependency.class, DependencyWithBean.class);
            assertTrue(exception.getDependencies().containsAll(dependencies));
        }

    }

    @Nested
    class DependencySelection {
    }

    @Nested
    class LifeCycleManagement {
    }

}

interface Component {
}

interface Dependency {
}

interface AnotherDependency {
}

class DependencyWithAnotherDependency implements Dependency {
    AnotherDependency anotherDependency;

    @Inject
    public DependencyWithAnotherDependency(AnotherDependency anotherDependency) {
        this.anotherDependency = anotherDependency;
    }
}

class DependencyWithBean implements AnotherDependency {
    Component component;

    @Inject
    public DependencyWithBean(Component component) {
        this.component = component;
    }
}
