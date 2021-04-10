.. highlight:: java

Data Route
==========
Data routes provide a simple and compact way for developers to access MetaWear's advanced features such as logging, data processing, and on-board event 
handling.  The routing API has been improved upon from MetaWear API v2 and has also been refactored to accommodate lambda expressions.

Creating Routes
---------------
Routes are created by calling the 
`addRouteAsync <https://mbientlab.com/docs/metawear/android/latest/com/mbientlab/metawear/DataProducer.html#addRouteAsync-com.mbientlab.metawear.builder.RouteBuilder->`_ 
function of the ``DataProducer`` you want to interact with.  The ``addRouteAsync`` function accepts a 
`RouteBuilder <https://mbientlab.com/docs/metawear/android/latest/com/mbientlab/metawear/builder/RouteBuilder.html>`_ countaining the collection of 
`RouteComponent <https://mbientlab.com/docs/metawear/android/latest/com/mbientlab/metawear/builder/RouteComponent.html>`_ calls that define how data flows 
from a producer to different endpoints.

::

    public void createRouteExample(DataProducer producer) {
        producer.addRouteAsync(new RouteBuilder() {
            @Override
            public void configure(RouteComponent source) {
                // define route here with RouteComponent objects
            }
        });
    }

All route objects have a unique numerical value associated with them.  You can retrieve routes anytime using this numerical ID with the 
`lookupRoute <https://mbientlab.com/docs/metawear/android/latest/com/mbientlab/metawear/MetaWearBoard.html#lookupRoute-int->`_ method.

Handling Data
-------------
Data created by the data producers is represented by the 
`Data <https://mbientlab.com/docs/metawear/android/latest/com/mbientlab/metawear/Data.html>`_ interface, encapsulating key attributes such as the time the 
data was created (or receieved) and the value of the data sample.  ``Data`` objects are consumed by 
`Subscribers <https://mbientlab.com/docs/metawear/android/latest/com/mbientlab/metawear/Subscriber.html>`_ attached to the route via the ``stream`` 
or ``log`` component.

When accessing the data value, you need to specify what type the value should be casted to.  Valid class types passed to the 
`value <https://mbientlab.com/docs/metawear/android/latest/com/mbientlab/metawear/Data.html#value-java.lang.Class->`_ method differ depending on the data 
producer; a ClassCastException is thrown if an invalid class type is used.  Developers can use the  
`types <https://mbientlab.com/docs/metawear/android/latest/com/mbientlab/metawear/Data.html#types-->`_ method to get a list of valid class types that can be 
used with the ``value`` function of the particular ``Data`` object.

::

    public static void logDataTypes(Data data) {
        Log.i("MainActivity", "Class types; " + Arrays.toString(data.types()));
    }

Stream
^^^^^^
Creating a live data stream to your Android device is handled with the 
`stream <https://mbientlab.com/docs/metawear/android/latest/com/mbientlab/metawear/builder/RouteComponent.html#stream-com.mbientlab.metawear.Subscriber->`_ 
component.  The data from the most recent producer will be sent live to the ``Subscriber``.

::

    public void streamData(DataProducer producer) {
        producer.addRouteAsync(new RouteBuilder() {
            @Override
            public void configure(RouteComponent source) {
                source.stream(new Subscriber() {
                    @Override
                    public void apply(Data data, Object... env) {
                        Log.i("MainActivity", data.toString());
                    }
                });
            }
        });
    }

Log
^^^
Alternatively, you may want to record data to the on-board flash memory and retrieve it at a later time.  Constructing a logging route follows the same 
steps as a streaming route except you use the 
`log <https://mbientlab.com/docs/metawear/android/latest/com/mbientlab/metawear/builder/RouteComponent.html#log-com.mbientlab.metawear.Subscriber->`_ 
component.  

::

    public void logData(DataProducer producer) {
        producer.addRouteAsync(new RouteBuilder() {
            @Override
            public void configure(RouteComponent source) {
                source.log(new Subscriber() {
                    @Override
                    public void apply(Data data, Object... env) {
                        Log.i("MainActivity", data.toString());
                    }
                });
            }
        });
    }

