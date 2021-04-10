Automated Reconnect
^^^^^^^^^^^^^^^^^^^
If the `connectAsync` task fails or connection is lost, you can reconnect by calling `connectAsync` until it succeeds.  retry the 
You can retry connect attempts, even 
If your app needs to maintain an active connection, you can continually try the `connectAsync` function until it succeeds.  ::

    public static Task<Void> reconnect(final MetaWearBoard board) {
        return board.connectAsync().continueWithTask(new Continuation<Void, Task<Void>>() {
            @Override
            public Task<Void> then(Task<Void> task) throws Exception {
                if (task.isFaulted()) {
                    return reconnect(board);
                }
                return task;
            }
        });
    }
::
    public static Task<Void> reconnect(final MetaWearBoard board) {
        return board.connectAsync().continueWithTask(task -> task.isFaulted() ? reconnect(board) : task);
    }


