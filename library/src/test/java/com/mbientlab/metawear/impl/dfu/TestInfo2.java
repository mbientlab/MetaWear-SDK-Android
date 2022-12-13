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

import static com.mbientlab.metawear.JunitPlatform.RES_PATH;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.mbientlab.metawear.IllegalFirmwareFile;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

public class TestInfo2 {
    private static JSONObject root;

    @BeforeAll
    public static void classSetup() throws IOException, JSONException {
        InputStream is = new FileInputStream(new File(RES_PATH, "info2.json"));
        int size = is.available();
        byte[] buffer = new byte[size];
        is.read(buffer);
        is.close();
        root = new JSONObject(new String(buffer, StandardCharsets.UTF_8));
    }

    private Info2 info;

    @BeforeEach
    public void setup() {
        info = new Info2(root, "3.7.1");
    }

    @Test
    public void bootloaderMatches() throws JSONException {
        assertTrue(info.findBootloaderImages("0.4", "5", "0.3.3", "0.3.3").isEmpty());
    }

    @Test
    public void bootloaderChain() throws JSONException {
        final String hardware = "0.2";
        final String model = "6";
        final String build = "bootloader";

        List<Image> expected = Arrays.asList(
                new Image(hardware, model, build, "0.2.2", "bl.zip", "0.2.1", "3.5.0"),
                new Image(hardware, model, build, "0.3.1", "sd_bl.zip", "0.2.2", "3.5.0"),
                new Image(hardware, model, build, "0.3.2", "bl.zip", "0.3.1", "3.5.0"),
                new Image(hardware, model, build, "0.3.3", "bl.zip", "0.3.2", "3.5.0")
        );
        assertEquals(expected, info.findBootloaderImages(hardware, model, "0.2.1", "0.3.3"));
    }

    @Test
    public void findSpecificFirmware() throws JSONException, IllegalFirmwareFile {
        final String hardware = "0.4";
        final String model = "5";
        final String build = "vanilla";

        Image expected = new Image(hardware, model, build, "1.4.5", "firmware.zip", "0.3.3", "3.6.0");
        assertEquals(expected, info.findFirmwareImage(hardware, model, build, "1.4.5"));
    }

    @Test
    public void findSpecificFirmwareWithOldApi() {
        final String hardware = "0.2";
        final String model = "6";
        final String build = "vanilla";

        assertThrows(IllegalFirmwareFile.class, () -> info.findFirmwareImage(hardware, model, build, "2.7.11"));
    }

    @Test
    public void findNonExistentFirmware() {
        final String hardware = "0.4";
        final String model = "5";
        final String build = "vanilla";

        assertThrows(IllegalFirmwareFile.class, () -> info.findFirmwareImage(hardware, model, build, "3.1.41"));
    }

    @Test
    public void findLatestFirmware() throws JSONException, IllegalFirmwareFile {
        final String hardware = "0.1";
        final String model = "5";
        final String build = "vanilla";

        Image expected = new Image(hardware, model, build, "1.5.0", "firmware.zip", "0.3.1", "3.5.0");
        assertEquals(expected, info.findFirmwareImage(hardware, model, build));
    }

    @Test
    public void findLatestFirmwareWithOldApi() {
        final String hardware = "0.2";
        final String model = "6";
        final String build = "vanilla";

        assertThrows(UnsupportedOperationException.class, () -> info.findFirmwareImage(hardware, model, build));
    }
}
