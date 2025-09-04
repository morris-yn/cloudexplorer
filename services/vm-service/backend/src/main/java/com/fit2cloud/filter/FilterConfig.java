package com.fit2cloud.filter;

import com.alibaba.fastjson2.JSONObject;
import com.fit2cloud.dao.goodsMapper.LiveGoodsMapper;
import com.fit2cloud.utils.RedisUtils;
import com.fit2cloud.utils.UserContext;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import okhttp3.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.ObjectUtils;

import java.io.IOException;

@Configuration
public class FilterConfig {

    static OkHttpClient client = new OkHttpClient();

    private final LiveGoodsMapper liveGoodsMapper;

    @Autowired
    public FilterConfig(LiveGoodsMapper liveGoodsMapper) {
        this.liveGoodsMapper = liveGoodsMapper;
    }

    @Bean
    public FilterRegistrationBean<loginInfoCheck> loggingFilter() {
        FilterRegistrationBean<loginInfoCheck> registrationBean = new FilterRegistrationBean<>();
        registrationBean.setFilter(new loginInfoCheck(liveGoodsMapper));
        registrationBean.addUrlPatterns("/goods/*");
        registrationBean.setOrder(1); // 过滤器顺序，值越小越优先
        return registrationBean;
    }

    public class loginInfoCheck implements Filter {
        private final LiveGoodsMapper liveGoodsMapper;
        public loginInfoCheck(LiveGoodsMapper liveGoodsMapper) {
            this.liveGoodsMapper = liveGoodsMapper;
        }
        @Override
        public void doFilter(ServletRequest request, ServletResponse response,
                             FilterChain chain) throws IOException, ServletException {
            HttpServletRequest httpRequest = (HttpServletRequest) request;
            String token = httpRequest.getHeader("Authorization");
            if(token == null){
                HttpServletResponse httpResponse = (HttpServletResponse) response;
                httpResponse.setStatus(HttpServletResponse.SC_UNAUTHORIZED); // 设置 401 状态码
                httpResponse.setContentType("application/json;charset=UTF-8");
                httpResponse.getWriter().write("{\"code\":401, \"msg\":\"空token\"}");
                return;
            }
            Object userData = RedisUtils.get(token);
            //获取userinfo
            if(ObjectUtils.isEmpty(userData)){
                Request okRequest = new Request.Builder()
                        .url("http://ecshop-api.livepartner.fans/?service=User.memberInfoApi")
                        .header("Weibo-Token",token)
                        .build();
                Response okResponse = null;
                String resStr = "";
                try {
                    okResponse = client.newCall(okRequest).execute();
                    resStr = okResponse.body().string();
                    JSONObject resJo = JSONObject.parseObject(resStr);
                    if(resJo.getInteger("ret") != 200){
                        HttpServletResponse httpResponse = (HttpServletResponse) response;
                        httpResponse.setStatus(HttpServletResponse.SC_UNAUTHORIZED); // 设置 401 状态码
                        httpResponse.setContentType("application/json;charset=UTF-8");
                        httpResponse.getWriter().write("{\"code\":401, \"msg\":\""+resJo.get("msg")+"\"}");
                        return;
                    }
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
                userData = resStr;
                RedisUtils.set(token,userData);
            }else{
                String userId = liveGoodsMapper.selectUserId(token);
                if(userId == null){
                    HttpServletResponse httpResponse = (HttpServletResponse) response;
                    httpResponse.setStatus(HttpServletResponse.SC_UNAUTHORIZED); // 设置 401 状态码
                    httpResponse.setContentType("application/json;charset=UTF-8");
                    httpResponse.getWriter().write("{\"code\":401, \"msg\":\"token已过期\"}");
                    return;
                }
                //验证登录有效性
                RequestBody req = new FormBody.Builder()
                        .add("user_id",liveGoodsMapper.selectUserId(token))
                        .add("token", token)
                        .build();
                Request okRequest = new Request.Builder()
                        .header("Content-Type", "application/x-www-form-urlencoded")
                        .post(req)
                        .url("http://ecshop-api.livepartner.fans/?service=Passport.checkLoginApi")
                        .build();
                Response okResponse = null;
                try {
                    okResponse = client.newCall(okRequest).execute();
                    String resStr = okResponse.body().string();
                    JSONObject resJo = JSONObject.parseObject(resStr);
                    if(resJo.getInteger("ret") != 200){
                        HttpServletResponse httpResponse = (HttpServletResponse) response;
                        httpResponse.setStatus(HttpServletResponse.SC_UNAUTHORIZED); // 设置 401 状态码
                        httpResponse.setContentType("application/json;charset=UTF-8");
                        httpResponse.getWriter().write("{\"code\":401, \"msg\":\""+resJo.getString("msg")+"\"}");
                        return;
                    }
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
            JSONObject user = JSONObject.parseObject(userData.toString()).getJSONObject("data");
            UserContext.setUser(user);
            UserContext.setToken(token);
            try {
                chain.doFilter(request, response);
            } finally {
                UserContext.clear();
            }
        }
    }
}

