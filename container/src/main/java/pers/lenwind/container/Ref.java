package pers.lenwind.container;

import lombok.Builder;

import java.util.Objects;

@Builder
public class Ref {
    public static Ref of(Class<?> componentType) {
        return builder().type(componentType).build();
    }

    private Class<?> type;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Ref ref = (Ref) o;

        return Objects.equals(type, ref.type);
    }

    @Override
    public int hashCode() {
        return type != null ? type.hashCode() : 0;
    }
}
