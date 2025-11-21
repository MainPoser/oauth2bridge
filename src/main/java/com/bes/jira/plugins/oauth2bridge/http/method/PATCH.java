package com.bes.jira.plugins.oauth2bridge.http.method;


import javax.ws.rs.HttpMethod;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Custom annotation for HTTP PATCH method.
 * 用于支持 HTTP PATCH 方法的自定义注解。
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@HttpMethod("PATCH") // 关键：使用 JAX-RS 的 @HttpMethod("PATCH")
public @interface PATCH {
}
