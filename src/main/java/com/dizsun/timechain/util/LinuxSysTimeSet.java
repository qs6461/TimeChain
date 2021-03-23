package com.dizsun.timechain.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.apache.log4j.Logger;

import com.dizsun.timechain.interfaces.JNative;

public class LinuxSysTimeSet implements JNative {
	@Override
	public boolean setLocalTime(Date date) {
		if (date == null) {
			return false;
		}
		DateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
		String dateNow = formatter.format(date);
		String[] command = new String[] { "date", "-s", dateNow };
		try {
			Process process = Runtime.getRuntime().exec(command);
			StreamGobbler errorGobbler = new StreamGobbler(process.getErrorStream(), "ERROR");
			errorGobbler.start();
			StreamGobbler outGobbler = new StreamGobbler(process.getInputStream(), "STDOUT");
			outGobbler.start();
			process.waitFor();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return true;
	}
}

class StreamGobbler extends Thread {
	InputStream is;
	String type;
	OutputStream os;
	Logger logger = Logger.getLogger(LinuxSysTimeSet.class);

	StreamGobbler(InputStream is, String type) {
		this(is, type, null);
	}

	StreamGobbler(InputStream is, String type, OutputStream os) {
		this.is = is;
		this.type = type;
		this.os = os;
	}

	@Override
	public void run() {
		InputStreamReader isr = null;
		BufferedReader br = null;
		PrintWriter pw = null;
		try {
			if (os != null) {
				pw = new PrintWriter(os);
			}
			isr = new InputStreamReader(is);
			br = new BufferedReader(isr);
			String line = null;
			while ((line = br.readLine()) != null) {
				if (pw != null) {
					pw.println(line);
				}
				if (type.equals("ERROR")) {
					logger.error(type + ">" + line);
				} else {
					logger.info(type + ">" + line);
				}
			}
			if (pw != null) {
				pw.flush();
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
