package com.mbientlab.metawear.data;

import java.util.Calendar;

/**
 * Container class for one axis of magnetometer data from the MMA8452Q chip.  The data can only be interpreted
 * as a float and is returned in units of micro Teslas.
 * @author Eric Tsai
 */
public class Bmm150SingleAxisMessage extends SignedMessage {
    public static float getScale() { return Bmm150ThreeAxisMessage.getScale(); }

    public Bmm150SingleAxisMessage(byte[] data) {
        this(null, data);
    }

    public Bmm150SingleAxisMessage(Calendar timestamp, byte[] data) {
        super(timestamp, data);
    }

    @Override
    public <T> T getData(Class<T> type) {
        if (type.equals(Float.class)) {
            return type.cast(super.getData(Integer.class) / Bmm150SingleAxisMessage.getScale());
        }

        throw new UnsupportedOperationException(String.format("Type \'%s\' not supported for message class: %s",
                type.toString(), getClass().toString()));

    }
}
