package com.example.authplugin;

import com.google.inject.Inject;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.proxy.ProxyServer;
import org.slf4j.Logger;

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

    @Inject
    public AuthPlugin(ProxyServer server, Logger logger) {
        this.server = server;
        this.logger = logger;
    }

    @Subscribe
    public void onProxyInitialize(ProxyInitializeEvent event) {
        logger.info("Auth Plugin 正在初始化...");
        
        this.authManager = new AuthManager(this, server, logger);
        server.getEventManager().register(this, new AuthListener(this));
        
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
} 