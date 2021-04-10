.. highlight:: java

BMA255 Accelerometer
====================
The feature set on the `BMA255 <https://ae-bst.resource.bosch.com/media/_tech/media/datasheets/BST-BMA255-DS004-05_published.pdf>`_ accelerometer 
almost matches the :doc:`accelerometer_bosch` interface with the only difference being the available output data rates.  Unlike the 
:doc:`accelerometer_bmi160` or the :doc:`accelerometer_mma8452q` accelerometers, the BMA255's ODR values are multiples of 1000Hz.  

This accelerometer is only used on MetaEnvironment and MetaDetector boards.

::

    import com.mbientlab.metawear.module.AccelerometerBma255;
    import com.mbientlab.metawear.module.AccelerometerBma255.OutputDataRate;
    import com.mbientlab.metawear.module.AccelerometerBosch.AccRange;

    AccelerometerBma255 accBma255 = board.getModule(AccelerometerBma255.class);
    accBma255.configure()
            .odr(OutputDataRate.ODR_62_5_HZ)    // set odr to 62.5Hz
            .range(AccRange.AR_16G)             // set range to +/-16g
            .commit();
