.. highlight:: java

MMA8452Q Accelerometer
======================
Developers looking for finer control over boards with the MMA8452Q accelerometer should use the 
`MMA8452Q accelerometer <https://mbientlab.com/docs/metawear/android/latest/com/mbientlab/metawear/module/AccelerometerMma8452q.html>`_ interface.  This 
sensor supports many of the same features as the Bosch accelerometers and is currently only on MetaWear R boards.

::

    import com.mbientlab.metawear.module.AccelerometerMma8452q;

    AccelerometerMma8452q accMma8452q = board.getModule(AccelerometerMma8452q.class);;

Configuration
-------------
Unlike the Bosch accelerometers, the MMA8452Q only has a max range of +/-8g and does not have a data rate over 1Khz, however, it does have a high pass 
filter which can be used to filter gravity from the measured data.  

::

    import com.mbientlab.metawear.module.AccelerometerMma8452q.*;

    accMma8452q.configure()
        .odr(OutputDataRate.ODR_12_5_HZ)        // Set data rate to 12.5Hz
        .range(FullScaleRange.FSR_8G)           // Set range to +/-8g
        .commit();

Movement Detection
------------------
Movement detection checks whether the measured acceleration is below or above a threshold.  General motion detection checks if the acceleration on any 
enabled axes exceeds the threshold whereas free fall detection checks if the acceleration on all enabled axes is below the threshold.  The threshold, enabled axes, and movement type is set with a 
`MovementConfigEditor <https://mbientlab.com/docs/metawear/android/latest/com/mbientlab/metawear/module/AccelerometerMma8452q.MovementConfigEditor.html>`_.  Note that unlike the Bosch accelerometers, motion and free fall detection are mutually exclusive.  

Movement data is represented by the `Movement <https://mbientlab.com/docs/metawear/android/latest/com/mbientlab/metawear/module/AccelerometerMma8452q.Movement.html>`_ type.

::

    import com.mbientlab.metawear.data.CartesianAxis;
    import com.mbientlab.metawear.module.AccelerometerMma8452q.Movement;

    // enable free fall detection using all axes 
    // and set threshold to 0.333g
    accMma8452q.freeFall().configure()
            .threshold(0.333f)
            .axes(CartesianAxis.values())
            .commit();

    accMma8452q.freeFall().addRouteAsync(new RouteBuilder() {
        @Override
        public void configure(RouteComponent source) {
            source.stream(new Subscriber() {
                @Override
                public void apply(Data data, Object... env) {
                    Log.i("MainActivity", data.value(Movement.class).toString());
                }
            });
        }
    }).continueWith(new Continuation<Route, Void>() {
        @Override
        public Void then(Task<Route> task) throws Exception {
            accMma8452q.freeFall().start();
            accMma8452q.start();
            return null;
        }
    });

Tap Detection
-------------
The MMA8452Q chip characterizes a single tap as the measured acceleration crossing a threshold then dropping below it within an interval of time.  Double 
taps are defined as a second tap that occurs after the latency period of the first tap but also within a window of time.  These parameters, along with 
what tap type and axis to detect are set with a 
`TapConfigEditor <https://mbientlab.com/docs/metawear/android/latest/com/mbientlab/metawear/module/Mma8452qAccelerometer.TapConfigEditor.html>`_ to configure tap detection.  Tap data is represented by the MMA8452Q's 
`Tap <https://mbientlab.com/docs/metawear/android/latest/com/mbientlab/metawear/module/AccelerometerMma8452q.Tap.html>`_ type.

::

    import com.mbientlab.metawear.data.CartesianAxis;
    import com.mbientlab.metawear.module.AccelerometerMma8452q.Tap;

    // enable single tap detection on Z-axis
    accMma8452q.tap().configure()
            .enableSingleTap()
            .axis(CartesianAxis.Z)
            .commit();
    accMma8452q.tap().addRouteAsync(new RouteBuilder() {
        @Override
        public void configure(RouteComponent source) {
            source.stream(new Subscriber() {
                @Override
                public void apply(Data data, Object... env) {
                    Tap tap = data.value(Tap.class);
                    switch(tap.type) {
                        case SINGLE:
                            Log.i("MainActivity", "Single tap");
                            break;
                        case DOUBLE:
                            Log.i("MainActivity", "Double tap");
                            break;
                    }
                }
            });
        }
    }).continueWith(new Continuation<Route, Void>() {
        @Override
        public Void then(Task<Route> task) throws Exception {
            accMma8452q.tap().start();
            accMma8452q.start();
            return null;
        }
    });

Orientation Detection
---------------------
The orientation detector alerts you when the sensor's orientation changes between portrait/landscape and front/back.  Data is represented as a 
`SensorOrientation <https://mbientlab.com/docs/metawear/android/latest/com/mbientlab/metawear/data/SensorOrientation.html>`_ type.

::

    accMma8452q.orientation().addRouteAsync(new RouteBuilder() {
        @Override
        public void configure(RouteComponent source) {
            source.stream(new Subscriber() {
                @Override
                public void apply(Data data, Object... env) {
                    Log.i("MainActivity", "Orientation = " + data.value(SensorOrientation.class));
                }
            });
        }
    }).continueWith(new Continuation<Route, Void>() {
        @Override
        public Void then(Task<Route> task) throws Exception {
            accMma8452q.orientation().start();
            accMma8452q.start();
            return null;
        }
    });

Shake Detection
---------------
The shake detection algorithm uses high-pass filtered acceleration data to determine when the senseor is shaking back and forth.  While similar to the 
motion detection algorithm described in the previous section, shake detection is useful when the device may be tiled and interest is more about the 
dynamic acceleration crossing the threshold in a short amount of time.  There are only two parameters to configure for this algorithm, the 
``threshold`` and the ``duration`` for which the criteria is met, and these are set with a 
`ShakeConfigEditor <https://mbientlab.com/docs/metawear/android/latest/com/mbientlab/metawear/module/AccelerometerMma8452q.ShakeConfigEditor.html>`_.  

Data from this algorithm is also represented as a ``Movement`` type.

::

    import com.mbientlab.metawear.data.CartesianAxis;
    import com.mbientlab.metawear.module.AccelerometerMma8452q.Movement;

    // enable shake detection on the x-axis
    accMma8452q.shake().configure()
            .axis(CartesianAxis.X)
            .commit();
    accMma8452q.shake().addRouteAsync(new RouteBuilder() {
        @Override
        public void configure(RouteComponent source) {
            source.stream(new Subscriber() {
                @Override
                public void apply(Data data, Object... env) {
                    Log.i("MainActivity", "Shake = " + data.value(Movement.class).toString());
                }
            });
        }
    }).continueWith(new Continuation<Route, Void>() {
        @Override
        public Void then(Task<Route> task) throws Exception {
            accMma8452q.shake().start();
            accMma8452q.start();
            return null;
        }
    });


