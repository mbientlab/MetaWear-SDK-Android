.. highlight:: java

BMI270 Accelerometer
====================
The `BMI270 <https://www.bosch-sensortec.com/media/boschsensortec/downloads/datasheets/bst-bmi270-ds000.pdf>`_ accelerometer is the second most commonly used 
accelerometer on the available MbientLab boards. It comes with a :doc:`gyroscrope <gyro_bmi270>` giving 6 degrees of freedom.

::

    import com.mbientlab.metawear.module.AccelerometerBmi270;

    AccelerometerBmi270 accBmi270 = board.getModule(AccelerometerBmi270.class);

Output Data Rate
----------------
Available output data rates for the BMI270 device is enumerated by its 
`OutputDataRate <https://mbientlab.com/docs/metawear/android/latest/com/mbientlab/metawear/module/AccelerometerBmi270.OutputDataRate.html>`_ enum.  You can 
use these entries to set the ODR to a known quantity.  

::

    import com.mbientlab.metawear.module.AccelerometerBmi270.OutputDataRate;
    import com.mbientlab.metawear.module.AccelerometerBmi270.AccRange;

    accBmi270.configure()
            .odr(OutputDataRate.ODR_25_HZ)  // set odr to 25Hz
            .range(AccRange.AR_4G)          // set range to +/-4g
            .commit();

