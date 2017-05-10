package cn.zhouyafeng.itchat4j.api;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.logging.Logger;
import java.util.regex.Matcher;

import javax.activation.MimetypesFileTypeMap;

import org.apache.http.HttpEntity;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.util.EntityUtils;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;

import cn.zhouyafeng.itchat4j.tools.CommonTool;
import cn.zhouyafeng.itchat4j.utils.Constant;
import cn.zhouyafeng.itchat4j.utils.Core;
import cn.zhouyafeng.itchat4j.utils.MsgType;
import cn.zhouyafeng.itchat4j.utils.MyHttpClient;

/**
 * 消息处理类
 * 
 * @author https://github.com/yaphone
 * @date 创建时间：2017年4月23日 下午2:30:37
 * @version 1.0
 *
 */
public class MessageTools {
	private static Logger logger = Logger.getLogger("Message");
	private static Core core = Core.getInstance();
	private static MyHttpClient myHttpClient = core.getMyHttpClient();

	/**
	 * 接收消息，放入队列
	 * 
	 * @author https://github.com/yaphone
	 * @date 2017年4月23日 下午2:30:48
	 * @param msgList
	 * @return
	 */
	public static JSONArray produceMsg(JSONArray msgList) {
		JSONArray result = new JSONArray();
		for (int i = 0; i < msgList.size(); i++) {
			JSONObject msg = new JSONObject();
			JSONObject m = msgList.getJSONObject(i);
			m.put("groupMsg", false);// 是否是群消息
			if (m.getString("FromUserName").contains("@@") || m.getString("ToUserName").contains("@@")) { // 群聊消息
				// produceGroupChat(core, m);
				// m.remove("Content");
				if (m.getString("FromUserName").contains("@@")
						&& !core.getGroupIdList().contains(m.getString("FromUserName"))) {
					core.getGroupIdList().add((m.getString("FromUserName")));
				} else if (m.getString("ToUserName").contains("@@")
						&& !core.getGroupIdList().contains(m.getString("ToUserName"))) {
					core.getGroupIdList().add((m.getString("ToUserName")));
				}
				// 群消息与普通消息不同的是在其消息体（Content）中会包含发送者id及":<br/>"消息，这里需要处理一下，去掉多余信息，只保留消息内容
				if (m.getString("Content").contains("<br/>")) {
					String content = m.getString("Content").substring(m.getString("Content").indexOf("<br/>") + 5);
					m.put("Content", content);
					m.put("groupMsg", true);
				}
			} else {
				CommonTool.msgFormatter(m, "Content");
			}
			if (m.getInteger("MsgType") == MsgType.MSGTYPE_TEXT) { // words 文本消息
				if (m.getString("Url").length() != 0) {
					String regEx = "(.+?\\(.+?\\))";
					Matcher matcher = CommonTool.getMatcher(regEx, m.getString("Content"));
					String data = "Map";
					if (matcher.find()) {
						data = matcher.group(1);
					}
					msg.put("Type", "Map");
					msg.put("Text", data);
				} else {
					msg.put("Type", MsgType.TEXT);
					msg.put("Text", m.getString("Content"));
				}
				m.put("Type", msg.getString("Type"));
				m.put("Text", msg.getString("Text"));
			} else if (m.getInteger("MsgType") == MsgType.MSGTYPE_IMAGE
					|| m.getInteger("MsgType") == MsgType.MSGTYPE_EMOTICON) { // 图片消息
				m.put("Type", MsgType.PIC);
			} else if (m.getInteger("MsgType") == MsgType.MSGTYPE_VOICE) { // 语音消息
				m.put("Type", MsgType.VOICE);
			} else if (m.getInteger("MsgType") == 37) {// friends 好友确认消息

			} else if (m.getInteger("MsgType") == 42) { // 共享名片
				m.put("Type", MsgType.NAMECARD);

			} else if (m.getInteger("MsgType") == MsgType.MSGTYPE_VIDEO
					|| m.getInteger("MsgType") == MsgType.MSGTYPE_MICROVIDEO) {// viedo
				m.put("Type", MsgType.VIEDO);
			} else if (m.getInteger("MsgType") == 49) { // sharing 分享链接

			} else if (m.getInteger("MsgType") == 51) {// phone init 微信初始化消息

			} else if (m.getInteger("MsgType") == 10000) {// 系统消息

			} else if (m.getInteger("MsgType") == 10002) { // 撤回消息

			} else {
				logger.info("Useless msg");
			}
			result.add(m);
		}
		return result;
	}