Note that this only creates a route to handle logged data; you still need to tell the logger when to start/stop logging data and when to download the 
recorded data.  More information on controlling the logger is provided in the :doc:`logging` section.

Reaction
--------
A reaction is a collection of MetaWear commands programmed onto the board that is executed when the source producer has created new data.  Developers 
can use this feature to have the board react to new data without needing maintain an active connection to the board.  

The MetaWear commands you want programmed onto the board are contained in an 
`Action <https://mbientlab.com/docs/metawear/android/latest/com/mbientlab/metawear/builder/RouteComponent.Action.html>`_ object which is passed into the 
`react <https://mbientlab.com/docs/metawear/android/latest/com/mbientlab/metawear/builder/RouteComponent.html#react-com.mbientlab.metawear.builder.RouteComponent.Action->`_ component.

::

    // Turn on the led everytime new data is available from the producer
    public void addReaction(DataProducer producer, final Led led) {
        producer.addRouteAsync(new RouteBuilder() {
            @Override
            public void configure(RouteComponent source) {
                source.react(new RouteComponent.Action() {
                    @Override
                    public void execute(DataToken token) {
                        led.editPattern(Led.Color.BLUE, Led.PatternPreset.SOLID);
                        led.play();
                    }
                });
            }
        });
    }

Split
-----
Splitters break down combined data into its individual components i.e. the xyz values in acceleration data.  When you add the 
`split <https://mbientlab.com/docs/metawear/android/latest/com/mbientlab/metawear/builder/RouteComponent.html#split-->`_ component, you can refer to each data 
component with the `index <https://mbientlab.com/docs/metawear/android/latest/com/mbientlab/metawear/builder/RouteComponent.html#index-int->`_ component.  Note 
that you must call ``index`` immediately after calling ``split``.

::

    public void splitAccData(AccelerationDataProducer acceleration) {
        // stream z-axis data from accelerometer
        acceleration.addRouteAsync(new RouteBuilder() {
            @Override
            public void configure(RouteComponent source) {
                source.split().index(2).stream(new Subscriber() {
                    @Override
                    public void apply(Data data, Object... env) {
                        Log.i("MainActivity", "Z-axis: " + data.value(Float.class));
                    }
                });
            }
        });
    }

Multicast
---------
The `multicast <https://mbientlab.com/docs/metawear/android/latest/com/mbientlab/metawear/builder/RouteComponent.html#multicast-->`_ component creates branches 
in the route where the same data can be pass to different route components.  Starting a new branch is expressed with the 
`to <https://mbientlab.com/docs/metawear/android/latest/com/mbientlab/metawear/builder/RouteMulticast.html#to-->`_ component and you can specify as many 
branches as you need provided the firmware has enough resources to allocate the additional route components.  Keep in mind that you must call ``to`` 
immediately after calling ``multicast``.  

::

    // Convert value to Kelvin and Farenheit for all temperature data
    public void createMulticast(Temperature.Sensor tempSensor) {
        tempSensor.addRouteAsync(new RouteBuilder() {
            @Override
            public void configure(RouteComponent source) {
                source.multicast()
                    .to().stream(new Subscriber() {
                        @Override
                        public void apply(Data data, Object... env) {
                            Log.i("MainActivity", "Celsius = " + data.value(Float.class));
                        }
                    }).to().map(Function2.MULTIPLY, 18)
                        .map(Function2.DIVIDE, 10)
                        .map(Function2.ADD, 32)
                        .stream(new Subscriber() {
                            @Override
                            public void apply(Data data, Object... env) {
                                Log.i("MainActivity", "Fahrenheit = " + data.value(Float.class));
                            }
                        })
                    .to().map(Function2.ADD, 273.15).stream(new Subscriber() {
                        @Override
                        public void apply(Data data, Object... env) {
                            Log.i("MainActivity", "Kelvin = " + data.value(Float.class));
                        }
                    });
            }
        });
    }

Data Processing
---------------
One of the neat features of the MetaWear firmware is the abiliy to manipulate data on-board before passing it to the user.  Processors can be chained 
together to combine multiple operations in one route.  Note that data processors can have ``stream`` and ``log`` components attached to them as well.

