package com.mbientlab.metawear.impl.platform;

import static com.mbientlab.metawear.Executors.IMMEDIATE_EXECUTOR;

import com.google.android.gms.tasks.CancellationTokenSource;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.TaskCompletionSource;

import java.util.ArrayList;
import java.util.concurrent.TimeoutException;

/**
 * Created by eric on 12/8/17.
 */
public class TimedTask<T> {
    private TaskCompletionSource<T> taskSource;
    private CancellationTokenSource cts;

    public TimedTask() { }

    public Task<T> execute(String msgFormat, long timeout, Runnable action) {
        if (taskSource != null && !taskSource.getTask().isComplete()) {
            return taskSource.getTask();
        }

        cts = new CancellationTokenSource();
        taskSource = new TaskCompletionSource<>();
        action.run();

        if (timeout != 0) {
            final ArrayList<Task<?>> tasks = new ArrayList<>();
            tasks.add(taskSource.getTask());
            tasks.add(TaskHelper.delay(timeout, cts.getToken()));

            TaskHelper.whenAny(tasks).continueWith(IMMEDIATE_EXECUTOR, task -> {
                if (task.getResult() != tasks.get(0)) {
                    setError(new TimeoutException(String.format(msgFormat, timeout)));
                } else {
                    cts.cancel();
                }
                return null;
            });
        }
        return taskSource.getTask();
    }

    public boolean isCompleted() {
        return taskSource != null && taskSource.getTask().isComplete();
    }

    public void cancel() {
        cts.cancel();
    }

    public void setResult(T result) {
        taskSource.trySetResult(result);
    }

    public void setError(Exception error) {
        taskSource.setException(error);
    }
}