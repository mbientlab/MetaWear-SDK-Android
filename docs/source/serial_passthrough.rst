.. highlight:: java

Serial Passthrough
==================
The `SerialPassthrough <https://mbientlab.com/docs/metawear/android/latest/com/mbientlab/metawear/module/SerialPassthrough.html>`_ interface encapsulates the 
I2C and SPI buses which can be used to communicate with more complex devices.

::

    import com.mbientlab.metawear.module.SerialPassthrough;

    final SerialPassthrough serialPassthrough = board.getModule(SerialPassthrough.class);

I2C
---
Reading and writing data over the I2C bus is done with the 
`readI2cAsync <https://mbientlab.com/docs/metawear/android/latest/com/mbientlab/metawear/module/SerialPassthrough.html#readI2cAsync-byte-byte-byte->`_ and 
`writeI2c <https://mbientlab.com/docs/metawear/android/latest/com/mbientlab/metawear/module/SerialPassthrough.html#writeI2c-byte-byte-byte:A->`_ methods 
respectively.  Note that the MeteaWear performs a register read not a raw read.

::

    // On MetaWear R boards, this reads the WHO_AM_I register from the accelerometer
    serialPassthrough.readI2cAsync((byte) 0x1c, (byte) 0x0d, (byte) 1)
            .continueWith(new Continuation<byte[], Void>() {
                @Override
                public Void then(Task<byte[]> task) throws Exception {
                    Log.i("MainActivity", String.format("WHO_AM_I= %d", result[0]));
                    return null;
                }
            });
    
    // Reads the ID register from the BMP280 barometer (rpro, cpro, motion)
    serialPassthrough.readI2cAsync((byte) 0x77, (byte) 0xd0, (byte) 1)
            .continueWith(new Continuation<byte[], Void>() {
                @Override
                public Void then(Task<byte[]> task) throws Exception {
                    Log.i("MainActivity", String.format("bmp280 id= %d", result[0]));
                    return null;
                }
            });

SPI
---
Reading and writing data over the SPI bus is done with the 
`readSpiAsync <https://mbientlab.com/docs/metawear/android/latest/com/mbientlab/metawear/module/SerialPassthrough.html#readSpiAsync-byte->`_ and 
`writeSpi <https://mbientlab.com/docs/metawear/android/latest/com/mbientlab/metawear/module/SerialPassthrough.html#writeSpi-->`_ methods 
respectively.  Because the SPI operations require a plethora of parameters, an 
`SpiParameterBuilder <https://mbientlab.com/docs/metawear/android/latest/com/mbientlab/metawear/module/SerialPassthrough.SpiParameterBuilder.html>`_ is 
provided to facilitate setting the parameters.  

::

    // read registers from the BMI160 IMU (rg, rpro, c, cpro, motion, tracker)
    serialPassthrough.readSpiAsync((byte) 5)
            .data(new byte[] {(byte) 0xda})
            .slaveSelectPin((byte) 10)
            .clockPin((byte) 0)
            .mosiPin((byte) 11)
            .misoPin((byte) 7)
            .useNativePins()
            .mode((byte) 3)
            .frequency(SerialPassthrough.SpiFrequency.FREQ_8_MHZ)
            .commit()
            .continueWith(new Continuation<byte[], Void>() {
                @Override
                public Void then(Task<byte[]> task) throws Exception {
                    Log.i("MainActivity", "bmi160 register read: " + Arrays.toString(task.getResult()));
                    return null;
                }
            });

Data Route
----------
Typically I2C and SPI data is passed directly to the Android device.  They can be used with a data route by creating a data producer using  
`i2c <https://mbientlab.com/docs/metawear/android/latest/com/mbientlab/metawear/module/SerialPassthrough.html#i2c-byte-byte->`_ and 
`spi <https://mbientlab.com/docs/metawear/android/latest/com/mbientlab/metawear/module/SerialPassthrough.html#spi-byte-byte->`_ respectively.  When calling 
these functions, a unique numerical value needs to be given to identify the object along with the number of bytes the object will read.  If the ``id`` 
parameter corresponds to an existing object, the ``length`` parameter is ignored and the existing object is returned instead.

