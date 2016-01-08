package com.mbientlab.metawear.data;

import java.util.Calendar;

/**
 * Created by etsai on 1/8/2016.
 */
public class Bmm150SingleAxisUnsignedMessage extends UnsignedMessage {
    public static float getScale() { return Bmm150ThreeAxisMessage.getScale(); }

    public Bmm150SingleAxisUnsignedMessage(byte[] data) {
        this(null, data);
    }

    public Bmm150SingleAxisUnsignedMessage(Calendar timestamp, byte[] data) {
        super(timestamp, data);
    }

    @Override
    public <T> T getData(Class<T> type) {
        if (type.equals(Float.class)) {
            return type.cast(super.getData(Long.class) / Bmm150SingleAxisUnsignedMessage.getScale());
        }

        throw new UnsupportedOperationException(String.format("Type \'%s\' not supported for message class: %s",
                type.toString(), getClass().toString()));
    }
}
