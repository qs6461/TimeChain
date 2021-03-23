package com.dizsun.timechain.util;

import java.net.UnknownHostException;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.apache.commons.net.ntp.TimeStamp;
import org.apache.log4j.Logger;

import com.dizsun.timechain.constant.Config;
import com.dizsun.timechain.constant.R;
import com.dizsun.timechain.interfaces.JNative;
import com.dizsun.timechain.service.NTPClient;

public class DateUtil {
	private Date date;
	private SimpleDateFormat sdf;
	private String time;
	private JNative jNative;
	private Logger logger = Logger.getLogger(DateUtil.class);

	private DateUtil() {
	}

	private static class Holder {
		private static DateUtil dateUtil = new DateUtil();
	}

	public static DateUtil getInstance() {
		return Holder.dateUtil;
	}

	public void init() {
		sdf = new SimpleDateFormat(R.NTP_DATE_FORMAT);
		date = new Date();
		time = sdf.format(date);
		jNative = NativeFactory.newNative();
	}

	public int getCurrentMinute() {
		sdf = new SimpleDateFormat("mm");
		date = new Date();
		return Integer.parseInt(sdf.format(date));
	}

	public int getCurrentSecond() {
		sdf = new SimpleDateFormat("ss");
		date = new Date();
		return Integer.parseInt(sdf.format(date));
	}

//    public int getCurrentTime() {
//
//    }

	public String getTime() {
		time = TimeStamp.getNtpTime(System.currentTimeMillis()).toDateString();
		return time;
	}

	public void setTime(String time) {
		this.time = time;
	}

	public String getTimeFromRC() {
		Config config = Config.getInstance();
		try {
			NTPClient ntpClient = new NTPClient(config.getNtpReqTimeout(), config.getTimeCenterIp());
			logger.info(config.getLocalHost() + "向" + config.getTimeCenterIp() + "获取时间...");
			date = ntpClient.getNTPTime();
			if (date == null) {
				logger.info(config.getLocalHost() + "向" + config.getTimeCenterIp() + "获取时间失败！");
				return null;
			}

			boolean flag = jNative.setLocalTime(date);

			sdf = new SimpleDateFormat(R.NTP_DATE_FORMAT);
			time = sdf.format(date);
			logger.info(config.getLocalHost() + "向" + config.getTimeCenterIp() + "获取时间为：" + time);
			if (!flag) {
				logger.info("设置系统时间失败");
			} else {
				logger.info("设置系统时间成功");
			}
		} catch (UnknownHostException e) {
			e.printStackTrace();
		}
		return time;
	}
}
