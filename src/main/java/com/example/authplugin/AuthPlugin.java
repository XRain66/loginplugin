package com.example.authplugin;

import com.google.inject.Inject;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.ProxyServer;
import org.slf4j.Logger;

import java.nio.file.Path;

@Plugin(
    id = "auth-plugin",
    name = "Auth Plugin",
    version = "1.0-SNAPSHOT",
    description = "A simple auth plugin for Velocity",
    authors = {"XRain666"}
)
public class AuthPlugin {
    private static AuthPlugin instance;
    private final ProxyServer server;
    private final Logger logger;
    private final Path dataDirectory;
    private AuthManager authManager;

    @Inject
    public AuthPlugin(ProxyServer server, Logger logger, @DataDirectory Path dataDirectory) {
        instance = this;
        this.server = server;
        this.logger = logger;
        this.dataDirectory = dataDirectory;
    }

    @Subscribe
    public void onProxyInitialize(ProxyInitializeEvent event) {
        logger.info("Auth Plugin 正在初始化...");
        
        this.authManager = new AuthManager(this, server, logger);
        server.getEventManager().register(this, new AuthListener(this));
        
        server.getCommandManager().register("login", new LoginCommand(authManager));
        server.getCommandManager().register("register", new RegisterCommand(authManager));
        
        logger.info("Auth Plugin 已加载！");
    }

    public static AuthPlugin getInstance() {
        return instance;
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

    public Path getDataDirectory() {
        return dataDirectory;
    }
} 