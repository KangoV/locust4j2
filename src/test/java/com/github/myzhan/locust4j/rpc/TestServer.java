package com.github.myzhan.locust4j.rpc;

import java.io.IOException;
import java.util.Arrays;

import com.github.myzhan.locust4j.message.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zeromq.ZContext;
import org.zeromq.ZMQ;
import org.zeromq.ZMQException;

/**
 * @author myzhan
 */
public class TestServer {

    private static final Logger logger = LoggerFactory.getLogger(TestServer.class);

    private final ZContext context;
    private final String bindHost;
    private final int bindPort;
    private ZMQ.Socket routerSocket;

    private Thread serverThread;

    public TestServer(String bindHost, int bindPort) {
        this.context = new ZContext();
        this.bindHost = bindHost;
        this.bindPort = bindPort;
    }

    public TestServer start() {
        routerSocket = context.createSocket(ZMQ.ROUTER);
        routerSocket.bind(String.format("tcp://%s:%d", bindHost, bindPort));

        serverThread = new Thread(() -> {
            try {
                while (true) {
                    byte[] packet = routerSocket.recv();
                    if (Arrays.equals(packet, "testClient".getBytes())) {
                        routerSocket.sendMore(packet);
                        continue;
                    }
                    var message = MessageDeser.deserialise(packet);
                    routerSocket.send(MessageDeser.serialise(message), 0);
                }
            } catch (ZMQException ex) {
                // ignore ZMQException, it may be interrupted.
            } catch (IOException ex) {
                logger.error(ex.getMessage());
            }
        });

        serverThread.start();
        return this;
    }

    public void stop() {
        serverThread.interrupt();
        routerSocket.close();
        context.close();
    }
}
