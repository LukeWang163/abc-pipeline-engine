package abc_pipeline_engine.user.controller;

import abc_pipeline_engine.user.api.IUserInfoService;
import abc_pipeline_engine.user.data.UserInfo;
import abc_pipeline_engine.utils.JobConfigUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import sdk.security.util.SecurityProvider;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

@Controller
@RequestMapping("/user")
public class UserInfoController {

	@Autowired
	private IUserInfoService userInfoService;

	@RequestMapping(value = "/info", method = RequestMethod.GET)
	@ResponseBody
	public UserInfo getLoginUser() {
		return userInfoService.getLoginUser();
	}

	@RequestMapping(value = "/authorizations/token")
	@ResponseBody
	public Map<String, String> applyToken() {
		String userId = userInfoService.getLoginUserId();
//		String token = userInfoService.applyToken(userId);
		Map<String, String> result = new HashMap<String, String>();
		result.put("server", "/user/" + userId);
		result.put("userId", userId);
//		result.put("token", token);
		return result;
	}

	@RequestMapping(value = "/logout")
	@ResponseBody
	public Map<String, String> logout(@RequestParam("redirect_uri") String url,HttpServletRequest request) {
		Map<String, String> result = new HashMap<String, String>();
		String userId = userInfoService.getLoginUserId();
//		userInfoService.logout(userId);
		HttpSession session=request.getSession();
		session.invalidate();
		result.put("url", SecurityProvider.getLogoutUrl(url));
		return result;
	}
	
	@RequestMapping(value = "/realm/{user}",method=RequestMethod.GET)
	@ResponseBody
	public Map<String, String> realm(@PathVariable String user) {
		Map<String, String> result = new HashMap<String, String>();
		Properties prop = new Properties();
		InputStream is = JobConfigUtil.class.getClassLoader().getResourceAsStream("/config/user.properties");
		try {
			prop.load(is);
		} catch (IOException e) {
			e.printStackTrace();
		}
		result.put("realm", prop.getProperty(user));
		return result;
	}
}
