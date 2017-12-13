/*
 * Copyright 2014-2015 MbientLab Inc. All rights reserved.
 *
 * IMPORTANT: Your use of this Software is limited to those specific rights granted under the terms of a software
 * license agreement between the user who downloaded the software, his/her employer (which must be your
 * employer) and MbientLab Inc, (the "License").  You may not use this Software unless you agree to abide by the
 * terms of the License which can be found at www.mbientlab.com/terms.  The License limits your use, and you
 * acknowledge, that the Software may be modified, copied, and distributed when used in conjunction with an
 * MbientLab Inc, product.  Other than for the foregoing purpose, you may not use, reproduce, copy, prepare
 * derivative works of, modify, distribute, perform, display or sell this Software and/or its documentation for any
 * purpose.
 *
 * YOU FURTHER ACKNOWLEDGE AND AGREE THAT THE SOFTWARE AND DOCUMENTATION ARE PROVIDED "AS IS" WITHOUT WARRANTY
 * OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING WITHOUT LIMITATION, ANY WARRANTY OF MERCHANTABILITY, TITLE,
 * NON-INFRINGEMENT AND FITNESS FOR A PARTICULAR PURPOSE. IN NO EVENT SHALL MBIENTLAB OR ITS LICENSORS BE LIABLE OR
 * OBLIGATED UNDER CONTRACT, NEGLIGENCE, STRICT LIABILITY, CONTRIBUTION, BREACH OF WARRANTY, OR OTHER LEGAL EQUITABLE
 * THEORY ANY DIRECT OR INDIRECT DAMAGES OR EXPENSES INCLUDING BUT NOT LIMITED TO ANY INCIDENTAL, SPECIAL, INDIRECT,
 * PUNITIVE OR CONSEQUENTIAL DAMAGES, LOST PROFITS OR LOST DATA, COST OF PROCUREMENT OF SUBSTITUTE GOODS, TECHNOLOGY,
 * SERVICES, OR ANY CLAIMS BY THIRD PARTIES (INCLUDING BUT NOT LIMITED TO ANY DEFENSE THEREOF), OR OTHER SIMILAR COSTS.
 *
 * Should you have any questions regarding your right to use this Software, contact MbientLab via email:
 * hello@mbientlab.com.
 */

package com.mbientlab.metawear.impl;

import com.mbientlab.metawear.AsyncDataProducer;
import com.mbientlab.metawear.Route;
import com.mbientlab.metawear.builder.RouteBuilder;
import com.mbientlab.metawear.impl.platform.TimedTask;
import com.mbientlab.metawear.module.AccelerometerBmi160;

import java.util.Arrays;

import bolts.Task;

import static com.mbientlab.metawear.impl.Constant.Module.ACCELEROMETER;

/**
 * Created by etsai on 9/1/16.
 */
class AccelerometerBmi160Impl extends AccelerometerBoschImpl implements AccelerometerBmi160 {
    static String createUri(DataTypeBase dataType) {
        switch (Util.clearRead(dataType.eventConfig[1])) {
            case STEP_DETECTOR_INTERRUPT:
                return "step-detector";
            case STEP_COUNTER_DATA:
                return "step-counter";
        }
        return AccelerometerBoschImpl.createUri(dataType);
    }

    final static byte IMPLEMENTATION= 0x1;
    private static final byte STEP_DETECTOR_INTERRUPT_ENABLE = 0x17,
            STEP_DETECTOR_CONFIG= 0x18,
            STEP_DETECTOR_INTERRUPT= 0x19,
            STEP_COUNTER_DATA= 0x1a,
            STEP_COUNTER_RESET= 0x1b;
    private static final long serialVersionUID = 6590506443181115665L;
    private static final String STEP_DETECTOR_PRODUCER= "com.mbientlab.metawear.impl.AccelerometerBmi160Impl.STEP_DETECTOR_PRODUCER",
            STEP_COUNTER_PRODUCER= "com.mbientlab.metawear.impl.AccelerometerBmi160Impl.STEP_COUNTER_PRODUCER";
    private static final byte[] DEFAULT_MOTION_CONFIG = new byte[] {0x00, 0x14, 0x14, 0x14};

