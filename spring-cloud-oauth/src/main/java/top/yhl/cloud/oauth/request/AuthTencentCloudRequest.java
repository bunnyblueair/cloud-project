package top.yhl.cloud.oauth.request;

import com.alibaba.fastjson.JSONObject;
import top.yhl.cloud.oauth.cache.AuthStateCache;
import top.yhl.cloud.oauth.entity.AuthConfig;
import top.yhl.cloud.oauth.entity.AuthDefaultSource;
import top.yhl.cloud.oauth.enums.AuthUserGender;
import top.yhl.cloud.oauth.exception.AuthException;
import top.yhl.cloud.oauth.entity.AuthCallback;
import top.yhl.cloud.oauth.entity.AuthToken;
import top.yhl.cloud.oauth.entity.AuthUser;
import top.yhl.cloud.oauth.utils.UrlBuilder;


/**
 * 腾讯云登录
 *
 * @author yadong.zhang (yadong.zhang0415(a)gmail.com)
 * @since 1.0.0
 */
public class AuthTencentCloudRequest extends AuthDefaultRequest {

    public AuthTencentCloudRequest(AuthConfig config) {
        super(config, AuthDefaultSource.TENCENT_CLOUD);
    }

    public AuthTencentCloudRequest(AuthConfig config, AuthStateCache authStateCache) {
        super(config, AuthDefaultSource.TENCENT_CLOUD, authStateCache);
    }

    @Override
    protected AuthToken getAccessToken(AuthCallback authCallback) {
        String response = doGetAuthorizationCode(authCallback.getCode());
        JSONObject accessTokenObject = JSONObject.parseObject(response);
        this.checkResponse(accessTokenObject);
        return AuthToken.builder()
            .accessToken(accessTokenObject.getString("access_token"))
            .expireIn(accessTokenObject.getIntValue("expires_in"))
            .refreshToken(accessTokenObject.getString("refresh_token"))
            .build();
    }

    @Override
    protected AuthUser getUserInfo(AuthToken authToken) {
        String response = doGetUserInfo(authToken);
        JSONObject object = JSONObject.parseObject(response);
        this.checkResponse(object);

        object = object.getJSONObject("data");
        return AuthUser.builder()
            .uuid(object.getString("id"))
            .username(object.getString("name"))
            .avatar("https://dev.tencent.com/" + object.getString("avatar"))
            .blog("https://dev.tencent.com/" + object.getString("path"))
            .nickname(object.getString("name"))
            .company(object.getString("company"))
            .location(object.getString("location"))
            .gender(AuthUserGender.getRealGender(object.getString("sex")))
            .email(object.getString("email"))
            .remark(object.getString("slogan"))
            .token(authToken)
            .source(source.toString())
            .build();
    }

    /**
     * 检查响应内容是否正确
     *
     * @param object 请求响应内容
     */
    private void checkResponse(JSONObject object) {
        if (object.getIntValue("code") != 0) {
            throw new AuthException(object.getString("msg"));
        }
    }

    /**
     * 返回带{@code state}参数的授权url，授权回调时会带上这个{@code state}
     *
     * @param state state 验证授权流程的参数，可以防止csrf
     * @return 返回授权地址
     * @since 1.9.3
     */
    @Override
    public String authorize(String state) {
        return UrlBuilder.fromBaseUrl(source.authorize())
            .queryParam("response_type", "code")
            .queryParam("client_id", config.getClientId())
            .queryParam("redirect_uri", config.getRedirectUri())
            .queryParam("scope", "user")
            .queryParam("state", getRealState(state))
            .build();
    }
}
