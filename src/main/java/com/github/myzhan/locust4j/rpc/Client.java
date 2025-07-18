package com.github.myzhan.locust4j.rpc;

import java.io.IOException;
import java.util.function.UnaryOperator;

import com.github.myzhan.locust4j.message.Message;

/**
 * RPC Client interface.
 *
 * @author myzhan
 */
public interface Client {

    /**
     * receive message from master
     *
     * @return Message
     * @throws IOException network IO exception
     */
    Message recv() throws IOException;

    /**
     * send message to master
     *
     * @param message msgpack message sent to the master
     * @throws IOException network IO exception
     */
    void send(Message message) throws IOException;

    default void send(UnaryOperator<Message.Builder> spec) throws IOException{
        var msg = Message.create(spec);
        send(msg);
    }

    /**
     * close client
     */
    void close();

}