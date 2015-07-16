/*
 * Copyright 2015 MbientLab Inc. All rights reserved.
 *
 * IMPORTANT: Your use of this Software is limited to those specific rights
 * granted under the terms of a software license agreement between the user who
 * downloaded the software, his/her employer (which must be your employer) and
 * MbientLab Inc, (the "License").  You may not use this Software unless you
 * agree to abide by the terms of the License which can be found at
 * www.mbientlab.com/terms . The License limits your use, and you acknowledge,
 * that the  Software may not be modified, copied or distributed and can be used
 * solely and exclusively in conjunction with a MbientLab Inc, product.  Other
 * than for the foregoing purpose, you may not use, reproduce, copy, prepare
 * derivative works of, modify, distribute, perform, display or sell this
 * Software and/or its documentation for any purpose.
 *
 * YOU FURTHER ACKNOWLEDGE AND AGREE THAT THE SOFTWARE AND DOCUMENTATION ARE
 * PROVIDED "AS IS" WITHOUT WARRANTY OF ANY KIND, EITHER EXPRESS OR IMPLIED,
 * INCLUDING WITHOUT LIMITATION, ANY WARRANTY OF MERCHANTABILITY, TITLE,
 * NON-INFRINGEMENT AND FITNESS FOR A PARTICULAR PURPOSE. IN NO EVENT SHALL
 * MBIENTLAB OR ITS LICENSORS BE LIABLE OR OBLIGATED UNDER CONTRACT, NEGLIGENCE,
 * STRICT LIABILITY, CONTRIBUTION, BREACH OF WARRANTY, OR OTHER LEGAL EQUITABLE
 * THEORY ANY DIRECT OR INDIRECT DAMAGES OR EXPENSES INCLUDING BUT NOT LIMITED
 * TO ANY INCIDENTAL, SPECIAL, INDIRECT, PUNITIVE OR CONSEQUENTIAL DAMAGES, LOST
 * PROFITS OR LOST DATA, COST OF PROCUREMENT OF SUBSTITUTE GOODS, TECHNOLOGY,
 * SERVICES, OR ANY CLAIMS BY THIRD PARTIES (INCLUDING BUT NOT LIMITED TO ANY
 * DEFENSE THEREOF), OR OTHER SIMILAR COSTS.
 *
 * Should you have any questions regarding your right to use this Software,
 * contact MbientLab Inc, at www.mbientlab.com.
 */

package com.mbientlab.metawear.example;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.mbientlab.metawear.AsyncResult;
import com.mbientlab.metawear.DataProcessor;
import com.mbientlab.metawear.MessageToken;
import com.mbientlab.metawear.MetaWearBoard;
import com.mbientlab.metawear.data.AccelAxisG;
import com.mbientlab.metawear.data.AccelAxisMilliG;
import com.mbientlab.metawear.data.Bmi160GyroMessage;
import com.mbientlab.metawear.impl.MetaWearBleService;
import com.mbientlab.metawear.module.*;
import com.mbientlab.metawear.processor.*;
import com.mbientlab.metawear.processor.Math;
import com.mbientlab.metawear.DataSignal;
import com.mbientlab.metawear.Message;
import com.mbientlab.metawear.RouteManager;

import java.util.Arrays;
import java.util.Locale;
import java.util.Map;

import static com.mbientlab.metawear.AsyncResult.CompletionHandler;
import static com.mbientlab.metawear.MetaWearBoard.ConnectionStateHandler;


public class MainActivity extends ActionBarActivity implements ServiceConnection {
    private MetaWearBoard mwBoard;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        getApplicationContext().bindService(new Intent(this, MetaWearBleService.class),
                this, Context.BIND_AUTO_CREATE);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        ///< Unbind the service when the activity is destroyed
        getApplicationContext().unbindService(this);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    //private final String MW_MAC_ADDRESS= "E9:64:E9:1A:BC:F0";
    //private final String MW_MAC_ADDRESS= "C5:24:07:C3:20:9F";
    private final String MW_MAC_ADDRESS= "C8:D2:BA:90:60:03";
    //private final String MW_MAC_ADDRESS= "E5:58:D9:C4:1C:AF";
    //private final String MW_MAC_ADDRESS= "D6:0E:50:D0:AB:CE";
    //private final String MW_MAC_ADDRESS= "DD:F2:D5:07:B6:57";

