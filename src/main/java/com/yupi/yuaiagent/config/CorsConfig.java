package com.yupi.yuaiagent.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 全局 Web 配置：跨域 + 登录拦截
 */
@Configuration
public class CorsConfig implements WebMvcConfigurer {

    private final LoginInterceptor loginInterceptor;

    public CorsConfig(LoginInterceptor loginInterceptor) {
        this.loginInterceptor = loginInterceptor;
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowCredentials(true)
                .allowedOriginPatterns("*")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .exposedHeaders("*");
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(loginInterceptor)
                .addPathPatterns(
                        "/employee/**",
                        "/hr/**",
                        "/admin/**",
                        "/ai/employee/**",
                        "/ai/hr/**",
                        "/ai/admin/**",
                        "/manage/**"
                )
                .excludePathPatterns(
                        "/",
                        "/index.html",
                        "/login.html",
                        "/auth/login",
                        "/auth/register",
                        "/auth/current",
                        "/assets/**",
                        "/favicon.ico",
                        "/error"
                );
    }
}