Data processors are identified by a globally unique name using the 
`name <https://mbientlab.com/docs/metawear/android/latest/com/mbientlab/metawear/builder/RouteComponent.html#name-java.lang.String->`_ component.  This name 
is used to identify the processor in the :doc:`data_processor` module or to construct feedback loops with the comparator or mapper.

Account
^^^^^^^
The accounter processor adds additional information to the BTLE packet to reconstruct the data's timestamp, typically used with streaming raw 
accelerometer, gyro, and magnetometer data.  This processor is designed specifically for streaming, do not use with the logger.  ::

    public void accountAccData(AccelerationDataProducer acceleration) {
        acceleration.addRouteAsync(new RouteBuilder() {
            @Override
            public void configure(RouteComponent source) {
                source.account().stream(new Subscriber() {
                    @Override
                    public void apply(Data data, Object... env) {
                        Log.i("MainActivity", "realtime: " + data.formattedTimestamp());
                    }
                });
            }
        });
    }

If there is not enough space to append timestamp data, i.e. sensor fusion outputs, a sample count can instead be added to the packet.  The count value is accessed via the 
`extra <http://mbientlab.com/docs/metawear/android/3.4/com/mbientlab/metawear/Data.html#extra-java.lang.Class->`_ function.  ::

    import com.mbientlab.metawear.builder.RouteComponent.AccountType;

    public async void accountData(IAsyncDataProducer producer) {
        acceleration.addRouteAsync(new RouteBuilder() {
            @Override
            public void configure(RouteComponent source) {
                source.account(AccountType.COUNT).stream(new Subscriber() {
                    @Override
                    public void apply(Data data, Object... env) {
                        Log.i("MainActivity", "sample: " + data.extra(Long.class));
                    }
                });
            }
        });
    }

Accumulate
^^^^^^^^^^
An accumlator tallies a running sum of all data that passes through.  The running sum can be reset to 0 or set to a specific value using an 
`AccumulatorEditor <https://mbientlab.com/docs/metawear/android/latest/com/mbientlab/metawear/module/DataProcessor.AccumulatorEditor.html>`_.  

::

    public void accumAbsRef(Gpio.Pin pin) {
        pin.analogAbsRef().addRouteAsync(new RouteBuilder() {
            @Override
            public void configure(RouteComponent source) {
                // sum the values from gpio abs reference voltage 
                source.accumulate();
            }
        });
    }

Average
^^^^^^^
This component is renamed to ``lowpass`` in SDK v3.1.

Buffer
^^^^^^
Buffers store the most recent input in its internal state which can accessed using the 
`state <https://mbientlab.com/docs/metawear/android/latest/com/mbientlab/metawear/module/DataProcessor.html#state-java.lang.String->`_ method from the 
:doc:`data_processor` module.  As there is no output from the 
`buffer <https://mbientlab.com/docs/metawear/android/latest/com/mbientlab/metawear/builder/RouteComponent.html#buffer-->`_ processor, you cannot chain 
additional route components after the buffer.

::

    public void bufferTempData(Temperature.Sensor tempSensor) {
        tempSensor.addRouteAsync(new RouteBuilder() {
            @Override
            public void configure(RouteComponent source) {
                // store temp data in buffer named "temp_buffer"
                // read buffer state with DataProcessor module
                source.buffer().name("temp_buffer");
            }
        });
    }

Count
^^^^^
Add a `count <https://mbientlab.com/docs/metawear/android/latest/com/mbientlab/metawear/builder/RouteComponent.html#count-->`_ component to tally the number 
of data samples received.  The output from this processor is the current running count.  Use a 
`CounterEditor <https://mbientlab.com/docs/metawear/android/latest/com/mbientlab/metawear/module/DataProcessor.CounterEditor.html>`_ to reset the count or 
set it to a specific value.

::

    public void countData(DataProducer producer) {
        producer.addRouteAsync(new RouteBuilder() {
            @Override
            public void configure(RouteComponent source) {
                // Count number of data samples produced
                source.count();
            }
        });
    }

Delay
^^^^^
The `delay <https://mbientlab.com/docs/metawear/android/latest/com/mbientlab/metawear/builder/RouteComponent.html#delay-byte->`_ component stalls further 
route activity until it has collected N samples.

