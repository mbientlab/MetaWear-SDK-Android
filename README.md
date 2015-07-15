# MetaWear Android API #

## Version ##
1.9.0

## About ##
This project provides an Android API for interacting with the MetaWear board.  For more information on MetaWear platform, check out the MbientLab product page: [https://mbientlab.com/product/](https://mbientlab.com/product/)

## Getting Started ##
With Gradle's dependency manager, it is very easy to add the MetaWear API to your project.  First, update the repositories closure to include the MbientLab Ivy Repo in the project's **build.gradle** file:

```gradle
repositories {
    ivy {
        url "http://ivyrep.mbientlab.com"
        layout "gradle"
    }
}
```

Then, add the compile element to the dependencies closure in the module's **build.gradle** file.

```gradle
dependencies {
    compile 'com.mbientlab:metawear:1.9.0'
}
```

## Documentation ##
An Android API guide and documentation is available on the MbientLab web page:
[http://docs.mbientlab.com/?page_id=40](http://docs.mbientlab.com/?page_id=40).  The guide provides an overview of how to use the API and the features it provides.

## Build ##
The API was built in Android Studio 1.1.0. It is targeted for Android 5.0.1 (SDK 21) with Android 4.3 (SDK 18) as the minimum required SDK, and requires a JDK compiler compliance level of 1.7.  You will also need to have the Android Support Library package.  Links for either cloning the project or downloading via HTTP are available on the project's GitHub page: [https://github.com/mbientlab/Metawear-AndroidAPI](https://github.com/mbientlab/Metawear-AndroidAPI).
