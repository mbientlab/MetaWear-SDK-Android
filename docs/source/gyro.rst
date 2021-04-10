.. highlight:: java

Gyroscope
==========
The MetaWear boards come with either the BMI160 or the BMI270 gyroscope from Bosch. 

While there are specific classes for either type of gyroscope, a generic class has been created to configure the gyroscope generically. 

Only the output data rate and somes modes can be changed using this class. The gyro is represented by the 
`Gyro <https://mbientlab.com/docs/metawear/android/latest/com/mbientlab/metawear/module/Gyro.html>`_ interface. ::

    import com.mbientlab.metawear.module.Gyro;

    final Gyro gyro = board.getModule(Gyro.class);

Configuration
-------------
Like the accelerometer, the gyro also has a configurable output data rate and range.  Use the gyro's 
`ConfigEditor <https://mbientlab.com/documents/metawear/android/latest/com/mbientlab/metawear/module/Gyro.ConfigEditor.html>`_. to set these parameters.

::

    import com.mbientlab.metawear.module.Gyro.Range;
    import com.mbientlab.metawear.module.Gyro.OutputDataRate;

    // set the data rat to 50Hz and the 
    // data range to +/- 2000 degrees/s
    gyro.configure()
            .odr(OutputDataRate.ODR_50_HZ)
            .range(Range.FSR_2000)
            .commit();

This is particularly helpful when configuring the sensor fusion settings.

To retrieve the angular velocity (gyroscope) signal, please see the specific gyro interface.
