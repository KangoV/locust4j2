package com.github.myzhan.locust4j.message;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author myzhan
 */
public class TestLongIntMap {

    @Test
    public void TestAddAndGet() {
        LongIntMap map = new LongIntMap();
        map.add(1000L);
        map.add(1000L);

        assertThat((int)map.get(1000L)).isEqualTo(2);
    }

    @Test
    public void TestToString() {
        LongIntMap map = new LongIntMap();
        map.add(1000L);
        map.add(1000L);

        assertThat(map.toString()).isEqualTo("{1000=2}");
    }
}
