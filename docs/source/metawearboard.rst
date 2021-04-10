.. highlight:: java

MetaWearBoard
=============
The `MetaWearBoard <https://mbientlab.com/docs/metawear/android/latest/com/mbientlab/metawear/MetaWearBoard.html>`_ interface is a software representation 
of the MetaWear boards and is the central class of the MetaWear API.

Bluetooth LE Connection
-----------------------
Before using any API features, you must first connect to the board with 
`connectAsync <https://mbientlab.com/docs/metawear/android/latest/com/mbientlab/metawear/MetaWearBoard.html#connectAsync-->`_.  The returned task will 
finish when a connection has been established and the ``MetaWearBoard`` state has been initialized.  ::

    board.connectAsync().continueWith(new Continuation<Void, Void>() {
        @Override
        public Void then(Task<Void> task) throws Exception {
            if (task.isFaulted()) {
                Log.i("MainActivity", "Failed to connect");
            } else {
                Log.i("MainActivity", "Connected");
            }
            return null;
        }
    });

Conversely, call
`disconnectAsync <https://mbientlab.com/docs/metawear/android/latest/com/mbientlab/metawear/MetaWearBoard.html#disconnectAsync-->`_ to close the connection.  
If there is a pending ``connectAsync`` task when ``disconnectAsync`` is called, the connect task will be cancelled.  ::

    board.disconnectAsync().continueWith(new Continuation<Void, Void>() {
        @Override
        public Void then(Task<Void> task) throws Exception {
            Log.i("MainActivity", "Disconnected");
            return null;
        }
    });

Unexpected Disconnects 
^^^^^^^^^^^^^^^^^^^^^^
Sometimes the BLE connection will unexepectedly drop i.e. the BLE connection was not closed by the API.  Call 
`onUnexpectedDisconnect <https://mbientlab.com/docs/metawear/android/latest/com/mbientlab/metawear/MetaWearBoard.html#onUnexpectedDisconnect-com.mbientlab.metawear.MetaWearBoard.UnexpectedDisconnectHandler->`_ 
to register a callback function to handle such events.  ::

    board.onUnexpectedDisconnect(new MetaWearBoard.UnexpectedDisconnectHandler() {
        @Override
        public void disconnected(int status) {
            Log.i("MainActivity", "Unexpectedly lost connection: " + status);
        }
    });

Model
-----
Despite the name, the ``MetaWearBoard`` interface communicates with all MetaSensor boards, not just MetaWear boards.  Because of this, the interface 
provides a `getModel <https://mbientlab.com/docs/metawear/android/latest/com/mbientlab/metawear/MetaWearBoard.html#getModel-->`_ method that determines 
exactly which board the interface is currently connected to.

::

    Log.i("MainActivity", "board model = " + board.getModel());

BLE Information
---------------
RSSI and some GATT characetristics (battery level, device information) can be read from the MetaWearBoard interface using 
`readRssiAsync <https://mbientlab.com/docs/metawear/android/latest/com/mbientlab/metawear/MetaWearBoard.html#readRssiAsync-->`_, 
`readBatteryLevelAsync <https://mbientlab.com/docs/metawear/android/latest/com/mbientlab/metawear/MetaWearBoard.html#readBatteryLevelAsync-->`_, 
and `readDeviceInformationAsync <https://mbientlab.com/docs/metawear/android/latest/com/mbientlab/metawear/MetaWearBoard.html#readDeviceInformationAsync-->`_ 
respectively.  ::

    board.readDeviceInformationAsync()
        .continueWith(new Continuation<DeviceInformation, Void>() {
            @Override
            public Void then(Task<DeviceInformation> task) throws Exception {
                Log.i("MainActivity", "Device Information: " + task.getResult());
                return null;
            }
        });

Modules
-------
MetaWear modules, represented by the `Module <https://mbientlab.com/docs/metawear/android/latest/com/mbientlab/metawear/MetaWearBoard.Module.html>`_ 
interface, are sensors, peripherals, or on-board firmware features.  To interact with the underlying MetaWear modules, retrieve a reference to the 
desired interface with the 
`getModule <https://mbientlab.com/docs/metawear/android/latest/com/mbientlab/metawear/MetaWearBoard.html#getModule-java.lang.Class->`_ method.  A null 
pointer will be returned if any of the following conditions are true:

* Requested module is not supported on the board  
* Board is in MetaBoot mode  
* Has not yet connected  

A variant function, 
`getModuleOrThrow <https://mbientlab.com/docs/metawear/android/latest/com/mbientlab/metawear/MetaWearBoard.html#getModuleOrThrow-java.lang.Class->`_ will 
throw a checked exception in lieu of returning null.  ::

    import com.mbientlab.metawear.module.Led;

    Led led;
    if ((led= board.getModule(Led.class)) != null) {
        led.editPattern(Led.Color.BLUE, Led.PatternPreset.BLINK)
                .repeatCount((byte) 10)
                .commit();
        led.play();
    }

Tear Down
---------
When you are done using your board, call `tearDown <https://mbientlab.com/docs/metawear/android/latest/com/mbientlab/metawear/MetaWearBoard.html#tearDown-->`_ 
to remove resources allocated by the firmware and API such as routes, loggers, and data processors.  this method does not reset the board so any 
configuration changes are preserved.
