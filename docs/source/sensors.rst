Sensors
=======
MetaWear comes with plenty of sensors ready to be used with only a few API calls.  Most boards have a different combination of sensors or even different 
sensor models so it is important to add a null pointer check for the ``getModule`` method if your app is to be used with different boards e.g. the 
`fitness tracker <https://github.com/mbientlab-projects/AndroidFitnessTracker>`_ app.  

.. toctree::
    :hidden:
    :maxdepth: 1

    accelerometer
    accelerometer_bosch
    accelerometer_bma255
    accelerometer_bmi160
    accelerometer_bmi270
    accelerometer_mma8452q
    light_sensor_ltr329
    barometer
    color_sensor
    gyro
    gyro_bmi160
    gyro_bmi270
    humidity_bme280
    magnetometer_bmm150
    proximity_tsl2671
    temperature
