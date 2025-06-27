package com.github.myzhan.locust4j.message;

import java.awt.*;
import java.util.*;
import java.util.List;

import org.assertj.core.api.*;
import org.junit.Test;

import static com.github.myzhan.locust4j.message.TestMessage.MessageAssert.assertThat;
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
        Message message = new Message("test", null, -1, "nodeId");
        Message message2 = new Message(message.getBytes());

        assertThat(message2.getType()).isEqualTo("test");
        assertThat(message2.getData()).isNull();
        assertThat(message2.getNodeID()).isEqualTo("nodeId");
    }

    @Test
    public void TestEncodeAndDecodeWithData() throws Exception {
        Map<String, Object> data = new HashMap<>();
        data.put("string", "world");
        data.put("int", 1);
        data.put("float", 0.5f);
        data.put("boolean", true);
        data.put("null", null);
        data.put("array", new ArrayList<String>(Arrays.asList("foo", "bar")));

        Message message = new Message("test", data, -1, "nodeId");
        Message message2 = new Message(message.getBytes());

        assertEquals("test", message2.getType());
        assertEquals("world", message2.getData().get("string"));
        assertEquals(1, message2.getData().get("int"));
        assertEquals(0.5f, message2.getData().get("float"));
        assertEquals(true, message2.getData().get("boolean"));
        assertNull(message2.getData().get("null"));
        assertEquals(new ArrayList<String>(Arrays.asList("foo", "bar")),message2.getData().get("array"));
        assertEquals("nodeId", message2.getNodeID());

        assertThat(message2)
            .hasType("test")
            .hasNodeId("nodeId")
            .data().contains(
                entry("string", "world"),
                entry("int", 1),
                entry("float", 0.5f),
                entry("boolean", true),
                entry("array", List.of("foo", "bar")),
                entry("null", null)
            );
    }

    public static class MessageAssert extends AbstractAssert<MessageAssert, Message> {

        public MessageAssert(Message actual) {
            super(actual, MessageAssert.class);
        }

        public static MessageAssert assertThat(Message actual) {
            return new MessageAssert(actual);
        }

        public MessageAssert hasType(String type) {
            isNotNull();
            // check condition
            if (!Objects.equals(actual.getType(), type)) {
                failWithMessage("Expected messages's type to be <%s> but was <%s>", type, actual.getType());
            }
            return this;
        }

        public MessageAssert hasNodeId(String nodeId) {
            isNotNull();
            // check condition
            if (!Objects.equals(actual.getNodeID(), nodeId)) {
                failWithMessage("Expected messages's nodeId to be <%s> but was <%s>", nodeId, actual.getNodeID());
            }
            return this;
        }

        public MapAssert<String,Object> data() {
            isNotNull();
            Map<String,Object> data = actual.getData();
            if (data == null) {
                failWithMessage("Expected data to be not null");
            }
            return new MapAssert<>(data);
        }

    }
}
