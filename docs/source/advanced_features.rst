.. highlight:: java

Advanced Features 
=================

Lambda Expressions
------------------
The Android SDK v3 code base has been designed with lambda expressions in mind.  For example, the code snippet provided to stream accelerometer data 
from the :doc:`data_route` page can be rewritten as follows:  ::

    acceleration.addRouteAsync(source -> source.stream((msg, env) -> 
        Log.i("MainActivity", data.value(Acceleration.class).toString())
    ));
    
The ``Continuation`` interface used with ``Task`` objects can also be written as a lambda expression.  ::

    board.readDeviceInformationAsync().continueWith(task -> 
        Log.i("MainActivity", task.getResult().toString())
    );

High Frequency Streaming
------------------------
Some developers may want to stream data from multiple motion sensors simultaneously or individually at frequencies higher than 100Hz.  To accommodate 
this use case, acceleration, angular velocity, and magnetic field data have a packed output mode that combines 3 data samples into 1 ble packett 
increasing the data throughput by 3x.  ::

    accelerometer.configure()
            .odr(200f)      // stream at 200Hz
            .range(4f)
            .commit();
    accelerometer.packedAcceleration().addRouteAsync(new RouteBuilder() {
        @Override
        public void configure(RouteElement source) {
            source.stream(new Subscriber() {
                @Override
                public void apply(Data data, Object... env) {
                    Log.i("MainActivity", data.value(Acceleration.class).toString());
                }
            });
        }
    }).continueWith(new Continuation<Route, Void>() {
        @Override
        public Void then(Task<Route> task) throws Exception {
            accelerometer.packedAcceleration().start();
            accelerometer.start();
            return null;
        }
    });

In addition to using packed output, developers will also need to reduce the max connection interval to 7.5ms (11.25ms for Android M+).  Reducing the max 
connection interval can also be used to speed up log downloads.  ::

    import android.os.Build;

    settings.editBleConnParams()
            .maxConnectionInterval(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ? 11.25f : 7.5f)
            .commit();

Serialization
-------------
The internal state of the ``MetaWearBoard`` interface can be saved to persist the object through app crashes or combined with the :doc:`macro` ssystem 
to rebuild the object state after saved commands are executed.  Use  
`serialize <https://mbientlab.com/docs/metawear/android/latest/com/mbientlab/metawear/MetaWearBoard.html#serialize-->`_ to save the state to the local disk 
and call `deserialize <https://mbientlab.com/docs/metawear/android/latest/com/mbientlab/metawear/MetaWearBoard.html#deserialize-->`_ to restore the state.  

Unlike in the previous API, the ``Subsriber`` objects used to handle sensor data can be serialized with the data route provided they do not have any 
non-serializable references.  What this means is that anonymous ``Subscriber`` objects must be declared as static variables and the ``apply`` function
can only access external variables through the ``env`` parameter rather than directly referencing them.  A subscriber's environment is set by calling 
`setEnvironment <https://mbientlab.com/docs/metawear/android/latest/com/mbientlab/metawear/Route.html#setEnvironment-int-java.lang.Object...->`_ from its 
owning ``Route`` object.

::

    public class MainActivity extends Activity implements ServiceConnection {
        // Declared as static variable so no reference to outer Activity class 
        // is held by this object
        private static Subscriber DATA_HANDLER = new Subscriber() {
            @Override
            public void apply(Data data, Object... env) {
                try {
                    FileOutputStream fos = (FileOutputStream) env[0];
                    Acceleration casted = data.value(Acceleration.class);
                    fos.write(String.format(Locale.US, "%s,%.3f,%.3f,%.3f%n", 
                            data.formattedTimestamp(), 
                            casted.x(), casted.y(), casted.z()).getBytes());
                } catch (IOException ex) {
                    Log.e("MainActivity", "Error writing to file", ex);
                }
            }
        };

        private FileOutputStream fos;

        public void setup() {
            acceleration.addRouteAsync(new RouteBuilder() {
                @Override
                public void configure(RouteComponent source) {
                    source.stream(DATA_HANDLER);
                }
            }).continueWith(new Continuation<Route, Void>() {
                @Override
                public Void then(Task<Route> task) throws Exception {
                    fos = openFileOutput("acceleration_data", MODE_PRIVATE);
                    // Pass the output stream to the first Subscriber (idx 0) 
                    // by seting its environment
                    task.getResult().setEnvironment(0, fos);

                    accelerometer.acceleration().start();
                    accelerometer.start();

                    return null;
                }
            });
        }
    }

