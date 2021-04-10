package com.mbientlab.metawear.impl;

import com.mbientlab.metawear.IllegalRouteOperationException;
import com.mbientlab.metawear.builder.RouteComponent;
import com.mbientlab.metawear.builder.filter.ComparisonOutput;
import com.mbientlab.metawear.builder.filter.DifferentialOutput;
import com.mbientlab.metawear.builder.filter.ThresholdOutput;
import com.mbientlab.metawear.builder.predicate.PulseOutput;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.security.InvalidParameterException;
import java.util.Locale;

import static com.mbientlab.metawear.impl.RouteComponentImpl.MULTI_CHANNEL_MATH;
import static com.mbientlab.metawear.impl.RouteComponentImpl.MULTI_COMPARISON_MIN_FIRMWARE;

/**
 * Created by eric on 8/27/17.
 */

abstract class DataProcessorConfig {
    static DataProcessorConfig from(Version firmware, byte revision, byte[] config) {
        switch(config[0]) {
            case Passthrough.ID:
                return new Passthrough(config);
            case Accumulator.ID:
                return new Accumulator(config);
            case Average.ID:
                return new Average(config);
            case Comparison.ID:
                return firmware.compareTo(MULTI_COMPARISON_MIN_FIRMWARE) >= 0 ?
                        new MultiValueComparison(config) : new SingleValueComparison(config);
            case Combiner.ID:
                return new Combiner(config);
            case Time.ID:
                return new Time(config);
            case Maths.ID:
                return new Maths(firmware.compareTo(MULTI_CHANNEL_MATH) >= 0, config);
            case Delay.ID:
                return new Delay(revision >= DataProcessorImpl.EXPANDED_DELAY, config);
            case Pulse.ID:
                return new Pulse(config);
            case Differential.ID:
                return new Differential(config);
            case Threshold.ID:
                return new Threshold(config);
            case Buffer.ID:
                return new Buffer(config);
            case Packer.ID:
                return new Packer(config);
            case Accounter.ID:
                return new Accounter(config);
            case Fuser.ID:
                return new Fuser(config);
        }
        throw new InvalidParameterException("Unrecognized config id: " + config[0]);
    }

    final byte id;

    DataProcessorConfig(byte id) {
        this.id = id;
    }
    abstract byte[] build();
    abstract String createUri(boolean state, byte procId);

    static class Passthrough extends DataProcessorConfig {
        static final byte ID = 0x1;

        final com.mbientlab.metawear.builder.filter.Passthrough type;
        final short value;

        Passthrough(byte[] config) {
            super(config[0]);

            type = com.mbientlab.metawear.builder.filter.Passthrough.values()[config[1] & 0x7];
            value = ByteBuffer.wrap(config, 2, 2).order(ByteOrder.LITTLE_ENDIAN).getShort();
        }

        Passthrough(com.mbientlab.metawear.builder.filter.Passthrough mode, short value) {
            super(ID);

            this.type = mode;
            this.value = value;
        }

        @Override
        byte[] build() {
            return ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN)
                    .put(ID)
                    .put((byte) (type.ordinal() & 0x7))
                    .putShort(value).array();
        }