	public static void send(String msg, String toUserName, String mediaId) {
		sendMsg(msg, toUserName);
	}

	/**
	 * 根据UserName发送文本消息
	 * 
	 * @author https://github.com/yaphone
	 * @date 2017年5月4日 下午11:17:38
	 * @param msg
	 * @param toUserName
	 */
	public static void sendMsg(String text, String toUserName) {
		logger.info(String.format("Request to send a text message to %s: %s", toUserName, text));
		sendRawMsg(1, text, toUserName);
	}

	/**
	 * 根据ID发送文本消息
	 * 
	 * @author https://github.com/yaphone
	 * @date 2017年5月6日 上午11:45:51
	 * @param text
	 * @param id
	 */
	public static void sendMsgById(String text, String id) {
		sendMsg(text, id);
	}

	/**
	 * 根据NickName发送文本消息
	 * 
	 * @author https://github.com/yaphone
	 * @date 2017年5月4日 下午11:17:38
	 * @param msg
	 * @param toUserName
	 */
	public static boolean sendMsgByNickName(String text, String nickName) {
		if (nickName != null) {
			String toUserName = WechatTools.getUserNameByNickName(nickName);
			if (toUserName != null) {
				sendRawMsg(1, text, toUserName);
				return true;
			}
		}
		return false;

	}

	/**
	 * 消息发送
	 * 
	 * @author https://github.com/yaphone
	 * @date 2017年4月23日 下午2:32:02
	 * @param msgType
	 * @param content
	 * @param toUserName
	 */
	public static void sendRawMsg(int msgType, String content, String toUserName) {
		String url = String.format("%s/webwxsendmsg", core.getLoginInfo().get("url"));

		Map<String, Object> paramMap = new HashMap<String, Object>();
		@SuppressWarnings("unchecked")
		Map<String, Map<String, String>> baseRequestMap = (Map<String, Map<String, String>>) core.getLoginInfo()
				.get("baseRequest");
		paramMap.put("BaseRequest", baseRequestMap.get("BaseRequest"));
		Map<String, Object> msgMap = new HashMap<String, Object>();
		msgMap.put("Type", msgType);
		msgMap.put("Content", content);
		msgMap.put("FromUserName", core.getStorageClass().getUserName());
		msgMap.put("ToUserName", toUserName == null ? core.getStorageClass().getUserName() : toUserName);
		msgMap.put("LocalID", new Date().getTime() * 10);
		msgMap.put("ClientMsgId", new Date().getTime() * 10);
		paramMap.put("Msg", msgMap);
		paramMap.put("Scene", 0);
		try {
			String paramStr = JSON.toJSONString(paramMap);
			HttpEntity entity = myHttpClient.doPost(url, paramStr);
			EntityUtils.toString(entity, "UTF-8");
		} catch (Exception e) {
			logger.info(e.getMessage());
		}
	}