Updating Firmware
-----------------
Updating the firmware requires the `Android DFU library <https://github.com/NordicSemiconductor/Android-DFU-Library>`_ from Nordic Semiconductor.  
Add the library as a compile dependency and configure your project as described in the 
`documentation <https://github.com/NordicSemiconductor/Android-DFU-Library/tree/release/documentation#usage>`_.  Once you have setup your project:  

1. Call `downloadFirmwareUpdateFilesAsync <https://mbientlab.com/documents/metawear/android/latest/com/mbientlab/metawear/MetaWearBoard.html#downloadFirmwareUpdateFilesAsync-->`_ to retrieve the latest available firmware for the board  
2. Reboot the board in MetaBoot mode with `jumpToBootloaderAsync <https://mbientlab.com/docs/metawear/android/latest/com/mbientlab/metawear/module/Debug.html#jumpToBootloaderAsync-->`_ 
3. Upload the returned files in their list order with the `DfuServiceInitiator <https://github.com/NordicSemiconductor/Android-DFU-Library/blob/release/dfu/src/main/java/no/nordicsemi/android/dfu/DfuServiceInitiator.java>`_ class  

::

    private TaskCompletionSource<Void> dfuTaskSource;
    private final DfuProgressListener dfuProgressListener= new DfuProgressListenerAdapter() {
        @Override
        public void onDfuCompleted(String deviceAddress) {
            dfuTaskSource.setResult(null);
        }

        @Override
        public void onDfuAborted(String deviceAddress) {
            dfuTaskSource.setCancelled();
        }

        @Override
        public void onError(String deviceAddress, int error, int errorType, String message) {
            dfuTaskSource.setError(new RuntimeException("DFU error: " + message));
        }
    };
    public void updateFirmware(final Context ctx, final Class<? extends DfuBaseService> dfuServiceClass) {
        Capture<List<File>> files = new Capture<>();
        board.downloadLatestFirmwareAsync()
            .onSuccessTask(new Continuation<List<File>, Task<Void>>() {
                @Override
                public Task<Void> then(Task<List<File>> task) throws Exception {
                    files.set(task.getResult());
                    return board.getModule(Debug.class).jumpToBootloaderAsync();
                }
            }).onSuccessTask(new Continuation<Void, Task<Void>>() {
                @Override
                public Task<Void> then(Task<Void> ignored) throws Exception {
                    Task<Void> task = Task.forResult(null);
                    for(final File f: files.get()) {
                        task = task.onSuccessTask(new Continuation<Void, Task<Void>>() {
                            @Override
                            public Task<Void> then(Task<Void> ignored2) throws Exception {
                                return Task.delay(1000);
                            }
                        }).onSuccessTask(new Continuation<Void, Task<Void>>() {
                            @Override
                            public Task<Void> then(Task<Void> ignored2) throws Exception {
                                dfuTaskSource = new TaskCompletionSource<>();

                                DfuServiceInitiator starter = new DfuServiceInitiator(board.getMacAddress())
                                        .setKeepBond(false)
                                        .setForceDfu(true);
                                int i = f.getName().lastIndexOf('.');
                                String extension = f.getName().substring(i + 1);

                                if (extension.equals("hex") || extension.equals("bin")) {
                                    starter.setBinOrHex(DfuBaseService.TYPE_APPLICATION, f.getAbsolutePath());
                                } else {
                                    starter.setZip(f.getAbsolutePath());
                                }
                                starter.start(ctx, dfuServiceClass);

                                return dfuTaskSource.getTask();
                            }
                        })
                    }

                    return task;
                }
            }).continueWith(new Continuation<Void, Void>() {
                @Override
                public Void then(Task<Void> task) throws Exception {
                    if (task.isFaulted()) {
                        Log.w("MainActivity", "firmware update failed", task.getError());
                    } else if (task.isCancelled()) {
                        Log.w("MainActivity", "firmware update cancelled");
                    } else {
                        Log.w("MainActivity", "firmware update successful");
                    }
                    return null;
                }
            });
    }

