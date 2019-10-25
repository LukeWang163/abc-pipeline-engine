package abc_pipeline_engine.user.api;

import abc_pipeline_engine.user.data.UserInfo;

public interface IUserInfoService {

	UserInfo getLoginUser();

	String getLoginUserId();

	String getLoginUserName();

	String getToken();

	String getToken(String userId);
}
