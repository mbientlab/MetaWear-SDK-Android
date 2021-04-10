.. highlight:: java

Temperature
===========
Temperature plays an import role in our every day life.  We rely on it to monitor our health, food, and electronics to name a few examples.  All MbientLab 
boards come with at least one temperature sensor, represented by the 
`Sensor <https://mbientlab.com/docs/metawear/android/latest/com/mbientlab/metawear/module/Temperature.Sensor.html>`_ interface, and is acceesed through the 
`Temperature <https://mbientlab.com/docs/metawear/android/latest/com/mbientlab/metawear/module/Temperature.html>`_ interface.

::

    import com.mbientlab.metawear.module.Temperature;

    final Temperature temperature = board.getModule(Temperature.class);

Sensors
-------
There are 4 types of temperature sensors on the MetaWear enumerated by the 
`SensorType <https://mbientlab.com/docs/metawear/android/latest/com/mbientlab/metawear/module/Temperature.SensorType.html>`_ enum.  

===================  ===================================================================
Sensor               Description
===================  ===================================================================
nRF SOC              Temperature sensor on the nRF SOC
External Thermistor  Separate thermistor that can be connected to the gpio pins
Bosch Env            Temperature sensor from either the BMP280 or BME280 devices
Preset Thermistor    Thermistor placed on the MetaWear board
===================  ===================================================================

All MbientLab boards, except MetaWear R, come with an `NCP15XH103F03RC <http://www.murata.com/en-us/products/productdetail?partno=NCP15XH103F03RC>`_ 
therimstor preset on the pcb and boards equipped with a Bosch :doc:`barometer` can use the environmental unit to measure temperature as well.  While 
the nRF SOC also has a temperature sensor, it is better use one of the other sensor types instead.

Available sensors are presented as an array returned by the 
`sensors <https://mbientlab.com/docs/metawear/android/latest/com/mbientlab/metawear/module/Temperature.html#sensors-->`_ method.  You can also find a 
specific sensor using 
`findSensors <https://mbientlab.com/docs/metawear/android/latest/com/mbientlab/metawear/module/Temperature.html#findSensors-com.mbientlab.metawear.module.Temperature.SensorType->`_.

::

    import com.mbientlab.metawear.module.Temperature.SensorType;

    // Does not work for MeteaWear R boards, use SensorType.NRF_SOC
    final Temperature.Sensor tempSensor = temperature.findSensors(SensorType.PRESET_THERMISTOR)[0];

External Thermistor
^^^^^^^^^^^^^^^^^^^
Using the external thermistor requires some additional configuration before reading sensor data.  After attaching the thermistor to the board, as 
outlined in our `blog post <http://projects.mbientlab.com/metawear-and-thermistor/>`_, you need to tell the firmware which pins the thermistor is 
connected to and whether the thermistor is active high or low.

::

    import com.mbientlab.metawear.module.Temperature.ExternalThermistor;

    // Read data from pin 0, pulldown resistor is on pin 1, active low
    ((ExternalThermistor) temperature.findSensors(SensorType.EXT_THERMISTOR)[0])
                .configure((byte) 0, (byte) 1, false);

Bosch Env
^^^^^^^^^
There is no extra configuration needed for using the temperature sensors on the BMP280 and BME280; the only thing you need to do is start the the 
devices.

::

    // only for RPro, CPro, Env, and Motion boards
    board.getModule(BarometerBosch.class).start();
    temperature.findSensors(SensorType.BOSCH_ENV)[0]).read();


Temperature Data
----------------
Temperature data is reported in Celsius and interepreted as a float value.  It is represented as a forced data producer.

::

    tempSensor.addRouteAsync(new RouteBuilder() {
        @Override
        public void configure(RouteComponent source) {
            source.stream(new Subscriber() {
                @Override
                public void apply(Data data, Object ... env) {
                    Log.i("MainActivity", "Temperature (C) = "  data.value(Float.class));
                }
            });
        }
    }).continueWith(new Continuation<Route, Void>() {
        @Override
        public Void then(Task<Route> task) throws Exception {
            tempSensor.read();
            return null;
        }
    });

