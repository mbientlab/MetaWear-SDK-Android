package com.mbientlab.metawear.module;

import com.mbientlab.metawear.DataSignal;
import com.mbientlab.metawear.MetaWearBoard;

/**
 * Created by etsai on 1/7/2016.
 */
public interface Bmm150Magnetometer extends MetaWearBoard.Module {
    enum PowerPreset {
        LOW_POWER,
        REGULAR,
        ENHANCED_REGULAR,
        HIGH_ACCURACY
    }

    interface SourceSelector {
        DataSignal fromBField();
    }

    SourceSelector routeData();

    void setPowerPrsest(PowerPreset preset);

    void disableBFieldSampling();
    void enableBFieldSampling();

    void start();
    void stop();
}
