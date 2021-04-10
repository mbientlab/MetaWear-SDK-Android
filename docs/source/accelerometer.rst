.. highlight:: java

Accelerometer
=============
All MbientLab boards come with an accelerometer, a device which measure acceleration forces e.g. gravity and motion, accessed through the 
`Accelerometer <https://mbientlab.com/docs/metawear/android/latest/com/mbientlab/metawear/module/Accelerometer.html>`_ interface.  

Beecause there are different accelerometers used across the boards, the base ``Accelerometer`` interface was designed to be used with any accelerometer 
thus it can only access the measured acceleration data.  Developers building apps for specific MetaWear models should also review the 
:ref:`Bosch Accelerometer` or :ref:`Mma8452q Accelerometer` sections`.

::

    import com.mbietnlab.metawear.Accelerometer;

    Accelerometer accelerometer= board.getModule(Accelerometer.class);

Global Enable
-------------
To retrieve data from the accelerometers, developers will also need to set a global enable bit with the ``Accelerometer`` level 
`start <https://mbientlab.com/docs/metawear/android/latest/com/mbientlab/metawear/module/Accelerometer.html#start-->`_ function.  The global ``start`` 
method must be called last, after all the sensor configuration has been set and async producers enabled.  Conversly to return the accelerometer to 
standby mode, call `stop <https://mbientlab.com/docs/metawear/android/latest/com/mbientlab/metawear/module/Accelerometer.html#stop-->`_.

::

    // enable the sensor
    // call last after configuring and enabling other data producers
    accelerometer.start();

    // disable the sensor 
    accelerometer.stop();

Configuration
-------------
It is important that you first set the output data rate and data range before using the accelerometer as these setings are typically global settings 
that affect the configuration of other acceleromter features.  To configure the accelerometer, call 
`config <https://mbientlab.com/docs/metawear/android/latest/com/mbientlab/metawear/module/Accelerometer.html#configure-->`_ and use the returned editor object 
to set the output data rate and range.  As this is a generic editor interface, only float values are accepted and the actual settings used may not be 
the same as the values passed in.  ::

    accelerometer.configure()
            .odr(25f)       // Set sampling frequency to 25Hz, or closest valid ODR
            .range(4f)      // Set data range to +/-4g, or closet valid range
            .commit();

    Log.i("MainActivity", "Actual Odr = " + accelerometer.getOdr());
    Log.i("MainActivity", "Actual Range = " + accelerometer.getRange());

Acceleration Data
-----------------
The accelerometers provide access to the raw acceleration data they measure.  Use the 
`acceleration <https://mbientlab.com/docs/metawear/android/latest/com/mbientlab/metawear/module/Accelerometer.html#acceleration-->`_ method to get the 
``AsyncDataProducer`` object controlling the acceleration data.  Then, setup a data route to handle the dadta, and finally, start the accelerometer.  

Raw acceleration data is represented by the API as a 
`Acceleration <https://mbientlab.com/docs/metawear/android/latest/com/mbientlab/metawear/data/Acceleration.html>`_ type.  

::

    accelerometer.acceleration().addRouteAsync(new RouteBuilder() {
        @Override
        public void configure(RouteElement source) {
            source.stream(new Subscriber() {
                @Override
                public void apply(Data data, Object... env) {
                    Log.i("MainActivity", data.value(Acceleration.class).toString());
                }
            });
        }
    }).continueWith(new Continuation<Route, Void>() {
        @Override
        public Void then(Task<Route> task) throws Exception {
            accelerometer.acceleration().start();
            accelerometer.start();
            return null;
        }
    });
