.. highlight:: java

Humidity
========
Electronic humidity sensors (hydrometer) measure humidity by measuring the capacitance or resistance of air samples.  This sensor comes packaged with 
the `BME280 <https://ae-bst.resource.bosch.com/media/_tech/media/datasheets/BST-BME280_DS001-11.pdf>`_ integrated environmental unit, only available on 
MetaEnvironment boards, and is accessible through the 
`HumidityBme280 <https://mbientlab.com/docs/metawear/android/latest/com/mbientlab/metawear/module/HumidityBme280.html>`_ interface.

::

    import com.mbientlab.metawear.module.HumidityBme280;

    final HumidityBme280 humidity; = board.getModule(HumidityBme280.class);

Configuration
-------------
For humidity measurements, oversampling can be used to reduce the noise.  Oversampling modes are set with 
`setOversampling <https://mbientlab.com/docs/metawear/android/latest/com/mbientlab/metawear/module/HumidityBme280.html#setOversampling-com.mbientlab.metawear.module.HumidityBme280.OversamplingMode->`_.

::

    import com.mbientlab.metawear.module.HumidityBme280.OversamplingMode;

    // set oversampling to 16x
    humidity.setOversampling(OversamplingMode.SETTING_16X);

Humidity Data
-------------
Relative humidity data is a float value from 0 to 100 percent and is represented as a forced data producer.  ::

    humidity.value().addRouteAsync(new RouteBuilder() {
        @Override
        public void configure(RouteComponent source) {
            source.stream(new Subscriber() {
                @Override
                public void apply(Data data, Object ... env) {
                    Log.i("MainActivity", "Humidity = " + data.value(Float.class));
                }
            });
        }
    }).continueWith(new Continuation<Route, Void>() {
        @Override
        public Void then(Task<Route> task) throws Exception {
            humidity.value().read();
            return null;
        }
    });
