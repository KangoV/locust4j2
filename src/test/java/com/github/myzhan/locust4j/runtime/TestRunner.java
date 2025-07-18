package com.github.myzhan.locust4j.runtime;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import com.github.myzhan.locust4j.AbstractTask;
import com.github.myzhan.locust4j.LocustTestHelper;
import com.github.myzhan.locust4j.message.Message;
import com.github.myzhan.locust4j.stats.Stats;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InOrder;
import org.mockito.Mockito;

import static com.github.myzhan.locust4j.test.MessageAssert.assertThat;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

/**
 * @author myzhan
 */
public class TestRunner {

    private static final Logger logger = LoggerFactory.getLogger(TestRunner.class);

    private Runner runner;

    private MockRPCClient client;

    @Before
    public void before() {
        runner = new Runner();
        runner.setStats(new Stats());
        runner.setTasks(Collections.singletonList((AbstractTask) new TestTask()));
        client = new MockRPCClient();
        runner.setRPCClient(client);
        LocustTestHelper.setLocustRunner(runner);
    }

    private static class TestTask extends AbstractTask {

        private final int weight = 1;
        private final String name = "test";

        public TestTask() {
        }

        @Override
        public int getWeight() {
            return this.weight;
        }

        @Override
        public String getName() {
            return this.name;
        }

        @Override
        public void execute() {
            try {
                Thread.sleep(10);
            } catch (Exception ex) {
                logger.error(ex.getMessage());
            }
        }
    }

    @Test
    public void TestStartSpawning() {
        runner.startSpawning(10);
        assertEquals(10, runner.numClients);
        runner.stop();
    }

    @Test
    public void TestOnInvalidSpawnMessage() {
        runner.getReady();

        // send spawn message
        client.getFromServerQueue().offer(Message.create(s -> s
            .type("spawn")
            .putData("no_user_classes_count", 0f)
            .nodeId("test")));

        try {
            Thread.sleep(10);
        } catch (InterruptedException ex) {
            logger.error(ex.getMessage());
            return;
        }

        assertEquals(RunnerState.Ready, runner.getState());
        runner.quit();
    }

    @Test
    public void TestOnMessage() throws Exception {
        runner.setHeartbeatStopped(true);

        runner.getReady();

        Map<String, Object> spawnData = new HashMap<>();
        Map<String, Integer> userClassesCount = new HashMap<String, Integer>(1);
        userClassesCount.put("dummy", 1);
        spawnData.put("user_classes_count", userClassesCount);
        spawnData.put("host", "www.github.com");

        // send spawn message
        client.getFromServerQueue().offer(Message.create(s -> s
            .type("spawn")
            .putAllData(spawnData)
        ));

        Message spawning = client.getToServerQueue().take();
        assertEquals("spawning", spawning.type());
        assertThat(spawning.data()).isEmpty();
        assertThat(spawning).hasNodeId(runner.nodeID);

        // wait for spawn complete
        Thread.sleep(100);

        // check remote params
        Assert.assertEquals(userClassesCount, runner.getRemoteParams().get("user_classes_count"));
        Assert.assertEquals("www.github.com", runner.getRemoteParams().get("host"));

        Message spawnComplete = client.getToServerQueue().take();
        assertEquals("spawning_complete", spawnComplete.type());
        assertEquals(1, spawnComplete.data().get("count"));
        assertThat(spawnComplete).hasNodeId(runner.nodeID);

        // send stop message
        client.getFromServerQueue().offer(Message.create(s -> s.type("stop")));
        Message clientStopped = client.getToServerQueue().take();
        assertEquals("client_stopped", clientStopped.type());
        assertThat(clientStopped.data()).isEmpty();
        assertThat(clientStopped).hasNodeId(runner.nodeID);

        // send spawn message again
        client.getFromServerQueue().offer(Message.create(s -> s.type("spawn").putAllData(spawnData)));

        Message spawningAgain = client.getToServerQueue().take();
        assertEquals("spawning", spawningAgain.type());
        assertThat(spawningAgain.data()).isEmpty();
        assertThat(spawningAgain).hasNodeId(runner.nodeID);

        runner.quit();
    }

