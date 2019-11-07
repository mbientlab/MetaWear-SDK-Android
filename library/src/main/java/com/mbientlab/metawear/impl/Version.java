/*
 * Copyright 2014-2015 MbientLab Inc. All rights reserved.
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
 * hello@mbientlab.com.
 */

package com.mbientlab.metawear.impl;

import java.io.Serializable;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by etsai on 9/5/16.
 */
public class Version implements Comparable<Version>, Serializable {
    private static final Pattern VERSION_STRING_PATTERN = Pattern.compile("(\\d+)\\.(\\d+)\\.(\\d+)(-([\\p{Alnum}]+))?");
    private static final long serialVersionUID = -6928626294821091652L;

    public final int major, minor, step;
    public final String preRelease;

    Version(int major, int minor, int step) {
        this.major= major;
        this.minor= minor;
        this.step= step;
        this.preRelease = null;
    }

    public Version(String versionString) {
        Matcher m= VERSION_STRING_PATTERN.matcher(versionString);

        if (!m.matches()) {
            throw new RuntimeException("Version string: \'" + versionString + "\' does not match pattern X.Y.Z");
        }

        System.out.printf("m: %s", m.toString());
        major= Integer.valueOf(m.group(1));
        minor= Integer.valueOf(m.group(2));
        step= Integer.valueOf(m.group(3));
        preRelease = m.groupCount()>= 4 ? m.group(5) : null;
    }

    private int weightedCompare(int left, int right) {
        if (left < right) {
            return -1;
        } else if (left > right) {
            return 1;
        }
        return 0;
    }

    @Override
    public int compareTo(Version version) {
        int sum= 4 * weightedCompare(major, version.major) + 2 * weightedCompare(minor, version.minor) + weightedCompare(step, version.step);

        if (sum < 0) {
            return -1;
        } else if (sum > 0) {
            return 1;
        }
        return 0;
    }

    @Override
    public String toString() {
        return preRelease == null ?
                String.format(Locale.US, "%d.%d.%d", major, minor, step) :
                String.format(Locale.US, "%d.%d.%d-%s", major, minor, step, preRelease);
    }
}
