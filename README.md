# MetaWear Android API #

## Version ##
1.5.0

## About ##
This project provides an Android API for interacting with the MetaWear board.  For more information on MetaWear platform, check out the MbientLab product page: [https://mbientlab.com/product/](https://mbientlab.com/product/)

## Documentation ##
An Android API guide / documentation is available on the MbientLab web page:
[http://docs.mbientlab.com/?page_id=40](http://docs.mbientlab.com/?page_id=40).  The guide provides an overview of all the classes and instructions on how to add the Android library as a dependency.

## Build ##
The API was created with Android Developer Tools v22.6.2-1085508. It is targeted for Android 4.4.2 (API 19) SDK with Android 4.3 (API 18) as the minimum required SDK, and requires a JDK compiler compliance level of 1.7.  You will also need to have the Android Support Library package.  Links for either cloning the project or downloading via HTTP are available on the project's GitHub page: [https://github.com/mbientlab/Metawear-AndroidAPI](https://github.com/mbientlab/Metawear-AndroidAPI).

### Eclipse ###
1. Import the project into Eclipse  
2. Check that the JDK compiler compliance level is 1.7  
   * This can be view and/or changed in the project properties menu, under the "Java Compiler" section  
3. Select Project -> Build Project from the top menu  

The build will produce a jar file, **metawearapi.jar**, in the bin folder.

### Android Studio ###
1. Select "Import Non-Android Studio project" and navigate to the folder with the API code  
2. Rename the destination directory to "MetaWearAPI"  
   * The default name will be <Android Studio Workspace>/MetaWear-AndroidAPI<version>  
3. Set the source and target compatiblity to 1.7 from the "Module Settings" page  
   * The intial build will fail with complaints about the diamond operator  
4. Reload the project to have the updated gradle settings take effect  

The build will produce an aar archive, **app-debug.aar**, located in the **app/build/outputs/aar** folder