	/**
	 * 上传多媒体文件到 微信服务器，目前应该支持3种类型:
	 * <p>
	 * 1. pic 直接显示，包含图片，表情
	 * </p>
	 * <p>
	 * 2.video
	 * </p>
	 * <p>
	 * 3.doc 显示为文件，包含PDF等
	 * </p>
	 * 
	 * @author https://github.com/yaphone
	 * @date 2017年5月7日 上午12:41:13
	 * @param filePath
	 * @return
	 */
	private static JSONObject uploadMediaToServer(String filePath) {
		File f = new File(filePath);
		if (!f.exists() && f.isFile()) {
			logger.info("file is not exist");
			return null;
		}
		String url = (String) core.getLoginInfo().get("fileUrl") + "/webwxuploadmedia?f=json";
		String mimeType = new MimetypesFileTypeMap().getContentType(f);
		String mediaType = "";
		if (mimeType == null) {
			mimeType = "text/plain";
		} else {
			mediaType = mimeType.split("/")[0].equals("image") ? "pic" : "doc";
		}
		String lastModifieDate = new SimpleDateFormat("yyyy MM dd HH:mm:ss").format(new Date());
		long fileSize = f.length();
		String passTicket = (String) core.getLoginInfo().get("pass_ticket");
		String clientMediaId = String.valueOf(new Date().getTime())
				+ String.valueOf(new Random().nextLong()).substring(0, 4);
		String webwxDataTicket = MyHttpClient.getCookie("webwx_data_ticket");
		if (webwxDataTicket == null) {
			logger.info("get cookie webwx_data_ticket error");
			return null;
		}

		Map<String, Object> paramMap = new HashMap<String, Object>();
		@SuppressWarnings("unchecked")
		Map<String, Map<String, String>> baseRequestMap = (Map<String, Map<String, String>>) core.getLoginInfo()
				.get("baseRequest");
		paramMap.put("BaseRequest", baseRequestMap.get("BaseRequest"));
		paramMap.put("ClientMediaId", clientMediaId);
		paramMap.put("TotalLen", fileSize);
		paramMap.put("StartPos", 0);
		paramMap.put("DataLen", fileSize);
		paramMap.put("MediaType", 4);

		MultipartEntityBuilder builder = MultipartEntityBuilder.create();
		builder.setMode(HttpMultipartMode.BROWSER_COMPATIBLE);

		builder.addTextBody("id", "WU_FILE_0", ContentType.TEXT_PLAIN);
		builder.addTextBody("name", filePath, ContentType.TEXT_PLAIN);
		builder.addTextBody("type", mimeType, ContentType.TEXT_PLAIN);
		builder.addTextBody("lastModifieDate", lastModifieDate, ContentType.TEXT_PLAIN);
		builder.addTextBody("size", String.valueOf(fileSize), ContentType.TEXT_PLAIN);
		builder.addTextBody("mediatype", mediaType, ContentType.TEXT_PLAIN);
		builder.addTextBody("uploadmediarequest", JSON.toJSONString(paramMap), ContentType.TEXT_PLAIN);
		builder.addTextBody("webwx_data_ticket", webwxDataTicket, ContentType.TEXT_PLAIN);
		builder.addTextBody("pass_ticket", passTicket, ContentType.TEXT_PLAIN);
		builder.addBinaryBody("filename", f, ContentType.create(mimeType), filePath);
		HttpEntity reqEntity = builder.build();
		HttpEntity entity = myHttpClient.doPostFile(url, reqEntity);
		if (entity != null) {
			try {
				String result = EntityUtils.toString(entity, "UTF-8");
				return JSON.parseObject(result);
			} catch (Exception e) {
				logger.info(e.getMessage());
			}

		}
		return null;
	}

	/**
	 * 根据NickName发送图片消息
	 * 
	 * @author https://github.com/yaphone
	 * @date 2017年5月7日 下午10:32:45
	 * @param nackName
	 * @return
	 */
	public static boolean sendPicMsgByNickName(String nickName, String filePath) {
		if (nickName != null) {
			String toUserName = WechatTools.getUserNameByNickName(nickName);
			if (toUserName != null) {
				return sendPicMsgByUserId(toUserName, filePath);
			}
		}
		return false;
	}

	/**
	 * 根据用户id发送图片消息
	 * 
	 * @author https://github.com/yaphone
	 * @date 2017年5月7日 下午10:34:24
	 * @param nickName
	 * @param filePath
	 * @return
	 */
	public static boolean sendPicMsgByUserId(String userId, String filePath) {
		JSONObject responseObj = uploadMediaToServer(filePath);
		if (responseObj != null) {
			String mediaId = responseObj.getString("MediaId");
			if (mediaId != null) {
				return webWxSendMsgImg(userId, mediaId);
			}
		}
		return false;
	}

