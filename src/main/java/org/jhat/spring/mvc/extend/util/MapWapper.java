package org.jhat.spring.mvc.extend.util;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * @project spring-mvc-extend
 * @author jhat
 * @email cpf624@126.com
 * @date Mar 19, 20138:38:06 PM
 */
public class MapWapper<K, V> {
    
    private Map<K, V> innerMap;
    
    public MapWapper(int initialCapacity, float loadFactor) {
    	innerMap = new HashMap<K, V>(initialCapacity, loadFactor);
    }

    public MapWapper(int initialCapacity) {
    	innerMap = new HashMap<K, V>(initialCapacity);
    }
    
    public MapWapper() {
    	innerMap = new HashMap<K, V>();
    }
    
    public MapWapper(Map<? extends K, ? extends V> m) {
    	innerMap = new HashMap<K, V>(m);
    }
    
    public void setInnerMap(Map<K, V> innerMap) {
        this.innerMap = innerMap;
    }
    
    public Map<K, V> getInnerMap() {
        return innerMap;
    }

    public int size() {
        return innerMap.size();
    }
    
    public boolean isEmpty() {
        return innerMap.isEmpty();
    }
    
    public boolean containsKey(Object key) {
        return innerMap.containsKey(key);
    }
    
    public boolean containsValue(Object value) {
        return innerMap.containsValue(value);
    }
    
    public V get(Object key) {
        return innerMap.get(key);
    }
    
    public V put(K key, V value) {
        return innerMap.put(key, value);
    }
    
    public V remove(Object key) {
        return innerMap.remove(key);
    }
    
    public void putAll(Map<? extends K, ? extends V> m) {
        innerMap.putAll(m);
    }
    
    public void clear() {
        innerMap.clear();
    }
    
    public Set<K> keySet() {
        return innerMap.keySet();
    }

    public Collection<V> values() {
        return innerMap.values();
    }
    
    public Set<Map.Entry<K, V>> entrySet() {
        return innerMap.entrySet();
    }
    
    @Override
    public boolean equals(Object o) {
        return innerMap.equals(o);
    }

    @Override
    public int hashCode() {
        return innerMap.hashCode();
    }
    
    @Override
    public String toString() {
        return innerMap.toString();
    }

}