::

    public void delayData(DataProducer producer) {
        producer.addRouteAsync(new RouteBuilder() {
            @Override
            public void configure(RouteComponent source) {
                // Collect 16 data samples before letting it pass
                source.split().index(2).delay((byte) 16);
            }
        });
    }

Filter
^^^^^^
Filter processors remove data that do not satisfy a given condition and are added to a route using the ``filter`` component.

Comparator
##########
The comparison 
`filter <https://mbientlab.com/docs/metawear/android/latest/com/mbientlab/metawear/builder/RouteComponent.html#filter-com.mbientlab.metawear.builder.filter.Comparison-java.lang.Number...->`_ 
removes data from the route whose value does not satisfy the comparison operation.  All 6 comparison operations (eq, neq, lt, lte, gt, gte) are 
supported.

::

    import com.mbientlab.metawear.builder.filter.Comparison;

    public void compareTempData(Temperature.Sensor tempSensor) {
        tempSensor.addRouteAsync(new RouteBuilder() {
            @Override
            public void configure(RouteComponent source) {
                // removes temperature data that is not greater than 21C 
                // from the route
                source.filter(Comparison.GT, 21f);
            }
        });
    }

As of firmware v1.2.3, the comparator has been updated to compare against multiple values.  The variant `filter <https://mbientlab.com/docs/metawear/android/latest/com/mbientlab/metawear/builder/RouteComponent.html#filter-com.mbientlab.metawear.builder.filter.Comparison-com.mbientlab.metawear.builder.filter.ComparisonOutput-java.lang.Number...->`_ 
component accepts an extra `ComparisonOutput <https://mbientlab.com/docs/metawear/android/latest/com/mbientlab/metawear/builder/filter/ComparisonOutput.html>`_ 
enum which provides other information about the multi-value comparison.  

===========  ======================================================================================================
Output       Descripion
===========  ======================================================================================================
Absolute     Input value is returned when the comparison is satisfied, behavior of old comparator
Reference    The reference value that satisified the comparison is outputted
Zone         Outputs the index (0 to n-1) of the reference value that satisfied the comparison, n if none are valid
Pass / Fail  0 if the comparison fails, 1 if it passed
===========  ======================================================================================================

::

    import com.mbientlab.metawear.builder.filter.ComparisonOutput;

    public void multiCompareTempData(Temperature.Sensor tempSensor) {
        tempSensor.addRouteAsync(new RouteBuilder() {
            @Override
            public void configure(RouteComponent source) {
                // Create 3 ranges: T < 0C [0], 0C < T < 21f [1], and 21C < T < 31C [2]
                // return which range the input resides in
                source.filter(Comparison.LT, ComparisonOutput.ZONE, 0f, 21f, 38f);
            }
        });
    }

Keep in mind that if you are using zone or pass/fail type comparisons, the comparison will be treated like a :ref:`map` component instead.  You will 
need to chain an additional absolute or reference type comparison to restore the original filter behavior.  ::

    public void zoneCompareFilter(Temperature.Sensor tempSensor) {
        tempSensor.addRouteAsync(new RouteBuilder() {
            @Override
            public void configure(RouteComponent source) {
                source.filter(Comparison.LT, ComparisonOutput.ZONE, 0f, 21f, 38f)
                        // do not let (zone == 3) values through i.e. prior zone comparison failed
                        .filter(Comparison.NEQ, ComparisonOutput.ABSOLUTE, 3);
            }
        });
    }

Differential
############
Differential filters compute the distance between the current data value and a reference value, and only lets the data through if the distance is 
greater than a set threshold.  When data that satisfies this criteria is found, the reference point will be updated to the last allowed value.

This filter also has three output modes that provide different information about the input data:

=============  ===================================================
Output         Description
=============  ===================================================
Absolute       Input passed through as is
Differential   Difference between current and reference  
Binary         1 if current < reference, -1 if current > reference  
=============  ===================================================

::

    import com.mbientlab.metawear.builder.filter.DifferentialOutput;

    public void adcDifferentialFilter(Gpio.Pin pin) {
        pin.analogAdc().addRouteAsync(new RouteBuilder() {
            @Override
            public void configure(RouteComponent source) {
                // Remove ADC data that is not at least 128 steps 
                // from the reference point
                // Output the difference between the reference and input values
                source.filter(DifferentialOutput.DIFFERENCE, 128);
            }
        });
    }

