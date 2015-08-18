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

package com.mbientlab.metawear.example;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
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

import com.mbientlab.metawear.AsyncOperation;
import com.mbientlab.metawear.MetaWearBoard;
import com.mbientlab.metawear.RouteManager;
import com.mbientlab.metawear.UnsupportedModuleException;
import com.mbientlab.metawear.data.CartesianFloat;
import com.mbientlab.metawear.data.CartesianShort;
import com.mbientlab.metawear.MetaWearBleService;
import com.mbientlab.metawear.data.Units;
import com.mbientlab.metawear.module.*;
import com.mbientlab.metawear.processor.*;
import com.mbientlab.metawear.processor.Maths;
import com.mbientlab.metawear.DataSignal;
import com.mbientlab.metawear.Message;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import static com.mbientlab.metawear.AsyncOperation.CompletionHandler;
import static com.mbientlab.metawear.MetaWearBoard.ConnectionStateHandler;


public class MainActivity extends ActionBarActivity implements ServiceConnection {
    private static String SHARED_PREF_KEY= "com.mbientlab.metawear.example.MainActivity", ROUTE_STATE= "route_state", ROUTE_ID= "route_id";

    private MetaWearBoard mwBoard;
    private SharedPreferences sharedPrefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        getApplicationContext().bindService(new Intent(this, MetaWearBleService.class),
                this, Context.BIND_AUTO_CREATE);
        sharedPrefs= getSharedPreferences(SHARED_PREF_KEY, Context.MODE_PRIVATE);
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
    //private final String MW_MAC_ADDRESS= "C8:D2:BA:90:60:03";
    //private final String MW_MAC_ADDRESS= "E5:58:D9:C4:1C:AF";
    //private final String MW_MAC_ADDRESS= "D6:0E:50:D0:AB:CE";
    //private final String MW_MAC_ADDRESS= "DD:F2:D5:07:B6:57";
    private final String MW_MAC_ADDRESS= "D5:7B:B9:7D:CE:0E";