        @Override
        String createUri(boolean state, byte procId) {
            return String.format(Locale.US, "passthrough%s?id=%d", state ? "-state" : "", procId);
        }
    }

    static class Accumulator extends DataProcessorConfig {
        static final byte ID = 0x2;

        final boolean counter;
        final byte output;
        final byte input;

        Accumulator(boolean counter, byte output, byte input) {
            super(ID);

            this.counter = counter;
            this.output = output;
            this.input = input;
        }

        Accumulator(byte[] config) {
            super(config[0]);

            counter = (config[1] & 0x10) == 0x10;
            output = (byte) ((config[1] & 0x3) + 1);
            input = (byte) (((config[1] >> 2) & 0x3) + 1);
        }

        @Override
        byte[] build() {
            return new byte[] {ID, (byte) (((output - 1) & 0x3) | (((input - 1) & 0x3) << 2) | (counter ? 0x10 : 0))};
        }

        @Override
        String createUri(boolean state, byte procId) {
            return String.format(Locale.US, "%s%s?id=%d", counter ? "count" : "accumulate", state ? "-state" : "", procId);
        }
    }

    static class Average extends DataProcessorConfig {
        static final byte ID = 0x3;

        final byte output;
        final byte input;
        final byte samples;
        byte nInputs;
        boolean hpf, supportsHpf;

        Average(DataAttributes attributes, byte samples, boolean hpf, boolean supportsHpf) {
            super(ID);

            this.output = attributes.length();
            this.input = attributes.length();
            this.samples = samples;
            this.nInputs = (byte) attributes.sizes.length;
            this.hpf = hpf;
            this.supportsHpf = supportsHpf;
        }

        Average(byte[] config) {
            super(config[0]);

            output = (byte) ((config[1] & 0x3) + 1);
            input = (byte) (((config[1] >> 2) & 0x3) + 1);
            samples = config[2];

            if (config.length == 4) {
                nInputs = config[3];
                hpf = (config[1] >> 5) == 1;
                supportsHpf = true;
            }
        }

        @Override
        byte[] build() {
            ByteBuffer buffer = ByteBuffer.allocate(supportsHpf ? 4 : 3).order(ByteOrder.LITTLE_ENDIAN)
                    .put(ID)
                    .put((byte) ((((output - 1) & 0x3) | (((input - 1) & 0x3) << 2)) | ((supportsHpf ? (hpf ? 1 : 0) : 0) << 5)))
                    .put(samples);
            if (supportsHpf) {
                buffer.put((byte) (nInputs - 1));
            }

            return buffer.array();
        }

        @Override
        String createUri(boolean state, byte procId) {
            return String.format(Locale.US, "%s?id=%d", hpf ? "high-pass" : "low-pass", procId);
        }
    }

    static abstract class Comparison extends  DataProcessorConfig {
        static final byte ID = 0x6;

        Comparison(byte id) {
            super(id);
        }

        @Override
        String createUri(boolean state, byte procId) {
            return String.format(Locale.US, "comparison?id=%d", procId);
        }
    }
    static class MultiValueComparison extends Comparison {
        static void fillReferences(ByteBuffer buffer, byte length, Number... references) {
            switch(length) {
                case 1:
                    for(Number it: references) {
                        buffer.put(it.byteValue());
                    }
                    break;
                case 2:
                    for(Number it: references) {
                        buffer.putShort(it.shortValue());
                    }
                    break;
                case 4:
                    for(Number it: references) {
                        buffer.putInt(it.intValue());
                    }
                    break;
            }
        }

        static Number[] extractReferences(ByteBuffer buffer, byte length) {
            Number[] references = null;
            int remaining = buffer.capacity() - buffer.position();

            switch(length) {
                case 1:
                    references = new Number[remaining];
                    for(int i = 0; i < references.length; i++) {
                        references[i] = buffer.get();
                    }
                    break;
                case 2:
                    references = new Number[remaining / 2];
                    for(int i = 0; i < references.length; i++) {
                        references[i]= buffer.getShort();
                    }
                    break;
                case 4:
                    references = new Number[remaining / 4];
                    for(int i = 0; i < references.length; i++) {
                        references[i]= buffer.getInt();
                    }
                    break;
            }

            return references;
        }

        byte input;
        final Number[] references;
        final com.mbientlab.metawear.builder.filter.Comparison op;
        final ComparisonOutput mode;
        final boolean isSigned;

        MultiValueComparison(boolean isSigned, byte input, com.mbientlab.metawear.builder.filter.Comparison op, ComparisonOutput mode, Number[] references) {
            super(ID);

            this.isSigned = isSigned;
            this.input = input;
            this.op = op;
            this.mode = mode;
            this.references = references;
        }

        MultiValueComparison(byte[] config) {
            super(config[0]);

            isSigned = (config[1] & 0x1) == 0x1;
            input = (byte) (((config[1] >> 1) & 0x3) + 1);
            op = com.mbientlab.metawear.builder.filter.Comparison.values()[(config[1] >> 3) & 0x7];
            mode = ComparisonOutput.values()[(config[1] >> 6) & 0x3];

            references = extractReferences(ByteBuffer.wrap(config, 2, config.length - 2).order(ByteOrder.LITTLE_ENDIAN), input);
        }

        @Override
        byte[] build() {
            ByteBuffer buffer= ByteBuffer.allocate(2 + references.length * input).order(ByteOrder.LITTLE_ENDIAN)
                    .put((byte) 0x6)
                    .put((byte) ((isSigned ? 1 : 0) | ((input - 1) << 1) | (op.ordinal() << 3) | (mode.ordinal() << 6)));
            fillReferences(buffer, input, references);

            return buffer.array();
        }
    }
    static class SingleValueComparison extends Comparison {
        final boolean isSigned;
        final com.mbientlab.metawear.builder.filter.Comparison op;
        final int reference;

        SingleValueComparison(boolean isSigned, com.mbientlab.metawear.builder.filter.Comparison op, int reference) {
            super(ID);

            this.isSigned = isSigned;
            this.op = op;
            this.reference = reference;
        }

        SingleValueComparison(byte[] config) {
            super(config[0]);

            isSigned = config[1] == 0x1;
            op = com.mbientlab.metawear.builder.filter.Comparison.values()[config[2]];
            reference = ByteBuffer.wrap(config, 4, 4).order(ByteOrder.LITTLE_ENDIAN).getInt();
        }

        @Override
        byte[] build() {
            return ByteBuffer.allocate(8).order(ByteOrder.LITTLE_ENDIAN)
                    .put(ID)
                    .put((byte) (isSigned ? 1 : 0))
                    .put((byte) op.ordinal())
                    .put((byte) 0)
                    .putInt(reference).array();
        }
    }

    static class Combiner extends DataProcessorConfig {
        static final byte ID = 0x7;

        final byte output;
        final byte input;
        final byte nInputs;
        final boolean isSigned;
        final boolean rss;

        Combiner(DataAttributes attributes, boolean rss) {
            super(ID);

            this.output = attributes.sizes[0];
            this.input = attributes.sizes[0];
            this.nInputs = (byte) attributes.sizes.length;
            this.isSigned = attributes.signed;
            this.rss = rss;
        }

        Combiner(byte[] config) {
            super(config[0]);

            output = (byte) ((config[1] & 0x3) + 1);
            input = (byte) (((config[1] >> 2) & 0x3) + 1);
            nInputs = (byte) (((config[1] >> 4) & 0x3) + 1);
            isSigned = (config[1] & 0x80) == 0x80;
            rss = config[2] == 1;
        }

        @Override
        byte[] build() {
            return new byte[] {
                    ID,
                    (byte) (((output - 1) & 0x3) | (((input - 1) & 0x3) << 2) | (((nInputs - 1) & 0x3) << 4) | (isSigned ? 0x80 : 0)),
                    (byte) (rss ? 1 : 0)
            };
        }

        @Override
        String createUri(boolean state, byte procId) {
            return String.format(Locale.US, "%s?id=%d", rss ? "rss" : "rms", procId);
        }
    }

    static class Time extends DataProcessorConfig {
        static final byte ID = 0x8;

        final byte input;
        final byte type;
        final int period;

        Time(byte input, byte type, int period) {
            super(ID);

            this.input = input;
            this.type = type;
            this.period = period;
        }

        Time(byte[] config) {
            super(config[0]);

            period = ByteBuffer.wrap(config, 2, 4).order(ByteOrder.LITTLE_ENDIAN).getInt();
            input = (byte) ((config[1] & 0x7) + 1);
            type = (byte) ((config[1] >> 3) & 0x7);
        }

        @Override
        byte[] build() {
            return ByteBuffer.allocate(6).order(ByteOrder.LITTLE_ENDIAN).put(ID)
                    .put((byte) ((input - 1) & 0x7 | (type << 3))).putInt(period).array();
        }

        @Override
        String createUri(boolean state, byte procId) {
            return String.format(Locale.US, "time?id=%d", procId);
        }
    }

    static class Maths extends DataProcessorConfig {
        static final byte ID = 0x9;

        enum Operation {
            /** Add the data */
            ADD,
            /** Multiply the data */
            MULTIPLY,
            /** Divide the data */
            DIVIDE,
            /** Calculate the remainder */
            MODULUS,
            /** Calculate exponentiation of the data */
            EXPONENT,
            /** Calculate square root */
            SQRT,
            /** Perform left shift */
            LEFT_SHIFT,
            /** Perform right shift */
            RIGHT_SHIFT,
            /** Subtract the data */
            SUBTRACT,
            /** Calculates the absolute value */
            ABS_VALUE,
            /** Transforms the input into a constant value */
            CONSTANT
        }

        byte output;
        final byte input;
        byte nInputs;
        final boolean isSigned;
        boolean multiChnlMath;
        final Operation op;
        final int rhs;

        Maths(DataAttributes attributes, boolean multiChnlMath, Operation op, int rhs) {
            super(ID);

            this.output = -1;
            this.input = attributes.sizes[0];
            this.nInputs = (byte) attributes.sizes.length;
            this.isSigned = attributes.signed;
            this.multiChnlMath = multiChnlMath;
            this.op = op;
            this.rhs = rhs;
        }

        Maths(boolean multiChnlMath, byte[] config) {
            super(config[0]);

            output = (byte) ((config[1] & 0x3) + 1);
            input = (byte) (((config[1] >> 2) & 0x3) + 1);
            isSigned = (config[1] & 0x10) == 0x10;
            op = Operation.values()[config[2] - 1];
            rhs = ByteBuffer.wrap(config, 3, 4).order(ByteOrder.LITTLE_ENDIAN).getInt();

            if (multiChnlMath) {
                this.multiChnlMath = true;
                nInputs = (byte) (config[7] + 1);
            }
        }

        @Override
        byte[] build() {
            if (output == -1) {
                throw new IllegalStateException("Output length cannot be negative");
            }
            ByteBuffer buffer= ByteBuffer.allocate(multiChnlMath ? 8 : 7)
                    .order(ByteOrder.LITTLE_ENDIAN)
                    .put(ID)
                    .put((byte) ((output - 1) & 0x3 | ((input - 1) << 2) | (isSigned ? 0x10 : 0)))
                    .put((byte) (op.ordinal() + 1))
                    .putInt(rhs);
            if (multiChnlMath) {
                buffer.put((byte) (nInputs - 1));
            }

            return buffer.array();
        }

        @Override
        String createUri(boolean state, byte procId) {
            return String.format(Locale.US, "math?id=%d", procId);
        }
    }

    static class Delay extends DataProcessorConfig {
        static final byte ID = 0xa;

        final boolean expanded;
        final byte input;
        final byte samples;

        Delay(boolean expanded, byte input, byte samples) {
            super(ID);

            this.expanded = expanded;
            this.input = input;
            this.samples = samples;
        }

        Delay(boolean expanded, byte[] config) {
            super(config[0]);

            this.expanded = expanded;
            input = (byte) ((config[1] & (expanded ? 0xf : 0x3)) + 1);
            samples = config[2];
        }

        @Override
        byte[] build() {
            return new byte[]{ID, (byte) ((input - 1) & (expanded ? 0xf : 0x3)), samples};
        }

        @Override
        String createUri(boolean state, byte procId) {
            return String.format(Locale.US, "delay?id=%d", procId);
        }
    }

    static class Pulse extends DataProcessorConfig {
        static final byte ID = 0xb;

        final byte input;
        final int threshold;
        final short samples;
        final PulseOutput mode;

        Pulse(byte input, int threshold, short samples, PulseOutput mode) {
            super(ID);

            this.input = input;
            this.threshold = threshold;
            this.samples = samples;
            this.mode = mode;
        }

        Pulse(byte[] config) {
            super(config[0]);

            input = (byte) (config[2] + 1);
            threshold = ByteBuffer.wrap(config, 4, 4).getInt();
            samples = ByteBuffer.wrap(config, 8, 2).getShort();
            mode = PulseOutput.values()[config[4]];
        }

        byte[] build() {
            return ByteBuffer.allocate(10).order(ByteOrder.LITTLE_ENDIAN)
                    .put(ID)
                    .put((byte) (input- 1))
                    .put((byte) 0)
                    .put((byte) mode.ordinal())
                    .putInt(threshold)
                    .putShort(samples)
                    .array();
        }

        @Override
        String createUri(boolean state, byte procId) {
            return String.format(Locale.US, "pulse?id=%d", procId);
        }
    }

    static class Differential extends DataProcessorConfig {
        static final byte ID = 0xc;

        final byte input;
        final boolean isSigned;
        final DifferentialOutput mode;
        final int differential;

        Differential(byte input, boolean isSigned, DifferentialOutput mode, int differential) {
            super(ID);

            this.input = input;
            this.isSigned = isSigned;
            this.mode = mode;
            this.differential = differential;
        }

        Differential(byte[] config) {
            super(config[0]);

            input = (byte) ((config[1] & 0x3) + 1);
            isSigned = (config[1] & 0x4) == 0x4;
            mode = DifferentialOutput.values()[(config[1] >> 3) & 0x7];
            differential = ByteBuffer.wrap(config, 1, 4).getInt();
        }

        @Override
        byte[] build() {
            return ByteBuffer.allocate(6).order(ByteOrder.LITTLE_ENDIAN)
                    .put(ID)
                    .put((byte) (((input - 1) & 0x3) | (isSigned ? 0x4 : 0) | (mode.ordinal() << 3)))
                    .putInt(differential)
                    .array();
        }

        @Override
        String createUri(boolean state, byte procId) {
            return String.format(Locale.US, "differential?id=%d", procId);
        }
    }

    static class Threshold extends DataProcessorConfig {
        static final byte ID = 0xd;

        final byte input;
        final boolean isSigned;
        final ThresholdOutput mode;
        final int boundary;
        final short hysteresis;

        Threshold(byte input, boolean isSigned, ThresholdOutput mode, int boundary, short hysteresis) {
            super(ID);

            this.input = input;
            this.isSigned = isSigned;
            this.mode = mode;
            this.boundary = boundary;
            this.hysteresis = hysteresis;
        }

        Threshold(byte[] config) {
            super(config[0]);

            input = (byte) ((config[1] & 0x3) + 1);
            isSigned = (config[1] & 0x4) == 0x4;
            mode = ThresholdOutput.values()[(config[1] >> 3) & 0x7];
            boundary = ByteBuffer.wrap(config, 2, 4).order(ByteOrder.LITTLE_ENDIAN).getInt();
            hysteresis = ByteBuffer.wrap(config, 6, 2).order(ByteOrder.LITTLE_ENDIAN).getShort();
        }

        @Override
        byte[] build() {
            return ByteBuffer.allocate(8).order(ByteOrder.LITTLE_ENDIAN)
                    .put(ID)
                    .put((byte) ((input - 1) & 0x3 | (isSigned ? 0x4 : 0) | (mode.ordinal() << 3)))
                    .putInt(boundary)
                    .putShort(hysteresis)
                    .array();
        }

        @Override
        String createUri(boolean state, byte procId) {
            return String.format(Locale.US, "threshold?id=%d", procId);
        }

    }

    static class Buffer extends DataProcessorConfig {
        static final byte ID = 0xf;

        final byte input;

        Buffer(byte input) {
            super(ID);

            this.input = input;
        }

        Buffer(byte[] config) {
            super(config[0]);

            input = (byte) ((config[1] & 0x1f) + 1);
        }

        @Override
        byte[] build() {
            return new byte[] {ID, (byte) ((input - 1) & 0x1f)};
        }

        @Override
        String createUri(boolean state, byte procId) {
            return String.format(Locale.US, "buffer%s?id=%d", state ? "-state" : "", procId);
        }
    }

    static class Packer extends DataProcessorConfig {
        static final byte ID = 0x10;

        final byte input;
        final byte count;

        Packer(byte input, byte count) {
            super(ID);

            this.input = input;
            this.count = count;
        }

        Packer(byte[] config) {
            super(config[0]);

            input = (byte) ((config[1] & 0x1f) + 1);
            count = (byte) ((config[2] & 0x1f) + 1);
        }

        @Override
        byte[] build() {
            return new byte[] {ID, (byte) ((input - 1) & 0x1f), (byte) ((count - 1)& 0x1f)};
        }

        @Override
        String createUri(boolean state, byte procId) {
            return String.format(Locale.US, "packer?id=%d", procId);
        }
    }

    static class Accounter extends DataProcessorConfig {
        static final byte ID = 0x11;

        final byte length;
        final RouteComponent.AccountType type;

        Accounter(byte length, RouteComponent.AccountType type) {
            super(ID);

            this.length = length;
            this.type = type;
        }

        Accounter(byte[] config) {
            super(config[0]);

            length = (byte) (((config[1] >> 4) & 0x3) + 1);
            type = RouteComponent.AccountType.values()[config[1] & 0xf];
        }

        @Override
        byte[] build() {
            return new byte[] {ID, (byte) (type.ordinal() | ((length - 1) << 4)), 0x3};
        }

        @Override
        String createUri(boolean state, byte procId) {
            return String.format(Locale.US, "account?id=%d", procId);
        }
    }

    static class Fuser extends DataProcessorConfig {
        static final byte ID = 0x1b;

        final String[] names;
        final byte[] filterIds;

        Fuser(String[] names) {
            super(ID);

            this.filterIds = new byte[names.length];
            this.names = names;
        }

        Fuser(byte[] config) {
            super(config[0]);

            names = null;
            filterIds = new byte[config[1] & 0x1f];
            System.arraycopy(config, 2, filterIds, 0, filterIds.length);
        }

        void syncFilterIds(DataProcessorImpl dpModule) {
            int i = 0;
            for(String it: names) {
                if (!dpModule.nameToIdMapping.containsKey(it)) {
                    throw new IllegalRouteOperationException("No processor named \"" + it + "\" found");
                }

                byte id = dpModule.nameToIdMapping.get(it);
                DataProcessorImpl.Processor value = dpModule.activeProcessors.get(id);
                if (!(value.editor.configObj instanceof DataProcessorConfig.Buffer)) {
                    throw new IllegalRouteOperationException("Can only use buffer processors as inputs to the fuser");
                }

                filterIds[i] = id;
                i++;
            }
        }

        @Override
        byte[] build() {
            return ByteBuffer.allocate(2 + filterIds.length).order(ByteOrder.LITTLE_ENDIAN)
                    .put(ID)
                    .put((byte)(filterIds.length))
                    .put(filterIds)
                    .array();
        }

        @Override
        String createUri(boolean state, byte procId) {
            return String.format(Locale.US, "fuser?id=%d", procId);
        }
    }
}