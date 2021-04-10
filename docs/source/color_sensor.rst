.. highlight:: java

Color Sensor
============
Similiar to :doc:`ambient light sensors <light_sensor_ltr329>`, color sensors are responsive to light, however they are typically manufactured to only 
capture red, green, and blue light though some models are responsive to all visible light.  MetaEnvironment boards come equipped with a 
`TCS34725 <http://ams.com/eng/content/download/319364/1117183/287875>`_ color sensor, accessed through the 
`ColorTcs34725 <https://mbientlab.com/docs/metawear/android/latest/com/mbientlab/metawear/module/ColorTcs34725.html>`_ interface.  

::

    import com.mbientlab.metawear.module.ColorSensorTcs34725;

    final ColorSensorTcs34725 colorSensor = board.getModule(ColorSensorTcs34725.class);

Configuration
-------------
The color sensor has 2 configurable parameters that affect the data range, resultion, and sensitivity.

================  ============================================
Parameter         Description
================  ============================================
Gain              Analog signal scale
Integration Time  Amount of time spent to aggregate adc values
================  ============================================

There is also a white illuminator LED next to the sensor that can be used to provide additional light if the surrounding area is too dark.

::

    import com.mbientlab.metawear.module.ColorSensorTcs34725.Gain;

    // set gain to 4x, integration time to 4.8ms, 
    // keep illuminator led off
    colorDetector.configure()
            .gain(Gain.TCS34725_4X)
            .integrationTime(4.8f);
            .commit();

ADC Values
----------
The red, green, blue, and clear ADC values measured by the TCS3472 device are represented by the 
`ColorAdc <https://mbientlab.com/docs/metawear/android/latest/com/mbientlab/metawear/module/ColorTcs34725.ColorAdc.html>`_ class and is classified as a 
forced data producer.

::

    import com.mbientlab.metawear.module.ColorTcs34725;
    import com.mbientlab.metawear.module.ColorTcs34725.ColorAdc;
    
    final ColorTcs34725 colorSensor= mwBoard.get(ColorTcs34725.class);
    colorDetector.adc().addRouteAsync(new RouteBuilder() {
        @Override
        public void configure(RouteComponent source) {
            source.stream(new Subscriber() {
                @Override
                public void apply(Data data, Object ... env) {
                    Log.i("MainActivity", "color adc = " + data.value(ColorAdc.class).toString());
                }
            });
        }
    }).continueWith(new Continuation<Route, Void>() {
        @Override
        public Void then(Task<Route> task) throws Exception {
            colorDetector.adc().read();
            return null;
        }
    });
