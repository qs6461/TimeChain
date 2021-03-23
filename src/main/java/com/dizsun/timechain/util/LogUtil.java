package com.dizsun.timechain.util;

import com.dizsun.timechain.constant.R;

import java.io.*;
import java.nio.charset.StandardCharsets;

public class LogUtil {
    // TODO 修改为单例模式并将常量外置
    public static String CONSENSUS = "consensus";
    public static String NTP = "ntp";

    public static void init(int index) {
        LogUtil.CONSENSUS = R.LOG_FILE_PATH + "consensus_" + index + ".txt";
        LogUtil.NTP = R.LOG_FILE_PATH + "ntp_" + index + ".txt";
    }

    public static void writeLog(String msg, String fileName) {
        File file = new File(fileName);
        if (!file.exists()) {
            try {
                file.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        try {
            FileOutputStream out = new FileOutputStream(file, true);
            out.write((msg + "\n").getBytes(StandardCharsets.UTF_8));
            out.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static String readLastLog(String fileName) {
        File file = new File(fileName);
        if (!file.exists()) {
            System.out.println(fileName + "还未生成！");
            return null;
        }
        RandomAccessFile raf = null;
        try {
            raf = new RandomAccessFile(file, "r");
            try {
                if (raf.length() == 0L) {
                    System.out.println("还未进行有效共识！");
                    return null;
                } else {
                    long pos = raf.length() - 1;
                    while (pos > 0) {
                        pos--;
                        raf.seek(pos);
                        if (raf.readByte() == '\n') {
                            break;
                        }
                    }
                    if (pos == 0) {
                        raf.seek(0);
                    }
                    byte[] bytes = new byte[(int) (raf.length() - pos)];
                    raf.read(bytes);
                    return new String(bytes);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        } catch (FileNotFoundException e1) {
            e1.printStackTrace();
        } finally {
            if (raf != null) {
                try {
                    raf.close();
                } catch (IOException e2) {
                    e2.printStackTrace();
                }
            }
        }
        return null;
    }
}
