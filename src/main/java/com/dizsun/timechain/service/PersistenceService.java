package com.dizsun.timechain.service;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.dizsun.timechain.component.Block;
import com.dizsun.timechain.component.PubPriKey;
import com.dizsun.timechain.constant.R;

/**
 * 本类整合了区块链数据、公私密钥对数据的持久化与初始化读取操作
 */
public class PersistenceService {
	private Logger logger = Logger.getLogger(PersistenceService.class);
	private int latestBlockIndex = 0;

	private PersistenceService() {
	}

	private static class Holder {
		private static final PersistenceService persistenceService = new PersistenceService();
	}

	public static PersistenceService getInstance() {
		return Holder.persistenceService;
	}

	// 数据持久化操作
	/**
	 * 区块链数据持久化
	 * 
	 * @param uniqueName     自己节点的独一无二的名称（推荐用IP地址参数，要保证每次一样，才能每次读取同一数据文件，如果每个节点独立运行在docker中也可不传入该参数），
	 * @param tempBlockchain 当前节点要持久化的区块链
	 * @return 返回操作是否成功
	 */
	public boolean blockchainPersistence(String uniqueName, List<Block> tempBlockchain) {
		// 文件夹名称
		File peerDir = new File(uniqueName);
		// 文件相对路径
		String fileSrc = uniqueName + "/" + R.CHAIN_PATH;
		File dataFile = new File(fileSrc);
		try {
			// 判断文件夹是否存在，如果不存在先创建该文件夹
			if (!peerDir.exists())
				peerDir.mkdirs();
			// 判断文件是否存在，如果不存在先创建该文件
			if (!dataFile.exists())
				dataFile.createNewFile();
			// 写入数据
			// 这是直接往里重新写
			FileWriter resultFile = new FileWriter(dataFile);
			PrintWriter myFile = new PrintWriter(resultFile);
			myFile.println(JSON.toJSON(tempBlockchain));
			resultFile.close();
			logger.info(uniqueName + "节点区块链数据文件保存成功！");
			return true;
		} catch (Exception e) {
			logger.error(uniqueName + "节点保存区块链数据出错！");
			e.printStackTrace();
			return false;
		}
	}

	/**
	 * 区块链数据动态持久化（追加模式
	 * 
	 * @param uniqueName     自己节点的独一无二的名称（推荐用IP地址参数，要保证每次一样，才能每次读取同一数据文件，如果每个节点独立运行在docker中也可不传入该参数），
	 * @param tempBlockchain 当前节点要持久化的区块链
	 * @return 返回操作是否成功
	 */
	public boolean blockchainPersistenceAppend(String uniqueName, List<Block> tempBlockchain) {
		// 文件夹名称
		File peerDir = new File(uniqueName);
		// 文件相对路径
		String fileSrc = uniqueName + "/" + R.CHAIN_PATH;
		File dataFile = new File(fileSrc);
		try {
			// 判断文件夹是否存在，如果不存在先创建该文件夹
			if (!peerDir.exists())
				peerDir.mkdirs();
			// 判断文件是否存在，如果不存在先创建该文件
			if (!dataFile.exists())
				dataFile.createNewFile();
			// 写入数据
			// 这是直接往里重新写
			FileWriter resultFile = new FileWriter(dataFile, true);
			@SuppressWarnings("resource")
			PrintWriter myFile = new PrintWriter(resultFile);
			if (tempBlockchain.size() > latestBlockIndex) {
				for (; latestBlockIndex < tempBlockchain.size(); latestBlockIndex++) {
					myFile.println(JSON.toJSON(tempBlockchain.get(latestBlockIndex)));
				}
			}
			resultFile.close();
			logger.info(uniqueName + "节点区块链数据文件更新成功！");
			return true;
		} catch (Exception e) {
			logger.error(uniqueName + "节点保存区块链数据出错！");
			e.printStackTrace();
			return false;
		}
	}

//    public boolean blockchainPersistence(List<Block> tempBlockchain){
//        //直接使用区块链文件名称在当前目录下创建文件对象
//        File dataFile = new File(R.CHAIN_PATH);
//        try {
//            //判断文件是否存在，如果不存在先创建该文件
//            if(!dataFile.exists())
//                dataFile.createNewFile();
//            //写入数据
//            //这是直接往里重新写
//            FileWriter resultFile = new FileWriter(dataFile);
//            PrintWriter myFile = new PrintWriter(resultFile);
//            myFile.println(JSON.toJSON(tempBlockchain));
//            resultFile.close();
//            System.out.println("节点区块链数据文件保存成功！");
//            return true;
//        }catch(Exception e) {
//            System.out.println("节点保存区块链数据出错！");
//            e.printStackTrace();
//            return false;
//        }
//    }

