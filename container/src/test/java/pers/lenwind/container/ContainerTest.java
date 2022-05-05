package pers.lenwind.container;

import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Named;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import pers.lenwind.container.exception.BaseException;
import pers.lenwind.container.exception.CyclicDependencyException;
import pers.lenwind.container.exception.DependencyNotFoundException;

import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.params.provider.Arguments.arguments;

public class ContainerTest {
    private ContextConfiguration contextConfiguration;

    @BeforeEach
    void setUp() {
        contextConfiguration = new ContextConfiguration();
    }

    @Nested
    class TypeBind {
        @Test
        void should_bind_specific_instance_to_context() {
            Component instance = new Component() {
            };
            contextConfiguration.bind(Component.class, instance);

            Component component = new Context(contextConfiguration.getComponentProviders()).get(Component.class).get();
            assertSame(instance, component);
        }

        @ParameterizedTest(name = "bind {0}")
        @MethodSource("typeSource")
        void should_bind_type_to_context(Class<? extends Component> type) {
            Dependency dependency = new Dependency() {
            };
            contextConfiguration.bind(Component.class, type);
            contextConfiguration.bind(Dependency.class, dependency);

            GetDependency component = (GetDependency) new Context(contextConfiguration.getComponentProviders()).get(Component.class).get();
            assertEquals(dependency, component.geDependency());
        }


        static Stream<Arguments> typeSource() {
            return Stream.of(arguments(Named.of("Construction inject type", ConstructionInject.class)),
                arguments(Named.of("Method inject type", MethodInject.class)),
                arguments(Named.of("Field inject type", FieldInject.class)));
        }

        static class ConstructionInject implements Component, GetDependency {

            Dependency dependency;

            @Inject
            public ConstructionInject(Dependency dependency) {
                this.dependency = dependency;
            }
            @Override
            public Dependency geDependency() {
                return dependency;
            }

        }
        static class MethodInject implements Component, GetDependency {

            Dependency dependency;

            @Inject
            public void setDependency(Dependency dependency) {
                this.dependency = dependency;
            }
            @Override
            public Dependency geDependency() {
                return dependency;
            }

        }
        static class FieldInject implements Component, GetDependency {

            @Inject
            Dependency dependency;
            @Override
            public Dependency geDependency() {
                return dependency;
            }

        }
        interface GetDependency {
            Dependency geDependency();

        }
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
