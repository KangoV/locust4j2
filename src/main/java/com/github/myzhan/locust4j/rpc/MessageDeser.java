package com.github.myzhan.locust4j.rpc;

import com.github.myzhan.locust4j.message.LongIntMap;
import com.github.myzhan.locust4j.message.Message;
import com.github.myzhan.locust4j.message.Null;
import org.msgpack.core.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

abstract class MessageDeser {

    private static final String TYPE_CLIENT_READY = "client_ready";

    private MessageDeser() {
        // cannot instantiate
    }

    static final Message copy(Message message) throws IOException {
        return deserialise(serialise(message));
    }

    static Message deserialise(byte[] bytes) {
        try (MessageUnpacker unpacker = MessagePack.newDefaultUnpacker(bytes)) {
            var builder = Message.builder();
            int arrayHeader = unpacker.unpackArrayHeader();
            builder.type(unpacker.unpackString());
            // unpack data
            if (unpacker.getNextFormat() != MessageFormat.NIL) {
                builder.data(unpackMap(unpacker));
            } else {
                unpacker.unpackNil();
            }
            if (unpacker.getNextFormat() != MessageFormat.NIL) {
                builder.nodeId(unpacker.unpackString());
            } else {
                unpacker.unpackNil();
            }
            return builder.build();
        } catch (IOException ioe) {
            throw new RuntimeException("Failed to create message from byte array", ioe);
        }
    }

    public static byte[] serialise(Message message) throws IOException {

        final var type = message.type();
        final var version = message.version();

        MessageBufferPacker packer = MessagePack.newDefaultBufferPacker();
        // a message contains three fields, (type & data & nodeID)
        packer.packArrayHeader(3);

        // pack the first field
        packer.packString(type);

        // pack the second field
        if (TYPE_CLIENT_READY.equals(type)) {
            if (version == -1) {
                packer.packInt(version);
            } else {
                packer.packNil();
            }
        } else {
            if (message.hasData()) {
                var data = message.data();
                packer.packMapHeader(data.size());
                for (var entry : data.entrySet()) {
                    packer.packString(entry.getKey());
                    visit(packer, entry.getValue());
                }
            } else {
                packer.packNil();
            }
        }

        // pack the third field
        packer.packString(message.nodeId().get());
        byte[] bytes = packer.toByteArray();
        packer.close();
        return bytes;
    }

    @SuppressWarnings("unchecked")
    static MessagePacker visit(MessagePacker packer, Object value) throws IOException {
        switch (value) {
            case Null n                 -> packer.packNil();
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
                var map = longIntMap.asMap();
                packer.packMapHeader(map.size());
                for (Map.Entry<Long, Integer> entry : map.entrySet()) {
                    packer.packLong(entry.getKey());
                    packer.packInt(entry.getValue());
                }
            }
            default ->
                throw new IOException("Cannot pack type unknown type:" + value.getClass().getSimpleName());
        }
        return packer;
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
                        yield Null.TYPE;
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

}
