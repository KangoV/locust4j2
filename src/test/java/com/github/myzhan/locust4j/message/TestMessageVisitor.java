package com.github.myzhan.locust4j.message;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.msgpack.core.MessageBufferPacker;
import org.msgpack.core.MessagePack;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;

/**
 * @author myzhan
 */
public class TestMessageVisitor {

    private MessageBufferPacker packer;

    @Before
    public void before() {
        packer = MessagePack.newDefaultBufferPacker();
    }

    @Test
    public void TestVisitNull() throws IOException {
        Message.visit(packer, null);
        byte[] result = packer.toByteArray();

        assertThat(result).hasSize(1);
        assertThat(result).startsWith(-64);
    }

    @Test
    public void TestVisitString() throws IOException {
        var str = "HelloWorld";
        Message.visit(packer, str);

        byte[] result = packer.toByteArray();

        assertThat(result).hasSize(11);
        assertThat(result).startsWith(-86);
        assertThat(Arrays.copyOfRange(result, 1, 11)).containsExactly(str.getBytes());
    }

    @Test
    public void TestVisitLong() throws IOException {
        Message.visit(packer, Long.MAX_VALUE);

        byte[] result = packer.toByteArray();

        assertEquals(9, result.length);
        assertEquals(-49, result[0]);
    }

    @Test
    public void TestVisitDouble() throws IOException {
        Message.visit(packer, Double.MAX_VALUE);

        byte[] result = packer.toByteArray();

        assertEquals(9, result.length);
        assertEquals(-53, result[0]);
    }

    @Test
    public void TestVisitMap() throws IOException {
        Map<String, Object> m = new HashMap<>();
        m.put("foo", "bar");
        Message.visit(packer, m);

        byte[] result = packer.toByteArray();

        assertEquals(9, result.length);
        assertEquals(-127, result[0]);
    }

    @Test
    public void TestVisitList() throws IOException {
        Message.visit(packer, Arrays.asList("foo", "bar"));

        byte[] result = packer.toByteArray();

        assertEquals(9, result.length);
        assertEquals(-110, result[0]);
    }

    @Test
    public void TestVisitLongIntMap() throws IOException {
        LongIntMap data = new LongIntMap();
        data.add(1000L);
        data.add(1000L);

        Message.visit(packer, data);

        byte[] result = packer.toByteArray();

        assertEquals(5, result.length);
        assertEquals(2, result[4]);
    }

    @Test(expected = IOException.class)
    public void TestVisitUnknownType() throws IOException {
        Message.visit(packer, BigDecimal.ONE);
    }
}
