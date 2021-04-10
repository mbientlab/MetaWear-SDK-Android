Project Setup
=============
Before you begin coding your app, you will need to add the MetaWear API to your project.  If you are new to Android and not familiar with Gradle, 
background services, and Android Bluetooth functions, we have povided an 
`app template <https://github.com/mbientlab/MetaWear-Tutorial-Android/tree/master/starter>`_ that has taken care of all the steps outlined on this page.

Compile Dependency
------------------

.. highlight:: groovy 

To add the library as a compile dependency to your project, first update the repositories closure to include the MbientLab Ivy repository in the 
project's *build.gradle* file.  ::

    repositories {
        ivy {
            url "https://mbientlab.com/releases/ivyrep"
            layout "gradle"
        }
    }

Then, add a compile element to module's *build.gradle* file and sync the project.  ::

    dependencies {
        compile 'com.mbientlab:metawear:3.8.1'
    }

If you are using SDK v3.3 or newer, you will need to enable Java 8 feature support the module's ``build.gradle`` file.  See this 
[page](https://developer.android.com/studio/write/java8-support.html) in the Android Studio user guide.  

Service Setup
-------------

.. highlight:: xml

Once the IDE has synced with the updated Gradle files, declare the 
`BtleService <https://mbientlab.com/docs/metawear/android/latest/com/mbientlab/metawear/android/BtleService.html>`_ in the module's 
*AndroidManifest.xml* file.  ::

    <application
        android:allowBackup="true"
        android:icon="@drawable/ic_launcher"
        android:label="@string/app_name"
        android:theme="@style/AppTheme" >
            
        <service android:name="com.mbientlab.metawear.android.BtleService" />
        <!-- Other application info -->
    </application>

.. highlight:: java

Next, bind the service in your application and retain a reference to the 
`LocalBinder <https://mbientlab.com/docs/metawear/android/latest/com/mbientlab/metawear/android/BtleService.LocalBinder.html>`_ class.  This should be done 
in any activity or fragment that needs to access a 
`MetaWearBoard <https://mbientlab.com/docs/metawear/android/latest/com/mbientlab/metawear/MetaWearBoard.html>`_ object.

::

    import android.app.Activity;
    import android.content.*;
    import android.os.Bundle;
    import android.os.IBinder;
    
    import com.mbientlab.metawear.android.BtleService;
    
    public class MainActivity extends Activity implements ServiceConnection {
        private BtleService.LocalBinder serviceBinder;
    
        @Override
        protected void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            setContentView(R.layout.activity_main);
    
            // Bind the service when the activity is created
            getApplicationContext().bindService(new Intent(this, BtleService.class),
                    this, Context.BIND_AUTO_CREATE);
        }
    
        @Override
        public void onDestroy() {
            super.onDestroy();
    
            // Unbind the service when the activity is destroyed
            getApplicationContext().unbindService(this);
        }
    
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            // Typecast the binder to the service's LocalBinder class
            serviceBinder = (BtleService.LocalBinder) service;
        }
    
        @Override
        public void onServiceDisconnected(ComponentName componentName) { }
    }

Finding Your Device
-------------------
The last thing to do is retrieve the 
`BluetoothDevice <https://developer.android.com/reference/android/bluetooth/BluetoothDevice.html>`_ object corresponding to your MetaWear board.  If 
you know the board's MAC address, you can directly retrieve a BluetoothDevice object with 
`getRemoteDevice <http://developer.android.com/reference/android/bluetooth/BluetoothAdapter.html#getRemoteDevice%28java.lang.String%29>`_, otherwise, 
you will have to initiate a `Bluetooth LE scan <http://developer.android.com/guide/topics/connectivity/bluetooth-le.html#find>`_ to find the 
board.

When you have your ``BluetoothDevice`` object, call   
`getMetaWearBoard <https://mbientlab.com/docs/metawear/android/latest/com/mbientlab/metawear/android/BtleService.LocalBinder.html#getMetaWearBoard-android.bluetooth.BluetoothDevice->`_ 
to retrieve a MetaWearBoard object for the device.

::

    // Other required imports
    import android.bluetooth.BluetoothDevice;
    import android.bluetooth.BluetoothManager;
    
    public class MainActivity extends Activity implements ServiceConnection {
        private final String MW_MAC_ADDRESS= "EC:2C:09:81:22:AC";
        private MetaWearBoard board;
    
        public void retrieveBoard() {
            final BluetoothManager btManager= 
                    (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
            final BluetoothDevice remoteDevice= 
                    btManager.getAdapter().getRemoteDevice(MW_MAC_ADDRESS);
    
            // Create a MetaWear board object for the Bluetooth Device
            board= serviceBinder.getMetaWearBoard(remoteDevice);
        }
    }
