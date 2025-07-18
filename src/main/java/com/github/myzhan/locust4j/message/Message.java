package com.github.myzhan.locust4j.message;

import java.io.IOException;
import java.util.*;
import java.util.function.UnaryOperator;

import com.github.myzhan.locust4j.utils.ImmutableStyle;
import org.immutables.value.Value;
import org.msgpack.core.*;

/**
 * @author vrajat
 */
@ImmutableStyle
@Value.Immutable
public abstract class Message {

    public static class Builder extends MessageImpl.Builder {
        public Builder from(byte[] bytes) {
            try (MessageUnpacker unpacker = MessagePack.newDefaultUnpacker(bytes)) {
                int arrayHeader = unpacker.unpackArrayHeader();
                type(unpacker.unpackString());
                // unpack data
                if (unpacker.getNextFormat() != MessageFormat.NIL) {
                    data(Message.unpackMap(unpacker));
                } else {
                    unpacker.unpackNil();
                }
                if (unpacker.getNextFormat() != MessageFormat.NIL) {
                    nodeId(unpacker.unpackString());
                } else {
                    unpacker.unpackNil();
                }
            } catch (IOException ioe) {
                throw new RuntimeException("Failed to create message from byte array", ioe);
            }
            return this;
        }
    }

    public static Message from(byte[] bytes) { return create(s -> s.from(bytes)); }
    public static Message create(UnaryOperator<Builder> spec) { return spec.apply(builder()).build(); }
    public static Message createMessage(UnaryOperator<Builder> spec) { return create(spec); }
    public static Builder builder(UnaryOperator<Builder> spec) { return spec.apply(builder()); }
    public static Builder builder() { return new Builder(); }

    private static final String TYPE_CLIENT_READY = "client_ready";

    public abstract String type();

    public abstract Map<String, Object> data();

    public abstract Optional<String> nodeId();

    @Value.Default
    public int version() {
        return -1;
    }

    @Value.Lazy
    public byte[] bytes() throws IOException {

        MessageBufferPacker packer = MessagePack.newDefaultBufferPacker();
        // a message contains three fields, (type & data & nodeID)
        packer.packArrayHeader(3);

        // pack the first field
        packer.packString(type());

        // pack the second field
        if (Message.TYPE_CLIENT_READY.equals(type())) {
            if (version() == -1) {
                packer.packInt(version());
            } else {
                packer.packNil();
            }
        } else {
            if (data() != null) {
                packer.packMapHeader(data().size());
                for (Map.Entry<String, Object> entry : data().entrySet()) {
                    packer.packString(entry.getKey());
                    visit(packer, entry.getValue());
                }
            } else {
                packer.packNil();
            }
        }

        // pack the third field
        packer.packString(nodeId().get());
        byte[] bytes = packer.toByteArray();
        packer.close();
        return bytes;
    }

    @Override
    @Value.Lazy
    public String toString() {
        return String.format("%s-%s-%s", nodeId().orElse("nodeId?"), type(), data());
    }

    private static Map<String, Object> unpackMap(MessageUnpacker unpacker) throws IOException {
        int mapSize = unpacker.unpackMapHeader();
        Map<String, Object> result = new HashMap<>(6);
        while (mapSize > 0) {
            String key = null;
            // unpack key
            if (unpacker.getNextFormat() == MessageFormat.NIL) {
                unpacker.unpackNil();
            } else {
                key = unpacker.unpackString();
            }
            // unpack value
            MessageFormat messageFormat = unpacker.getNextFormat();
            Object value =

                switch (messageFormat.getValueType()) {
                    case BOOLEAN -> unpacker.unpackBoolean();
                    case FLOAT   -> unpacker.unpackFloat();
                    case INTEGER -> unpacker.unpackInt();
                    case STRING  -> unpacker.unpackString();
                    case MAP     -> unpackMap(unpacker);
                    case NIL     -> {
                        unpacker.unpackNil();
                        yield null;
                    }
                    case ARRAY -> {
                        int size = unpacker.unpackArrayHeader();
                        var val = new ArrayList<>(size);
                        for (int index = 0; index < size; ++index) {
                            val.add(unpacker.unpackString());
                        }
                        yield val;
                    }
                    default -> throw new IOException("Message received unsupported type: " + messageFormat.getValueType());
                };
            if (null != key) {
                result.put(key, value);
            }
            mapSize--;
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    static void visit(MessagePacker packer, Object value) throws IOException {
        switch (value) {
            case null                   -> packer.packNil();
            case String s               -> packer.packString(s);
            case Integer i              -> packer.packInt(i);
            case Long l                 -> packer.packLong(l);
            case Boolean b              -> packer.packBoolean(b);
            case Float v                -> packer.packFloat(v);
            case Double v               -> packer.packDouble(v);
            case Map<?,?> m -> {
                var map = (Map<String,Object>)m;
                packer.packMapHeader(map.size());
                for (Map.Entry<String, Object> entry : map.entrySet()) {
                    packer.packString(entry.getKey());
                    visit(packer, entry.getValue());
                }
            }
            case List<?> l      -> {
                var list = (List<Object>)l;
                packer.packArrayHeader(list.size());
                for (Object object : list) {
                    visit(packer, object);
                }
            }
            case LongIntMap longIntMap  -> {
                packer.packMapHeader(longIntMap.internalStore.size());
                for (Map.Entry<Long, Integer> entry : longIntMap.internalStore.entrySet()) {
                    packer.packLong(entry.getKey());
                    packer.packInt(entry.getValue());
                }
            }
            default ->
                throw new IOException("Cannot pack type unknown type:" + value.getClass().getSimpleName());
        }
    }

}