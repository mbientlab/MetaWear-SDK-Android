.. highlight:: java

NeoPixel
========
NeoPixels are `Adafruit's brand <https://learn.adafruit.com/adafruit-neopixel-uberguide/overview#important-things-to-know-about-neopixels-in-general>`_ 
of LED strips that can be individually controlled.  The MetaWear firmware can communicate with up to 3 strands on the WS2811 protocol with the 
`NeoPixel <https://mbientlab.com/docs/metawear/android/latest/com/mbientlab/metawear/module/NeoPixel.html>`_ interface.

We have a `blog post <http://projects.mbientlab.com/?p=82>`_ that provides instructions on how to connect a strand to a MetaWear board.  Though the 
example code is for an older API, the usage and structure of the v3 API is similar to the v1 API.

::

    import com.mbientlab.metawear.module.NeoPixel;

    final NeoPixel neopixel = board.getModule(NeoPixel.class)

Initializing a Strand
---------------------
Before we can interact with a NeoPixel strand, we first need to initialize memory on the MetaWear board to store NeoPixel information. To initialize the 
strand, call the 
`initializeStrand <https://mbientlab.com/docs/metawear/android/latest/com/mbientlab/metawear/module/NeoPixel.html#initializeStrand-byte-com.mbientlab.metawear.module.NeoPixel.ColorOrdering-com.mbientlab.metawear.module.NeoPixel.StrandSpeed-byte-byte->`_ method.  
When calling the function, you will need to take into consideration these parameters:

* Operating frequency (either 800 Hz or 400 Hz)  
* How many LEDs to use  
* What color ordering the strand requires (RGB, GRB, etc.)  
* Which gpio pin the data wire is connected to  

::

    import com.mbientlab.metawear.module.NeoPixel.*;

    // initialize memory for a strand #2 that operates at 800KHz (fast), has 60 leds, 
    // uses rbg color order, and has a data wire connected to gpio pin 1
    Strand npStrand = neoPixel.initializeStrand((byte) 2, ColorOrdering.MW_WS2811_RBG, StrandSpeed.FAST, (byte) 1, (byte) 60);

The function returns a `Strand <https://mbientlab.com/docs/metawear/android/latest/com/mbientlab/metawear/module/NeoPixel.Strand.html>`_ object and can be 
retrieved at a later time by calling 
`lookupStrand <https://mbientlab.com/docs/metawear/android/latest/com/mbientlab/metawear/module/NeoPixel.html#lookupStrand-byte->`_ with the same strand 
number used for `initializeStrand`.

::

    Strand npStrand = neoPixel.lookupStrand((byte) 2);

Setting Colors
--------------
When you have received your strand object, you can start turning on the LEDs using 
`setRgb <https://mbientlab.com/docs/metawear/android/latest/com/mbientlab/metawear/module/NeoPixel.Strand.html#setRgb-byte-byte-byte-byte->`_.  Any changes 
to the led state will immediately be propogated to the strand unless a hold is enabled with the  
`hold <https://mbientlab.com/docs/metawear/android/latest/com/mbientlab/metawear/module/NeoPixel.Strand.html#hold-->`_ function.  When 
`release <https://mbientlab.com/docs/metawear/android/latest/com/mbientlab/metawear/module/NeoPixel.Strand.html#release-->`_ is called, all the LED changes 
will appear simultaneously.

::

    // Enable hold to lock the strand state
    npStrand.hold();

    npStrand.setRgb((byte) 0, (byte) 255, (byte) 0, (byte) 0);
    npStrand.setRgb((byte) 1, (byte) 0, (byte) 255, (byte) 0);
    npStrand.setRgb((byte) 2, (byte) 0, (byte) 0, (byte) 255);

    // Disable hold, which will now update the strand with any state changes
    npStrand.release();

Pattern Rotation
----------------
Once you have a color pattern on your strand, you can rotate the pattern indefinitely using 
`rotate <https://mbientlab.com/docs/metawear/android/latest/com/mbientlab/metawear/module/NeoPixel.Strand.html#rotate-com.mbientlab.metawear.module.NeoPixel.Strand.RotationDirection-short->`_ 
or its 
`variant <https://mbientlab.com/docs/metawear/android/latest/com/mbientlab/metawear/module/NeoPixel.Strand.html#rotate-com.mbientlab.metawear.module.NeoPixel.Strand.RotationDirection-byte-short->`_ function to specify a fixed number of rotations.  If using the former method, call 
`stopRotation <https://mbientlab.com/docs/metawear/android/latest/com/mbientlab/metawear/module/NeoPixel.Strand.html#stopRotation-->`_ to terminate the rotation.

::

    import com.mbientlab.metawear.module.NeoPixel.Strand.RotationDirection;

    // Rotate pattern 16 times, moving away from the board with a delay of 500ms
    npStrand.rotate(RotationDirection.AWAY, (byte) 16, (short) 500);
     
    // Rotate pattern indefinitely, moving towards the board with a delay of 250ms
    npStrand.rotate(RotationDirection.TOWARDS, (short) 250);
     
    // Stop pattern rotation
    npStrand.stopRotation();

Clean Up
--------
When you are down using your NeoPixel strand, make sure you turn off the LEDs and deallocate the memory used for that particular strand.  You can discard 
the ``Strand`` object after calling `free <https://mbientlab.com/docs/metawear/android/latest/com/mbientlab/metawear/module/NeoPixel.Strand.html#free-->`_.

::

    // turn off leds from index [0, 59] (60 leds)
    npStrand.turnOff(0, 59);

    // free
    npStrand.free();
