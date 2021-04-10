.. highlight:: java

Timer
=====
The `Timer <https://mbientlab.com/docs/metawear/android/latest/com/mbientlab/metawear/module/Timer.html>`_ module schedules tasks to be executed periodically 
on the board.  Unlike using a ``Timer`` or ``Handler`` from the Android side, the MetaWear timer is only used with MetaWear commands and solely exists 
on they board.

::

    import com.mbientlab.metawear.module.Timer;

    final Timer timer = board.getModule(Timer.class);

Scheduling a Task
-----------------
Use 
`scheduleAsync <https://mbientlab.com/docs/metawear/android/latest/com/mbientlab/metawear/module/Timer.html#scheduleAsync-int-boolean-com.mbientlab.metawear.CodeBlock->`_ to schedule tasks to be run in the future.  Typically this is used with the forced data producer's 
`read <https://mbientlab.com/docs/metawear/android/latest/com/mbientlab/metawear/ForcedDataProducer.html#read-->`_ function to have the board periodically 
request data from that producer.

::

    import com.mbientlab.metawear.module.Gpio;

    public Task<ScheduledTask> scheduleRead(final ForcedDataProducer producer) {
        // send a read command to the dadta producer every 30 seconds, start immediately
        return timer.scheduleAsync(30000, false, new CodeBlock() {
            producer.read();
        });
    }

Task Management
---------------
After scheduling a task, use the created 
`ScheduledTask <https://mbientlab.com/docs/metawear/android/latest/com/mbientlab/metawear/module/Timer.ScheduledTask.html>`_ object to 
`start <https://mbientlab.com/docs/metawear/android/latest/com/mbientlab/metawear/module/Timer.ScheduledTask.html#start-->`_, 
`stop <https://mbientlab.com/docs/metawear/android/latest/com/mbientlab/metawear/module/Timer.ScheduledTask.html#stop-->`_, and 
`remove <https://mbientlab.com/docs/metawear/android/latest/com/mbientlab/metawear/module/Timer.ScheduledTask.html#remove-->`_ the 
task.  Furthermore, all ``ScheduledTask`` objects have a unique numerical value that is used to retieve a previously created task at any time with 
`lookupScheduledTask <https://mbientlab.com/docs/metawear/android/latest/com/mbientlab/metawear/module/Timer.html#lookupScheduledTask-byte->`_.

::
    
    ScheduledTask mwTask;

    // lookup a task with id = 0
    if ((mwTask = timer.lookupScheduledTask((byte) 0) != null) {
        // start the task
        mwTask.start();

        // stop the task
        mwTask.stop();

        // remove the task, id 0 no longer valid id 
        mwTask.remove();
    }
