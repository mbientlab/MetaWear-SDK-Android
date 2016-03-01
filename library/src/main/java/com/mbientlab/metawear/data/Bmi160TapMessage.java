package com.mbientlab.metawear.data;

import com.mbientlab.metawear.Message;
import com.mbientlab.metawear.module.Bmi160Accelerometer.*;

import java.util.Calendar;

/**
 * Container class for tap data from the BMI160 and BMA255 accelerometers.  The data can only be interpreted as a
 * TapResponse type.
 * @author Eric Tsai
 * @see TapResponse
 */
public class Bmi160TapMessage extends Message {
    private final TapResponse response;

    public Bmi160TapMessage(byte[] data) {
        this(null, data);
    }

    public Bmi160TapMessage(Calendar timestamp, final byte[] data) {
        super(timestamp, data);

        response= new TapResponse() {
            @Override
            public TapType type() {
                if ((data[0] & 0x1) == 0x1) {
                    return TapType.DOUBLE;
                } else if ((data[0] & 0x2) == 0x2) {
                    return TapType.SINGLE;
                } else {
                    return null;
                }
            }

            @Override
            public Sign sign() {
                return (data[0] & 0x20) == 0x20 ? Sign.NEGATIVE : Sign.POSITIVE;
            }

            @Override
            public String toString() {
                return String.format("{type: %s, sign: %s}", type(), sign());
            }
        };
    }

    @Override
    public <T> T getData(Class<T> type) {
        if (type.equals(TapResponse.class)) {
            return type.cast(response);
        }

        throw new UnsupportedOperationException(String.format("Type \'%s\' not supported for message class: %s",
                type.toString(), getClass().toString()));
    }
}
