package com.dizsun.timechain.constant;

import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

import com.dizsun.timechain.interfaces.ISubscriber;
import com.dizsun.timechain.util.DateUtil;

/**
 * 广播时间变化事件,相当于计时器
 */
public class Broadcaster {
	private Timer timer;
	private ArrayList<ISubscriber> subscribers;
	private DateUtil dateUtil;

	public Broadcaster() {
		subscribers = new ArrayList<>();
		dateUtil = DateUtil.getInstance();
		dateUtil.init();
	}

//    private static class Holder {
//    	public static Broadcaster instance = new Broadcaster();
//    }
//    
//    public static Broadcaster getInstance() {
//    	return Holder.instance;
//    }

	public void broadcast() {
		timer = new Timer();
		timer.schedule(new TimerTask() {
			@Override
			public void run() {
				if (dateUtil.getCurrentSecond() == 0) {
					for (ISubscriber s : subscribers) {
						s.doPerRunning();
					}
				} else if (dateUtil.getCurrentSecond() == 15) {
					for (ISubscriber s : subscribers) {
						s.doPerTP();
					}
				} else if (dateUtil.getCurrentSecond() == 30) {
					for (ISubscriber s : subscribers) {
						s.doPerTC();
					}
				} else if (dateUtil.getCurrentSecond() == 45) {
					for (ISubscriber s : subscribers) {
						s.doPerTE();
					}
				}
			}
		}, 1, 1000);
	}

	public void subscribe(ISubscriber subscriber) {
		subscribers.add(subscriber);
	}

	public void unSubscribe(ISubscriber subscriber) {
		subscribers.remove(subscriber);
	}

//    public void destroy() {
//    	timer.cancel();
//    	timer = null;
//    }

}
