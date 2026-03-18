package com.auth.platform.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.savedrequest.HttpSessionRequestCache;
import org.springframework.security.web.savedrequest.SavedRequest;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 表单登录成功处理器
 *
 * <p>登录成功后返回 JSON，包含：
 * <ul>
 *   <li>{@code redirectUrl}：OAuth2 授权流程的回调地址（或前端首页地址）</li>
 *   <li>{@code nickname}：当前登录用户的昵称，供授权确认页展示</li>
 *   <li>{@code username}：当前登录用户的用户名</li>
 * </ul>
 */
@Component
@RequiredArgsConstructor
public class LoginSuccessHandler implements AuthenticationSuccessHandler {

    private final ObjectMapper objectMapper;

    @Value("${app.frontend-url:http://localhost:5173}")
    private String frontendUrl;

    private final HttpSessionRequestCache requestCache = new HttpSessionRequestCache();

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException {
        response.setContentType("application/json;charset=UTF-8");

        SavedRequest savedRequest = requestCache.getRequest(request, response);
        String redirectUrl = savedRequest != null ? savedRequest.getRedirectUrl() : frontendUrl;

        String username = authentication.getName();
        String nickname = username;
        if (authentication.getPrincipal() instanceof LoginUser loginUser) {
            String n = loginUser.getUser().getNickname();
            nickname = (n != null && !n.isEmpty()) ? n : loginUser.getUsername();
        }

        Map<String, String> body = new LinkedHashMap<>();
        body.put("redirectUrl", redirectUrl);
        body.put("nickname", nickname);
        body.put("username", username);

        objectMapper.writeValue(response.getWriter(), body);
    }
}
