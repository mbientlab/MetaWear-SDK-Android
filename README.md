# MetaWear Android API #
The MetaWear Android API is a library for interacting with [MbientLab's sensor boards](https://mbientlab.com/sensors/) on an Android device.  A minimum of Android 4.3 (SDK 18) is required to use this library, however for the best results, it is recommended that users be on **Android 4.4 (SDK 19) or higher**.  

# Setup  #
## Adding Compile Dependency ##
To add the library to your project, first, update the repositories closure to include the MbientLab Ivy Repo in the project's *build.gradle* file.

```gradle
repositories {
    ivy {
        url "http://ivyrep.mbientlab.com"
        layout "gradle"
    }
}
```

Then, add the compile element to the dependencies closure in the module's *build.gradle* file.

```gradle
dependencies {
    compile 'com.mbientlab:metawear:3.0.+'
}
```

## Declaring the Service ##
Once your project has synced with the updated Gradle files, declare the MetaWear Bluetooth LE service in the module's *AndroidManifest.xml* file.
```xml
<application
    android:allowBackup="true"
    android:icon="@drawable/ic_launcher"
    android:label="@string/app_name"
    android:theme="@style/AppTheme" >

    <service android:name="com.mbientlab.metawear.android.BtleService" />
    <!-- Other application info below i.e. activity definitions -->
</application>
```

## Binding the Service ##
Lastly, bind the service in your application and retrain a reference to the service's LocalBinder class.  This can be done in any activity or fragment that needs access to a MetaWearBoard object.

```java
import android.app.Activity;
import android.content.*;
import android.os.Bundle;
import android.os.IBinder;

import com.mbientlab.metawear.android.BtleService;

public class ExampleActivity extends Activity implements ServiceConnection {
    private BtleService.LocalBinder serviceBinder;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ///< Bind the service when the activity is created
        getApplicationContext().bindService(new Intent(this, BtleService.class),
                this, Context.BIND_AUTO_CREATE);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        ///< Unbind the service when the activity is destroyed
        getApplicationContext().unbindService(this);
    }

    @Override
    public void onServiceConnected(ComponentName name, IBinder service) {
        ///< Typecast the binder to the service's LocalBinder class
        serviceBinder = (BtleService.LocalBinder) service;
    }

    @Override
    public void onServiceDisconnected(ComponentName componentName) { }
}
```

# Upgrading From API v2 #
If you have already published an app using the v2 API, you should not switch to API v3 if you are saving serialized board states as the 
serialization format is not backwards compatible.  Other than that, read on below to see what changes you'll need to make to switch to API v3.  

## Asynchronous Tasks ##
Asynchronous tasks are now handled with the [Bolts Framework](https://github.com/BoltsFramework/Bolts-Android) rather than the library specific [AsyncOperation](https://mbientlab.com/docs/metawear/android/2/com/mbientlab/metawear/AsyncOperation.html) interface.  Developers should review the README on the Bolts GitHub page, specifically the sections on chaining tasks and error handling.  

```java
    board.readRssiAsync().continueWith(new Continuation<DeviceInformation, Void>() {
        @Override
        public Void then(Task<Integer> task) throws Exception {
            Log.i("MainActivity", "rssi = " + task.getResult());
            return null;
        }
    });
```

## New Service Class ##
The MetaWear service has been renamed to ``BtleService`` and now resides in the ``android`` package.  

```xml
    <service android:name="com.mbientlab.metawear.android.BtleService" />
```

## Data Route ##
Functions from the old [DataSignal](https://mbientlab.com/docs/metawear/android/2/com/mbientlab/metawear/DataSignal.html) have either been renamed, removed, or repurposed to more resemble Java 8's [Stream](https://docs.oracle.com/javase/8/docs/api/java/util/stream/Stream.html) interface and Apache Camel's [context component](http://camel.apache.org/context.html).  

### Data Processing ###
DataSigna's ``process`` component has been replaced with:

* accumulate  
* buffer  
* count  
* delay  
* filter  
* find  
* limit  
* map  

For example, to accumulate data, you instead will write ``.accumulate()`` vs. ``.process(new Accumulator())`` with API v2.

### Route Component Names ###
Data monitoring and route splitting are still supported in route API v3 albiet using different names.  

|Name v2    |Name v3  |
|-----------|---------|
|split      |multicast|
|branch     |to       |
|monitor    |react    |

Furthermore, the ``split`` component in api v3, along with the ``index`` component, now breaks down combined data into its individual values.  No more starting your route with ``fromZAxis`` if you wanted to use only z-axis data.

Data Producers
--------------
API v3 introduces the [DataProducer](https://mbientlab.com/docs/metawear/android/3/com/mbientlab/metawear/DataProducer.html) interface which represents all components that create data.  Rather than calling functions such as [enableAxisSampling](https://mbientlab.com/docs/metawear/android/2/com/mbientlab/metawear/module/Accelerometer.html#enableAxisSampling--) to receive data from that source, a base [start](https://mbientlab.com/docs/metawear/android/3/com/mbientlab/metawear/AsyncDataProducer.html#start--) method is used instead.