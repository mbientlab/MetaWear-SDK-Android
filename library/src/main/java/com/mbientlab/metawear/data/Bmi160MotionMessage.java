package com.mbientlab.metawear.data;

import com.mbientlab.metawear.Message;
import com.mbientlab.metawear.module.Bmi160Accelerometer.*;

import java.util.Calendar;

/**
 * Container class for motion data from the BMI160 and BMA255 accelerometers.  The data can only be interpreted as a
 * MotionResponse type.
 * @author Eric Tsai
 * @see MotionResponse
 */
public class Bmi160MotionMessage extends Message {
    private final MotionResponse response;

    public Bmi160MotionMessage(byte[] data) {
        this(null, data);
    }

    public Bmi160MotionMessage(Calendar timestamp, final byte[] data) {
        super(timestamp, data);

        response= new MotionResponse() {
            @Override
            public Sign anyMotionSign() {
                return (data[0] & 0x40) == 0x40 ? Sign.NEGATIVE : Sign.POSITIVE;
            }

            @Override
            public boolean anyMotionDetected(Axis axis) {
                byte mask= (byte) (0x1 << (axis.ordinal() + 3));
                return (data[0] & mask) == mask;
            }

            @Override
            public String toString() {
                return String.format("{any_motion_sign: %s, any_motion_x: %s, any_motion_y: %s, any_motion_z: %s}",
                        anyMotionSign(), anyMotionDetected(Axis.X), anyMotionDetected(Axis.Y), anyMotionDetected(Axis.Z));
            }
        };
    }

    @Override
    public <T> T getData(Class<T> type) {
        if (type.equals(MotionResponse.class)) {
            return type.cast(response);
        }

        throw new UnsupportedOperationException(String.format("Type \'%s\' not supported for message class: %s",
                type.toString(), getClass().toString()));
    }
}
