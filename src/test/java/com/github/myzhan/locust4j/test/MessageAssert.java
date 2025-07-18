package com.github.myzhan.locust4j.test;

import com.github.myzhan.locust4j.message.Message;
import org.assertj.core.api.AbstractAssert;
import org.assertj.core.api.MapAssert;

import java.util.Objects;

public class MessageAssert extends AbstractAssert<MessageAssert, Message> {

    public MessageAssert(Message actual) {
        super(actual, MessageAssert.class);
    }

    public static MessageAssert assertThat(Message actual) {
        return new MessageAssert(actual);
    }

    public MessageAssert hasType(String type) {
        isNotNull();
        // check condition
        if (!Objects.equals(actual.type(), type)) {
            failWithMessage("Expected messages's type to be <%s> but was <%s>", type, actual.type());
        }
        return this;
    }

    public MessageAssert hasNodeId(String nodeId) {
        isNotNull();
        // check condition
        if (actual.nodeId().isEmpty()) {
            failWithMessage("Expected messages's nodeId to be <%s> but was empty", nodeId);
        }
        if (!Objects.equals(actual.nodeId().get(), nodeId)) {
            failWithMessage("Expected messages's nodeId to be <%s> but was <%s>", nodeId, actual.nodeId());
        }
        return this;
    }

    public MapAssert<String, Object> data() {
        isNotNull();
        var data = actual.data();
        if (data == null) failWithMessage("Expected data to be not null");
        return new MapAssert<>(data);
    }

    public MessageAssert hasNoData() {
        isNotNull();
        if (!actual.data().isEmpty()) failWithMessage("Expected data to be empty");
        return this;
    }

}