	/**
	 * 发送图片消息，内部调用
	 * 
	 * @author https://github.com/yaphone
	 * @date 2017年5月7日 下午10:38:55
	 * @return
	 */
	private static boolean webWxSendMsgImg(String userId, String mediaId) {
		String url = String.format("%s/webwxsendmsgimg?fun=async&f=json&pass_ticket=%s", core.getLoginInfo().get("url"),
				core.getLoginInfo().get("pass_ticket"));
		Map<String, Object> msgMap = new HashMap<String, Object>();
		msgMap.put("Type", 3);
		msgMap.put("MediaId", mediaId);
		msgMap.put("FromUserName", core.getUserSelfList().get(0).getString("UserName"));
		msgMap.put("ToUserName", userId);
		String clientMsgId = String.valueOf(new Date().getTime())
				+ String.valueOf(new Random().nextLong()).substring(1, 5);
		msgMap.put("LocalID", clientMsgId);
		msgMap.put("ClientMsgId", clientMsgId);
		Map<String, Object> paramMap = new HashMap<String, Object>();
		@SuppressWarnings("unchecked")
		Map<String, Map<String, String>> baseRequestMap = (Map<String, Map<String, String>>) core.getLoginInfo()
				.get("baseRequest");
		paramMap.put("BaseRequest", baseRequestMap.get("BaseRequest"));
		paramMap.put("Msg", msgMap);
		String paramStr = JSON.toJSONString(paramMap);
		HttpEntity entity = myHttpClient.doPost(url, paramStr);
		if (entity != null) {
			try {
				String result = EntityUtils.toString(entity, "UTF-8");
				return JSON.parseObject(result).getJSONObject("BaseResponse").getInteger("Ret") == 0;
			} catch (Exception e) {
				logger.info(e.getMessage());
			}
		}
		return false;

	}

	/**
	 * 根据用户id发送文件
	 * 
	 * @author https://github.com/yaphone
	 * @date 2017年5月7日 下午11:57:36
	 * @param userId
	 * @param filePath
	 * @return
	 */
	public static boolean sednFileMsgByUserId(String userId, String filePath) {
		String title = new File(filePath).getName();
		Map<String, String> data = new HashMap<String, String>();
		data.put("appid", Constant.API_WXAPPID);
		data.put("title", title);
		data.put("totallen", "");
		data.put("attachid", "");
		data.put("type", "6"); // APPMSGTYPE_ATTACH
		data.put("fileext", title.split("\\.")[1]); // 文件后缀
		JSONObject responseObj = uploadMediaToServer(filePath);
		if (responseObj != null) {
			data.put("totallen", responseObj.getString("StartPos"));
			data.put("attachid", responseObj.getString("MediaId"));
		} else {
			logger.info("sednFileMsgByUserId error");
		}
		return webWxSendAppMsg(userId, data);
	}

	/**
	 * 内部调用
	 * 
	 * @author https://github.com/yaphone
	 * @date 2017年5月10日 上午12:21:28
	 * @param userId
	 * @param data
	 * @return
	 */
	private static boolean webWxSendAppMsg(String userId, Map<String, String> data) {
		String url = String.format("%s/webwxsendappmsg?fun=async&f=json&pass_ticket=%s", core.getLoginInfo().get("url"),
				core.getLoginInfo().get("pass_ticket"));
		String clientMsgId = String.valueOf(new Date().getTime())
				+ String.valueOf(new Random().nextLong()).substring(1, 5);
		String content = "<appmsg appid='wxeb7ec651dd0aefa9' sdkver=''><title>" + data.get("title")
				+ "</title><des></des><action></action><type>6</type><content></content><url></url><lowurl></lowurl>"
				+ "<appattach><totallen>" + data.get("totallen") + "</totallen><attachid>" + data.get("attachid")
				+ "</attachid><fileext>" + data.get("fileext") + "</fileext></appattach><extinfo></extinfo></appmsg>";
		Map<String, Object> msgMap = new HashMap<String, Object>();
		msgMap.put("Type", data.get("type"));
		msgMap.put("Content", content);
		msgMap.put("FromUserName", core.getUserSelfList().get(0).getString("UserName"));
		msgMap.put("ToUserName", userId);
		msgMap.put("LocalID", clientMsgId);
		msgMap.put("ClientMsgId", clientMsgId);
		Map<String, Object> paramMap = new HashMap<String, Object>();
		@SuppressWarnings("unchecked")
		Map<String, Map<String, String>> baseRequestMap = (Map<String, Map<String, String>>) core.getLoginInfo()
				.get("baseRequest");
		paramMap.put("BaseRequest", baseRequestMap.get("BaseRequest"));
		paramMap.put("Msg", msgMap);
		paramMap.put("Scene", 0);
		String paramStr = JSON.toJSONString(paramMap);
		HttpEntity entity = myHttpClient.doPost(url, paramStr);
		if (entity != null) {
			try {
				String result = EntityUtils.toString(entity, "UTF-8");
				return JSON.parseObject(result).getJSONObject("BaseResponse").getInteger("Ret") == 0;
			} catch (Exception e) {
				logger.info(e.getMessage());
			}
		}
		return false;
	}

}
