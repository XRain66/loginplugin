package com.example.authplugin;

import com.google.inject.Inject;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import org.slf4j.Logger;

import java.util.Optional;

@Plugin(
    id = "auth-plugin",
    name = "Auth Plugin",
    version = "1.0-SNAPSHOT",
    description = "A simple auth plugin for Velocity",
    authors = {"XRain666"}
)
public class AuthPlugin {
    private final ProxyServer server;
    private final Logger logger;
    private AuthManager authManager;
    private RegisteredServer loginServer;

    @Inject
    public AuthPlugin(ProxyServer server, Logger logger) {
        this.server = server;
        this.logger = logger;
    }

    @Subscribe
    public void onProxyInitialize(ProxyInitializeEvent event) {
        logger.info("Auth Plugin 正在初始化...");
        
        Optional<RegisteredServer> loginServerOptional = server.getServer("login");
        if (!loginServerOptional.isPresent()) {
            logger.error("找不到登录服务器！请确保配置了名为 'login' 的服务器！");
            return;
        }
        this.loginServer = loginServerOptional.get();
        
        this.authManager = new AuthManager(this, server, logger);
        server.getEventManager().register(this, new AuthListener(this));
        
        server.getCommandManager().register("login", new LoginCommand(authManager));
        server.getCommandManager().register("register", new RegisterCommand(authManager));
        
        logger.info("Auth Plugin 已加载！");
    }

    public AuthManager getAuthManager() {
        return authManager;
    }

    public ProxyServer getServer() {
        return server;
    }

    public Logger getLogger() {
        return logger;
    }

    public RegisteredServer getLoginServer() {
        return loginServer;
    }
} 