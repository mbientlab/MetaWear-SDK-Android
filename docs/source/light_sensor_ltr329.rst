.. highlight:: java

Ambient Light Sensor
====================
Ambient light sensors convert light intensity into a digital signal using a combination of photodiodes and analog-to-digital converters.  The light 
sensor of choice on select boards is the `LTR-329ALS <http://www.mouser.com/ds/2/239/Lite-On_LTR-329ALS-01%20DS_ver1.1-348647.pdf>`_ device from 
Lite-On, represented by the 
`AmbientLightLtr329 <https://mbientlab.com/docs/metawear/android/latest/com/mbientlab/metawear/module/AmbientLightLtr329.html>`_ interface.

::

    import com.mbientlab.metawear.module.AmbientLightLtr329;

    final AmbientLightLtr329 alsLtr329 = board.getModule(AmbientLightLtr329.class);


Configuration
-------------
The sensor has three configurable parameters, set with the module's 
`ConfigEditor <https://mbientlab.com/docs/metawear/android/latest/com/mbientlab/metawear/module/AmbientLightLtr329.ConfigEditor.html>`_ class: 

================  =========================================
Parameter         Description
================  =========================================
Gain              Controls data range and resolution
Integration Time  Measurement time for each cycle
Measurement Rate  How frequently to update illuminance data
================  =========================================

::

    import com.mbientlab.metawear.module.AmbientLightLtr329.*;
    
    // Set the gain to 8x
    // Set integration time to 250ms
    // Set measurement rate to 50ms
    alsLtr329.configure()
            .gain(Gain.LTR329_8X)
            .integrationTime(IntegrationTime.LTR329_TIME_250MS)
            .measurementRate(MeasurementRate.LTR329_RATE_50MS)
            .commit();

Illuminance Data
----------------
Illuminance data is categorized as an async data producer; data is interpreted as a float value and is in units of lux (lx).

::

    alsLtr329.illuminance().addRouteAsync(new RouteBuilder() {
        @Override
        public void configure(RouteComponent source) {
            source.stream(new Subscriber() {
                @Override
                public void apply(Data data, Object... env) {
                    Log.i("MainActivity", String.format(Locale.US, "illuminance = %.3f lx". 
                            data.value(Float.class)));
                }
            });
        }
    }).continueWith(new Continuation<Route, Void>() {
        @Override
        public Void then(Task<Route> task) throws Exception {
            alsLtr329.illuminance().start();
            return null;
        }
    });