Forwarding Data
---------------
As you may have noticed, data producers have a `name <https://mbientlab.com/docs/metawear/android/latest/com/mbientlab/metawear/DataProducer.html#name-->`_ 
method and there are variant 
`map <https://mbientlab.com/docs/metawear/android/latest/com/mbientlab/metawear/builder/RouteComponent.html#map-com.mbientlab.metawear.builder.function.Function2-java.lang.String...->`_ 
and 
`filter <https://mbientlab.com/docs/metawear/android/latest/com/mbientlab/metawear/builder/RouteElement.html#filter-com.mbientlab.metawear.builder.filter.Comparison-java.lang.String...->`_ 
components that accept strings rather than numbers.  These strings are used to signify that right hand operands for math and comparison operations should 
come from the data producer, and will automatically update with the newest data vaues.  

For example, lets say you wanted to find the difference between the y and x axis values for acceleration data.  Setup the mapper to use the x-axis data 
by passing in the x-axis producer name to the ``map`` construct.

::

    acceleration.addRouteAsync(new RouteBuilder() {
        @Override
        public void configure(RouteComponent source) {
            // use x-axis value for the rhs of the subtract operation
            source.split().index(1).delay((byte) 1).map(Function2.SUBTRACT, acceleration.xAxisName())
                    .stream(new Subscriber() {
                        @Override
                        public void apply(Data data, Object... env) {
                            Log.i("MainActivity", "y - x = " + data.value(Float.class));
                        }
                    })
                    .end();
        }
    });

You can use the same ideas to create feedback loops by passing in the name assigned to the source processor.  ::

    // reusing tempSensor variable assigned from the Temperature section
    tempSensor.addRouteAsync(new RouteBuilder() {
        @Override
        public void configure(RouteComponent source) {
            // initial reference is 0, then updates the reference to 
            // every value that satisfies the "greater than" comparison
            source.filter(Comparison.GT, "reference").name("reference");
        }
    });

Anonymous Routes
----------------
Anonymous routes are a pared down variant of the `Route <http://mbientlab.com/docs/metawear/android/latest/com/mbientlab/metawear/Route.html>`_ interface 
that only has one subscriber.  They are used to retrieved logged data from a board that was not programmed by the current Android device.  

Because of the anonymous nature of the interface, users will need to rely on an identifier string to determine what kind of data is being passed to each 
route.  Developers can manage these identifiers by calling 
`generateIdentifier <https://mbientlab.com/docs/metawear/android/latest/com/mbientlab/metawear/Route.html#generateIdentifier-int->`_ for every logging 
subscriber and hardcoding the strings into the anonymous routes.  ::

    // create a route to log gyro y-axis data
    mwBoard.getModule(GyroBmi160.class).angularVelocity().addRouteAsync(new RouteBuilder() {
        @Override
        public void configure(RouteComponent source) {
            source.split().index(1).log(null);
        }
    }).continueWith(new Continuation<Route, Void>() {
        @Override
        public Void then(Task<Route> task) throws Exception {
            // save the result of generateIdentifier and hardcode 
            // value in anonymous route
            Log.i("MainActivity", "subscriber (0) = " + task.getResult().generateIdentifier(0));
            return null;
        }
    });

::

    // Use createAnonymousRoutesAsync to retrieve log data from 
    // another Android device
    mwBoard.createAnonymousRoutesAsync().onSuccessTask(new Continuation<AnonymousRoute[], Task<Void>>() {
        @Override
        public Task<Void> then(Task<AnonymousRoute[]> task) throws Exception {
            for(final AnonymousRoute it: task.getResult()) {
                it.subscribe(new Subscriber() {
                    @Override
                    public void apply(Data data, Object... env) {
                        switch (it.identifier()) {
                            // identifier earlier extracted from calling 
                            // generateIdentifier, use in switch statement to identify 
                            // which anonymous route represents gyro y-axis data
                            case "angular-velocity[1]":
                                Log.i("MainActivity", "gyro y-axis: " + data.value(Float.class));
                                break;
                        }
                    }
                });
            }

            return mwBoard.getModule(Logging.class).downloadAsync();
        }
    }).continueWith(new Continuation<Void, Void>() {
        @Override
        public Void then(Task<Void> task) throws Exception {
            Log.i("MainActivity", "Download completed");
            return null;
        }
    });
