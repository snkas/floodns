package ch.ethz.systems.floodns.ext.utils;

import java.util.HashMap;

/**
 * Mapping which, regardless of which elements are in it, returns a constant value.
 *
 * @param <K>   Key type
 * @param <V>   Value type
 */
public class ConstantMap<K, V> extends HashMap<K, V> {

    private final V constantValue;

    public ConstantMap(V constantValue) {
        this.constantValue = constantValue;
    }

    @Override
    public V get(Object k) {
        return constantValue;
    }

}