Threshold
#########
The threshold filter only allows data through whose value crosses a boundary value, whether rising above the boundary or falling below it.  It also has 
an alternate output mode that reports which direction the boundary was crossed.

=============  ==========================================
Output         Transformation                            
=============  ==========================================
Absolute       Input passed through untouched            
Binary         1 if value rose above, -1 if it fell below
=============  ==========================================

To prevent oscillations around the boundary from sending multiple data samples through, a hysteresis value can be set so that the threshold filter will 
only allow values that cross the boundary and lay outside the range [boundary - hysteresis, boundary + hysteresis].

::

    public void thsAccXAxis(AccelerationDataProducer acceleration) {
        acceleration.addRouteAsync(new RouteBuilder() {
            @Override
            public void configure(RouteComponent source) {
                // let x-axis acceleration data through if it crosses the 1g boundary 
                // with +/- 0.0001g of hysteresis i.e. 
                // must be below 0.999g and above 1.0001g
                source.split().index(0).filter(ThresholdOutput.BINARY, 1f, 0.001f);
            }
        });
    }

Find
^^^^
The ``find`` component scans the data to see if it satisfies a pattern.  Currently, the only available pattern is a pulse which is defined as a minimum 
number of consecutive data points that rises above then falls below a threshold.  Both the threshold and minimum sample size can be later modified using 
a `PulseEditor <https://mbientlab.com/docs/metawear/android/latest/com/mbientlab/metawear/module/DataProcessor.PulseEditor.html>`_.

This processor also has 4 output modes that provide different contextual information about the pulse.

========= ========================================
Output    Description                             
========= ========================================
Width     Number of samples that made up the pulse
Area      Summation of all the data in the pulse  
Peak      Highest value in the pulse              
On Detect Return 0x1 as soon as pulse is detected 
========= ========================================

::

    public void findAdcPulse(Gpio.Pin pin) {
        pin.analogAdc().addRouteAsync(new RouteBuilder() {
            @Override
            public void configure(RouteComponent source) {
                // find a pulse that has a minimum of 16 samples 
                // rise above then fall below  512
                // Output the max value of the pulse
                source.find(PulseOutput.PEAK, 512, (short) 16);
            }
        });
    }

Fuser
^^^^^
The fuser processor combines data from multiple sensors into 1 message.  When fusing multiple data sources, ensure that they are sampling at the same frequency, or at the very least, 
integer multiples of the fastest frequency.  Data sources sampling at the lower frequencies will repeat the last received value.

To use the fuser, you first need to direct the other pieces of data to a named :ref:`buffer` processor.  Then, pass the processor names into the 
`fuse <https://mbientlab.com/documents/metawear/android/3.6/com/mbientlab/metawear/builder/RouteComponent.html#fuse-java.lang.String...->`_ component.  ::

    public void fuseImuData(Accelerometer acc, GyroBmi160 gyro) 
        gyro.angularVelocity().addRouteAsync(source ->
                source.buffer().name("gyro-buffer")
        ).onSuccessTask(ignored ->acc.acceleration().addRouteAsync(source ->
                source.fuse("gyro-buffer").limit(20).stream((data, env) -> {
                    Data[] values = data.value(Data[].class);
                    // accelerometer is the source input, index 0
                    // gyro name is first input, index 1
                    System.out.printf("acc = %s, gyro = %s%n", values[0].value(Acceleration.class), 
                            values[1].value(AngularVelocity.class));
                })
        ));
    }

Unlike the other data sources, fuser data is represented as an `Data <https://mbientlab.com/docs/metawear/android/latest/com/mbientlab/metawear/Data.html>`_ array, which is indexed based on the 
order of the buffer names passed into ``fuse`` component.  

