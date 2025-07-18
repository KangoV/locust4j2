package com.github.myzhan.locust4j.taskset;

import java.util.ArrayList;
import java.util.List;

import com.github.myzhan.locust4j.AbstractTask;

/**
 * @author myzhan
 * @since 1.0.3
 *
 * TaskSet is an experimental feature, the API is not stabilized.
 * It needs to be more considered and tested.
 */
public abstract class AbstractTaskSet extends AbstractTask {

    private List<AbstractTask> tasks;

    public AbstractTaskSet() {
        this.tasks = new ArrayList<>();
    }

    /**
     * Add a task to the task set.
     *
     * @param task test task that runs in a task set
     */
    public void addTask(AbstractTask task) {
        tasks.add(task);
    }

}