	/**
	 * 公私钥对的数据持久化
	 * 
	 * @param uniqueName 自己节点的独一无二的名称（推荐用IP地址参数，要保证每次一样，才能每次读取同一数据文件，如果每个节点独立运行在docker中也可不传入该参数），
	 * @param pubPriKey  当前节点要持久化的公私钥对
	 * @return 返回操作是否成功
	 */
	public boolean pubPriKeysPersistence(String uniqueName, PubPriKey pubPriKey) {
		// 文件夹名称
		File peerDir = new File(uniqueName);
		// 文件相对路径
		String fileSrc = uniqueName + "/" + R.KEY_PATH;
		File dataFile = new File(fileSrc);
		try {
			// 判断文件夹是否存在，如果不存在先创建该文件夹
			if (!peerDir.exists())
				peerDir.mkdirs();
			// 判断文件是否存在，如果不存在先创建该文件
			if (!dataFile.exists())
				dataFile.createNewFile();
			// 写入数据
			// 这是直接往里重新写
			FileWriter resultFile = new FileWriter(dataFile);
			PrintWriter myFile = new PrintWriter(resultFile);
			myFile.println(JSON.toJSON(pubPriKey));
			resultFile.close();
			logger.info(uniqueName + "节点密钥数据文件保存成功！");
			return true;
		} catch (Exception e) {
			logger.error(uniqueName + "节点保存密钥数据出错！");
			e.printStackTrace();
			return false;
		}
	}

//    public boolean pubPriKeysPersistence(PubPriKey pubPriKeys){
//        //直接使用区块链文件名称在当前目录下创建文件对象
//        File dataFile = new File(R.KEY_PATH);
//        try {
//            //判断文件是否存在，如果不存在先创建该文件
//            if(!dataFile.exists())
//                dataFile.createNewFile();
//            //写入数据
//            //这是直接往里重新写
//            FileWriter resultFile = new FileWriter(dataFile);
//            PrintWriter myFile = new PrintWriter(resultFile);
//            myFile.println(JSON.toJSON(pubPriKeys));
//            resultFile.close();
//            System.out.println("节点密钥数据文件保存成功！");
//            return true;
//        }catch(Exception e) {
//            System.out.println("节点保存密钥数据出错！");
//            e.printStackTrace();
//            return false;
//        }
//    }

