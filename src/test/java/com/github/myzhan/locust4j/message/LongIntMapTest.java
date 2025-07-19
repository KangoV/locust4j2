package com.github.myzhan.locust4j.message;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author myzhan
 */
class LongIntMapTest {

    @Test
    void TestAddAndGet() {
        var map = new LongIntMap(1000L, 1000L);
        assertThat(map.get(1000L)).isEqualTo(2L);
    }

    @Test
    void TestToString() {
        LongIntMap map = new LongIntMap(1000L, 1000L);
        assertThat(map.toString()).isEqualTo("{1000=2}");
    }
}
