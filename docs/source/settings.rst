.. highlight:: java

Settings
========
Developers can use the `Settings <https://mbientlab.com/docs/metawear/android/latest/com/mbientlab/metawear/module/Settings.html>`_ module to configure ble 
pereferences and retrieve general board information.  You can combine preference changes with the :doc:`macro` module to make them permanent.

::

    import com.mbientlab.metawear.module.Settings;

    final Settings settings = board.getModule(Settings.class);

Ble Advertising
---------------
Advertising parameters control how the Bluetooth LE radio sends its adversiting packets and are configured with the 
`BleAdvertisementConfigEditor <https://mbientlab.com/docs/metawear/android/latest/com/mbientlab/metawear/module/Settings.BleAdvertisementConfigEditor.html>`_ 
interface.  You can configure paramters such as the device name, timeout, tx power, and scan response.

::

    import com.mbientlab.metawear.module.Settings;
    
    settings.editBleAdConfig()
            .deviceName("AntiWare")
            .txPower((byte) -4)
            .interval((short) 1024)
            .timeout((byte) 100)
            .commit();

Check out these posts from the Nordic Developer Zone if you are unsure of what these parameters do:  

* https://devzone.nordicsemi.com/question/53348/advertising-interval-and-advertising-timeout/?answer=53385#post-id-53385  
* https://devzone.nordicsemi.com/question/39731/range-of-values-for-tx-power/?answer=39733#post-id-39733  
    
Connection Parameters
---------------------

Bluetooth LE connection parameters control how the ble devices communicate with each other.  Configuring thes parameters is done with the 
`BleConnectionParameterEditor <https://mbientlab.com/docs/metawear/android/latest/com/mbientlab/metawear/module/Settings.BleConnectionParametersEditor.html>`_ 
interface.  A more detailed explanation on connection parameters can be found on the Nordic Developer Zone:  

* https://devzone.nordicsemi.com/question/60/what-is-connection-parameters/  

::

    // change min conn interval to 10ms, 
    // max conn interval to 1024ms
    settings.editBleConnParams()
            .minConnectionInterval(100f)
            .maxConnectionInterval(1024f)
            .commit();

Battery State
-------------
Battery state is defined as a forced data producer and represented by the 
`BatteryState <https://mbientlab.com/docs/metawear/android/latest/com/mbientlab/metawear/module/Settings.BatteryState.html>`_ class.  Unlike reading the 
battery characteristc with the ``MetaWearBoard`` class, reading the battery state with the ``Settings`` module lets you use the battery data with the 
route API.

::

    settings.battery().addRouteAsync(new RouteBuilder() {
        @Override
        public void configure(RouteComponent source) {
            source.stream(new Subscriber() {
                @Override
                public void apply(Data data, Object ... env) {
                    Log.i("MainActivity", "battery state = " + data.value(BatteryState.class));
                }
            });
        }
    }).continueWith(new Continuation<Route, Void>() {
        @Override
        public Void then(Task<Route> task) throws Exception {
            settings.battery().read();
            return null;
        }
    });

Handling Disconnects
--------------------
The board can be programmed to react to a disconnect by registering an 
`Observer <https://mbientlab.com/docs/metawear/android/latest/com/mbientlab/metawear/Observer.html>`_ with the 
`onDisconnectAsync <https://mbientlab.com/docs/metawear/android/latest/com/mbientlab/metawear/module/Settings.html#onDisconnectAsync-com.mbientlab.metawear.CodeBlock->`_ function.

::

    final Led led = board.getModule(Led.class);

    settings.onDisconnectAsync(new CodeBlock() {
        @Override
        public void program() {
            led.editPattern(Color.RED, PatternPreset.SOLID)
                    .repeatCount((byte) 2)
                    .commit();
            led.play();
        }
    });


Power Status
------------
Firmware v1.3.2 exposes battery charging and power status notifications which provides information on when the battery is charging / not charging and 
when a power source is attached / removed, respectively.  The data is interpreted as a byte or boolean with 1 (true) signifying battery charging / 
power source attached, and 0 (false) meaning battery not charging / power source removed.  Not all boards support this feature and a null pointer will 
be returned if `powerStatus <https://mbientlab.com/documents/metawear/android/latest/com/mbientlab/metawear/module/Settings.html#powerStatus-->`_ or 
`chargeStatus <https://mbientlab.com/documents/metawear/android/latest/com/mbientlab/metawear/module/Settings.html#chargeStatus-->`_ is called on an 
unsupported board.

::

    settings.powerStatus().addRouteAsync(new RouteBuilder() {
        @Override
        public void configure(RouteComponent source) {
            source.stream(new Subscriber() {
                @Override
                public void apply(Data data, Object... env) {
                    Log.i("MainActivity", "power source state changed: " + data.value(Byte.class));
                }
            });
        }
    });

Because the power and charge statuses are active data producers, the module has provided the ``readCurrentChargeStatusAsync`` and 
``readCurrentPowerStatusAsync`` methods so your app can retrieve the current charge and power statuses respectively.  Unlike the normal ``read`` 
function, this variant eschews the data route system and sends the data directly to the Android device.

::

    settings.readCurrentPowerStatusAsync().continueWith(new Continuation<Byte, Void>() {
        @Override
        public Void then(Task<Byte> task) throws Exception {
            Log.i("MainActivity", "power source attached? " + task.getResult();
            return null;
        }
    });

MMS 3V Regulator
---------------------
The MMS (MetaMotion) board has a 3V regulator that can be turned on and off for IOs.

It is automatically turned on to power the coin vibration motor (if there is one attached), the ambient light sensor, and the LED.

However, if you have an external peripheral on the IOs that needs 3V power (such as a buzzer or UV sensor), you can use this function to turn on the power: ::

    settings.enable3VRegulator(true);

And to turn it off: ::

    settings.enable3VRegulator(false);

Forcing 1MHz PHY
---------------------
BTLE 5 adds 2MHz radio band support, which may affect performance. This endpoint forces 1Mhz for compatibility or testing purposes.

To force the 1MHz PHY: ::

    settings.enableForce1MPhy(true);

To allow the 2MHz PHY: ::

    settings.enableForce1MPhy(false);