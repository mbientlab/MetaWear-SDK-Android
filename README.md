# MetaWear API v2.0.0 Beta #

The MetaWear Android API is undergoing a major overhaul to improve the ease of use of the API.  Since this library is currently in a beta state, there may be many breaking changes in between releases leading up to the official v2.0.0 release.

As of beta 01, the major changes from API v1.x are:

1. Background service no longer communicates to apps via a BroadcastReceiver.  All asynchronous responses are executed in the background.  
   * UI tasks in callback functions need to be explicitly run on the UI thread  
2. Callbacks for receiving asynchronous responses (i.e. reading RSSI values) are replaced with the AsyncResult class.  
3. Created a Java DSL to express how sensor data should be manipulated and routed.  
4. Package renamed to com.mbientlab.metawear  

# Getting Started #
Integrating the v2.0.0 beta library into your app is essentially the same as for the v1.x libraries.  First, add MbientLab's Ivy repository in the repositories closure.

```gradle
///< in the project's build.gradle file
repositories {
    ivy {
        url "http://ivyrep.mbientlab.com"
        layout "gradle"
    }
}
```

Then, add the MetaWear v2.0.0 beta library as a compile dependency.  The 2.0.0 beta releases will be tagged with the pattern: 2.0.0-beta.[0-9][0-9].  

```gradle
///< in the module's build.gradle file
compile 'com.mbientlab:metawear:2.0.0-beta.01'
```

Once your project has synced with the updated Gradle files, declare the MetaWear service in the AndroidManifest.xml file.

```xml
<application
    android:allowBackup="true"
    android:icon="@drawable/ic_launcher"
    android:label="@string/app_name"
    android:theme="@style/AppTheme" >
        
    <service android:name="com.mbientlab.metawear.impl.MetaWearBleService" />
    <!-- Other application info below i.e. activity definitions -->
</application>
```

Finally, bind the service in your application and retrain a reference to the service' LocaBinder class.  This can be done in any activity or fragment that needs access to a MetaWearBoard object.

```java
import android.app.Activity;
import android.content.*;
import android.os.Bundle;
import android.os.IBinder;

import com.mbientlab.metawear.impl.MetaWearBleService;

public class ExampleActivity extends Activity implements ServiceConnection {
    private MetaWearBleService.LocalBinder serviceBinder;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ///< Bind the service when the activity is created
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
    public void onServiceConnected(ComponentName name, IBinder service) {
        ///< Typecast the binder to the service's LocalBinder class
        serviceBinder = (MetaWearBleService.LocalBinder) service;
    }

    @Override
    public void onServiceDisconnected(ComponentName componentName) { }
}
```

Once the service is bound, you can retrieve a MetaWearBoard object from the service's LocalBinder to start interacting with your board.  More information and tutorials are available on [project's wiki](https://github.com/mbientlab/Metawear-AndroidAPI/wiki/MetaWearBoard-Class).