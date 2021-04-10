.. highlight:: java

IBeacon
=======
A cool feature of the MetaWear is that it can also function as an IBeacon; all you need to do is call the 
`enable <https://mbientlab.com/docs/metawear/android/latest/com/mbientlab/metawear/module/IBeacon.html#enable-->`_ method and disconnect from the board.  

::

    import com.mbientlab.metawear.module.IBeacon;

    final IBeacon iBeacon = board.getModule(IBeacon.class);
    iBeacon.enable();

The advertisement parameters can be modified with the module's 
`ConfigEditor <https://mbientlab.com/docs/metawear/android/latest/com/mbientlab/metawear/module/IBeacon.ConfigEditor.html>`_.

::

    // set major = 31415 and minor = 9265
    iBeacon.configure()
        .major((short) 31415)
        .minor((short) 9265)
        .commit();
