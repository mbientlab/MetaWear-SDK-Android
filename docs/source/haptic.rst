.. highlight:: java

Haptic
======

The `Haptic <https://mbientlab.com/docs/metawear/android/latest/com/mbientlab/metawear/module/Haptic.html>`_ class interacts with the haptic driver pin to 
drive a buzzer or motor.  Circuit diagrams for the haptic driver are in section 8 of the 
`product spec <https://mbientlab.com/docs/MetaWearPPSv0.7.pdf>`_.

::

    import com.mbientlab.metawear.module.Haptic;
    
    // Run a buzzer for 500ms
    board.getModule(Haptic.class).startBuzzer((short) 500);
    
    // Run a motor for 1000ms at 50% strength
    board.getModule(Haptic.class).startMotor(50.f, (short) 1000);

