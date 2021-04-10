.. highlight:: java

Led
===
All boards come with an RGB led on the pcb which is controlled through the 
`Led <https://mbientlab.com/docs/metawear/android/latest/com/mbientlab/metawear/module/Led.html>`_ interface.  

::

    import com.mbientlab.metawear.module.Led;

    final Led led = board.getModule(Led.class);

Setting Patterns
----------------
The ``Led`` interface comes with 3 preset patterns for ease of access to the LED.  

======= ================================================================
Pattern Description
======= ================================================================
Blink   Quickly flash the LED followed by an longer off period
Pulse   Gradual and periodic transition between a low and high intensity
Solid   Keep the LED on at a fixed brightness
======= ================================================================

::
    import com.mbientlab.metawear.module.Led.*;

    // use the solid preset pattern for the blue LED
    led.editPattern(Color.BLUE, PatternPreset.SOLID).commit();

Developers can directly set the parameters themselves if desired using the variant 
`editPattern <https://mbientlab.com/docs/metawear/android/latest/com/mbientlab/metawear/module/Led.html#editPattern-com.mbientlab.metawear.module.Led.Color->`_ method.

::

    // Set the green channel to turn on for 5 seconds
    led.editPattern(Color.GREEN)
            .riseTime((short) 0)
            .pulseDuration((short) 1000)
            .repeatCount((byte) 5)
            .highTime((short) 500)
            .highIntensity((byte) 16)
            .lowIntensity((byte) 16)
            .commit();

Pattern Playback
----------------
Controlling pattern playback is handled with 
`play <https://mbientlab.com/docs/metawear/android/latest/com/mbientlab/metawear/module/Led.html#play-->`_, 
`pause <https://mbientlab.com/docs/metawear/android/latest/com/mbientlab/metawear/module/Led.html#pause-->`_, and 
`stop <https://mbientlab.com/docs/metawear/android/latest/com/mbientlab/metawear/module/Led.html#stop-boolean->`_ methods.  The ``stop`` method also clears 
programmed patterns if called with ``true``.  ::

    import com.mbientlab.metawear.module.Led;
    
    // play the pattern
    led.play();

    // stop and clear all patterns
    led.stop(true);
