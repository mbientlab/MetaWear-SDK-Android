.. highlight:: java

BMI160 Gyroscope
=================
As previously mentioned, the `BMI160 <https://www.bosch-sensortec.com/media/boschsensortec/downloads/datasheets/bst-bmi160-ds000.pdf>`_ is a 6-axis IMU that has 
both an accelerometer and gyroscope.  The gyro sensor on this device is represented by the 
`GyroBmi160 <https://mbientlab.com/docs/metawear/android/latest/com/mbientlab/metawear/module/GyroBmi160.html>`_ interface and uses the Coriolis effect to 
measure angular velocity .

::

    import com.mbientlab.metawear.module.GyroBmi160;

    final GyroBmi160 gyroBmi160 = board.getModule(GyroBmi160.class);

Angular Velocity Data
---------------------
To retrieve angular velocity data, add a data route to the async data producer returned by the 
`angularVelocity <https://mbientlab.com/docs/metawear/android/latest/com/mbientlab/metawear/module/GyroBmi160.html#angularVelocity-->`_ function.  
Data values from that async producer are represented by the 
`AngularVelocity <https://mbientlab.com/docs/metawear/android/latest/com/mbientlab/metawear/data/AngularVelocity.html>`_ class.

::

    gyroBmi160.angularVelocity().addRouteAsync(new RouteBuilder() {
        @Override
        public void configure(RouteComponent source) {
            source.stream(new Subscriber() {
                @Override
                public void apply(Data data, Object ... env) {
                    Log.i("MainActivity", data.value(AngularVelocity.class).toString());
                }
            });
        }
    }).continueWith(new Continuation<Route, Void>() {
        @Override
        public Void then(Task<Route> task) throws Exception {
            gyroBmi160.angularVelocity();
            gyroBmi160.start();
            return null;
        }
    });