    @Override
    public void onServiceConnected(ComponentName name, IBinder service) {
        MetaWearBleService.LocalBinder binder = (MetaWearBleService.LocalBinder) service;

        final BluetoothManager btManager= (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        final BluetoothDevice remoteDevice= btManager.getAdapter().getRemoteDevice(MW_MAC_ADDRESS);

        mwBoard= binder.getMetaWearBoard(remoteDevice);
        mwBoard.setConnectionStateHandler(new ConnectionStateHandler() {
            @Override
            public void connected() {
                MainActivity.this.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(MainActivity.this, "Connected", Toast.LENGTH_LONG).show();
                    }
                });

                Log.i("test", "Connected");

                mwBoard.readDeviceInformation().onComplete(new CompletionHandler<MetaWearBoard.DeviceInformation>() {
                    @Override
                    public void success(MetaWearBoard.DeviceInformation result) {
                        Log.i("test", "Device Information: " + result.toString());
                    }

                    @Override
                    public void failure(Throwable error) {
                        Log.e("test", "Error reading device information", error);
                    }
                });
            }

            @Override
            public void disconnected() {
                MainActivity.this.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(MainActivity.this, "Disconnected", Toast.LENGTH_LONG).show();
                    }
                });
                Log.i("test", "Disconnected");
            }

            @Override
            public void failure(int status, Throwable error) {
                MainActivity.this.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(MainActivity.this, "Error connecting", Toast.LENGTH_LONG).show();
                    }
                });

                Log.e("test", "Error connecting", error);
            }
        });
    }

    @Override
    public void onServiceDisconnected(ComponentName componentName) {

    }

    public void connectMe(View v) {
        mwBoard.connect();
    }

    public void disconnectMe(View v) {
        mwBoard.disconnect();
    }

    public void rssiMe(View v) {
        mwBoard.readRssi().onComplete(new CompletionHandler<Integer>() {
            @Override
            public void success(final Integer result) {
                MainActivity.this.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        ((TextView) findViewById(R.id.textView)).setText(String.format(Locale.US, "%d", result));
                    }
                });
            }

            @Override
            public void failure(Throwable error) {
                Log.e("test", "Error reading RSSI value", error);
            }
        });
    }

    public void batteryMe(View v) {
        mwBoard.readBatteryLevel().onComplete(new CompletionHandler<Byte>() {
            @Override
            public void success(final Byte result) {
                MainActivity.this.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        ((TextView) findViewById(R.id.textView2)).setText(String.format(Locale.US, "%d", result));
                    }
                });
            }

            @Override
            public void failure(Throwable error) {
                Log.e("test", "Error reading battery level", error);
            }
        });
    }

    private boolean accelSetup= false;
    private AsyncResult<RouteManager> accelManager;
    public void accelerometerMe(View v) {
        final Switch mySwitch= (Switch) v;
        if (mySwitch.isChecked()) {
            if (!accelSetup) {
                accelManager= mwBoard.routeData().fromAccelAxis().subscribe(new DataSignal.MessageProcessor() {
                    @Override
                    public void process(Message msg) {
                        final AccelAxisG axisData = msg.getData(AccelAxisG.class);
                        Log.i("test", String.format("Stream: %.3f,%.3f,%.3f", axisData.x(), axisData.y(), axisData.z()));
                        MainActivity.this.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                ((TextView) findViewById(R.id.textView3)).setText(String.format("%.3f,%.3f,%.3f", axisData.x(), axisData.y(), axisData.z()));
                            }
                        });
                    }
                }).log(new DataSignal.MessageProcessor() {
                    @Override
                    public void process(Message msg) {
                        final AccelAxisG axisData = msg.getData(AccelAxisG.class);
                        Log.i("test", String.format("Log: %.3f,%.3f,%.3f", axisData.x(), axisData.y(), axisData.z()));
                    }
                }).commit();
                accelSetup= true;
            }
            accelManager.onComplete(new CompletionHandler<RouteManager>() {
                @Override
                public void success(RouteManager result) {
                    Accelerometer genericAccel= mwBoard.getModule(Accelerometer.class);
                    genericAccel.setOutputDataRate(50.f);
                    genericAccel.setAxisSamplingRange(8.f);

                    genericAccel.startAxisSampling();
                    genericAccel.globalStart();
                }

                @Override
                public void failure(Throwable error) {
                    Log.e("test", "Error committing route", error);
                    accelSetup= false;
                }
            });
        } else {
            accelManager.onComplete(new CompletionHandler<RouteManager>() {
                @Override
                public void success(RouteManager result) {
                    Accelerometer genericAccel= mwBoard.getModule(Accelerometer.class);
                    genericAccel.stopAxisSampling();
                    genericAccel.globalStop();
                }
            });
        }
    }

    public void downloadMe(View v) {
        mwBoard.getModule(Logging.class).downloadLog(0.05f, new Logging.DownloadHandler() {
            @Override
            public void onProgressUpdate(int nEntriesLeft, int totalEntries) {
                Log.i("test", String.format("Progress= %d / %d", nEntriesLeft, totalEntries));
            }
        });
    }

    private Timer.Controller timerTempResult;
    private AsyncResult<RouteManager> tempManager;
    public void temperatureMe(View v) {
        final Switch mySwitch= (Switch) v;
        if (mySwitch.isChecked()) {
            mwBoard.getModule(Timer.class).createTimer(1000, (short) -1, true).onComplete(new CompletionHandler<Timer.Controller>() {
                @Override
                public void success(Timer.Controller result) {
                    result.schedule(new Timer.Task() {
                        @Override
                        public void execute() {
                            mwBoard.getModule(Temperature.class).readTemperarure();
                        }
                    });
                    timerTempResult= result;
                }
            });

            tempManager= mwBoard.routeData().fromTemperature().subscribe(new DataSignal.MessageProcessor() {
                @Override
                public void process(final Message msg) {
                    Log.i("test", String.format("%.3f C", msg.getData(Float.class)));
                    MainActivity.this.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            ((TextView) findViewById(R.id.textView4)).setText(String.format("%.3f C", msg.getData(Float.class)));
                        }
                    });
                }
            }).split()
                .branch()
                    .process(new Math(Math.Operation.MULTIPLY, 18))
                    .process(new Math(Math.Operation.DIVIDE, 10))
                    .process(new Math(Math.Operation.ADD, 32.f)).subscribe(new DataSignal.MessageProcessor() {
                        @Override
                        public void process(Message msg) {
                            Log.i("test", String.format("%.3f F", msg.getData(Float.class)));
                        }
                    })
                .branch()
                    .process("math?operation=add&rhs=273.15").subscribe(new DataSignal.MessageProcessor() {
                        @Override
                        public void process(Message msg) {
                            Log.i("test", String.format("%.3f K", msg.getData(Float.class)));
                        }
                    })
            .end()
            .commit();
            tempManager.onComplete(new CompletionHandler<RouteManager>() {
                @Override
                public void success(RouteManager result) {
                    mwBoard.getModule(Temperature.class).enableThermistorMode((byte) 0, (byte) 1);
                    timerTempResult.start();
                }
            });
        } else {
            mwBoard.getModule(Temperature.class).disableThermistorMode();
            timerTempResult.remove();
            tempManager.onComplete(new CompletionHandler<RouteManager>() {
                @Override
                public void success(RouteManager result) {
                    result.remove();
                }
            });
        }
    }

    private boolean processSetup= false;
    private AsyncResult<RouteManager> processManager;
    public void filterMe(View v) {
        final Switch mySwitch= (Switch) v;
        if (mySwitch.isChecked()) {
            if (!processSetup) {
                processManager= mwBoard.routeData().fromAccelAxis()
                        .process(new Time(5000, Time.Mode.ABSOLUTE)).log(new DataSignal.MessageProcessor() {
                            @Override
                            public void process(Message msg) {
                                AccelAxisMilliG milliG= msg.getData(AccelAxisMilliG.class);
                                Log.i("test", String.format("XYZ Axis: (%d, %d, %d)", milliG.x(), milliG.y(), milliG.z()));
                            }
                        })
                        .process(new Rms()).log(new DataSignal.MessageProcessor() {
                            @Override
                            public void process(Message msg) {
                                Log.i("test", String.format("RMS: %d", msg.getData(Short.class)));
                            }
                        }).commit();
                processSetup= true;
            }
            processManager.onComplete(new CompletionHandler<RouteManager>() {
                @Override
                public void success(RouteManager result) {
                    Accelerometer accelModule= mwBoard.getModule(Accelerometer.class);
                    accelModule.startAxisSampling();
                    accelModule.globalStart();
                }
            });
        } else {
            processManager.onComplete(new CompletionHandler<RouteManager>() {
                @Override
                public void success(RouteManager result) {
                    Accelerometer accelModule= mwBoard.getModule(Accelerometer.class);
                    accelModule.globalStop();
                    accelModule.stopAxisSampling();
                }
            });
        }
    }

    private AsyncResult<Timer.Controller> timerController;
    private AsyncResult<RouteManager> analogRoute, switchRoute;
    public void analogMe(View v) {
        final Switch mySwitch= (Switch) v;
        if (mySwitch.isChecked()) {
            analogRoute= mwBoard.routeData().fromAnalogGpio((byte) 0, Gpio.AnalogReadMode.ADC)
                    .process("mathprocesser", "math?operation=mult&rhs=1")
                    .subscribe(new DataSignal.MessageProcessor() {
                        @Override
                        public void process(final Message msg) {
                            MainActivity.this.runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    ((TextView) findViewById(R.id.textView6)).setText(String.format("%d", msg.getData(Short.class)));
                                }
                            });
                        }
                    }).commit();
            analogRoute.onComplete(new CompletionHandler<RouteManager>() {
                @Override
                public void success(final RouteManager result) {
                    mwBoard.getModule(Gpio.class).clearDigitalOut((byte) 1);
                    switchRoute= mwBoard.routeData().fromSwitch().monitor(new DataSignal.ActivityMonitor() {
                        @Override
                        public void onSignalActive(Map<String, DataProcessor> processors, MessageToken signalData) {
                            result.getDataProcessor("mathprocesser")
                                    .modifyConfiguration(new Math(Math.Operation.MULTIPLY, signalData));
                        }
                    }).commit();
                }
            });
            timerController= mwBoard.getModule(Timer.class).createTimer(500, (short) -1, false);
            timerController.onComplete(new CompletionHandler<Timer.Controller>() {
                @Override
                public void success(Timer.Controller result) {
                    result.schedule(new Timer.Task() {
                        @Override
                        public void execute() {
                            mwBoard.getModule(Gpio.class).readAnalogIn((byte) 0, Gpio.AnalogReadMode.ADC);
                        }
                    });
                    result.start();
                }
            });
        } else {
            timerController.onComplete(new CompletionHandler<Timer.Controller>() {
                @Override
                public void success(Timer.Controller result) {
                    result.stop();
                    result.remove();
                }
            });
            analogRoute.onComplete(new CompletionHandler<RouteManager>() {
                @Override
                public void success(RouteManager result) {
                    result.remove();
                }
            });
            switchRoute.onComplete(new CompletionHandler<RouteManager>() {
                @Override
                public void success(RouteManager result) {
                    result.remove();
                }
            });
        }
    }

    private AsyncResult<RouteManager> programMngr;
    public void programMe(View v) {
        final Switch mwSwitch= (Switch) v;

        if (mwSwitch.isChecked()) {
            programMngr = mwBoard.routeData().fromSwitch().process("accumprocesser", "accumulator").process("math?operation=modulus&rhs=2")
                    .split()
                    .branch()
                    .process("comparison?operation=eq&reference=1").monitor(new DataSignal.ActivityMonitor() {
                        @Override
                        public void onSignalActive(Map<String, DataProcessor> processors, MessageToken signalData) {
                            Led ledCtrllr = mwBoard.getModule(Led.class);
                            ledCtrllr.writeChannelAttributes(Led.ColorChannel.BLUE)
                                    .withRiseTime((short) 0).withPulseDuration((short) 1000)
                                    .withRepeatCount((byte) -1).withHighTime((short) 500)
                                    .withHighIntensity((byte) 16).withLowIntensity((byte) 16)
                                    .commit();
                            ledCtrllr.play(false);
                        }
                    })
                    .branch()
                    .process("comparison?operation=eq&reference=0").monitor(new DataSignal.ActivityMonitor() {
                        @Override
                        public void onSignalActive(Map<String, DataProcessor> processors, MessageToken signalData) {
                            mwBoard.getModule(Led.class).stop(true);
                        }
                    })
                    .end()
                    .commit();
        } else {
            programMngr.onComplete(new CompletionHandler<RouteManager>() {
                @Override
                public void success(RouteManager result) {
                    result.remove();
                }
            });
        }

    }

    public void resetMe(View v) {
        mwBoard.getModule(Debug.class).resetDevice();
    }

    private AsyncResult<RouteManager> gyroRoute;
    private boolean gyroSetup= false;
    public void gyroMe(View v) {
        final Switch mySwitch= (Switch) v;
        if (mySwitch.isChecked()) {
            if (!gyroSetup) {
                gyroRoute = mwBoard.routeData().fromGyro().subscribe(new DataSignal.MessageProcessor() {
                    @Override
                    public void process(Message msg) {
                        final Bmi160GyroMessage.GyroSpin spinData = msg.getData(Bmi160GyroMessage.GyroSpin.class);

                        Log.i("test", String.format("Gyro: %.3f,%.3f,%.3f", spinData.x(), spinData.y(), spinData.z()));
                        MainActivity.this.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                ((TextView) findViewById(R.id.textView7)).setText(String.format("%.3f,%.3f,%.3f", spinData.x(), spinData.y(), spinData.z()));
                            }
                        });
                    }
                }).log(new DataSignal.MessageProcessor() {
                    @Override
                    public void process(Message msg) {
                        final Bmi160GyroMessage.GyroSpin spinData = msg.getData(Bmi160GyroMessage.GyroSpin.class);

                        Log.i("test", String.format("Log Gyro: %.3f,%.3f,%.3f", spinData.x(), spinData.y(), spinData.z()));
                    }
                }).commit();
                gyroSetup= true;
            }
            gyroRoute.onComplete(new CompletionHandler<RouteManager>() {
                @Override
                public void success(RouteManager result) {
                    Bmi160Gyro gyroModule= mwBoard.getModule(Bmi160Gyro.class);
                    gyroModule.configure().withOutputDataRate(Bmi160Gyro.OutputDataRate.ODR_25_HZ)
                            .withFullScaleRange(Bmi160Gyro.FullScaleRange.FSR_125)
                            .commit();
                    gyroModule.globalStart();
                }
            });
        } else {
            gyroRoute.onComplete(new CompletionHandler<RouteManager>() {
                @Override
                public void success(RouteManager result) {
                    mwBoard.getModule(Bmi160Gyro.class).globalStop();
                }
            });
        }
    }

    private AsyncResult<RouteManager> gsrRoute;
    private boolean gsrSetup= false;
    public void gsrMe(View v) {
        if (!gsrSetup) {
            gsrRoute= mwBoard.routeData().fromGsr((byte) 0).subscribe(new DataSignal.MessageProcessor() {
                @Override
                public void process(Message msg) {
                    final Long conductance= msg.getData(Long.class);
                    Log.i("test", String.format("Conductance: %d", conductance));
                    MainActivity.this.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            ((TextView) findViewById(R.id.textView8)).setText(String.format("%d", conductance));
                        }
                    });
                }
            }).commit();
            mwBoard.getModule(Gsr.class).calibrate();
            gsrSetup= true;
        }
        mwBoard.getModule(Gsr.class).readConductance((byte) 0);
    }

    private AsyncResult<RouteManager> passthroughRoute, anotherSwitchRoute;
    private AsyncResult<Timer.Controller> anotherTimerCtrllr;
    public void passthroughMe(View v) {
        final Switch mySwitch= (Switch) v;
        if (mySwitch.isChecked()) {
            passthroughRoute= mwBoard.routeData().fromTemperature()
                    .process("pt", new Passthrough(Passthrough.Mode.COUNT, (short) 8))
                        .subscribe(new DataSignal.MessageProcessor() {
                            @Override
                            public void process(Message msg) {
                                Float temp= msg.getData(Float.class);
                                Log.i("test", String.format("Passthrough temp: %.3f", temp));
                            }
                        })
                    .split()
                        .branch().process(new Delta(Delta.Mode.DIFFERENTIAL, 2.f))
                            .subscribe(new DataSignal.MessageProcessor() {
                                @Override
                                public void process(Message msg) {
                                    Log.i("test", String.format("Delta temp: %.3f", msg.getData(Float.class)));
                                }
                            })
                        .branch().process(new Threshold(30.f, Threshold.Mode.ABSOLUTE))
                            .subscribe(new DataSignal.MessageProcessor() {
                                @Override
                                public void process(Message msg) {
                                    Log.i("test", String.format("Upper threshold crossed: %.3f", msg.getData(Float.class)));
                                }
                            })
                        .branch().process(new Threshold(28.f, Threshold.Mode.ABSOLUTE))
                            .subscribe(new DataSignal.MessageProcessor() {
                                @Override
                                public void process(Message msg) {
                                    Log.i("test", String.format("Lower threshold crossed: %.3f", msg.getData(Float.class)));
                                }
                            })
                    .end()
                    .commit();
            passthroughRoute.onComplete(new CompletionHandler<RouteManager>() {
                @Override
                public void success(final RouteManager result) {
                    anotherSwitchRoute= mwBoard.routeData().fromSwitch()
                            .monitor(new DataSignal.ActivityMonitor() {
                                @Override
                                public void onSignalActive(Map<String, DataProcessor> processors, MessageToken signalData) {
                                    result.getDataProcessor("pt").setState(new Passthrough.PassthroughStateEditor((short) 8));
                                }
                            }).commit();
                    anotherSwitchRoute.onComplete(new CompletionHandler<RouteManager>() {
                        @Override
                        public void success(RouteManager result) {
                            anotherTimerCtrllr= mwBoard.getModule(Timer.class).createTimer(500, (short) -1, true);
                            anotherTimerCtrllr.onComplete(new CompletionHandler<Timer.Controller>() {
                                @Override
                                public void success(Timer.Controller result) {
                                    mwBoard.getModule(Temperature.class).enableThermistorMode((byte) 0, (byte) 1);
                                    result.schedule(new Timer.Task() {
                                        @Override
                                        public void execute() {
                                            mwBoard.getModule(Temperature.class).readTemperarure();
                                        }
                                    });
                                    result.start();
                                }
                            });
                        }
                    });
                }
            });
        } else {
            passthroughRoute.onComplete(new CompletionHandler<RouteManager>() {
                @Override
                public void success(RouteManager result) {
                    result.remove();
                }
            });
            anotherSwitchRoute.onComplete(new CompletionHandler<RouteManager>() {
                @Override
                public void success(RouteManager result) {
                    result.remove();
                }
            });
            anotherTimerCtrllr.onComplete(new CompletionHandler<Timer.Controller>() {
                @Override
                public void success(Timer.Controller result) {
                    result.stop();
                    result.remove();
                }
            });
        }
    }

    public void ibeaconMe(View v) {
        final Switch mySwitch= (Switch) v;

        if (mySwitch.isChecked()) {
            IBeacon ibeacon= mwBoard.getModule(IBeacon.class);
            AsyncResult<IBeacon.Configuration> result = ibeacon.readConfiguration();
            result.onComplete(new CompletionHandler<IBeacon.Configuration>() {
                @Override
                public void success(IBeacon.Configuration result) {
                    Log.i("test", result.toString());
                }
            });
            ibeacon.enable();
        } else {
            mwBoard.getModule(IBeacon.class).disable();
        }
    }

    public void buzzerMe(View v) {
        mwBoard.getModule(Haptic.class).startBuzzer((short) 5000);
    }

    public void motorMe(View v) {
        mwBoard.getModule(Haptic.class).startMotor((short) 5000);
    }

    public void neopixelMe(View v) {
        final Switch mySwitch= (Switch) v;
        final byte nLeds= 30, strand= 0;

        if (mySwitch.isChecked()) {
            NeoPixel npModule= mwBoard.getModule(NeoPixel.class);

            npModule.initializeStrand(strand, NeoPixel.ColorOrdering.MW_WS2811_GRB, NeoPixel.StrandSpeed.SLOW, (byte) 0, nLeds);
            double delta= 2 * java.lang.Math.PI / nLeds;

            npModule.holdStrand(strand);
            for(byte i= 0; i < nLeds; i++) {
                double step= i * delta;
                double rRatio= java.lang.Math.cos(step),
                        gRatio= java.lang.Math.cos(step + 2 * java.lang.Math.PI/3),
                        bRatio= java.lang.Math.cos(step + 4 * java.lang.Math.PI/3);
                npModule.setPixel(strand, i, (byte)((rRatio < 0 ? 0 : rRatio) * 255),
                        (byte)((gRatio < 0 ? 0 : gRatio) * 255),
                        (byte)((bRatio < 0 ? 0 : bRatio) * 255));
            }
            npModule.releaseHold(strand);
            npModule.rotate(strand, NeoPixel.RotationDirection.AWAY, (short) 200);
        } else {
            NeoPixel npModule= mwBoard.getModule(NeoPixel.class);
            npModule.stopRotation(strand);
            npModule.clearStrand(strand, (byte) 0, nLeds);
        }
    }

    public void settingsMe(View v) {
        final Settings settingsModule= mwBoard.getModule(Settings.class);
        settingsModule.readAdConfig().onComplete(new CompletionHandler<Settings.AdvertisementConfig>() {
            @Override
            public void success(Settings.AdvertisementConfig result) {
                Log.i("test", "Ad config: " + result.toString());
                settingsModule.edit().withDeviceName("AntiWare").commit();
            }
        });
    }

    private MultiChannelTemperature.Source preset, onDie;
    private AsyncResult<RouteManager> multiTemp, onDieTemp;
    private boolean multitempSetup= false;
    public void multitempMe(View v) {
        if (!multitempSetup) {
            mwBoard.getModule(MultiChannelTemperature.class).readSources()
                    .onComplete(new CompletionHandler<MultiChannelTemperature.Source[]>() {
                        @Override
                        public void success(MultiChannelTemperature.Source[] result) {
                            preset = result[1];
                            multiTemp = mwBoard.routeData().fromTemperature(preset).subscribe(new DataSignal.MessageProcessor() {
                                @Override
                                public void process(Message msg) {
                                    Log.i("test", String.format("Preset thermistor: %.3f", msg.getData(Float.class)));
                                }
                            }).commit();

                            onDie = result[0];
                            onDieTemp = mwBoard.routeData().fromTemperature(onDie).subscribe(new DataSignal.MessageProcessor() {
                                @Override
                                public void process(Message msg) {
                                    Log.i("test", String.format("on-die temperature: %.3f", msg.getData(Float.class)));
                                }
                            }).commit();
                            multitempSetup = true;

                            multiTemp.onComplete(new CompletionHandler<RouteManager>() {
                                @Override
                                public void success(RouteManager result) {
                                    mwBoard.getModule(MultiChannelTemperature.class).readTemperature(onDie);
                                }
                            });
                            onDieTemp.onComplete(new CompletionHandler<RouteManager>() {
                                @Override
                                public void success(RouteManager result) {
                                    mwBoard.getModule(MultiChannelTemperature.class).readTemperature(preset);
                                }
                            });
                        }
                    });
        } else {
            multiTemp.onComplete(new CompletionHandler<RouteManager>() {
                @Override
                public void success(RouteManager result) {
                    mwBoard.getModule(MultiChannelTemperature.class).readTemperature(onDie);
                }
            });
            onDieTemp.onComplete(new CompletionHandler<RouteManager>() {
                @Override
                public void success(RouteManager result) {
                    mwBoard.getModule(MultiChannelTemperature.class).readTemperature(preset);
                }
            });
        }
    }

    public void i2cMe(View v) {
        AsyncResult<byte[]> result= mwBoard.getModule(I2C.class).readData((byte) 0x1c, (byte) 0xd, (byte) 1);
        result.onComplete(new CompletionHandler<byte[]>() {
            @Override
            public void failure(Throwable error) {
                Log.e("test", "Error reading I2C data", error);
            }

            @Override
            public void success(byte[] result) {
                Log.i("test", "result: " + Arrays.toString(result));
            }
        });
    }

    public void macroMe(View v) {
        Macro macroModule= mwBoard.getModule(Macro.class);
        Led ledModule= mwBoard.getModule(Led.class);

        macroModule.record(true).onComplete(new CompletionHandler<Byte>() {
            @Override
            public void success(Byte result) {
                Log.i("test", "Macro Id: " + result);
            }
        });
        ledModule.writeChannelAttributes(Led.ColorChannel.BLUE)
                .withRiseTime((short) 0).withPulseDuration((short) 1000)
                .withRepeatCount((byte) 5).withHighTime((short) 500)
                .withHighIntensity((byte) 16).withLowIntensity((byte) 16)
                .commit();
        ledModule.play(false);
        macroModule.stop();
    }

    public void clearMacroMe(View v) {
        mwBoard.getModule(Macro.class).eraseMacros();
        mwBoard.getModule(Debug.class).resetAfterGarbageCollect();
    }
}