High Pass
^^^^^^^^^
High pass filters compute the difference of the current value from a running average of the previous N samples.  Output from this processor is delayed 
until the first N samples have been received.  Use the 
`AverageEditor <https://mbientlab.com/docs/metawear/android/latest/com/mbientlab/metawear/module/DataProcessor.AverageEditor.html>`_ to reset the running 
average.  ::

    public void hpfAccData(AccelerationDataProducer acceleration) {
        acceleration.addRouteAsync(new RouteBuilder() {
            @Override
            public void configure(RouteComponent source) {
                // delay stream by 4 samples, 5th sample and on are high pass filtered
                source.highpass((byte) 4).stream(new Subscriber() {
                    @Override
                    public void apply(Data data, Object... env) {
                        Log.i("MainActivity", "hpf acc = " + data.value(Acceleration.class));
                    }
                });
            }
        });
    }

Low Pass
^^^^^^^^
Low pass filters compute a running average of the current and previous N samples.  Output from this processor is delayed until the first N samples have 
been received.  Use the `AverageEditor <https://mbientlab.com/docs/metawear/android/latest/com/mbientlab/metawear/module/DataProcessor.AverageEditor.html>`_ 
to reset the running average.  ::

    public void averageAdc(Gpio.Pin pin) {
        pin.analogAdc().addRouteAsync(new RouteBuilder() {
        @Override
            public void configure(RouteComponent source) {
                // compute running average over 4 ADC values
                source.average((byte) 4);
            }
        });
    }

Limit
^^^^^
Limiters control the amount of data that flows through the route.  Add them to a route using the ``limit`` component.

Passthrough
###########
The passthrough limiter functions as a user controlled gate using the ``value`` parameter to determine when to let data pass.  There are three types of 
`passthrough <https://mbientlab.com/docs/metawear/android/latest/com/mbientlab/metawear/builder/filter/Passthrough.html>`_ limiters:

=========== ============================================
Type        Description
=========== ============================================
All         Allows all data to pass
Conditional Only allow data through if value > 0
Count       Only allow a fixed number of samples through
=========== ============================================

Both the ``value`` and ``type`` parameters can be modified using a 
`PassthroughEditor <https://mbientlab.com/docs/metawear/android/latest/com/mbientlab/metawear/module/DataProcessor.PassthroughEditor.html>`_.

::

    public void dataPassthrough(DataProducer producer) {
        producer.addRouteAsync(new RouteBuilder() {
            @Override
            public void configure(RouteComponent source) {
                // Only allow 16 data samples through
                // Use DataProcessor module to reset the count when all 16 values pass
                source.limit(Passthrough.COUNT, (short) 16).name("acc_passthrough");
            }
        });
    }

Time
####
Time limiters reduce the frequency at which data flows through the route.  They are typically used to stream data at frequencies not 
natively supported by the sensor, or combined with a data processing chain to only stream processed data at certain intervals.

::

    public void limitData(DataProducer producer) {
        producer.addRouteAsync(new RouteBuilder() {
            @Override
            public void configure(RouteComponent source) {
                // Reduce data rate to 10Hz
                source.limit(100);
            }
        });
    }

Map
^^^
A mapper applies a function to the data letting developers modify the value of each data sample.  All basic arithemtic is supported along with some 
bit shifting, sqrt, vector magnitude and rms.

::

    public void mapAccData(AccelerationDataProducer acceleration) {
        acceleration.addRouteAsync(new RouteBuilder() {
            @Override
            public void configure(RouteComponent source) {
                // Apply the RMS function to all acceleration data
                source.map(Function1.RMS)
            }
        });
    }

Packer
^^^^^^
The packer processor combines multiple data samples into 1 BLE packet to increase the data throughput.  You can pack between 4 to 8 samples per packet 
depending on the data size.

Note that if you use the packer processor with raw motion data instead of using their packed data producer variants, you will only be able to combine 2 
data samples into a packet instead of 3 samples however, you can chain an accounter processor to associate a timestamp with the packed data.  ::

    public void packData(DataProducer producer) {
        producer.addRouteAsync(new RouteBuilder() {
            @Override
            public void configure(RouteComponent source) {
                // Combine 4 data samples into 1 BLE packet
                source.limit((byte) 4).stream(new Subscriber() {
                    int count = 0;
                    @Override
                    public void apply(Data data, Object... env) {
                        Log.i("MainActivity", "Samples: " + count);
                        count++;
                    }
                });
            }
        });
    }
