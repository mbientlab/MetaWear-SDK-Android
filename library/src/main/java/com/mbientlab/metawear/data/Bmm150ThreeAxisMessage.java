package com.mbientlab.metawear.data;

import com.mbientlab.metawear.Message;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Calendar;

/**
 * Created by etsai on 1/8/2016.
 */
public class Bmm150ThreeAxisMessage extends Message {
    private static final float SCALE = 16f;
    public static float getScale() { return SCALE; }

    private final float[] fieldStrength;

    public Bmm150ThreeAxisMessage(byte[] data) {
        this(null, data);
    }

    public Bmm150ThreeAxisMessage(Calendar timestamp, byte[] data) {
        super(timestamp, data);

        if (data.length >= 6) {
            ByteBuffer buffer = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN);
            short x = buffer.getShort(), y = buffer.getShort(), z = buffer.getShort();

            fieldStrength = new float[]{
                    x / SCALE,
                    y / SCALE,
                    z / SCALE,
            };
        } else {
            fieldStrength= null;
        }
    }

    @Override
    public <T> T getData(Class<T> type) {
        if (type.equals(CartesianFloat.class)) {
            return type.cast(new CartesianFloat() {
                @Override
                public Float x() {
                    return fieldStrength[0];
                }

                @Override
                public Float y() {
                    return fieldStrength[1];
                }

                @Override
                public Float z() {
                    return fieldStrength[2];
                }
            });
        }

        throw new UnsupportedOperationException(String.format("Type \'%s\' not supported for message class: %s",
                type.toString(), getClass().toString()));
    }
}
