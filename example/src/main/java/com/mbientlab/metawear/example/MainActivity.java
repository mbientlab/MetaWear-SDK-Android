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
import com.mbientlab.metawear.MetaWearBoard;
import com.mbientlab.metawear.impl.MetaWearBleService;
import com.mbientlab.metawear.module.*;
import com.mbientlab.metawear.processor.*;
import com.mbientlab.metawear.processor.Math;
import com.mbientlab.metawear.data.MwrAccelAxisMessage;
import com.mbientlab.metawear.DataSignal;
import com.mbientlab.metawear.Message;
import com.mbientlab.metawear.RouteManager;

import java.util.Locale;

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
    private final String MW_MAC_ADDRESS= "C5:24:07:C3:20:9F";
    //private final String MW_MAC_ADDRESS= "C8:D2:BA:90:60:03";

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
                        final MwrAccelAxisMessage.AxisMilliG milliGs = msg.getData(MwrAccelAxisMessage.AxisMilliG.class);
                        MainActivity.this.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                ((TextView) findViewById(R.id.textView3)).setText(String.format("%d, %d, %d", milliGs.x(), milliGs.y(), milliGs.z()));
                            }
                        });
                    }
                }).log(new DataSignal.MessageProcessor() {
                    @Override
                    public void process(Message msg) {
                        final MwrAccelAxisMessage.AxisMilliG milliGs = msg.getData(MwrAccelAxisMessage.AxisMilliG.class);
                        Log.i("test", String.format("%d, %d, %d", milliGs.x(), milliGs.y(), milliGs.z()));
                    }
                }).commit();
                accelSetup= true;
            }
            accelManager.onComplete(new CompletionHandler<RouteManager>() {
                @Override
                public void success(RouteManager result) {
                    mwBoard.getModule(MwrAccelerometer.class).startXYZSampling();
                    mwBoard.getModule(MwrAccelerometer.class).globalStart();
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
                    mwBoard.getModule(MwrAccelerometer.class).globalStop();
                    mwBoard.getModule(MwrAccelerometer.class).stopXYZSampling();
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
                    result.monitor(new DataSignal.ActivityMonitor() {
                        @Override
                        public void onSignalActive() {
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
                    .transform(new Math(Math.Operation.MULTIPLY, 18))
                    .transform(new Math(Math.Operation.DIVIDE, 10))
                    .transform(new Math(Math.Operation.ADD, 32.f)).subscribe(new DataSignal.MessageProcessor() {
                        @Override
                        public void process(Message msg) {
                            Log.i("test", String.format("%.3f F", msg.getData(Float.class)));
                        }
                    })
                .branch()
                    .transform("math?operation=add&rhs=273.15").subscribe(new DataSignal.MessageProcessor() {
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

    private boolean filterSetup= false;
    private AsyncResult<RouteManager> filterManager;
    public void filterMe(View v) {
        final Switch mySwitch= (Switch) v;
        if (mySwitch.isChecked()) {
            if (!filterSetup) {
                filterManager= mwBoard.routeData().fromAccelAxis()
                        .filter(new Time(5000)).log(new DataSignal.MessageProcessor() {
                            @Override
                            public void process(Message msg) {
                                MwrAccelAxisMessage.AxisMilliG milliG= msg.getData(MwrAccelAxisMessage.AxisMilliG.class);
                                Log.i("test", String.format("XYZ Axis: (%d, %d, %d)", milliG.x(), milliG.y(), milliG.z()));
                            }
                        })
                        .transform(new Rms()).log(new DataSignal.MessageProcessor() {
                            @Override
                            public void process(Message msg) {
                                Log.i("test", String.format("RMS: %d", msg.getData(Short.class)));
                            }
                        }).commit();
                filterSetup= true;
            }
            filterManager.onComplete(new CompletionHandler<RouteManager>() {
                @Override
                public void success(RouteManager result) {
                    mwBoard.getModule(MwrAccelerometer.class).startXYZSampling();
                    mwBoard.getModule(MwrAccelerometer.class).globalStart();
                }
            });
        } else {
            filterManager.onComplete(new CompletionHandler<RouteManager>() {
                @Override
                public void success(RouteManager result) {
                    mwBoard.getModule(MwrAccelerometer.class).globalStop();
                    mwBoard.getModule(MwrAccelerometer.class).stopXYZSampling();
                }
            });
        }
    }

    private boolean analogSetup= false;
    private AsyncResult<Timer.Controller> timerController;
    public void analogMe(View v) {
        final Switch mySwitch= (Switch) v;
        if (mySwitch.isChecked()) {
            if (!analogSetup) {
                mwBoard.routeData().fromAnalogGpio((byte) 0, Gpio.AnalogReadMode.ADC).subscribe(new DataSignal.MessageProcessor() {
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
                timerController= mwBoard.getModule(Timer.class).createTimer(500, (short) -1, false);
                timerController.onComplete(new CompletionHandler<Timer.Controller>() {
                    @Override
                    public void success(Timer.Controller result) {
                        result.monitor(new DataSignal.ActivityMonitor() {
                            @Override
                            public void onSignalActive() {
                                mwBoard.getModule(Gpio.class).readAnalogIn((byte) 0, Gpio.AnalogReadMode.ADC);
                            }
                        });
                    }
                });
                analogSetup= true;
            }
            timerController.onComplete(new CompletionHandler<Timer.Controller>() {
                @Override
                public void success(Timer.Controller result) {
                    result.start();
                }
            });
        } else {
            timerController.onComplete(new CompletionHandler<Timer.Controller>() {
                @Override
                public void success(Timer.Controller result) {
                    result.stop();
                }
            });
        }
    }

    private AsyncResult<RouteManager> programMngr;
    public void programMe(View v) {
        final Switch mwSwitch= (Switch) v;

        if (mwSwitch.isChecked()) {
            programMngr = mwBoard.routeData().fromSwitch().transform("accumulator").transform("math?operation=modulus&rhs=2")
                    .split()
                    .branch()
                    .filter("comparison?operation=eq&reference=1").monitor(new DataSignal.ActivityMonitor() {
                        @Override
                        public void onSignalActive() {
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
                    .filter("comparison?operation=eq&reference=0").monitor(new DataSignal.ActivityMonitor() {
                        @Override
                        public void onSignalActive() {
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
}
