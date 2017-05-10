package cn.zhouyafeng.itchat4j.api;

import cn.zhouyafeng.itchat4j.utils.Config;
import cn.zhouyafeng.itchat4j.utils.Core;
import cn.zhouyafeng.itchat4j.utils.MyHttpClient;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.apache.http.HttpEntity;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;

import java.util.*;
import java.util.logging.Logger;

/**
 * 微信小工具，如获好友列表等
 * 
 * @author https://github.com/yaphone
 * @date 创建时间：2017年5月4日 下午10:49:16
 * @version 1.0
 *
 */
public class WechatTools {

    private static Logger logger = Logger.getLogger("Wechat");
    private static Core core = Core.getInstance();
    private static MyHttpClient myHttpClient = core.getMyHttpClient();

	/**
	 * 根据用户名发送文本消息
	 * 
	 * @author https://github.com/yaphone
	 * @date 2017年5月4日 下午10:43:14
	 * @param msg
	 * @param toUserName
	 */
	public static void sendMsgByUserName(String msg, String toUserName) {
		MessageTools.sendMsg(msg, toUserName);
	}

	/**
	 * 获取好友列表，JSONObject格式
	 * 
	 * @author https://github.com/yaphone
	 * @date 2017年5月4日 下午10:55:18
	 * @return
	 */
	private static List<JSONObject> getJsonContactList() {
		return core.getContactList();
	}

	/**
	 * <p>
	 * 通过RealName获取本次UserName
	 * </p>
	 * <p>
	 * 如NickName为"yaphone"，则获取UserName=
	 * "@1212d3356aea8285e5bbe7b91229936bc183780a8ffa469f2d638bf0d2e4fc63"，
	 * 可通过UserName发送消息
	 * </p>
	 * 
	 * @author https://github.com/yaphone
	 * @date 2017年5月4日 下午10:56:31
	 * @param name
	 * @return
	 */
	public static String getUserNameByNickName(String nickName) {
		for (JSONObject o : core.getContactList()) {
			if (o.getString("NickName").equals(nickName)) {
				return o.getString("UserName");
			}
		}
		return null;
	}

	/**
	 * 返回好友昵称列表
	 * 
	 * @author https://github.com/yaphone
	 * @date 2017年5月4日 下午11:37:20
	 * @return
	 */
	public static List<String> getContactList() {
		List<String> contactList = new ArrayList<String>();
		for (JSONObject o : core.getContactList()) {
			contactList.add(o.getString("NickName"));
		}
		return contactList;
	}

	/**
	 * 返回群列表
	 * 
	 * @author https://github.com/yaphone
	 * @date 2017年5月5日 下午9:55:21
	 * @return
	 */
	public static List<String> getGroupList() {
		List<String> groupList = new ArrayList<String>();
		for (JSONObject o : core.getGroupList()) {
			groupList.add(o.getString("Name"));
		}
		return groupList;
	}

	public static List<String> getGroupIdList() {
		return core.getGroupIdList();
	}

    /**
     * 修改备注名
     *
     * @param toUserName 某个用户
     * @param remarkname 备注名
     * @return 成功或者失败
     */
    public static boolean changeRemarkName(String toUserName, String remarkname) {
        String url = String.format("%s/webwxoplog", core.getLoginInfo().get("url"));

        Map<String, Object> paramMap = new HashMap<String, Object>();
        @SuppressWarnings("unchecked")
        Map<String, Map<String, String>> baseRequestMap = (Map<String, Map<String, String>>) core.getLoginInfo()
                .get("baseRequest");
        paramMap.put("BaseRequest", baseRequestMap.get("BaseRequest"));
        paramMap.put("CmdId", 2);
        paramMap.put("RemarkName", remarkname);
        paramMap.put("UserName", toUserName);
        try {
            String paramStr = JSON.toJSONString(paramMap);
            HttpEntity entity = myHttpClient.doPost(url, paramStr);
            JSONObject jsonObject = JSON.parseObject(EntityUtils.toString(entity, "UTF-8"));
            JSONObject responseJsonObject = jsonObject.getJSONObject("BaseResponse");
            return responseJsonObject.getInteger("Ret") == 0;
        } catch (Exception e) {
            logger.info(e.getMessage());
        }
        return false;
    }

    /**
     * 更新微信联系人
     */
    public static boolean updateContact() {
        String url = String.format("%s/webwxgetcontact", core.getLoginInfo().get("url"));
        List<BasicNameValuePair> params = new ArrayList<BasicNameValuePair>();
        params.add(new BasicNameValuePair("pass_ticket", (String) core.getLoginInfo().get("pass_ticket")));
        params.add(new BasicNameValuePair("skey", (String) core.getLoginInfo().get("skey")));
        params.add(new BasicNameValuePair("r", String.valueOf(String.valueOf(new Date().getTime()))));
        HttpEntity entity = myHttpClient.doGet(url, params, true, null);
        String result;
        try {
            result = EntityUtils.toString(entity, "UTF-8");
        } catch (Exception e) {
            logger.info(e.getMessage());
            return false;
        }
        JSONObject fullFriendsJsonList = JSON.parseObject(result);
        core.setMemberCount(fullFriendsJsonList.getInteger(("MemberCount")));
        JSONArray memberJsonArray = fullFriendsJsonList.getJSONArray("MemberList");

        // 移除已存在core中的联系人
        core.getMemberList().clear();
        core.getPublicUsersList().clear();
        core.getSpecialUsersList().clear();
        core.getGroupList().clear();
        core.getContactList().clear();

        for (int i = 0; i < memberJsonArray.size(); i++) {
            core.getMemberList().add(memberJsonArray.getJSONObject(i));
        }
        for (JSONObject o : core.getMemberList()) {
            if ((o.getInteger("VerifyFlag") & 8) != 0) { // 公众号/服务号
                core.getPublicUsersList().add(o);
            } else if (Config.API_SPECIAL_USER.contains(o.getString("UserName"))) { // 特殊账号
                core.getSpecialUsersList().add(o);
            } else if (o.getString("UserName").contains("@@")) { // 群聊
                core.getGroupList().add(o);
            } else if (o.getString("UserName").equals(core.getUserSelfList().get(0).getString("UserName"))) { // 自己
                core.getContactList().remove(o);
            } else { // 普通联系人
                core.getContactList().add(o);
            }
        }
        return true;
    }

}
