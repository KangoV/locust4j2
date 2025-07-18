package com.github.myzhan.locust4j.message;

import java.util.*;
import java.util.List;

import org.junit.Test;

import static com.github.myzhan.locust4j.test.MessageAssert.assertThat;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

/**
 * @author myzhan
 */
public class TestMessage {

    @Test
    public void TestEncodeAndDecodeNullData() throws Exception {
        var message = Message.create(s -> s.type("test").nodeId("nodeId"));
        var message2 = Message.from(message.bytes());

        assertThat(message2)
            .hasType("test")
            .hasNoData()
            .hasNodeId("nodeId");
    }

    @Test
    public void TestEncodeAndDecodeWithData() throws Exception {

        Map<String, Object> data = Map.of(
            "string", "world",
            "int", 1,
            "float", 0.5f,
            "boolean", true,
//            "null", null,
            "array", List.of("foo", "bar"));

        var message = Message.create(s -> s.type("test").putAllData(data).nodeId("nodeId"));
        var message2 = Message.from(message.bytes());

        assertThat(message2)
            .hasType("test")
            .hasNodeId("nodeId")
            .data().contains(
                entry("string", "world"),
                entry("int", 1),
                entry("float", 0.5f),
                entry("boolean", true),
                entry("array", List.of("foo", "bar"))
//                entry("null", null)
            );
    }

}
