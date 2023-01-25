# MetaWear Android API
The MetaWear Android API is a library for interacting with [MbientLab's Bluetooth sensor Development boards](https://mbientlab.com/) on an Android device.  A minimum of Android 11.0 (SDK 30) is required to use this library, however for the best results, it is recommended that users be on **Android 13 (SDK 33)**.  

# Setup
## Adding Compile Dependency
To add the library to your project, first, update the repositories closure to include the MbientLab Ivy Repo in the project's  
``build.gradle`` file.

```gradle
repositories {
    ivy {
        url "https://mbientlab.com/releases/ivyrep"
        layout "gradle"
    }
}
```

Then, add the compile element to the dependencies closure in the module's ``build.gradle`` file.

```gradle
dependencies {
    compile 'com.mbientlab:metawear:4.0.0'
}
```

The library was built on Java 17 but works fine with the built-in Java 11 in Android Studio Eel.

## Declaring the Service
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

## Binding the Service
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
