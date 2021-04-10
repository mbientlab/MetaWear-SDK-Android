.. highlight:: java

Barometer
=========
Select MbientLab boards are equipped with Bosch environmental sensors, either the 
`BMP280 <https://ae-bst.resource.bosch.com/media/_tech/media/datasheets/BST-BMP280-DS001-12.pdf>`_ or the 
`BME280 <https://ae-bst.resource.bosch.com/media/_tech/media/datasheets/BST-BME280_DS001-11.pdf>`_.  These devices measure absolute barometeric pressure 
and can also measure :doc:`temperature <temperature>`.  

For pressuring sensing both devices have similar configuration parameters with the exception of slightly different available standby times.  Developer's 
can use either the `BarometerBmp280 <https://mbientlab.com/docs/metawear/android/latest/com/mbientlab/metawear/module/BarometerBmp280.html>`_ or 
`BarometerBme280 <https://mbientlab.com/docs/metawear/android/latest/com/mbientlab/metawear/module/BarometerBme280.html>`_ subclasses to configure the 
device with their respective standby enum values, or use the 
`BarometerBosch <https://mbientlab.com/docs/metawear/android/latest/com/mbientlab/metawear/module/BarometerBosch.html>`_ base class for barometer agnostic apps.  

::

    import com.mbientlab.metawear.module.BarometerBosch;

    BarometerBosch baroBosch = board.getModule(BarometerBosch.class);

Configuration
-------------
There are 3 parameters working in conjunction to control the noise, output resolution, and sampling rate:

* Oversampling  
* Infinite impulse filter (iir) coefficient  
* Standby time

The datasheets for the `BMP280 <https://ae-bst.resource.bosch.com/media/_tech/media/datasheets/BST-BMP280-DS001-12.pdf>`_ (table 14 and 15) and 
`BME280 <https://ae-bst.resource.bosch.com/media/_tech/media/datasheets/BST-BME280_DS001-11.pdf>`_ (section 5.5) have recommended settings for 
different use cases.

::

    import com.mbientlab.metawear.module.BarometerBosch.*;

    // configure the barometer with suggested values for 
    // indoor navigation
    baroBosch.configure()
            .filterCoeff(FilterCoeff.AVG_16)
            .pressureOversampling(OversamplingMode.ULTRA_HIGH)
            .standyTime(0.5f)
            .commit();

Pressure Data
-------------
Pressure data reported by the firmware is in Pascals (pa) and interpreted as a float value by the app side.

::

    baroBosch.pressure().addRouteAsync(new RouteBuilder() {
        @Override
        public void configure(RouteComponent source) {
            source.stream(new Subscriber() {
                @Override
                public void apply(Data data, Object ... env) {
                    Log.i("MainActivity", "Pressure (Pa) = " + data.value(Float.class));
                }
            });
        }
    }).continueWith(new Continuation<Route, Void>() {
        @Override
        public Void then(Task<Route> task) throws Exception {
            baroBosch.start();
            return null;
        }
    });

Altitude Data
-------------
Altitude data reported by the firmware is in meters (m) and interpreted as a float value by the app side.

::

    baroBosch.altitude().addRouteAsync(new RouteBuilder() {
        @Override
        public void configure(RouteComponent source) {
            source.stream(new Subscriber() {
                @Override
                public void apply(Data data, Object ... env) {
                    Log.i("MainActivity", "Altitude (m) = " + data.value(Float.class));
                }
            });
        }
    }).continueWith(new Continuation<Route, Void>() {
        @Override
        public Void then(Task<Route> task) throws Exception {
            baroBosch.altitude().start();
            baroBosch.start();
            return null;
        }
    });

