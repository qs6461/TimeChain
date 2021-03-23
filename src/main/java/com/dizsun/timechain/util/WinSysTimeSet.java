package com.dizsun.timechain.util;

import com.dizsun.timechain.interfaces.JNative;
import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.platform.win32.WinBase.SYSTEMTIME;
import org.apache.log4j.Logger;

import java.util.Calendar;
import java.util.Date;

public class WinSysTimeSet implements JNative {
    private Logger logger = Logger.getLogger(WinSysTimeSet.class);

    public interface Kernel32 extends Library {
        boolean SetLocalTime(SYSTEMTIME st);
    }

    public static Kernel32 kernel32Instance = null;

    public WinSysTimeSet() {
        kernel32Instance = (Kernel32) Native.load("kernel32", Kernel32.class);
    }

    @Override
    public boolean setLocalTime(Date date) {
        if (date == null) {
            return false;
        }
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);

        SYSTEMTIME st = new SYSTEMTIME();
        st.wYear = (short)calendar.get(Calendar.YEAR);
        st.wMonth = (short)(calendar.get(Calendar.MONTH) + 1);
        st.wDay = (short)calendar.get(Calendar.DAY_OF_MONTH);
        st.wHour = (short)calendar.get(Calendar.HOUR_OF_DAY);
        st.wMinute = (short)calendar.get(Calendar.MINUTE);
        st.wSecond = (short)calendar.get(Calendar.SECOND);
        st.wMilliseconds = (short)calendar.get(Calendar.MILLISECOND);

        kernel32Instance.SetLocalTime(st);

        return true;
    }
}
