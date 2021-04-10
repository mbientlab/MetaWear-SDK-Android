.. highlight:: java

Data Processor
==============
As mentioned in the :doc:`data_route` page, the data processor can perform simple calcutaions on the data before passing it to the user.  Adding and 
configuring processors is handled with the ``RouteBuilder`` api and the 
`DataProcessor <https://mbientlab.com/docs/metawear/android/latest/com/mbientlab/metawear/module/DataProcessor.html>`_ interface is used to interact with the 
processors that were tagged with the 
`name <https://mbientlab.com/docs/metawear/android/latest/com/mbientlab/metawear/builder/RouteComponent.html#name-java.lang.String->`_ component.

::

    import com.mbientlab.metawear.module.DataProcessor;

    final DataProcessor dataproc = board.getModule(DataProcessor.class);

Modifying Processors
--------------------
Some processors have parameters that can be modified after they are created.  Call 
`edit <https://mbientlab.com/docs/metawear/android/latest/com/mbientlab/metawear/module/DataProcessor.html#edit-java.lang.String-java.lang.Class->`_ using 
the same string passed into the ``name`` component and the type of editor to return.  Make sure the editor class is appropriate for the desired 
processor.

============ ================== ======================================
Processor    Editor             Editable Parameters
============ ================== ======================================
Accumulate   AccumulatorEditor  Accumulated sum
Average      AverageEditor      Sample size, reset running avg
Comparison   ComparatorEditor   Comparison operation, reference values
Counter      CounterEditor      Accumulated count
Differential DifferentialEditor Minimum distance
Map          MapEditor          Right hand value for 2 op functions
Passthrough  PassthroughEditor  Passthrough type and value
Pulse        PulseEditor        Threshold and min sample size
Threshold    ThresholdEditor    Boundary and hysteresis
Time         TimeEditor         Period
============ ================== ======================================

::

    import com.mbientlab.metawear.module.DataProcessor.CounterEditor;

    // assume route built with
    // .count().name("counter")

    // reset internal count
    dataproc.edit("counter", CounterEditor.class).reset();


Internal State
--------------
Some processors have an internal state, represented as a forced data producer.  These state data producers are retrieved with the 
`state <https://mbientlab.com/docs/metawear/android/latest/com/mbientlab/metawear/module/DataProcessor.html#state-java.lang.String->`_ method using the same 
string passed into the ``name`` component.

=========== =========================
Processor   Description
=========== =========================
Accumulate  Current accumulated sum
Buffer      Last received input
Counter     Current accumulated count
Passthrough Value parameter
=========== =========================

::

    // assume route built with
    // .buffer().name("buffer")

    // access the buffer state
    ForcedDataProducer producer = dataproc.state("buffer")