	/**
	 * 区块链数据初始化读取
	 * 
	 * @param uniqueName 自己节点的独一无二的名称（推荐用IP地址参数，要保证每次一样，才能每次读取同一数据文件，如果每个节点独立运行在docker中也可不传入该参数），
	 * @return 返回读取到的区块链List<Block>，若没有该文件或文件为空，或读取失败，返回空的List集合；
	 */
	public List<Block> blockchainUpload(String uniqueName) {
		File peerDir = new File(uniqueName);
		String fileSrc = uniqueName + "/" + R.CHAIN_PATH;
		File dataFile = new File(fileSrc);
		BufferedReader reader;
		try {
			if (peerDir.exists()) {
				if (dataFile.exists()) {
					// 读取数据
					reader = new BufferedReader(new FileReader(dataFile));
					String line = reader.readLine();
					if (line != null && !line.equals("")) {
						List<Block> tempBlockchain = new ArrayList<>();
						tempBlockchain.addAll(JSONArray.parseArray(line, Block.class));
						logger.info("从文件中读取区块链：" + JSON.toJSONString(tempBlockchain));
						return tempBlockchain;
					} else {
						logger.info("文件中没有存有区块链！");
					}
					reader.close();
				} else {
					// 创建文件
					dataFile.createNewFile(); // 如果已经存在文件其实也不会重新覆盖，等于没操作。
					logger.info("无此文件！已在节点目录下创建！");
				}
			} else {
				peerDir.mkdirs();
				dataFile.createNewFile();
				logger.info("无此路径！已创建目录及文件！");
			}
		} catch (Exception e) {
			logger.error("加载区块链数据出错！");
			e.printStackTrace();
		}
		return null;
	}

//    public List<Block> blockchainUpload(){
//        File dataFile = new File(R.CHAIN_PATH);
//        List<Block> tempBlockchain = new ArrayList<>();
//        BufferedReader reader;
//        try {
//            if(dataFile.exists()) {
//                //读取数据
//                reader = new BufferedReader(new FileReader(dataFile));
//                String line=reader.readLine();
//                if(line!=null && !line.equals("")) {
//                    tempBlockchain.addAll(JSONArray.parseArray(line, Block.class));
//                    System.out.println("从文件中读取区块链："+JSON.toJSONString(tempBlockchain));
//                }else {
//                    System.out.println("PersistenceServiceInfo: 文件中没有存有区块链。");
//                }
//                reader.close();
//            }else {
//                //创建文件
//                dataFile.createNewFile();//如果已经存在文件其实也不会重新覆盖，等于没操作。
//                System.out.println("无此文件！已在节点目录下创建！");
//            }
//
//        }catch(Exception e) {
//            System.out.println("加载数据出错！");
//            e.printStackTrace();
//        }
//        return tempBlockchain;
//    }

	/**
	 * 公私钥对的数据读取
	 * 
	 * @param uniqueName 自己节点的独一无二的名称（推荐用IP地址参数，要保证每次一样，才能每次读取同一数据文件，如果每个节点独立运行在docker中也可不传入该参数），
	 * @return 返回读取到的公私钥对List<PubPriKey>，若没有该文件或文件为空，或读取失败，返回空的List集合；
	 */
	public PubPriKey pubPriKeysUpload(String uniqueName) {
		File peerDir = new File(uniqueName);
		String fileSrc = uniqueName + "/" + R.KEY_PATH;
		File dataFile = new File(fileSrc);
		BufferedReader reader;
		try {
			if (peerDir.exists()) {
				if (dataFile.exists()) {
					// 读取数据
					reader = new BufferedReader(new FileReader(dataFile));
					String line = reader.readLine();
					if (line != null && !line.equals("")) {
						PubPriKey pubPriKey = JSON.parseObject(line, PubPriKey.class);
						logger.info("从文件中读取公私钥成功！");
						return pubPriKey;
					} else {
						logger.info("文件中没有存有公私钥！");
					}
					reader.close();
				} else {
					// 创建文件
					dataFile.createNewFile();// 如果已经存在文件其实也不会重新覆盖，等于没操作。
					logger.info("无此文件！已在节点目录下创建！");
				}
			} else {
				peerDir.mkdirs();
				dataFile.createNewFile();
				logger.info("无此路径！已创建目录及文件！");
			}
		} catch (Exception e) {
			logger.error("加载密钥数据出错！");
			e.printStackTrace();
		}
		return null;
	}

//    public List<PubPriKey> pubPriKeysUpload(){
//        File dataFile = new File(R.KEY_PATH);
//        List<PubPriKey> pubPriKeys = new ArrayList<>();
//        BufferedReader reader;
//        try {
//            if(dataFile.exists()) {
//                //读取数据
//                reader = new BufferedReader(new FileReader(dataFile));
//                String line=reader.readLine();
//                if(line!=null && !line.equals("")) {
//                    pubPriKeys.addAll(JSONArray.parseArray(line,PubPriKey.class));
//                    System.out.println("从文件中读取区块链："+JSON.toJSONString(pubPriKeys));
//                }else {
//                    System.out.println("BlockServiceInfo:文件中没有存有区块链。");
//                }
//                reader.close();
//            }else {
//                //创建文件
//                dataFile.createNewFile();//如果已经存在文件其实也不会重新覆盖，等于没操作。
//                System.out.println("无此文件！已在节点目录下创建！");
//            }
//
//        }catch(Exception e) {
//            System.out.println("加载数据出错！");
//            e.printStackTrace();
//        }
//        return pubPriKeys;
//    }
}