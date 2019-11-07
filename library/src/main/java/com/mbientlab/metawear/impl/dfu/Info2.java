/*
 * Copyright 2014-2019 MbientLab Inc. All rights reserved.
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
 *   hello@mbientlab.com.
 */

package com.mbientlab.metawear.impl.dfu;

import com.mbientlab.metawear.IllegalFirmwareFile;
import com.mbientlab.metawear.impl.Version;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.TreeSet;

public class Info2 {
    private interface Transform {
        Image apply(JSONObject value) throws JSONException, IllegalFirmwareFile;
    }

    private final JSONObject root;
    private final Version apiVersion;

    public Info2(JSONObject root, String apiVersionStr) {
        this.root = root;
        this.apiVersion = new Version(apiVersionStr);
    }

    private Image verifyImage(String hardware, String model, String build, String version, JSONObject attrs) throws JSONException {
        Image image = new Image(
                hardware,
                model,
                build,
                version,
                attrs.getString("filename"),
                attrs.getString("required-bootloader"),
                attrs.getString("min-android-version")
        );
        if (new Version(image.minSdkVersion).compareTo(apiVersion) > 0) {
            throw new UnsupportedOperationException(String.format(
                    Locale.US, "You must use Android SDK >= v'%s' with firmware v'%s'",
                    image.minSdkVersion, apiVersion.toString()
            ));
        }

        return image;
    }

    private Image findFirmwareImage(String hardware, String model, String build, Transform transform) throws JSONException, IllegalFirmwareFile {
        JSONObject models= root.getJSONObject(hardware);
        JSONObject builds = models.getJSONObject(model);
        JSONObject versions = builds.getJSONObject(build);

        return transform.apply(versions);
    }
    public Image findFirmwareImage(String hardware, String model, String build) throws JSONException, IllegalFirmwareFile {
        return findFirmwareImage(hardware, model, build, versions -> {
            Iterator<String> keys = versions.keys();
            TreeSet<Version> keyObjects = new TreeSet<>();
            while (keys.hasNext()) {
                keyObjects.add(new Version(keys.next()));
            }

            Iterator<Version> it = keyObjects.descendingIterator();
            if (it.hasNext()) {
                Version target = it.next();
                return verifyImage(
                    hardware,
                    model,
                    build,
                    target.toString(),
                    versions.getJSONObject(target.toString())
                );
            } else {
                throw new IllegalStateException("No information available for this board");
            }
        });
    }

    public Image findFirmwareImage(String hardware, String model, String build, String version) throws JSONException, IllegalFirmwareFile {
        return findFirmwareImage(hardware, model, build, versions -> {
            if (versions.has(version)) {
                return verifyImage(
                    hardware,
                    model,
                    build,
                    version,
                    versions.getJSONObject(version)
                );
            }
            throw new IllegalFirmwareFile(String.format(Locale.US, "Firmware v'%s' does not exist for this board", version));
        });
    }

    public List<Image> findBootloaderImages(String hardware, String model, String key, String current) throws JSONException {
        JSONObject models= root.getJSONObject(hardware);
        JSONObject builds = models.getJSONObject(model);
        JSONObject versions = builds.getJSONObject("bootloader");
        ArrayList<Image> acc = new ArrayList<>();

        Version keyVersion = new Version(key);

        while(new Version(current).compareTo(keyVersion) > 0) {
            acc.add(0, verifyImage(
                    hardware,
                    model,
                    "bootloader",
                    current,
                    versions.getJSONObject(current)
            ));
            current = acc.get(0).requiredBootloader;
        }
        return acc;
    }
}
