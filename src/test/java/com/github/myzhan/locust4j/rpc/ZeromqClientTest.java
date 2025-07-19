package com.github.myzhan.locust4j.rpc;

import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

import com.github.myzhan.locust4j.message.Message;
import com.github.myzhan.locust4j.test.MessageAssert;
import org.junit.jupiter.api.Test;

/**
 * @author myzhan
 */
public class ZeromqClientTest {

    @Test
    public void TestPingPong() throws Exception {
        // randomized the port to avoid conflicts
        var masterPort = ThreadLocalRandom.current().nextInt(1000) + 1024;
        var server = new TestServer("0.0.0.0", masterPort).start();
        var client = new ZeromqClient("0.0.0.0", masterPort, "testClient");
        var data = Map.of("hello", "world");

        client.send(Message.create(s -> s.type("test").putAllData(data).nodeId("node")));
        var message = client.recv();

        MessageAssert.assertThat(message)
            .hasType("test")
            .hasNodeId("node")
            .data()
            .containsAllEntriesOf(data);

        Thread.sleep(100);
        server.stop();
        client.close();
    }
}
