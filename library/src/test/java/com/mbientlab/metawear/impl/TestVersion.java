package com.mbientlab.metawear.impl;

import org.junit.Test;

import static junit.framework.TestCase.assertEquals;

public class TestVersion {
    @Test
    public void removePreRelease() {
        Version version = new Version("3.8.0-b00");
        assertEquals("b00", version.preRelease);
    }
}
