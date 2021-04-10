.. highlight:: java

Gpio
====
All boards are equipped with general purpose I/O pins which can be used to connect additional sensors to the board.  The GPIO pins are represented by 
the `Pin <https://mbientlab.com/docs/metawear/android/latest/com/mbientlab/metawear/module/Gpio.Pin.html>`_ interface and accessed through the 
`Gpio <https://mbientlab.com/docs/metawear/android/latest/com/mbientlab/metawear/module/Gpio.html>`_ interface.  

::

    import com.mbientlab.metawear.module.Gpio;

    final Gpio gpio = board.getModule(Gpio.class);

Output Voltage
--------------
Gpio pins have an output voltage that can be set to ~3V  or cleared to ~0V.  Depending on how your sensor is connected to the gpio pins, you may need to 
set (`setOutput <https://mbientlab.com/docs/metawear/android/latest/com/mbientlab/metawear/module/Gpio.Pin.html#setOutput-->`_) or clear 
(`clearOutput <https://mbientlab.com/docs/metawear/android/latest/com/mbientlab/metawear/module/Gpio.Pin.html#clearOutput-->`_) the output voltage with 
your app to complete the circuit as demonstrated with the optional 
`heart rate sensor <http://projects.mbientlab.com/heart-rate-data-with-the-pulse-sensor-and-metawear/>`_.

::

    // output 0V on pin 1
    gpio.pin((byte) 1).clearOutput();

Pull Mode
---------
To ensure the voltage is within the acceptable ranges for digital states, the MetaWear has resistors that can pull the voltage up or down, set with the 
`setPullMode <https://mbientlab.com/docs/metawear/android/latest/com/mbientlab/metawear/module/Gpio.Pin.html#setPullMode-com.mbientlab.metawear.module.Gpio.PullMode->`_ 
method.

::

    import com.mbientlab.metawear.module.Gpio.PullMode;

    gpio.pin(byte) 2).setPullMode(PullMode.PULL_UP);

Analog Data
-----------
Analog data comes as either an ADC ratio 
(`analogAdc <https://mbientlab.com/docs/metawear/android/latest/com/mbientlab/metawear/module/Gpio.Pin.html#analogAdc-->`_) or absolute reference voltage 
(`analogAbsRef <https://mbientlab.com/docs/metawear/android/latest/com/mbientlab/metawear/module/Gpio.Pin.html#analogAbsRef-->`_).  ADC values are unitless 
and are interpreted as a short value whereas the reference voltage is a float value in units of volts (V).

::

    // Get producer for analog adc data
    ForcedDataProducer adc = gpio.pin((byte) 0).analogAdc();

    adc.addRouteAsync(new RouteBuilder() {
        @Override
        public void configure(RouteComponent source) {
            source.stream(new Subscriber() {
                @Override
                public void apply(Data data, Object ... env) {
                    Log.i("MainActivity", "adc = " + data.value(Short.class));
                }
            });
        }
    });

Enhanced Analog Reads
^^^^^^^^^^^^^^^^^^^^^
Starting with firmware v1.2.3, additional features have been added to the analog read to accommodate more complex circuitry.  Prior to performing the 
analog read, the firmware can pull up/down another pin and wait up to 1 millisecond between setting the pull mode and reading analog data.  Furthermore, 
the analog data can be presented as data from another pin.

Call the variant `read <https://mbientlab.com/docs/metawear/android/latest/com/mbientlab/metawear/module/Gpio.Analog.html#read-byte-byte-short-byte->`_ method 
to use the enhanced read and 
`getVirtualPin <https://mbientlab.com/docs/metawear/android/latest/com/mbientlab/metawear/module/Gpio.html#getVirtualPin-byte->`_ to create ``Pin`` objects 
for handling data redirection.

::

    ForcedDataProducer analogVoltage = gpio.pin((byte) 1).analogAbsRef();

    analogVoltage.addRouteAsync(new RouteBuilder() {
        @Override
        public void configure(RouteComponent source) {
            source.stream(null);
        }
    }).continueWithTask(new Continuation<Route, Task<Route>>() {
        @Override
        public Task<Route> then(Task<Route> task) throws Exception {
            return gpio.getVirtualPin((byte) 0x15).addRouteAsync(new RouteBuilder() {
                @Override
                public void configure(RouteComponent source) {
                    source.stream(new Subscriber() {
                        @Override
                        public void apply(Data data, Object ... env) {
                            Log.i("MainActivity", "virtual pin voltage = " + data.value(Short.class));
                        }
                    });
                }
            });
        }
    }).continueWith(new Continuation<Route, Object>() {
        @Override
        public Object then(Task<Route> task) throws Exception {
            // before reading analog voltage from pin 1, 
            // pull up pin 1
            // pull down pin 2
            // wait 10 microseconds then read the voltage
            // present the data as coming from pin 0x15
            analogVoltage.read((byte) 0, (byte) 2, (short) 10, (byte) 0x15);
            return null;
        }
    });

Digital Data
------------
The GPIO pins can interpret the input data as a 1 or 0.  As per the `product spec <https://mbientlab.com/docs/MetaWearPPSv0.7.pdf>`_, a high state is 
between 2.1 and 3.0 volts and a low state is between 0 and 0.9 volts.  Don't forget to set the pull mode before reading the digial state.

::

    final ForcedDataProducer digital = gpio.pin((byte) 0).digital();

    digital.addRouteAsync(new RouteBuilder() {
        @Override
        public void configure(RouteComponent source) {
            source.stream(new Subscriber() {
                @Override
                public void apply(Data data, Object ... env) {
                    Log.i("MainActivity", "digital state = " + data.value(Byte.class));
                }
            });
        }
    }).continueWith(new Continuation<Route, Void>() {
        @Override
        public Void then(Task<Route> task) throws Exception {
            digital.read();
            return null;
        }
    });

Pin Monitoring
--------------
The pin's digital state can be monitored by the firmware, sending the new state when it changes.  There are 3 state transitions that the firmware can look 
for:  

======= ========================
Change  Description
======= ========================
Any     Either falling or rising
Falling Transitions from 1 -> 0
Rising  Transitions from 0 -> 1
======= ========================

After setting the pin change state 
(`setChangeType <https://mbientlab.com/docs/metawear/android/latest/com/mbientlab/metawear/module/Gpio.Pin.html#setChangeType-com.mbientlab.metawear.module.Gpio.PinChangeType->`_), 
start the async data producer returned from the 
`monitor <https://mbientlab.com/docs/metawear/android/latest/com/mbientlab/metawear/module/Gpio.Pin.html#monitor-->`_ method.

The data reported is the new digital input state.

::

    import com.mbientlab.metawear.module.Gpio.PinChangeType;

    Gpio.Pin pin = gpio.pin((byte) 1);

    // monitor transition from 1 -> 0
    pin.setChangeType(PinChangeType.FALLING);
    pin.monitor().addRouteAsync(new RouteBuilder() {
        @Override
        public void configure(RouteComponent source) {
            source.stream(new Subscriber() {
                @Override
                public void apply(Data data, Object ... env) {
                    Log.i("MainActivity", "state = " + data.value(Byte.class));
                }
            });
        }
    }).continueWith(new Continuation<Route, Void>() {
        @Override
        public Void then(Task<Route> task) throws Exception {
            pin.monitor().start();
            return null;
        }
    });
