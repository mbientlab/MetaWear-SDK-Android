.. highlight:: java

Proximity Sensor
================
Proximity sensors detect the presence of objects without physically touching them.  They are often used as a touch-less switch, automatically turning on 
faucets and opening doors to name a few examples.  

MetaDetector boards are outfitted with the `TSL2671 <http://ams.com/eng/content/download/250323/976177/142397>`_ proximity detector, a photoelectric 
style detector that refelcts an infrared signal off the target object to measure distance.  This sensor is accessed with the 
`ProximityTsl2671 <https://mbientlab.com/docs/metawear/android/latest/com/mbientlab/metawear/module/ProximityTsl2671.html>`_ interface.  

::

    import com.mbientlab.metawear.module.ProximityTsl2671;

    final ProximityTsl2671 proximity = board.getModule(ProximityTsl2671.class);

Configuration
-------------
The TSL2671 device has 4 configurable parameters that control the sensitivity and distance at which the detector can measure proximity.  These 
parameters are set with the interface's 
`ConfigEditor <https://mbientlab.com/docs/metawear/android/latest/com/mbientlab/metawear/module/ProximityTsl2671.ConfigEditor.html>`_ class.

===================  ===================================================================
Parameter            Description
===================  ===================================================================
Integration Time     How long the internal ADC converts analog input into digital counts
Pulse Count          Number of IR pulses emitted for distance measuring
Receiver Diode       Which photodiode to use for measure incoming light
Transmitter Current  Amount of current driving the IR transmitter
===================  ===================================================================

::

    import com.mbientlab.metawear.module.ProximityTsl2671.ReceiverDiode;
    import com.mbientlab.metawear.module.ProximityTsl2671.TransmitterDriveCurrent;

    // set integration time to 5.44ms
    // use both photodiodes for proximity detection
    // use default pulse count of 1,
    // set drive current to 25mA
    proximity.configure()
            .integrationTime(5.44f)
            .receiverDiode(ReceiverDiode.BOTH)
            .transmitterDriveCurrent(TransmitterDriveCurrent.CURRENT_25MA)
            .commit();

Proximity Data
--------------
Proximity data is an ADC value represented as an unsigned short; the higher the adc value, the closeer the distance to the object.

::

    proximity.adc().addRouteAsync(new RouteBuilder() {
        @Override
        public void configure(RouteComponent source) {
            source.stream(new Subscriber() {
                @Override
                public void apply(Data data, Object ... env) {
                    Log.i("MainActivity", "Proximity ADC = " + data.value(Integer.class));
                }
            });
        }
    }).continueWith(new Continuation<Route, Void>() {
        @Override
        public Void then(Task<Route> task) throws Exception {
            proximity.adc().read();
            return null;
        }
    });
