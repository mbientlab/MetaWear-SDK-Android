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

import com.mbientlab.metawear.impl.platform.IO;

import java.io.File;
import java.util.Arrays;
import java.util.Locale;

import bolts.Task;

public class Image {
    private static final String RELEASES_URL = "https://mbientlab.com/releases";

    public final String hardwareRev,
            modelNumber,
            build,
            version,
            filename,
            requiredBootloader,
            minSdkVersion;

    Image(
            String hardwareRev,
            String modelNumber,
            String build,
            String version,
            String filename,
            String requiredBootloader,
            String minSdkVersion
    ) {
        this.hardwareRev = hardwareRev;
        this.modelNumber = modelNumber;
        this.build = build;
        this.version = version;
        this.filename = filename;
        this.requiredBootloader = requiredBootloader;
        this.minSdkVersion = minSdkVersion;
    }

    private String generateLocalFilename() {
        return String.format(Locale.US, "%s_%s_%s_%s_%s",
                hardwareRev,
                modelNumber,
                build, version, filename
        );
    }

    private Task<File> downloadAsync(String dest, IO io) {
        String dlUrl = String.format(Locale.US, "%s/metawear/%s/%s/%s/%s/%s",
                RELEASES_URL,
                hardwareRev,
                modelNumber,
                build, version, filename
        );

        return io.downloadFileAsync(dlUrl, dest);
    }

    public Task<File> downloadAsync(IO io) {
        final String destName = generateLocalFilename();
        final File dest = io.findDownloadedFile(destName);

        return !dest.exists() ? downloadAsync(destName, io) : Task.forResult(dest);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Image image = (Image) o;
        return hardwareRev.equals(image.hardwareRev) &&
                modelNumber.equals(image.modelNumber) &&
                build.equals(image.build) &&
                version.equals(image.version) &&
                filename.equals(image.filename) &&
                requiredBootloader.equals(image.requiredBootloader) &&
                minSdkVersion.equals(image.minSdkVersion);
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(new String[] {hardwareRev, modelNumber, build, version, filename, requiredBootloader, minSdkVersion});
    }

    @Override
    public String toString() {
        return "Image{" +
                "hardwareRev='" + hardwareRev + '\'' +
                ", modelNumber='" + modelNumber + '\'' +
                ", build='" + build + '\'' +
                ", version='" + version + '\'' +
                ", filename='" + filename + '\'' +
                ", requiredBootloader='" + requiredBootloader + '\'' +
                ", minSdkVersion='" + minSdkVersion + '\'' +
                '}';
    }
}
