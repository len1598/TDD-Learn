package pers.lenwind.container;

public interface Provider<T> {
    T get(Context context);
}
