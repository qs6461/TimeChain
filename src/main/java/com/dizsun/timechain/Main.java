package com.dizsun.timechain;

import com.dizsun.timechain.constant.Broadcaster;
import com.dizsun.timechain.constant.Config;
import com.dizsun.timechain.service.*;
import org.apache.log4j.Logger;

import java.util.Timer;
import java.util.TimerTask;
//TODO 将websocket换成netty的tcp连接，并且不再持有长连接，而是每次使用时再建立
public class Main {
    private static String Drivder = "org.sqlite.JDBC";
    static Logger logger = Logger.getLogger(Main.class);

    public static void main(String[] args) {
        //初始化参数
        Config config = Config.getInstance();
        config.init();
        /**
         *  默认为从配置文件config.properties或者R.java中寻找参数,从命令行传入的参数则可以覆盖原有的数据
         */
        if (args == null || args.length == 0) {
            //使用配置文件或者默认值
        } else if (args.length == 4) {
            config.setIndex(Integer.parseInt(args[0]));
            config.setLocalHost(args[1]);
            config.setTimeCenterIp(args[2]);
            config.setMainNode(args[3]);
            logger.info("config inited");
        } else if (args.length == 6) {
            config.setHttpPort(Integer.parseInt(args[0]));
            config.setP2pPort(Integer.parseInt(args[1]));
            config.setIndex(Integer.parseInt(args[2]));
            config.setLocalHost(args[3]);
            config.setTimeCenterIp(args[4]);
            config.setMainNode(args[5]);
        } else {
            logger.error("传入参数错误，应传入参数为：\n" +
                    "  1.无参数\n" +
                    "  2.index, localHost, timeCenterIp, mainNode\n" +
                    "  3.httpPort, p2pPort, index, localHost, timeCenterIp, mainNode");
            System.exit(0);
        }
        // 打印输出参数
        logger.info("本节点IP：" + config.getLocalHost());
        logger.info("主节点IP：" + config.getMainNode());
        logger.info("授时中心IP：" + config.getTimeCenterIp());
        logger.info("本节点index：" + config.getIndex());
        logger.info("本节点HTTP端口：" + config.getHttpPort());
        logger.info("本节点P2P端口：" + config.getP2pPort());
        //初始化并启动各个组件
        try {
            CloseHook closeHook = new CloseHook();
            NTPServer ntpServer = new NTPServer();
            ntpServer.start();
            Broadcaster broadcaster = new Broadcaster();
            P2PService p2pService = P2PService.getInstance();
            broadcaster.subscribe(p2pService);
            p2pService.initP2PServer(config.getP2pPort());
            HTTPService httpService = new HTTPService();
            broadcaster.broadcast();
            new Timer().schedule(new TimerTask() {
                @Override
                public void run() {
                    if (config.getLocalHost().equals(config.getMainNode())) return;
                    p2pService.connect(config.getMainNode());
                }
            }, 5000);
            httpService.initHTTPServer(config.getHttpPort());
        } catch (Exception e) {
            logger.error("startup is error:" + e.getMessage());
        }
    }
}