    private final byte[] accDataConfig= new byte[] {0x28, 0x03};

    private transient AsyncDataProducer lowhigh, noMotion, slowMotion, anyMotion, significantMotion;
    private transient StepDetectorDataProducer stepDetector;
    private transient StepCounterDataProducer stepCounter;
    private transient TimedTask<byte[]> pullConfigTask;

    AccelerometerBmi160Impl(MetaWearBoardPrivate mwPrivate) {
        super(mwPrivate);

        mwPrivate.tagProducer(STEP_DETECTOR_PRODUCER, new UintData(ACCELEROMETER, STEP_DETECTOR_INTERRUPT,
                new DataAttributes(new byte[] {1}, (byte) 1, (byte) 0, false)));
        mwPrivate.tagProducer(STEP_COUNTER_PRODUCER, new UintData(ACCELEROMETER, Util.setSilentRead(STEP_COUNTER_DATA),
                new DataAttributes(new byte[] {2}, (byte) 1, (byte) 0, false)));
    }

    @Override
    protected void init() {
        pullConfigTask = new TimedTask<>();
        mwPrivate.addResponseHandler(new Pair<>(ACCELEROMETER.id, Util.setRead(DATA_CONFIG)), response -> pullConfigTask.setResult(response));
    }

    @Override
    protected float getAccDataScale() {
        return AccRange.bitMaskToRange((byte) (accDataConfig[1] & 0x0f)).scale;
    }

    @Override
    protected int getSelectedAccRange() {
        return AccRange.bitMaskToRange((byte) (accDataConfig[1] & 0x0f)).ordinal();
    }

    @Override
    protected int getMaxOrientHys() {
        return 0xf;
    }

    @Override
    public AccelerometerBmi160.ConfigEditor configure() {
        return new AccelerometerBmi160.ConfigEditor() {
            private OutputDataRate odr= OutputDataRate.ODR_100_HZ;
            private AccRange ar= AccRange.AR_2G;
            private FilterMode mode = FilterMode.NORMAL;

            @Override
            public AccelerometerBmi160.ConfigEditor odr(OutputDataRate odr) {
                this.odr= odr;
                return this;
            }

            @Override
            public AccelerometerBmi160.ConfigEditor range(AccRange ar) {
                this.ar= ar;
                return this;
            }

            @Override
            public AccelerometerBmi160.ConfigEditor filter(FilterMode mode) {
                this.mode = mode;
                return this;
            }

            @Override
            public AccelerometerBmi160.ConfigEditor odr(float odr) {
                return odr(OutputDataRate.values()[Util.closestIndex(OutputDataRate.frequencies(), odr)]);
            }

            @Override
            public AccelerometerBmi160.ConfigEditor range(float fsr) {
                return range(AccRange.values()[Util.closestIndex(AccRange.ranges(), fsr)]);
            }

            @Override
            public void commit() {
                accDataConfig[0]&= 0xf0;
                accDataConfig[0]|= odr.ordinal() + 1;

                accDataConfig[0]&= 0xf;
                if (odr.frequency < 12.5f) {
                    accDataConfig[0]|= 0x80;
                } else {
                    accDataConfig[0]|= (mode.ordinal() << 4);
                }

                accDataConfig[1]&= 0xf0;
                accDataConfig[1]|= ar.bitmask;

                mwPrivate.sendCommand(ACCELEROMETER, DATA_CONFIG, accDataConfig);
            }
        };
    }

    @Override
    public float getOdr() {
        return OutputDataRate.values()[(accDataConfig[0] & ~0xf0) - 1].frequency;
    }

    @Override
    public float getRange() {
        return AccRange.bitMaskToRange((byte) (accDataConfig[1] & ~0xf0)).range;
    }

    @Override
    public Task<Void> pullConfigAsync() {
        return pullConfigTask.execute("Did not receive BMI160 acc config within %dms", Constant.RESPONSE_TIMEOUT,
                () -> mwPrivate.sendCommand(new byte[] {ACCELEROMETER.id, Util.setRead(DATA_CONFIG)})
        ).onSuccessTask(task -> {
            System.arraycopy(task.getResult(), 2, accDataConfig, 0, accDataConfig.length);
            return Task.forResult(null);
        });
    }

