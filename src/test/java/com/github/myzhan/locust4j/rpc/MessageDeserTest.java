package com.github.myzhan.locust4j.rpc;

import com.github.myzhan.locust4j.message.*;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.msgpack.core.MessagePack;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.*;
import java.util.List;

import static com.github.myzhan.locust4j.message.Message.NULL;
import static com.github.myzhan.locust4j.test.MessageAssert.assertThat;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.data.Index.atIndex;

/**
 * @author myzhan
 */
public class MessageDeserTest {

    @Test
    public void testEncodeAndDecodeNullData() throws Exception {
        var message = Message.create(s -> s.type("test").nodeId("nodeId"));
        var message2 = MessageDeser.copy(message);
        assertThat(message2)
            .hasType("test")
            .hasNoData()
            .hasNodeId("nodeId");
    }

    @Test
    public void testEncodeAndDecodeWithData() throws Exception {

        var data = Map.of(
            "string", "world",
            "int", 1,
            "float", 0.5f,
            "boolean", true,
            "null", NULL,
            "array", List.of("foo", "bar"));

        var message = Message.create(s -> s.type("test").putAllData(data).nodeId("nodeId"));
        var message2 = MessageDeser.copy(message);

        assertThat(message2)
            .hasType("test")
            .hasNodeId("nodeId")
            .data().containsExactlyInAnyOrderEntriesOf(data);
    }

    @Test
    public void testVisitNull() throws IOException {
        byte[] result = serialise(NULL);
        assertThat(result)
            .hasSize(1)
            .startsWith(-64);
    }

    @Test
    public void testVisitString() throws IOException {
        var str = "HelloWorld";
        var bytes = serialise(str);
        assertThat(bytes)
            .hasSize(str.length()+1)
            .startsWith(-86)
            .endsWith(str.getBytes());
    }

    @Test
    public void testVisitLong() throws IOException {
        byte[] bytes = serialise(Long.MAX_VALUE);
        assertThat(bytes)
            .hasSize(9)
            .startsWith(-49);
    }

    @Test
    public void testVisitDouble() throws IOException {
        var bytes = serialise(Double.MAX_VALUE);
        assertThat(bytes)
            .hasSize(9)
            .startsWith(-53);
    }

    @Test
    public void testVisitMap() throws IOException {
        byte[] bytes = serialise(Map.ofEntries(Map.entry("foo", "bar")));
        assertThat(bytes)
            .hasSize(9)
            .startsWith(-127);
    }

    @Test
    public void testVisitList() throws IOException {
        byte[] bytes = serialise(Arrays.asList("foo", "bar"));
        assertThat(bytes)
            .hasSize(9)
            .startsWith(-110);
    }

    @Test
    public void testVisitLongIntMap() throws IOException {
        byte[] bytes = serialise(new LongIntMap(List.of(1000L, 1000L)));
        assertThat(bytes)
            .hasSize(5)
            .contains(2, atIndex(4));
    }

    @Test
    public void testVisitUnknownType()  {
        assertThatExceptionOfType(IOException.class)
            .isThrownBy(() -> serialise(BigDecimal.ONE));
    }

    private static byte[] serialise(Object val) throws IOException {
        try (var packer = MessagePack.newDefaultBufferPacker()) {
            if (val == null || (val instanceof Null)) {
                MessageDeser.visit(packer, Null.TYPE);
            } else {
                MessageDeser.visit(packer, val);
            }
            return packer.toByteArray();
        }
    }

}
