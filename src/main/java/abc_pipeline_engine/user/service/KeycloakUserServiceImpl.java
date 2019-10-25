package abc_pipeline_engine.user.service;

import abc_pipeline_engine.user.api.IUserInfoService;
import abc_pipeline_engine.user.dao.UserTokenMapper;
import abc_pipeline_engine.user.data.UserInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import sdk.security.authc.AuthenticationProvider;

import java.util.Map;

@Service("userInfoService")
public class KeycloakUserServiceImpl implements IUserInfoService {

	@Autowired
	private UserTokenMapper userTokenMapper;

	@Override
	public UserInfo getLoginUser() {
		Map<String, String> userMap = AuthenticationProvider.getLoginUserInfo();
		UserInfo userInfo = new UserInfo();
		userInfo.setUserId(userMap.get("userId"));
		userInfo.setUserName(userInfo.getUserId());
		String token = userTokenMapper.getUserToken(userInfo.getUserId());
		userInfo.setToken(token);
		return userInfo;
	}

	@Override
	public String getLoginUserId() {
		return AuthenticationProvider.getLoginUserId();
	}

	@Override
	public String getLoginUserName() {
		return AuthenticationProvider.getLoginUserId();
	}

	@Override
	public String getToken() {
		String userId = AuthenticationProvider.getLoginUserId();
		return userTokenMapper.getUserToken(userId);
	}

	@Override
	public String getToken(String userId) {
		return userTokenMapper.getUserToken(userId);
	}
}