    private class StepConfigEditorInner implements StepConfigEditor {
        private StepDetectorMode mode = StepDetectorMode.NORMAL;
        private final byte[] stepDetectorConfig = new byte[] {0x00, 0x00};

        StepConfigEditorInner(boolean counter) {
            if (counter) {
                stepDetectorConfig[1] |= 0x08;
            }
        }

        @Override
        public StepConfigEditor mode(StepDetectorMode mode) {
            this.mode = mode;
            return this;
        }

        @Override
        public void commit() {
            switch (mode) {
                case NORMAL:
                    stepDetectorConfig[0]= 0x15;
                    stepDetectorConfig[1]|= 0x3;
                    break;
                case SENSITIVE:
                    stepDetectorConfig[0]= 0x2d;
                    break;
                case ROBUST:
                    stepDetectorConfig[0]= 0x1d;
                    stepDetectorConfig[1]|= 0x7;
                    break;
            }

            mwPrivate.sendCommand(ACCELEROMETER, STEP_DETECTOR_CONFIG, stepDetectorConfig);
        }
    }

    @Override
    public StepDetectorDataProducer stepDetector() {
        if (stepDetector == null) {
            stepDetector = new StepDetectorDataProducer() {
                @Override
                public StepConfigEditor configure() {
                    return new StepConfigEditorInner(false);
                }

                @Override
                public Task<Route> addRouteAsync(RouteBuilder builder) {
                    return mwPrivate.queueRouteBuilder(builder, STEP_DETECTOR_PRODUCER);
                }

                @Override
                public String name() {
                    return STEP_DETECTOR_PRODUCER;
                }

                @Override
                public void start() {
                    mwPrivate.sendCommand(new byte[] {ACCELEROMETER.id, STEP_DETECTOR_INTERRUPT_ENABLE, (byte) 1, (byte) 0});
                }

                @Override
                public void stop() {
                    mwPrivate.sendCommand(new byte[] {ACCELEROMETER.id, STEP_DETECTOR_INTERRUPT_ENABLE, (byte) 0, (byte) 1});
                }
            };
        }
        return stepDetector;
    }

    @Override
    public StepCounterDataProducer stepCounter() {
        if (stepCounter == null) {
            stepCounter = new StepCounterDataProducer() {
                @Override
                public StepConfigEditor configure() {
                    return new StepConfigEditorInner(true);
                }

                @Override
                public void reset() {
                    mwPrivate.sendCommand(new byte[] {ACCELEROMETER.id, STEP_COUNTER_RESET});
                }

                @Override
                public Task<Route> addRouteAsync(RouteBuilder builder) {
                    return mwPrivate.queueRouteBuilder(builder, STEP_COUNTER_PRODUCER);
                }

                @Override
                public String name() {
                    return STEP_COUNTER_PRODUCER;
                }

                @Override
                public void read() {
                    mwPrivate.lookupProducer(STEP_COUNTER_PRODUCER).read(mwPrivate);
                }
            };
        }
        return stepCounter;
    }

    private class Bmi160FlatDataProducer extends BoschFlatDataProducer implements AccelerometerBmi160.FlatDataProducer {
        @Override
        public AccelerometerBmi160.FlatConfigEditor configure() {
            return new AccelerometerBmi160.FlatConfigEditor() {
                private FlatHoldTime holdTime = FlatHoldTime.FHT_640_MS;
                private float theta = 5.6889f;

                @Override
                public AccelerometerBmi160.FlatConfigEditor holdTime(FlatHoldTime time) {
                    holdTime = time;
                    return this;
                }

                @Override
                public AccelerometerBmi160.FlatConfigEditor holdTime(float time) {
                    return holdTime(FlatHoldTime.values()[Util.closestIndex(FlatHoldTime.delays(), time)]);
                }

                @Override
                public AccelerometerBmi160.FlatConfigEditor flatTheta(float angle) {
                    theta = angle;
                    return this;
                }

                @Override
                public void commit() {
                    writeFlatConfig(holdTime.ordinal(), theta);
                }
            };
        }
    }

