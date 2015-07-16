package com.mbientlab.metawear.module;

import com.mbientlab.metawear.MetaWearBoard;

import java.util.HashMap;

/**
 * Created by etsai on 7/9/2015.
 */
public interface Bmi160Gyro extends MetaWearBoard.Module {
    public enum OutputDataRate {
        ODR_25_HZ,
        ODR_50_HZ,
        ODR_100_HZ,
        ODR_200_HZ,
        ODR_400_HZ,
        ODR_800_HZ,
        ODR_1600_HZ,
        ODR_3200_HZ;

        public byte bitMask() { return (byte) (ordinal() + 6);}
    }

    public enum FullScaleRange {
        FSR_2000 {
            @Override
            public float scale() { return 16.4f; }
        },
        FSR_1000 {
            @Override
            public float scale() { return 32.8f; }
        },
        FSR_500 {
            @Override
            public float scale() { return 65.6f; }
        },
        FSR_250 {
            @Override
            public float scale() { return 131.2f; }
        },
        FSR_125 {
            @Override
            public float scale() { return 262.4f; }
        };

        public abstract float scale();
        public byte bitMask() { return (byte) ordinal(); }

        private static final HashMap<Byte, FullScaleRange> bitMaskToRanges;
        static {
            bitMaskToRanges= new HashMap<>();
            for(FullScaleRange it: FullScaleRange.values()) {
                bitMaskToRanges.put(it.bitMask(), it);
            }
        }
        public static FullScaleRange bitMaskToRange(byte mask) {
            return bitMaskToRanges.get(mask);
        }
    }

    public interface ConfigEditor {
        public ConfigEditor withFullScaleRange(FullScaleRange range);
        public ConfigEditor withOutputDataRate(OutputDataRate odr);
        public void commit();
    }
    public ConfigEditor configure();

    public void globalStart();
    public void globalStop();

}
