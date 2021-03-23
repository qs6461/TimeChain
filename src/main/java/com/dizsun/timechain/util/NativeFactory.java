package com.dizsun.timechain.util;

import com.dizsun.timechain.interfaces.JNative;
import com.sun.jna.Platform;

public class NativeFactory {
    public static JNative newNative() {
        if (Platform.isWindows()) {
            return new WinSysTimeSet();
        }
        return new LinuxSysTimeSet();
    }
}
