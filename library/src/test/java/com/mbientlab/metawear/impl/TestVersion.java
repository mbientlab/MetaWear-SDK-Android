package com.mbientlab.metawear.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

public class TestVersion {
    @Test
    public void removePreRelease() {
        Version version = new Version("3.8.0-b00");
        assertEquals("b00", version.preRelease);
    }
}