    @Override
    public AccelerometerBmi160.FlatDataProducer flat() {
        return new Bmi160FlatDataProducer();
    }

    @Override
    public LowHighDataProducer lowHigh() {
        if (lowhigh == null) {
            lowhigh = new LowHighDataProducerInner(new byte[] {0x07, 0x30, (byte) 0x81, 0x0b, (byte) 0xc0}, 2.5f);
        }
        return (LowHighDataProducer) lowhigh;
    }

    @Override
    public <T extends MotionDetection> T motion(Class<T> motionClass) {
        if (motionClass.equals(SignificantMotionDataProducer.class)) {
            return motionClass.cast(significantMotion());
        }
        if (motionClass.equals(NoMotionDataProducer.class)) {
            return motionClass.cast(noMotion());
        }
        if (motionClass.equals(AnyMotionDataProducer.class)) {
            return motionClass.cast(anyMotion());
        }
        if (motionClass.equals(SlowMotionDataProducer.class)) {
            return motionClass.cast(slowMotion());
        }
        return null;
    }

    private SignificantMotionDataProducer significantMotion() {
        if (significantMotion == null) {
            significantMotion = new SignificantMotionDataProducer() {
                @Override
                public void start() {
                    mwPrivate.sendCommand(new byte[] {ACCELEROMETER.id, MOTION_INTERRUPT_ENABLE, (byte) 0x07, (byte) 0});
                }

                @Override
                public void stop() {
                    mwPrivate.sendCommand(new byte[] {ACCELEROMETER.id, MOTION_INTERRUPT_ENABLE, (byte) 0x0, (byte) 0x7});
                }

                @Override
                public SignificantMotionConfigEditor configure() {
                    return new SignificantMotionConfigEditor() {
                        private SkipTime newSkipTime= null;
                        private ProofTime newProofTime= null;

                        @Override
                        public SignificantMotionConfigEditor skipTime(SkipTime time) {
                            newSkipTime= time;
                            return this;
                        }

                        @Override
                        public SignificantMotionConfigEditor proofTime(ProofTime time) {
                            newProofTime= time;
                            return this;
                        }


                        @Override
                        public void commit() {
                            byte[] motionConfig = Arrays.copyOf(DEFAULT_MOTION_CONFIG, DEFAULT_MOTION_CONFIG.length);
                            motionConfig[3]|= 0x2;

                            if (newSkipTime != null) {
                                motionConfig[3]|= (newSkipTime.ordinal() << 2);
                            }
                            if (newProofTime != null) {
                                motionConfig[3]|= (newProofTime.ordinal() << 4);
                            }

                            mwPrivate.sendCommand(ACCELEROMETER, MOTION_CONFIG, motionConfig);
                        }
                    };
                }

                @Override
                public Task<Route> addRouteAsync(RouteBuilder builder) {
                    return mwPrivate.queueRouteBuilder(builder, MOTION_PRODUCER);
                }

                @Override
                public String name() {
                    return MOTION_PRODUCER;
                }
            };
        }
        return (SignificantMotionDataProducer) significantMotion;
    }
    private NoMotionDataProducer noMotion() {
        if (noMotion == null) {
            noMotion = new NoMotionDataProducer() {
                @Override
                public void start() {
                    mwPrivate.sendCommand(new byte[] {ACCELEROMETER.id, MOTION_INTERRUPT_ENABLE, (byte) 0x38, (byte) 0});
                }

                @Override
                public void stop() {
                    mwPrivate.sendCommand(new byte[] {ACCELEROMETER.id, MOTION_INTERRUPT_ENABLE, (byte) 0, (byte) 0x38});
                }

                @Override
                public NoMotionConfigEditor configure() {
                    return new NoMotionConfigEditor() {
                        private Integer duration= null;
                        private Float threshold= null;

                        @Override
                        public NoMotionConfigEditor duration(int duration) {
                            this.duration= duration;
                            return this;
                        }

                        @Override
                        public NoMotionConfigEditor threshold(float threshold) {
                            this.threshold= threshold;
                            return this;
                        }

                        @Override
                        public void commit() {
                            byte[] motionConfig = Arrays.copyOf(DEFAULT_MOTION_CONFIG, DEFAULT_MOTION_CONFIG.length);
                            motionConfig[3]|= 0x1;

                            if (duration != null) {
                                motionConfig[0]&= 0x3;

                                if (duration >= 1280 && duration <= 20480) {
                                    motionConfig[0]|= ((byte) (duration / 1280.f - 1)) << 2;
                                } else if (duration >= 25600 && duration <= 102400) {
                                    motionConfig[0]|= (((byte) (duration / 5120.f - 5)) << 2) | 0x40;
                                } else if (duration >= 112640 && duration <= 430080) {
                                    motionConfig[0]|= (((byte) (duration / 10240.f - 11)) << 2) | 0x80;
                                }
                            }

                            if (threshold != null) {
                                motionConfig[2]= (byte) (threshold / BOSCH_NO_MOTION_THS_STEPS[getSelectedAccRange()]);
                            }

                            mwPrivate.sendCommand(ACCELEROMETER, MOTION_CONFIG, motionConfig);
                        }
                    };
                }

                @Override
                public Task<Route> addRouteAsync(RouteBuilder builder) {
                    return mwPrivate.queueRouteBuilder(builder, MOTION_PRODUCER);
                }

                @Override
                public String name() {
                    return MOTION_PRODUCER;
                }
            };
        }
        return (NoMotionDataProducer) noMotion;
    }
    private AnyMotionDataProducer anyMotion() {
        if (anyMotion == null) {
            anyMotion = new AnyMotionDataProducer() {
                @Override
                public void start() {
                    mwPrivate.sendCommand(new byte[] {ACCELEROMETER.id, MOTION_INTERRUPT_ENABLE, (byte) 0x07, (byte) 0});
                }

                @Override
                public void stop() {
                    mwPrivate.sendCommand(new byte[] {ACCELEROMETER.id, MOTION_INTERRUPT_ENABLE, (byte) 0x0, (byte) 0x7});
                }

                @Override
                public AnyMotionConfigEditor configure() {
                    byte[] initialConfig = Arrays.copyOf(DEFAULT_MOTION_CONFIG, DEFAULT_MOTION_CONFIG.length);
                    initialConfig[3]&= (~0x2);
                    return new AnyMotionConfigEditorInner(initialConfig);
                }

                @Override
                public Task<Route> addRouteAsync(RouteBuilder builder) {
                    return mwPrivate.queueRouteBuilder(builder, MOTION_PRODUCER);
                }

                @Override
                public String name() {
                    return MOTION_PRODUCER;
                }
            };
        }
        return (AnyMotionDataProducer) anyMotion;
    }
    private SlowMotionDataProducer slowMotion() {
        if (slowMotion == null) {
            slowMotion = new SlowMotionDataProducer() {
                @Override
                public void start() {
                    mwPrivate.sendCommand(new byte[] {ACCELEROMETER.id, MOTION_INTERRUPT_ENABLE, (byte) 0x38, (byte) 0});
                }

                @Override
                public void stop() {
                    mwPrivate.sendCommand(new byte[] {ACCELEROMETER.id, MOTION_INTERRUPT_ENABLE, (byte) 0, (byte) 0x38});
                }

                @Override
                public SlowMotionConfigEditor configure() {
                    byte[] initialConfig = Arrays.copyOf(DEFAULT_MOTION_CONFIG, DEFAULT_MOTION_CONFIG.length);
                    initialConfig[3]&= (~0x1);
                    return new SlowMotionConfigEditorInner(initialConfig);
                }

                @Override
                public Task<Route> addRouteAsync(RouteBuilder builder) {
                    return mwPrivate.queueRouteBuilder(builder, MOTION_PRODUCER);
                }

                @Override
                public String name() {
                    return MOTION_PRODUCER;
                }
            };
        }
        return (SlowMotionDataProducer) slowMotion;
    }
}
