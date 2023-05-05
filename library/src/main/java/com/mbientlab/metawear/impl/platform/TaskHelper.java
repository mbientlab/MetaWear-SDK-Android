package com.mbientlab.metawear.impl.platform;

import static com.mbientlab.metawear.Executors.IMMEDIATE_EXECUTOR;
import static com.mbientlab.metawear.Executors.SCHEDULED_EXECUTOR;

import androidx.annotation.NonNull;

import com.google.android.gms.tasks.CancellationToken;
import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.SuccessContinuation;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.TaskCompletionSource;
import com.google.android.gms.tasks.Tasks;
import com.mbientlab.metawear.Capture;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.Executor;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

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

        final TaskCompletionSource<Void> tcs = cancellationToken == null ? new TaskCompletionSource<>() : new TaskCompletionSource<>(cancellationToken);
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
            ((Task<Object>) task).continueWith(IMMEDIATE_EXECUTOR, new Continuation<Object, Void>() {
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

    public static <TResult> Task<TResult> callInBackground(Callable<TResult> callable) {
        return call(callable, SCHEDULED_EXECUTOR, null);
    }

    public static <TResult> Task<TResult> call(final Callable<TResult> callable, Executor executor,
                                               final CancellationToken ct) {
        final TaskCompletionSource<TResult> tcs = ct == null ? new TaskCompletionSource<>() : new TaskCompletionSource<>(ct);
        try {
            executor.execute(new Runnable() {
                @Override
                public void run() {
                    if (ct != null && ct.isCancellationRequested()) {
                        return;
                    }

                    try {
                        tcs.setResult(callable.call());
                    } catch (CancellationException e) {
                        tcs.setException(e);
                    } catch (Exception e) {
                        tcs.setException(e);
                    }
                }
            });
        } catch (Exception e) {
            tcs.setException(e);
        }

        return tcs.getTask();
    }

    public static Task<Void> whenAll(Collection<? extends Task<?>> tasks) {
        if (tasks.size() == 0) {
            return Tasks.forResult(null);
        }

        final TaskCompletionSource<Void> allFinished = new TaskCompletionSource<>();
        final ArrayList<Exception> causes = new ArrayList<>();
        final Object errorLock = new Object();
        final AtomicInteger count = new AtomicInteger(tasks.size());
        final AtomicBoolean isCancelled = new AtomicBoolean(false);

        for (Task<?> task : tasks) {
            @SuppressWarnings("unchecked")
            Task<Object> t = (Task<Object>) task;
            t.continueWith(new Continuation<Object, Void>() {
                @Override
                public Void then(Task<Object> task) {
                    if (task.getException() != null) {
                        synchronized (errorLock) {
                            causes.add(task.getException());
                        }
                    }

                    if (task.isCanceled()) {
                        isCancelled.set(true);
                    }

                    if (count.decrementAndGet() == 0) {
                        if (causes.size() != 0) {
                            if (causes.size() == 1) {
                                allFinished.setException(causes.get(0));
                            } else {
                                Exception error = new AggregateException(
                                        String.format("There were %d exceptions.", causes.size()),
                                        causes);
                                allFinished.setException(error);
                            }
                        } else if (isCancelled.get()) {
                            allFinished.setException(new CancellationException());
                        } else {
                            allFinished.setResult(null);
                        }
                    }
                    return null;
                }
            });
        }

        return allFinished.getTask();
    }

    public static <TResult> Task<List<TResult>> whenAllResult(final Collection<? extends Task<TResult>> tasks) {
        return whenAll(tasks).onSuccessTask(new SuccessContinuation<Void, List<TResult>>() {
            @NonNull
            @Override
            public Task<List<TResult>> then(Void unused) throws Exception {
                if (tasks.size() == 0) {
                    List<TResult> emptyList = new ArrayList<>();
                    return Tasks.forResult(emptyList);
                }

                List<TResult> results = new ArrayList<>();
                for (Task<TResult> individualTask : tasks) {
                    results.add(individualTask.getResult());
                }
                return Tasks.forResult(results);
            }
        });
    }
}
