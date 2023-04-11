package com.mbientlab.metawear.impl.platform;

import static com.mbientlab.metawear.Executors.SCHEDULED_EXECUTOR;

import com.google.android.gms.tasks.CancellationToken;
import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.SuccessContinuation;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.TaskCompletionSource;
import com.google.android.gms.tasks.Tasks;
import com.mbientlab.metawear.Capture;

import java.util.Collection;
import java.util.concurrent.Callable;
import java.util.concurrent.Executor;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class TaskHelper {
    public static Task<Void> continueWhile(final Callable<Boolean> predicate,
                                    final SuccessContinuation<Void, Void> continuation, final Executor executor,
                                    final CancellationToken ct) {
        final Capture<SuccessContinuation<Void, Void>> predicateContinuation =
                new Capture<>();
        predicateContinuation.set(new SuccessContinuation<Void, Void>() {

            @Override
            public Task<Void> then(Void task) throws Exception {
                if (ct != null && ct.isCancellationRequested()) {
                    return Tasks.forCanceled();
                }

                if (predicate.call()) {
                    return Tasks.<Void> forResult(null).onSuccessTask(executor, continuation)
                            .onSuccessTask(executor, predicateContinuation.get());
                }
                return Tasks.forResult(null);
            }
        });
        return Tasks.<Void>forResult(null).onSuccessTask(executor, predicateContinuation.get());
    }

    public static Task<Void> delay(long delay) {
        return delay(delay, SCHEDULED_EXECUTOR, null);
    }

    public static Task<Void> delay(long delay, CancellationToken ct) {
        return delay(delay, SCHEDULED_EXECUTOR, ct);
    }

    public static Task<Void> delay(long delay, ScheduledExecutorService executor, final CancellationToken cancellationToken) {
        if (cancellationToken != null && cancellationToken.isCancellationRequested()) {
            return Tasks.forCanceled();
        }

        if (delay <= 0) {
            return Tasks.forResult(null);
        }

        final TaskCompletionSource<Void> tcs = new TaskCompletionSource<>(cancellationToken);
        final ScheduledFuture<?> scheduled = executor.schedule(new Runnable() {
            @Override
            public void run() {
                tcs.trySetResult(null);
            }
        }, delay, TimeUnit.MILLISECONDS);

        if (cancellationToken != null) {
            cancellationToken.onCanceledRequested(() -> {
                scheduled.cancel(true);
            });
        }

        return tcs.getTask();
    }

    public static Task<Task<?>> whenAny(Collection<? extends Task<?>> tasks) {
        if (tasks.size() == 0) {
            return Tasks.forResult(null);
        }

        final TaskCompletionSource<Task<?>> firstCompleted = new TaskCompletionSource<>();
        final AtomicBoolean isAnyTaskComplete = new AtomicBoolean(false);

        for (Task<?> task : tasks) {
            ((Task<Object>) task).continueWith(new Continuation<Object, Void>() {
                @Override
                public Void then(Task<Object> task) {
                    if (isAnyTaskComplete.compareAndSet(false, true)) {
                        firstCompleted.setResult(task);
                    } else {
                        Throwable ensureObserved = task.getException();
                    }
                    return null;
                }
            });
        }
        return firstCompleted.getTask();
    }
}
