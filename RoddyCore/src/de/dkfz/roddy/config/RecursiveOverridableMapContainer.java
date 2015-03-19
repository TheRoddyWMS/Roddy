package de.dkfz.roddy.config;

import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * Helps configurations to store overridable versions of configuration values and other things
 */
public class RecursiveOverridableMapContainer<K, V extends RecursiveOverridableMapContainer.Identifiable, P extends ContainerParent> {


    public static interface Identifiable {
        public String getID();

    }

    /**
     * A list of values in this container's configuration
     */
    private final Map<K, V> values = new LinkedHashMap<>();


    private final P containerParent;

    private final String id;

    RecursiveOverridableMapContainer(P containerParent, String id) {
        this.containerParent = containerParent;
        this.id = id;
    }

    public P getContainerParent() {
        return containerParent;
    }

    public String getId() {
        return id;
    }

    public Map<K, V> getMap() {
        return values;
    }

    public List<K> getListOfMapsValues() {
        return new LinkedList<>(values.keySet());
    }

    public List<K> getListOfAllValues() {
        List<K> allValues = new LinkedList<>();
        for (P parent : (List<P>) containerParent.getContainerParents())
            allValues.addAll(parent.getContainer(id).getListOfAllValues());

        allValues.addAll(values.keySet());
        return allValues;
    }

    public Map<K, V> getAllValues() {

        Map<K, V> allValues = new LinkedHashMap<>();
        for (P parent : (List<P>) containerParent.getContainerParents()) {
            Map<K, V> tempValues = parent.getContainer(id).getAllValues();
            for (K key : tempValues.keySet()) {
//                if (!allValues.containsKey(key))
                allValues.put(key, tempValues.get(key));
            }
        }
        allValues.putAll(values);
//        for(K key : values.keySet()) {
//            System.err.println("ID: " + containerParent.getID() + "\t" + key);
//
//        }
        return allValues;
    }

    public List<V> getAllValuesAsList() {
        return new LinkedList<>(getAllValues().values());
    }

    public boolean hasValue(String id) {
        return getListOfAllValues().contains(id);
    }

    public V getValue(String id, V defaultValue) {
        if (!hasValue(id))
            return defaultValue;
        return getAllValues().get(id);
    }

    public V getValue(String id) {
        if (!hasValue(id))
            throw new RuntimeException("Value " + id + " could not be found in containers with id " + id);

        return getAllValues().get(id);
    }

    public void putAll(Map<K, V> values) {
        if (values == null) return;
        this.values.putAll(values);
    }

    public void put(K key, V value) {
        this.values.put(key, value);
    }

    public void add(V value) {
        this.values.put((K) value.getID(), value);
    }

    public int size() {
        return values.size();
    }

    public boolean is(String id) {
        return this.id.equals(id);
    }

    @Override
    public String toString() {
        return String.format("Rec map [%s] with %d values.", getId(), values.size());
    }
}
