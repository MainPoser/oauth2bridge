package com.bes.jira.plugins.oauth2bridge.rest;

import com.bes.jira.plugins.oauth2bridge.service.Oauth2BridgeConfigService;
import com.bes.jira.plugins.oauth2bridge.util.Encrpt;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.HashMap;
import java.util.Map;

@Path("/settings")
public class Settings {
    private static final Logger log = LoggerFactory.getLogger(Settings.class);
    private final Oauth2BridgeConfigService configService;

    @Inject
    public Settings(Oauth2BridgeConfigService configService) {
        this.configService = configService;
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response get() {
        // 1. 读取配置
        String clientId = configService.getConfig(Oauth2BridgeConfigService.KEY_CLIENT_ID);
        String clientSecret = configService.getConfig(Oauth2BridgeConfigService.KEY_CLIENT_SECRET);
        String authorizationEndpoint = configService.getConfig(Oauth2BridgeConfigService.KEY_AUTHORIZATION_ENDPOINT);
        String tokenEndpoint = configService.getConfig(Oauth2BridgeConfigService.KEY_TOKEN_ENDPOINT);
        String userInfoEndpoint = configService.getConfig(Oauth2BridgeConfigService.KEY_USERINFO_ENDPOINT);
        log.info("Loaded config: clientId={}, clientSecret={}, authEndpoint={}, tokenEndpoint={}, userInfoEndpoint={}",
                clientId, Encrpt.mask(clientSecret), authorizationEndpoint, tokenEndpoint, userInfoEndpoint);
        // 2. 准备 context
        Map<String, Object> context = new HashMap<>();
        context.put(Oauth2BridgeConfigService.KEY_CLIENT_ID, clientId);
        context.put(Oauth2BridgeConfigService.KEY_CLIENT_SECRET, clientSecret);
        context.put(Oauth2BridgeConfigService.KEY_AUTHORIZATION_ENDPOINT, authorizationEndpoint);
        context.put(Oauth2BridgeConfigService.KEY_TOKEN_ENDPOINT, tokenEndpoint);
        context.put(Oauth2BridgeConfigService.KEY_USERINFO_ENDPOINT, userInfoEndpoint);

        return Response.ok(context).build();
    }

    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    public Response put(Map<String, String> context) {
        String clientId = context.get(Oauth2BridgeConfigService.KEY_CLIENT_ID);
        String clientSecret = context.get(Oauth2BridgeConfigService.KEY_CLIENT_SECRET);
        String authorizationEndpoint = context.get(Oauth2BridgeConfigService.KEY_AUTHORIZATION_ENDPOINT);
        String tokenEndpoint = context.get(Oauth2BridgeConfigService.KEY_TOKEN_ENDPOINT);
        String userInfoEndpoint = context.get(Oauth2BridgeConfigService.KEY_USERINFO_ENDPOINT);
        // 1. 保存配置
        configService.saveConfig(Oauth2BridgeConfigService.KEY_CLIENT_ID, clientId);
        configService.saveConfig(Oauth2BridgeConfigService.KEY_CLIENT_SECRET, clientSecret);
        configService.saveConfig(Oauth2BridgeConfigService.KEY_AUTHORIZATION_ENDPOINT, authorizationEndpoint);
        configService.saveConfig(Oauth2BridgeConfigService.KEY_TOKEN_ENDPOINT, tokenEndpoint);
        configService.saveConfig(Oauth2BridgeConfigService.KEY_USERINFO_ENDPOINT, userInfoEndpoint);
        log.info("Saving config: clientId={}, clientSecret={}, authEndpoint={}, tokenEndpoint={}, userInfoEndpoint={}",
                clientId, Encrpt.mask(clientSecret), authorizationEndpoint, tokenEndpoint, userInfoEndpoint);

        return Response.ok().build();
    }
}