    @Override
    public void onServiceConnected(ComponentName name, IBinder service) {
        MetaWearBleService.LocalBinder binder = (MetaWearBleService.LocalBinder) service;

        final BluetoothManager btManager= (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        final BluetoothDevice remoteDevice= btManager.getAdapter().getRemoteDevice(MW_MAC_ADDRESS);

        //binder.clearCachedState(remoteDevice);
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
    public void accelerometerMe(View v) {
        final Switch mySwitch= (Switch) v;

        try {
            final Logging loggingModule = mwBoard.getModule(Logging.class);
            final Accelerometer accelModule = mwBoard.getModule(Accelerometer.class);

            if (mySwitch.isChecked()) {
                if (!accelSetup) {
                    accelModule.routeData().fromAxes().stream("accelSub").log("accelLogger").commit()
                            .onComplete(new CompletionHandler<RouteManager>() {
                                @Override
                                public void success(RouteManager result) {
                                    result.subscribe("accelSub", new RouteManager.MessageHandler() {
                                        @Override
                                        public void process(Message msg) {
                                            final CartesianShort axisData = msg.getData(CartesianShort.class);
                                            Log.i("test", String.format("Stream: %s", axisData.toString()));
                                            MainActivity.this.runOnUiThread(new Runnable() {
                                                @Override
                                                public void run() {
                                                    ((TextView) findViewById(R.id.textView3)).setText(axisData.toString());
                                                }
                                            });
                                        }
                                    });
                                    result.setLogMessageHandler("accelLogger", new RouteManager.MessageHandler() {
                                        @Override
                                        public void process(Message msg) {
                                            final CartesianShort axisData = msg.getData(CartesianShort.class);
                                            Log.i("test", String.format("Log: %s", axisData.toString()));
                                        }
                                    });

                                    accelModule.setOutputDataRate(50.f);

                                    accelSetup = true;
                                    loggingModule.startLogging();

                                    accelModule.enableAxisSampling();
                                    accelModule.start();
                                }

                                @Override
                                public void failure(Throwable error) {
                                    Log.e("test", "Error committing route", error);
                                    accelSetup = false;
                                }
                            });

                } else {
                    loggingModule.startLogging();

                    accelModule.enableAxisSampling();
                    accelModule.start();
                }

            } else {
                loggingModule.stopLogging();
                accelModule.disableAxisSampling();
                accelModule.stop();
            }
        } catch (UnsupportedModuleException e) {
            Toast.makeText(this, e.getLocalizedMessage(), Toast.LENGTH_LONG).show();
        }
    }

    public void downloadMe(View v) {
        try {
            mwBoard.getModule(Logging.class).downloadLog(0.05f, new Logging.DownloadHandler() {
                @Override
                public void onProgressUpdate(int nEntriesLeft, int totalEntries) {
                    Log.i("test", String.format("Progress= %d / %d", nEntriesLeft, totalEntries));
                }
            });
        } catch (UnsupportedModuleException e) {
            Toast.makeText(this, e.getLocalizedMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private Timer.Controller timerTempResult;
    private boolean tempSetup= false;
    public void temperatureMe(View v) {
        final Switch mySwitch= (Switch) v;

        try {
            final Timer timerModule = mwBoard.getModule(Timer.class);
            final Temperature tempModule = mwBoard.getModule(Temperature.class);

            if (mySwitch.isChecked()) {
                if (!tempSetup) {
                    timerModule.scheduleTask(new Timer.Task() {
                        @Override
                        public void commands() {
                            tempModule.readTemperature();
                        }
                    }, 1000, false).onComplete(new CompletionHandler<Timer.Controller>() {
                        @Override
                        public void success(Timer.Controller result) {
                            timerTempResult = result;
                            timerTempResult.start();
                        }
                    });

                    tempModule.routeData().fromSensor().stream("tempC")
                        .split()
                            .branch()
                                .process(new Maths(Maths.Operation.MULTIPLY, 18))
                                .process(new Maths(Maths.Operation.DIVIDE, 10))
                                .process(new Maths(Maths.Operation.ADD, 32.f)).stream("tempF")
                            .branch()
                                .process("math?operation=add&rhs=273.15").stream("tempK")
                        .end()
                        .commit().onComplete(new CompletionHandler<RouteManager>() {
                        @Override
                        public void success(RouteManager result) {
                            result.subscribe("tempC", new RouteManager.MessageHandler() {
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
                            });
                            result.subscribe("tempF", new RouteManager.MessageHandler() {
                                @Override
                                public void process(Message msg) {
                                    Log.i("test", String.format("%.3f F", msg.getData(Float.class)));
                                }
                            });
                            result.subscribe("tempK", new RouteManager.MessageHandler() {
                                @Override
                                public void process(Message msg) {
                                    Log.i("test", String.format("%.3f K", msg.getData(Float.class)));
                                }
                            });
                            if (timerTempResult != null) {
                                timerTempResult.start();
                            }
                        }

                        @Override
                        public void failure(Throwable error) {
                            Log.i("test", "Error commiting route", error);
                        }
                    });
                    tempSetup = true;
                } else {
                    timerTempResult.start();
                }
            } else {
                timerTempResult.stop();
            }
        } catch (UnsupportedModuleException e) {
            Toast.makeText(this, e.getLocalizedMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private int routeId= -1;
    public void programRouteMe(View v) {
        try {
            final Led ledModule = mwBoard.getModule(Led.class);
            final Macro macroModule = mwBoard.getModule(Macro.class);
            final Accelerometer accelModule= mwBoard.getModule(Accelerometer.class);
            final SharedPreferences.Editor editor = sharedPrefs.edit();

            macroModule.record(new Macro.CodeBlock() {
                @Override
                public void commands() {
                    ledModule.configureColorChannel(Led.ColorChannel.BLUE)
                            .setRiseTime((short) 0).setPulseDuration((short) 1000)
                            .setRepeatCount((byte) 5).setHighTime((short) 500)
                            .setHighIntensity((byte) 16).setLowIntensity((byte) 16)
                            .commit();
                    accelModule.routeData().fromAxes()
                            .process(new Time(Time.OutputMode.ABSOLUTE, 5000)).log("accelAxisLogger")
                            .process(new Rms()).log("accelRmsLogger")
                            .commit().onComplete(new CompletionHandler<RouteManager>() {
                        @Override
                        public void success(RouteManager result) {
                            routeId = result.id();
                            editor.putInt(ROUTE_ID, result.id());
                        }
                    });
                    ledModule.play(false);
                }
            }).onComplete(new CompletionHandler<Byte>() {
                @Override
                public void success(Byte result) {
                    final String state = new String(mwBoard.serializeState());
                    editor.putString(ROUTE_STATE, state);
                    editor.apply();
                    Log.i("test", state);
                }
            });
        } catch (UnsupportedModuleException e) {
            Toast.makeText(this, e.getLocalizedMessage(), Toast.LENGTH_LONG).show();
        }

    }

    public void syncMe(View v) {
        String stateString= sharedPrefs.getString(ROUTE_STATE, "");
        if (!stateString.isEmpty()) {
            mwBoard.deserializeState(stateString.getBytes());
            routeId= (byte) sharedPrefs.getInt(ROUTE_ID, -1);

        }
    }

    public void filterMe(View v) {
        final Switch mySwitch= (Switch) v;

        try {
            Accelerometer accelModule= mwBoard.getModule(Accelerometer.class);
            if (mySwitch.isChecked()) {

                accelModule.enableAxisSampling();
                accelModule.start();

                RouteManager manager= mwBoard.getRouteManager(routeId);

                manager.setLogMessageHandler("accelAxisLogger", new RouteManager.MessageHandler() {
                    @Override
                    public void process(Message msg) {
                        CartesianShort milliG = msg.getData(CartesianShort.class);
                        Log.i("test", String.format("XYZ Axis: (%d, %d, %d)", milliG.x(), milliG.y(), milliG.z()));
                    }
                });
                manager.setLogMessageHandler("accelRmsLogger", new RouteManager.MessageHandler() {
                    @Override
                    public void process(Message msg) {
                        Log.i("test", String.format("RMS: %d", msg.getData(Short.class)));
                    }
                });
            } else {
                accelModule.stop();
                accelModule.disableAxisSampling();
            }
        } catch (UnsupportedModuleException e) {
            Toast.makeText(this, e.getLocalizedMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private AsyncOperation<Timer.Controller> timerController;
    private AsyncOperation<RouteManager> analogRoute, switchRoute;
    public void analogMe(View v) {
        final Switch mySwitch= (Switch) v;

        try {
            final Gpio gpioModule = mwBoard.getModule(Gpio.class);
            final Timer timerModule= mwBoard.getModule(Timer.class);
            final com.mbientlab.metawear.module.Switch switchModule= mwBoard.getModule(com.mbientlab.metawear.module.Switch.class);

            if (mySwitch.isChecked()) {
                analogRoute= gpioModule.routeData().fromAnalogIn((byte) 0, Gpio.AnalogReadMode.ADC)
                        .process("mathprocesser", "math?operation=mult&rhs=1")
                        .stream("analog_gpio")
                        .commit();
                analogRoute.onComplete(new CompletionHandler<RouteManager>() {
                    @Override
                    public void success(RouteManager result) {
                        result.subscribe("analog_gpio", new RouteManager.MessageHandler() {
                            @Override
                            public void process(final Message msg) {
                                MainActivity.this.runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        ((TextView) findViewById(R.id.textView6)).setText(String.format("%d", msg.getData(Short.class)));
                                    }
                                });
                            }
                        });
                        gpioModule.clearDigitalOut((byte) 1);
                        switchRoute = switchModule.routeData().fromSensor().monitor(new DataSignal.ActivityHandler() {
                            @Override
                            public void onSignalActive(Map<String, DataProcessor> processors, DataSignal.DataToken token) {
                                processors.get("mathprocesser").modifyConfiguration(new Maths(Maths.Operation.MULTIPLY, token));
                            }
                        }).commit();
                        switchRoute.onComplete(new CompletionHandler<RouteManager>() {
                            @Override
                            public void success(RouteManager result) {
                                timerController = timerModule.scheduleTask(new Timer.Task() {
                                    @Override
                                    public void commands() {
                                        gpioModule.readAnalogIn((byte) 0, Gpio.AnalogReadMode.ADC);
                                    }
                                }, 500, false);
                                timerController.onComplete(new CompletionHandler<Timer.Controller>() {
                                    @Override
                                    public void success(Timer.Controller result) {
                                        result.start();
                                    }
                                });
                            }
                        });
                    }
                });
            } else {
                try {
                    analogRoute.result().remove();
                    switchRoute.result().remove();
                } catch (ExecutionException e) {
                    Log.e("test", "Exception in route", e);
                } catch (InterruptedException e) {
                    Log.e("test", "Result not ready");
                }
                timerController.onComplete(new CompletionHandler<Timer.Controller>() {
                    @Override
                    public void success(Timer.Controller result) {
                        result.stop();
                        result.remove();
                    }
                });
            }
        } catch (UnsupportedModuleException e) {
            Toast.makeText(this, e.getLocalizedMessage(), Toast.LENGTH_LONG).show();
        }
    }

    public void programMe(View v) {
        final Switch mwSwitch= (Switch) v;

        try {
            final com.mbientlab.metawear.module.Switch switchModule= mwBoard.getModule(com.mbientlab.metawear.module.Switch.class);
            final Led ledCtrllr = mwBoard.getModule(Led.class);
            if (mwSwitch.isChecked()) {
                switchModule.routeData().fromSensor()
                        .process("accumprocesser", "accumulator")
                        .process("math?operation=modulus&rhs=2")
                        .split()
                            .branch().process("comparison?operation=eq&reference=1").monitor(new DataSignal.ActivityHandler() {
                                @Override
                                public void onSignalActive(Map<String, DataProcessor> processors, DataSignal.DataToken token) {
                                    ledCtrllr.configureColorChannel(Led.ColorChannel.BLUE)
                                            .setRiseTime((short) 0).setPulseDuration((short) 1000)
                                            .setRepeatCount((byte) -1).setHighTime((short) 500)
                                            .setHighIntensity((byte) 16).setLowIntensity((byte) 16)
                                            .commit();
                                    ledCtrllr.play(false);
                                }
                            })
                            .branch().process("comparison?operation=eq&reference=0").monitor(new DataSignal.ActivityHandler() {
                                @Override
                                public void onSignalActive(Map<String, DataProcessor> processors, DataSignal.DataToken token) {
                                    ledCtrllr.stop(true);
                                }
                            })
                        .end()
                .commit();
            } else {
                mwBoard.removeRoutes();
            }
        } catch (UnsupportedModuleException e) {
            Toast.makeText(this, e.getLocalizedMessage(), Toast.LENGTH_LONG).show();
        }

    }

    public void resetMe(View v) {
        try {
            mwBoard.getModule(Debug.class).resetDevice();
        } catch (UnsupportedModuleException e) {
            Toast.makeText(this, e.getLocalizedMessage(), Toast.LENGTH_LONG).show();
        }
    }
    public void removeMe(View v) {
        mwBoard.removeRoutes();

        try {
            mwBoard.getModule(Timer.class).removeTimers();
        } catch (UnsupportedModuleException e) {
            Toast.makeText(this, e.getLocalizedMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private boolean gyroSetup= false;
    public void gyroMe(View v) {
        final Switch mySwitch= (Switch) v;

        try {
            final Bmi160Gyro gyroModule = mwBoard.getModule(Bmi160Gyro.class);
            final Logging loggingModule= mwBoard.getModule(Logging.class);
            if (mySwitch.isChecked()) {
                if (!gyroSetup) {
                    gyroModule.routeData().fromAxes().stream("gyroAxisSub")
                            .commit().onComplete(new CompletionHandler<RouteManager>() {
                        @Override
                        public void success(RouteManager result) {
                            result.subscribe("gyroAxisSub", new RouteManager.MessageHandler() {
                                @Override
                                public void process(Message msg) {
                                    final CartesianFloat spinData = msg.getData(CartesianFloat.class);

                                    Log.i("test", spinData.toString());
                                    MainActivity.this.runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            ((TextView) findViewById(R.id.textView7)).setText(spinData.toString() + Units.DEGS_PER_SEC);
                                        }
                                    });
                                }
                            });
                        }
                    });
                    gyroModule.routeData().fromYAxis().log("gyroAxisLogger").commit()
                            .onComplete(new CompletionHandler<RouteManager>() {
                                @Override
                                public void success(RouteManager result) {
                                    result.setLogMessageHandler("gyroAxisLogger", new RouteManager.MessageHandler() {
                                        @Override
                                        public void process(Message msg) {
                                            final float ySpinData = msg.getData(Float.class);

                                            Log.i("test", String.format("Log Gyro: %.3f", ySpinData));
                                        }
                                    });

                                    loggingModule.startLogging();
                                    gyroModule.configure().setOutputDataRate(Bmi160Gyro.OutputDataRate.ODR_25_HZ)
                                            .setFullScaleRange(Bmi160Gyro.FullScaleRange.FSR_250)
                                            .commit();
                                    gyroModule.start();
                                }
                            });
                    gyroSetup= true;
                } else {
                    loggingModule.startLogging();
                    gyroModule.configure().setOutputDataRate(Bmi160Gyro.OutputDataRate.ODR_25_HZ)
                            .setFullScaleRange(Bmi160Gyro.FullScaleRange.FSR_125)
                            .commit();
                    gyroModule.start();
                }
            } else {
                loggingModule.stopLogging();
                gyroModule.stop();
            }
        } catch (UnsupportedModuleException e) {
            Toast.makeText(this, e.getLocalizedMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private boolean gsrSetup= false;
    public void gsrMe(View v) {
        try {
            Gsr gsrModule= mwBoard.getModule(Gsr.class);
            if (!gsrSetup) {
                gsrModule.routeData((byte) 0).stream("gsr_sub")
                        .commit().onComplete(new CompletionHandler<RouteManager>() {
                    @Override
                    public void success(RouteManager result) {
                        result.subscribe("gsr_sub", new RouteManager.MessageHandler() {
                            @Override
                            public void process(Message msg) {
                                final Long conductance = msg.getData(Long.class);
                                Log.i("test", String.format("Conductance: %d", conductance));
                                MainActivity.this.runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        ((TextView) findViewById(R.id.textView8)).setText(String.format("%d", conductance));
                                    }
                                });
                            }
                        });
                    }
                });
                gsrModule.calibrate();
                gsrSetup= true;
            }
            gsrModule.readConductance((byte) 0);
        } catch (UnsupportedModuleException e) {
            Toast.makeText(this, e.getLocalizedMessage(), Toast.LENGTH_LONG).show();
        }

    }

    private AsyncOperation<Timer.Controller> anotherTimerCtrllr;
    private boolean passthroughTempSetup= false;
    public void passthroughMe(View v) {
        final Switch mySwitch= (Switch) v;

        try {
            final Timer timerModule = mwBoard.getModule(Timer.class);
            final Temperature tempModule = mwBoard.getModule(Temperature.class);
            final com.mbientlab.metawear.module.Switch switchModule= mwBoard.getModule(com.mbientlab.metawear.module.Switch.class);

            if (mySwitch.isChecked()) {
                if (!passthroughTempSetup) {
                    tempModule.routeData().fromSensor()
                        .process("pt", new Passthrough(Passthrough.Mode.COUNT, (short) 8))
                        .stream("passthrough_temp")
                        .split()
                            .branch().process(new Delta(Delta.OutputMode.DIFFERENTIAL, 2.f)).stream("differential_temp")
                            .branch().process(new Threshold(30.f, Threshold.OutputMode.ABSOLUTE)).stream("upper_ths_temp")
                            .branch().process(new Threshold(28.f, Threshold.OutputMode.ABSOLUTE)).stream("lower_ths_temp")
                        .end()
                        .commit().onComplete(new CompletionHandler<RouteManager>() {
                        @Override
                        public void success(final RouteManager result) {
                            result.subscribe("passthrough_temp", new RouteManager.MessageHandler() {
                                @Override
                                public void process(Message msg) {
                                    Float temp = msg.getData(Float.class);
                                    Log.i("test", String.format("Passthrough temp: %.3f", temp));
                                }
                            });
                            result.subscribe("differential_temp", new RouteManager.MessageHandler() {
                                @Override
                                public void process(Message msg) {
                                    Log.i("test", String.format("Delta temp: %.3f", msg.getData(Float.class)));
                                }
                            });
                            result.subscribe("upper_ths_temp", new RouteManager.MessageHandler() {
                                @Override
                                public void process(Message msg) {
                                    Log.i("test", String.format("Upper threshold crossed: %.3f", msg.getData(Float.class)));
                                }
                            });
                            result.subscribe("lower_ths_temp", new RouteManager.MessageHandler() {
                                @Override
                                public void process(Message msg) {
                                    Log.i("test", String.format("Lower threshold crossed: %.3f", msg.getData(Float.class)));
                                }
                            });
                            switchModule.routeData().fromSensor().monitor(new DataSignal.ActivityHandler() {
                                @Override
                                public void onSignalActive(Map<String, DataProcessor> processors, DataSignal.DataToken token) {
                                    processors.get("pt").setState(new Passthrough.State((short) 8));
                                }
                            })
                                    .commit().onComplete(new CompletionHandler<RouteManager>() {
                                @Override
                                public void success(RouteManager result) {
                                    anotherTimerCtrllr = timerModule.scheduleTask(new Timer.Task() {
                                        @Override
                                        public void commands() {
                                            tempModule.readTemperature();
                                        }
                                    }, 500, false);
                                    anotherTimerCtrllr.onComplete(new CompletionHandler<Timer.Controller>() {
                                        @Override
                                        public void success(Timer.Controller result) {
                                            result.start();
                                        }
                                    });
                                }
                            });
                        }
                    });
                    passthroughTempSetup = true;
                } else {
                    anotherTimerCtrllr.onComplete(new CompletionHandler<Timer.Controller>() {
                        @Override
                        public void success(Timer.Controller result) {
                            result.start();
                        }
                    });
                }
            } else {
                anotherTimerCtrllr.onComplete(new CompletionHandler<Timer.Controller>() {
                    @Override
                    public void success(Timer.Controller result) {
                        result.stop();
                    }
                });
            }
        } catch (UnsupportedModuleException e) {
            Toast.makeText(this, e.getLocalizedMessage(), Toast.LENGTH_LONG).show();
        }
    }

    public void ibeaconMe(View v) {
        final Switch mySwitch= (Switch) v;

        try {
            IBeacon ibeaconModule= mwBoard.getModule(IBeacon.class);
            if (mySwitch.isChecked()) {

                AsyncOperation<IBeacon.Configuration> result = ibeaconModule.readConfiguration();
                result.onComplete(new CompletionHandler<IBeacon.Configuration>() {
                    @Override
                    public void success(IBeacon.Configuration result) {
                        Log.i("test", result.toString());
                    }
                });
                ibeaconModule.enable();
            } else {
                ibeaconModule.disable();
            }
        } catch (UnsupportedModuleException e) {
            Toast.makeText(this, e.getLocalizedMessage(), Toast.LENGTH_LONG).show();
        }
    }

    public void buzzerMe(View v) {
        try {
            mwBoard.getModule(Haptic.class).startBuzzer((short) 5000);
        } catch (UnsupportedModuleException e) {
            Toast.makeText(this, e.getLocalizedMessage(), Toast.LENGTH_LONG).show();
        }
    }

    public void motorMe(View v) {
        try {
            mwBoard.getModule(Haptic.class).startMotor((short) 5000);
        } catch (UnsupportedModuleException e) {
            Toast.makeText(this, e.getLocalizedMessage(), Toast.LENGTH_LONG).show();
        }
    }

    public void neopixelMe(View v) {
        final Switch mySwitch= (Switch) v;
        final byte nLeds= 30, strand= 0;

        try {
            NeoPixel npModule = mwBoard.getModule(NeoPixel.class);

            if (mySwitch.isChecked()) {
                npModule.initializeStrand(strand, NeoPixel.ColorOrdering.MW_WS2811_GRB, NeoPixel.StrandSpeed.SLOW, (byte) 0, nLeds);
                double delta = 2 * java.lang.Math.PI / nLeds;

                npModule.holdStrand(strand);
                for (byte i = 0; i < nLeds; i++) {
                    double step = i * delta;
                    double rRatio = java.lang.Math.cos(step),
                            gRatio = java.lang.Math.cos(step + 2 * java.lang.Math.PI / 3),
                            bRatio = java.lang.Math.cos(step + 4 * java.lang.Math.PI / 3);
                    npModule.setPixel(strand, i, (byte) ((rRatio < 0 ? 0 : rRatio) * 255),
                            (byte) ((gRatio < 0 ? 0 : gRatio) * 255),
                            (byte) ((bRatio < 0 ? 0 : bRatio) * 255));
                }
                npModule.releaseHold(strand);
                npModule.rotate(strand, NeoPixel.RotationDirection.AWAY, (short) 200);
            } else {
                npModule.stopRotation(strand);
                npModule.clearStrand(strand, (byte) 0, nLeds);
            }
        } catch (UnsupportedModuleException e) {
            Toast.makeText(this, e.getLocalizedMessage(), Toast.LENGTH_LONG).show();
        }
    }

    public void settingsMe(View v) {
        try {
            final Settings settingsModule = mwBoard.getModule(Settings.class);
            settingsModule.readAdConfig().onComplete(new CompletionHandler<Settings.AdvertisementConfig>() {
                @Override
                public void success(Settings.AdvertisementConfig result) {
                    Log.i("test", "Ad config: " + result.toString());
                    settingsModule.configure().setDeviceName("AntiWare").commit();
                }
            });
        } catch (UnsupportedModuleException e) {
            Toast.makeText(this, e.getLocalizedMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private List<MultiChannelTemperature.Source> sources;
    private AsyncOperation<RouteManager> multiTemp, onDieTemp;
    private boolean multitempSetup= false;
    public void multitempMe(View v) {
        try {
            final MultiChannelTemperature multiTempModule= mwBoard.getModule(MultiChannelTemperature.class);

            if (!multitempSetup) {
                sources= multiTempModule.getSources();
                multiTemp = multiTempModule.routeData().fromSource(sources.get(1)).stream("multi_thermistor").commit();
                if (sources.get(1) instanceof MultiChannelTemperature.ExtThermistor) {
                    MultiChannelTemperature.ExtThermistor thermistor= (MultiChannelTemperature.ExtThermistor) sources.get(1);
                    thermistor.configure((byte) 0, (byte) 1, false);
                }

                onDieTemp = multiTempModule.routeData().fromSource(sources.get(0)).stream("multi_on_die").commit();
                multitempSetup = true;

                multiTemp.onComplete(new CompletionHandler<RouteManager>() {
                    @Override
                    public void success(RouteManager result) {
                        result.subscribe("multi_thermistor", new RouteManager.MessageHandler() {
                            @Override
                            public void process(Message msg) {
                                Log.i("test", String.format("Ext thermistor: %.3f", msg.getData(Float.class)));
                            }
                        });
                        multiTempModule.readTemperature(sources.get(0));
                    }
                });
                onDieTemp.onComplete(new CompletionHandler<RouteManager>() {
                    @Override
                    public void success(RouteManager result) {
                        result.subscribe("multi_on_die", new RouteManager.MessageHandler() {
                            @Override
                            public void process(Message msg) {
                                Log.i("test", String.format("on-die temperature: %.3f", msg.getData(Float.class)));
                            }
                        });
                        multiTempModule.readTemperature(sources.get(1));
                    }
                });

                multitempSetup= true;
            } else {
                multiTemp.onComplete(new CompletionHandler<RouteManager>() {
                    @Override
                    public void success(RouteManager result) {
                        multiTempModule.readTemperature(sources.get(0));
                    }
                });
                onDieTemp.onComplete(new CompletionHandler<RouteManager>() {
                    @Override
                    public void success(RouteManager result) {
                        multiTempModule.readTemperature(sources.get(1));
                    }
                });
            }
        } catch (UnsupportedModuleException e) {
            Toast.makeText(this, e.getLocalizedMessage(), Toast.LENGTH_LONG).show();
        }

    }

    public void i2cMe(View v) {
        try {
            I2C i2cModule = mwBoard.getModule(I2C.class);
            AsyncOperation<byte[]> result= i2cModule.readData((byte) 0x1c, (byte) 0xd, (byte) 1);
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
        } catch (UnsupportedModuleException e) {
            Toast.makeText(this, e.getLocalizedMessage(), Toast.LENGTH_LONG).show();
        }
    }

    public void macroMe(View v) {
        try {
            final Macro macroModule = mwBoard.getModule(Macro.class);
            final Led ledModule= mwBoard.getModule(Led.class);

            macroModule.record(new Macro.CodeBlock() {
                @Override
                public void commands() {
                    ledModule.configureColorChannel(Led.ColorChannel.BLUE)
                            .setRiseTime((short) 0).setPulseDuration((short) 1000)
                            .setRepeatCount((byte) 5).setHighTime((short) 500)
                            .setHighIntensity((byte) 16).setLowIntensity((byte) 16)
                            .commit();
                    ledModule.play(false);
                }
            }).onComplete(new CompletionHandler<Byte>() {
                @Override
                public void success(Byte result) {
                    Log.i("test", "Macro Id: " + result);
                }
            });
        } catch (UnsupportedModuleException e) {
            Toast.makeText(this, e.getLocalizedMessage(), Toast.LENGTH_LONG).show();
        }
    }

    public void clearMacroMe(View v) {
        try {
            mwBoard.getModule(Macro.class).eraseMacros();
            mwBoard.getModule(Debug.class).resetAfterGarbageCollect();
        } catch (UnsupportedModuleException e) {
            Toast.makeText(this, e.getLocalizedMessage(), Toast.LENGTH_LONG).show();
        }
    }

    public void serializeMe(View v) {
        byte[] state= mwBoard.serializeState();
        Log.i("test", new String(state));
    }

    private boolean orientationSetup= false;
    public void orientationMe(View v) {
        final Switch mySwitch= (Switch) v;

        try {
            final Mma8452qAccelerometer accelModule = mwBoard.getModule(Mma8452qAccelerometer.class);
            final Logging loggingModule = mwBoard.getModule(Logging.class);

            if (mySwitch.isChecked()) {
                if (!orientationSetup) {
                    accelModule.routeData().fromOrientation().stream("orientation_sub").log("orientation_log")
                            .commit().onComplete(new CompletionHandler<RouteManager>() {
                        @Override
                        public void success(RouteManager result) {
                            result.subscribe("orientation_sub", new RouteManager.MessageHandler() {
                                @Override
                                public void process(Message msg) {
                                    final String orientation = msg.getData(Mma8452qAccelerometer.Orientation.class).toString();
                                    Log.i("test", String.format(Locale.US, "Stream orientation: %s", orientation));
                                    MainActivity.this.runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            ((TextView) findViewById(R.id.textView9)).setText(orientation);
                                        }
                                    });
                                }
                            });
                            result.setLogMessageHandler("orientation_log", new RouteManager.MessageHandler() {
                                @Override
                                public void process(Message msg) {
                                    Log.i("test", String.format(Locale.US, "Log orientation: %s", msg.getData(Mma8452qAccelerometer.Orientation.class).toString()));
                                }
                            });
                        }

                        @Override
                        public void failure(Throwable error) {
                            Log.i("test", "Error setting up orientation", error);
                        }
                    });
                    orientationSetup = true;
                }
                loggingModule.startLogging();
                accelModule.enableOrientationDetection();
                accelModule.start();
            } else {
                loggingModule.stopLogging();
                accelModule.stop();
                accelModule.disableOrientationDetection();
            }
        } catch (UnsupportedModuleException e) {
            Toast.makeText(this, e.getLocalizedMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private boolean tapSetup;
    public void tapMe(View v) {
        final Switch mySwitch= (Switch) v;

        try {
            Mma8452qAccelerometer accelModule = mwBoard.getModule(Mma8452qAccelerometer.class);

            if (mySwitch.isChecked()) {
                if (!tapSetup) {
                    accelModule.routeData().fromTap().stream("tap_sub")
                            .commit().onComplete(new CompletionHandler<RouteManager>() {
                        @Override
                        public void success(RouteManager result) {
                            result.subscribe("tap_sub", new RouteManager.MessageHandler() {
                                @Override
                                public void process(Message msg) {
                                    final Mma8452qAccelerometer.TapData tapData = msg.getData(Mma8452qAccelerometer.TapData.class);

                                    MainActivity.this.runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            switch (tapData.type()) {
                                                case SINGLE:
                                                    ((TextView) findViewById(R.id.textView10)).setText("single tap");
                                                    break;
                                                case DOUBLE:
                                                    ((TextView) findViewById(R.id.textView10)).setText("double tap");
                                                    break;
                                            }

                                        }
                                    });
                                    Log.i("test", tapData.toString());
                                }
                            });
                        }
                    });
                    tapSetup = true;
                }
                accelModule.configureTapDetection().commit();
                accelModule.enableTapDetection(Mma8452qAccelerometer.TapType.DOUBLE, Mma8452qAccelerometer.TapType.SINGLE);
                accelModule.start();
            } else {
                accelModule.stop();
                accelModule.disableTapDetection();
            }
        } catch (UnsupportedModuleException e) {
            Toast.makeText(this, e.getLocalizedMessage(), Toast.LENGTH_LONG).show();
        }
    }

    public boolean shakeSetup;
    public int shakeCount= 0;
    public void shakeMe(View v) {
        final Switch mySwitch= (Switch) v;

        try {
            Mma8452qAccelerometer accelModule = mwBoard.getModule(Mma8452qAccelerometer.class);
            if (mySwitch.isChecked()) {
                if (!shakeSetup) {
                    accelModule.routeData().fromShake().stream("shake_sub").commit()
                            .onComplete(new CompletionHandler<RouteManager>() {
                                @Override
                                public void success(RouteManager result) {
                                    result.subscribe("shake_sub", new RouteManager.MessageHandler() {
                                        @Override
                                        public void process(Message msg) {
                                            shakeCount++;
                                            final Mma8452qAccelerometer.MovementData shakeData = msg.getData(Mma8452qAccelerometer.MovementData.class);
                                            Log.i("test", shakeData.toString());
                                            MainActivity.this.runOnUiThread(new Runnable() {
                                                @Override
                                                public void run() {
                                                    ((TextView) findViewById(R.id.textView11)).setText("shake " + shakeCount);
                                                }
                                            });
                                        }
                                    });
                                }
                            });
                    shakeSetup = true;
                }

                accelModule.enableShakeDetection();
                accelModule.start();
            } else {
                accelModule.stop();
                accelModule.disableShakeDetection();
            }
        } catch (UnsupportedModuleException e) {
            Toast.makeText(this, e.getLocalizedMessage(), Toast.LENGTH_LONG).show();
        }
    }

    public boolean moveSetup;
    public void moveMe(View v) {
        final Switch mySwitch= (Switch) v;

        try {
            Mma8452qAccelerometer accelModule = mwBoard.getModule(Mma8452qAccelerometer.class);

            if (mySwitch.isChecked()) {
                if (!moveSetup) {
                    accelModule.routeData().fromMovement().stream("move_sub").commit()
                            .onComplete(new CompletionHandler<RouteManager>() {
                                @Override
                                public void success(RouteManager result) {
                                    result.subscribe("move_sub", new RouteManager.MessageHandler() {
                                        @Override
                                        public void process(Message msg) {
                                            final Mma8452qAccelerometer.MovementData shakeData = msg.getData(Mma8452qAccelerometer.MovementData.class);
                                            Log.i("test", shakeData.toString());
                                            MainActivity.this.runOnUiThread(new Runnable() {
                                                @Override
                                                public void run() {
                                                    ((TextView) findViewById(R.id.textView12)).setText("motion");
                                                }
                                            });
                                        }
                                    });
                                }
                            });
                    moveSetup = true;
                }

                accelModule.configureMotionDetection().setAxes(Mma8452qAccelerometer.Axis.Z).commit();
                accelModule.enableMovementDetection(Mma8452qAccelerometer.MovementType.FREE_FALL);
                accelModule.start();
            } else {
                accelModule.stop();
                accelModule.disableMovementDetection();
            }
        } catch (UnsupportedModuleException e) {
            Toast.makeText(this, e.getLocalizedMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private boolean barometerSetup= false;
    public void barometerMe(View v) {
        final Switch mySwitch= (Switch) v;

        try {
            final Barometer barometerModule= mwBoard.getModule(Barometer.class);
            if (mySwitch.isChecked()) {
                if (!barometerSetup) {
                    barometerModule.routeData().fromPressure().process(new Time(Time.OutputMode.ABSOLUTE, 1000)).stream("pressure_sub").commit()
                            .onComplete(new CompletionHandler<RouteManager>() {
                                @Override
                                public void success(RouteManager result) {
                                    result.subscribe("pressure_sub", new RouteManager.MessageHandler() {
                                        @Override
                                        public void process(Message msg) {
                                            Log.i("test", String.format("Pressure= %.3f", msg.getData(Float.class)));
                                        }
                                    });
                                    barometerModule.start();
                                }
                            });
                    barometerModule.routeData().fromAltitude().process(new Time(Time.OutputMode.ABSOLUTE, 1000)).stream("altitude_sub").commit()
                            .onComplete(new CompletionHandler<RouteManager>() {
                                @Override
                                public void success(RouteManager result) {
                                    result.subscribe("altitude_sub", new RouteManager.MessageHandler() {
                                        @Override
                                        public void process(Message msg) {
                                            Log.i("test", String.format("Altitude= %.3f", msg.getData(Float.class)));
                                        }
                                    });
                                    barometerModule.start();
                                }
                            });
                    barometerSetup= true;
                } else {
                    barometerModule.start();
                }
            } else {
                barometerModule.stop();
            }
        } catch (UnsupportedModuleException e) {
            Toast.makeText(this, e.getLocalizedMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private boolean lightSetup= false;
    public void lightMe(View v) {
        final Switch mySwitch= (Switch) v;

        try {
            final AmbientLight lightModule= mwBoard.getModule(AmbientLight.class);

            if (mySwitch.isChecked()) {
                if (!lightSetup) {
                    lightSetup= true;
                    lightModule.routeData().fromSensor().stream("light_sub").commit()
                            .onComplete(new CompletionHandler<RouteManager>() {
                                @Override
                                public void success(RouteManager result) {
                                    result.subscribe("light_sub", new RouteManager.MessageHandler() {
                                        @Override
                                        public void process(Message msg) {
                                            Log.i("test", String.format("milli lux: %d", msg.getData(Long.class)));
                                        }
                                    });
                                    lightModule.start();
                                }
                            });
                } else {
                    lightModule.start();
                }
            } else {
                lightModule.stop();
            }
        } catch (UnsupportedModuleException e) {
            e.printStackTrace();
        }

    }
}
