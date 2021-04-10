.. highlight:: java

Data Producer
-------------
Components that create data, be it sensors or firmware features, are represented by the 
`DataProducer <https://mbientlab.com/docs/metawear/android/latest/com/mbientlab/metawear/DataProducer.html>`_ interface.  Manipulating the producer data, 
whether on the Android device or on the board itself, is expressed by a building a data route, more on that in the :doc:`data_route` section.  

There are three main types of data producers represented by the API:

* Asynchronous
* Forced
* Active

These three producers control the data flow slightly differently from each other, as expanded upon in the following sub sections.

Async Data Producer
^^^^^^^^^^^^^^^^^^^
Asynchronous data producers, when active, constantly measure data in the background and send it when new data is available.  Once configured, call 
`start <https://mbientlab.com/docs/metawear/android/latest/com/mbientlab/metawear/AsyncDataProducer.html#start-->`_ to begin collecting data and call 
`stop <https://mbientlab.com/docs/metawear/android/latest/com/mbientlab/metawear/AsyncDataProducer.html#stop-->`_ to put the producer back into standby mode.  

Most sensors, such as the accelerometer, gyroscope, and magnetometer, are async data producers.  

::

    import com.mbientlab.metawear.AsyncDataProducer;

    public void asycProducerCtrl(AsyncDataProducer producer) {
        // Tells producer to begin creating data
        producer.start();

        // Puts producer back to standby mode
        producer.stop();
    }


Forced Data Producer
^^^^^^^^^^^^^^^^^^^^
Unlike async producers, forced data producers only create data on demand when a read request is received via the  
`read <https://mbientlab.com/docs/metawear/android/latest/com/mbientlab/metawear/ForcedDataProducer.html#read-->`_ function.  Asynchronous behavior can be 
mimicked using the :doc:`timer` module to schedule periodic reads.

GPIO analog/digital, temperature, and humidity data are examples of forced data producers.

::

    import com.mbientlab.metawear.ForcedDataProducer;

    public void readForcedProducer(ForcedDataProducer producer) {
        // instructs producer to collect one data sample 
        producer.read();
    }

Active Data Producer
^^^^^^^^^^^^^^^^^^^^
Active data producers are similar to async data producers in that they send data whenever it is ready, however, as the name implies, they are always 
active and are not user controlled like the other two producers.  Only a few sensors are considered to be an active data producer, namely the push 
button switch and the power/charge status monitoring pins.
