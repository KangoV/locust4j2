package com.github.myzhan.locust4j.message;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * @author vrajat
 */
public class LongIntMap {
    protected Map<Long, Integer> internalStore;

    public LongIntMap() {
        internalStore = new HashMap<>(16);
    }

    public LongIntMap(long ... vals) {
        this();
        assert vals!=null;
        for (long v : vals) {
            add(v);
        }
    }

    public <I extends Iterable<Long>> LongIntMap(I iterable) {
        this();
        assert iterable!=null;
        for (long v : iterable) {
            add(v);
        }
    }

    public Map<Long,Integer> asMap() {
        return Collections.unmodifiableMap(internalStore);
    }

    public Integer get(Long k) {
        return internalStore.get(k);
    }

    public void add(Long k) {
        if (internalStore.containsKey(k)) {
            internalStore.put(k, internalStore.get(k) + 1);
        } else {
            internalStore.put(k, 1);
        }
    }

    @Override
    public String toString() {
        return this.internalStore.toString();
    }
}
