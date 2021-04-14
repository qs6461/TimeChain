package com.dizsun.timechain.service;

import com.dizsun.timechain.constant.Config;

/**
 * 本类包装了应用进程关闭时的处理操作，包括区块链持久化（公私钥持久化后面有需要再添加） 在整合时应在应用启动main函数中进行该类对象的创建与初始化 eg.
 * CloseHook closeHook = new CloseHook();
 */
public class CloseHook {
	Thread thread = new Thread(new Runnable() {
		@Override
		public void run() {
			// 退出时执行区块链持久化操作
//			BlockService.getInstance().blockchainPersistenceAndCut();
			persistenceAppend();
		}
	});

	public static void persistenceAppend() {
		// 区块链持久化
		Config config = Config.getInstance();
		if (PersistenceService.getInstance().blockchainPersistenceAppend(
				config.getLocalHost() + "." + config.getP2pPort(), BlockService.getInstance().getBlockChain()))
			System.out.println("区块链持久化成功！");
		else
			System.out.println("区块链持久化失败！");
	}

	public CloseHook() {
		Runtime.getRuntime().addShutdownHook(thread);
	}
}
