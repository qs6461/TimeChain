package com.dizsun.timechain.service;

import org.apache.commons.net.ntp.NTPUDPClient;
import org.apache.commons.net.ntp.TimeInfo;
import org.apache.commons.net.ntp.TimeStamp;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Date;

public class NTPClient {
    private NTPUDPClient client;
    private InetAddress inetAddress;

    public NTPClient() throws UnknownHostException {
        this(10_000, "120.25.108.11");
    }

    public NTPClient(int timeout, String server_name) throws UnknownHostException {
        this.client = new NTPUDPClient();
        client.setDefaultTimeout(timeout);
        this.inetAddress = InetAddress.getByName(server_name);
    }

    public long getCurrentTime() {
        TimeStamp systemNtpTime = TimeStamp.getCurrentTime();
        System.out.println("System time:\t" + "\t" + systemNtpTime.toDateString());

        return systemNtpTime.getTime();
    }

    public Date getNTPTime() {
        try {
            TimeInfo timeInfo = this.client.getTime(this.inetAddress);
            timeInfo.computeDetails();

            if (timeInfo.getOffset() != null) {
                long offset = timeInfo.getOffset();

                long currentTime = System.currentTimeMillis();
                TimeStamp atomicNtpTime = TimeStamp.getNtpTime(currentTime + offset);
                return atomicNtpTime.getDate();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;
    }
}
