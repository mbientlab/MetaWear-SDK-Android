.. highlight:: java

Magnetometer
============
A magnetometer measures magnetic field strength, typically of the Earth's magnetic field, using the Hall Effect.  The data from this sensor can be 
combined with the BMI160 data to provide 9 degrees of freedom for a :doc:`sensor_fusion` algorithm.  

A few select boards, namely the CPro and MetaMotion boards, have the 
`BMM150 <https://ae-bst.resource.bosch.com/media/_tech/media/datasheets/BST-BMM150-DS001-01.pdf>`_ geomagnetic sensor, represented by the 
`MagnetometerBmm150 <https://mbientlab.com/docs/metawear/android/latest/com/mbientlab/metawear/module/MagnetometerBmm150.html>`_ interface.  

::

    import com.mbientlab.metawear.module.MagnetometerBmm150;

    final MagnetometerBmm150 magnetometer = board.getModule(MagnetometerBmm150.class);

Configuration
-------------
Bosch provides 4 recommended configurations for the BMM150 chip that control the data rate, current, and noise.  Preset configurations are set by calling 
`usePreset <https://mbientlab.com/docs/metawear/android/latest/com/mbientlab/metawear/module/MagnetometerBmm150.html#usePreset-com.mbientlab.metawear.module.MagnetometerBmm150.Preset->`_

================ ==== =============== ===============================
Preset           ODR  Average Current Noise 
================ ==== =============== ===============================
LOW_POWER        10Hz 170µA           1.0µT (xy axis), 1.4µT (z axis)
REGULAR          10Hz 0.5mA           0.6µT
ENHANCED_REGULAR 10Hz 0.8mA           0.5µT
HIGH_ACCURACY    20Hz 4.9mA           0.3µT 
================ ==== =============== ===============================

::

    import com.mbientlab.metawear.module.MagnetometerBmm150.Preset;

    // use the regular preset configuration
    magnetometer.usePreset(Preset.REGULAR);

Advanced users can manually configure the device using the BMM150's 
`ConfigEditor <https://mbientlab.com/docs/metawear/android/latest/com/mbientlab/metawear/module/MagnetometerBmm150.ConfigEditor.html>`_ though it is highly 
recommended that one of the preset modes be used.

Magnetic Field Data
-------------------
The BMM150 measures magnetic field strength in Tesla (T) and its data is represented by the 
`MagneticField <https://mbientlab.com/docs/metawear/android/latest/com/mbientlab/metawear/data/MagneticField.html>`_ class.  

::

    magnetometer.magneticField().addRouteAsync(new RouteBuilder() {
        @Override
        public void configure(RouteComponent source) {
            source.stream(new Subscriber() {
                @Override
                public void apply(Data data, Object ... env) {
                    Log.i("MainActivity", data.value(MagneticField.class).toString());
                }
            });
        }
    }).continueWith(new Continuation<Route, Void>() {
        @Override
        public Void then(Task<Route> task) throws Exception {
            magnetometer.magneticField().start();
            magnetometer.start();
            return null;
        }
    });

