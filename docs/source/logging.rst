.. highlight:: java

Logging
=======
Mbientlab boards are equipped with flash memory that can store data if there is no connection with an Android device.  While the ``RouteComponent`` 
directs data to the logger, the `Logging <https://mbientlab.com/docs/metawear/android/latest/com/mbientlab/metawear/module/Logging.html>`_ interface provides 
the functions for controlling and configuring the logger.

::

    import com.mbientlab.metawear.module.Logging;

    final Logging logging = board.getModule(Logging.class);

Start the Logger
----------------
To start logging, call the `start <https://mbientlab.com/docs/metawear/android/latest/com/mbientlab/metawear/module/Logging.html#start-boolean->`_ method.  
If you wish to overwrite existing entries, pass ``true`` to the method.  When you are done logging, call 
`stop <https://mbientlab.com/docs/metawear/android/latest/com/mbientlab/metawear/module/Logging.html#stop-->`_.  

::

    // start logging, if log is full, no new data will be added
    logging.start(false);
    
    // start logging, if log is full, overrite existing data
    logging.start(true);

    // stop the logger
    logging.stop();

Note for the MMS
----------------
The MMS (MetaMotionS) board uses NAND flash memory to store data on the device itself. The NAND memory stores data in pages that are 512 entries large. When data is retrieved, it is retrieved in page sized chunks.

Before doing a full download of the log memory on the MMS, the final set of data needs to be written to the NAND flash before it can be downloaded as a page. To do this, you must call the function: ::

   logging.flushPage(board);

This should not be called if you are still logging data.

Downloading Data
----------------
When you are ready to retrieve the data, call 
`downloadAsync <https://mbientlab.com/docs/metawear/android/latest/com/mbientlab/metawear/module/Logging.html#downloadAsync-int-com.mbientlab.metawear.module.Logging.LogDownloadUpdateHandler->`_; 
the method returns a ``Task`` object that will be completed when the download finishes.  There are variants of ``downloadAsync`` that provide progress 
updates and error handing during the download.

::

    // download log data and send 100 progress updates during the download
    logging.downloadAsync(100, new Logging.LogDownloadUpdateHandler() {
        @Override
        public void receivedUpdate(long nEntriesLeft, long totalEntries) {
            Log.i("MainActivity", "Progress Update = " + nEntriesLeft + "/" + totalEntries);
        }
    }).continueWithTask(new Continuation<Void, Task<Void>>() {
        @Override
        public Task<Void> then(Task<Void> task) throws Exception {
            Log.i("MainActivity", "Download completed");
            return null;
        }
    });

