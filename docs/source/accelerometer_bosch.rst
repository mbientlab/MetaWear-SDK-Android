.. highlight:: java

Bosch Accelerometer
===================
With the exception of our first ever board, the MetaWear R, all other MbientLab boards use a Bosch accelerometer, represeted by the 
`AccelerometerBosch <https://mbientlab.com/docs/metawear/android/latest/com/mbientlab/metawear/module/AccelerometerBosch.html>`_ interface.  This interface 
is an extension of the ``Accelerometer`` interface providing methods and classes specific to Bosch accelerometers.  

Currently, we support both the :ref:`BMI160 <Bmi160 Accelerometer>` and :ref:`BMA255 <Bma255 Accelerometer>` sensors and they share much of their 
functionality with only a few differences enumerated in their respective interfaces.

::

    import com.mbientlab.metawear.module.AccelerometerBosch;

    AccelerometerBosch accBosch = board.getModule(AccelerometerBosch.class);

Low/High Detection
------------------
Low/High-g detection is an algorithm on the sensor that detects when the measure acceleration falls below a low bound or rises above a high bound.  
Before enabling the algorithm, first configure the algorithm parameters using the 
`LowHighConfigEditor <https://mbientlab.com/docs/metawear/android/latest/com/mbientlab/metawear/module/AccelerometerBosch.LowHighConfigEditor.html>`_ 
interface.  For low-g detection, set the low threshold, select which type of low-g criteria to use, and enable low-g detection.

::

    // enable low-g detection, use sum criteria,
    // detect when sum < 0.333g
    accBosch.lowHigh().configure()
            .enableLowG()
            .lowThreshold(0.333f)
            .lowGMode(AccelerometerBosch.LowGMode.SUM)
            .commit();

And, for high-g detection, set the high threshold and enable which axes to monitor.  

::

    // enable high-g detection on z-axis,
    // detect when acc > 2g
    accBosch.lowHigh().configure()
            .enableHighGz()
            .highThreshold(2f)
            .commit();

After configuring the algorithm, add a route for the low/high data represented by the 
`LowHighResponse <https://mbientlab.com/docs/metawear/android/latest/com/mbientlab/metawear/module/AccelerometerBosch.LowHighResponse.html>`_.

::

    import com.mbientlab.metawear.module.AccelerometerBosch.LowHighResponse;
    
    accBosch.lowHigh().addRouteAsync(new RouteBuilder() {
        @Override
        public void configure(RouteComponent source) {
            source.stream(new Subscriber() {
                @Override
                public void apply(Data data, Object... env) {
                    Log.i("MainActivity", data.value(LowHighResponse.class).toString());
                }
            });
        }
    }).continueWith(new Continuation<Route, Void>() {
        @Override
        public Void then(Task<Route> task) throws Exception {
            accBosch.lowHigh().start();
            accBosch.start();
            return null;
        }
    });

Flat Detection
--------------
Flat detection checks when the sensor enters or leaves a horizontal position i.e. laying on a table.  Data is interpretted as a boolean where true 
signifies the sensor is laying horizontally.

::

    accBosch.flat().addRouteAsync(new RouteBuilder() {
        @Override
        public void configure(RouteComponent source) {
            source.stream(new Subscriber() {
                int i = 0;
                @Override
                public void apply(Data data, Object... env) {
                    Log.i("MainActivity", "Flat? " + data.value(Boolean.class));
                }
            });
        }
    }).continueWith(new Continuation<Route, Void>() {
        @Override
        public Void then(Task<Route> task) throws Exception {
            accBosch.flat().start();
            accBosch.start();
            return null;
        }
    });

Orientation Detection
---------------------
The orientation detector alerts you when the sensor's orientation changes between portrait/landscape and front/back.  Data is represented as a 
`SensorOrientation <https://mbientlab.com/docs/metawear/android/latest/com/mbientlab/metawear/data/SensorOrientation.html>`_ type.

::

    import com.mbientlab.metawear.data.SensorOrientation;

    accBosch.orientation().addRouteAsync(new RouteBuilder() {
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
            accBosch.orientation().start();
            accBosch.start();
            return null;
        }
    });

Tap Detection
-------------
The tap detection algorithm checks if the difference in acceleration exceeds a threshold.  To detect double tap, a second tap must be registered within 
the quiet delay but before the double tap window ends.  The shock duration is a period of time where the direction of the first tap is locked; the quiet 
delay starts after the shock duration ends.  Use a 
`TapConfigEditor <https://mbientlab.com/docs/metawear/android/latest/com/mbientlab/metawear/module/AccelerometerBosch.TapConfigEditor.html>`_ to set the 
aforementioned parameters and to select which tap types to detect.

Data from the tap detection algorithm is represented as a 
`Tap <https://mbientlab.com/docs/metawear/android/latest/com/mbientlab/metawear/module/AccelerometerBosch.Tap.html>`_ type.

::

    import com.mbientlab.metawear.data.TapType;
    import com.mbientlab.metawear.module.AccelerometerBosch.TapShockTime;

    // enable single tap detection
    accBosch.tap().configure()
            .enableSingleTap()
            .threshold(2f)
            .shockTime(TapShockTime.TST_50_MS)
            .commit();
    accBosch.tap().addRouteAsync(new RouteBuilder() {
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
            accBosch.tap().start();
            accBosch.start();
            return null;
        }
    });

Motion Detection
----------------
The motion detection algorithms on the Bosch chips use the difference in consecutive acceleration data samples to determine different types of motion.

=========== =====================================================================
Motion      Description
=========== =====================================================================
Any         Difference exceeds threshold for N consecutive samples
No          Difference doesn't exceed the threshold for a period of time
Slow        Same as any motion but axis and direction information is not retained
=========== =====================================================================

The different motion detection algorithms are accessed with the 
`motion <https://mbientlab.com/docs/metawear/android/latest/com/mbientlab/metawear/module/AccelerometerBosch.html#motion-java.lang.Class->`_ method and only 
one algorithm can be active at any time.

::

    import com.mbientlab.metawear.module.AccelerometerBosch.NoMotionDataProducer;

    final NoMotionDataProducer noMotion = accBosch.motion(NoMotionDataProducer.class);

    // configure no motion detection
    // difference < 0.1g for 10 seconds before interrupt is fired
    noMotion.configure()
            .duration(10000)
            .threshold(0.1f)
            .commit();
    noMotion.addRouteAsync(new RouteBuilder() {
        @Override
        public void configure(RouteComponent source) {
            source.stream(new Subscriber() {
                @Override
                public void apply(Data data, Object... env) {
                    Log.i("MainActivity", "No motion detected");
                }
            });
        }
    }).continueWith(new Continuation<Route, Void>() {
        @Override
        public Void then(Task<Route> task) throws Exception {
            noMotion.start();
            accBosch.start();
            return null;
        }
    });