    @Test
    public void TestSendHeartbeat() throws Exception {
        runner.getReady();

        Message heartbeat = client.getToServerQueue().take();
        assertEquals("heartbeat", heartbeat.type());
        assertNotNull(heartbeat.data().get("current_cpu_usage"));
        assertNotNull(heartbeat.data().get("state"));

        runner.quit();
    }


    @Test
    public void TestSpawningWorkersScaleOut() throws Exception {
        runner.setHeartbeatStopped(true);
        runner.getReady();

        ThreadPoolExecutor threadPoolExecutor = new ThreadPoolExecutor(1, 1, 0L, TimeUnit.MILLISECONDS,
                new LinkedBlockingQueue<Runnable>(),
                new ThreadFactory() {
                    @Override
                    public Thread newThread(Runnable r) {
                        Thread thread = new Thread(r);
                        return thread;
                    }
                });
        ThreadPoolExecutor spyThreadPoolExecutor = Mockito.spy(threadPoolExecutor);
        runner.setTaskExecutor(spyThreadPoolExecutor);

        this.sendSpawnMessage(2);

        // Verify pool size is set correctly. Order is important.
        InOrder orderVerifier = Mockito.inOrder(spyThreadPoolExecutor);
        orderVerifier.verify(spyThreadPoolExecutor).setMaximumPoolSize(2);
        orderVerifier.verify(spyThreadPoolExecutor).setCorePoolSize(2);

        assertEquals(2, runner.numClients);

        // Scale in worker
        this.sendSpawnMessage(3);

        // Verify pool size is set correctly. Order is important.
        orderVerifier.verify(spyThreadPoolExecutor).setMaximumPoolSize(3);
        orderVerifier.verify(spyThreadPoolExecutor).setCorePoolSize(3);

        // Verify that only 3 tasks were submitted in total
        verify(spyThreadPoolExecutor, times(3)).submit(isA(AbstractTask.class));

        assertEquals(3, runner.numClients);

        runner.quit();
    }

    @Test
    public void TestSpawningWorkersScaleIn() throws Exception {
        runner.setHeartbeatStopped(true);
        runner.getReady();

        ThreadPoolExecutor threadPoolExecutor = new ThreadPoolExecutor(1, 1, 0L, TimeUnit.MILLISECONDS,
                new LinkedBlockingQueue<Runnable>(),
                new ThreadFactory() {
                    @Override
                    public Thread newThread(Runnable r) {
                        Thread thread = new Thread(r);
                        return thread;
                    }
                });
        ThreadPoolExecutor spyThreadPoolExecutor = Mockito.spy(threadPoolExecutor);
        runner.setTaskExecutor(spyThreadPoolExecutor);

        this.sendSpawnMessage(3);

        // Verify pool size is set correctly. Order is important.
        InOrder orderVerifier = Mockito.inOrder(spyThreadPoolExecutor);
        orderVerifier.verify(spyThreadPoolExecutor).setMaximumPoolSize(3);
        orderVerifier.verify(spyThreadPoolExecutor).setCorePoolSize(3);

        assertEquals(3, runner.numClients);

        // Scale in worker
        this.sendSpawnMessage(2);

        // Verify pool size is set correctly. Order is important.
        orderVerifier.verify(spyThreadPoolExecutor).setCorePoolSize(2);
        orderVerifier.verify(spyThreadPoolExecutor).setMaximumPoolSize(2);

        // Verify that only 3 tasks were submitted in total
        verify(spyThreadPoolExecutor, times(3)).submit(isA(AbstractTask.class));

        assertEquals(2, runner.numClients);

        runner.quit();
    }

    private void sendSpawnMessage(int usercount) throws Exception {
        Map<String, Object> spawnData = new HashMap<>();
        Map<String, Integer> userClassesCount = new HashMap<String, Integer>(1);
        userClassesCount.put("dummy", usercount);
        spawnData.put("user_classes_count", userClassesCount);
        spawnData.put("host", "www.github.com");

        // send spawn message
        client.getFromServerQueue().offer(Message.create(s -> s.type("spawn").putAllData(spawnData)));

        // wait for spawn complete
        Thread.sleep(100);
    }
}
