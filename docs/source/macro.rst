.. highlight:: java

Macro
=====
The on-board flash memory can also be used to store MetaWear commands instead of sensor data.  Recorded commands can be executed any time after being 
programmed with the `Macro <https://mbientlab.com/docs/metawear/android/latest/com/mbientlab/metawear/module/Macro.html>`_ module.  

::

    import com.mbientlab.metawear.module.Macro;

    final Macro macro = board.getModule(Macro.class);

Recording Commands
------------------
To record commands:

1. Call `startRecord <https://mbientlab.com/docs/metawear/android/latest/com/mbientlab/metawear/module/Macro.html#startRecord-->`_ to put the API in macro mode  
2. Use the MetaWear commands that you want programmed  
3. Exit macro mode with `endRecordAsync <https://mbientlab.com/docs/metawear/android/latest/com/mbientlab/metawear/module/Macro.html#endRecordAsync-->`_  

Macros can be set to run on boot by calling ``startRecord`` with ``true``.

::

    import com.mbientlab.metawear.module.Led;

    final Led led = mwBoard.getModule(Led.class);

    macro.startRecord(true);
    led.editPattern(Led.Color.GREEN, Led.PatternPreset.SOLID)
            .commit();
    led.play();
    macro.endRecordAsync().continueWith(new Continuation<Byte, Void>() {
        @Override
        public Void then(Task<Byte> task) throws Exception {
            Log.i("MainActivity", "Macro ID = " + task.getResult());
            return null;
        }
    });

Erasing Macros
--------------
Erasing macros is done with the `eraseAll <https://mbientlab.com/docs/metawear/android/latest/com/mbientlab/metawear/module/Macro.html#eraseAll-->`_ method.  
The erase operation will not occur until you disconnect from the board.

::

    macro.eraseMacros();
    board.getModule(Debug.class).disconnectAsync();

