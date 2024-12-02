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
    authors = {"YourName"}
)
public class AuthPlugin {
    private final ProxyServer server;
    private final Logger logger;
    private final AuthManager authManager;
    private final RegisteredServer loginServer;

    @Inject
    public AuthPlugin(ProxyServer server, Logger logger) {
        this.server = server;
        this.logger = logger;
        this.authManager = new AuthManager(this);
        
        Optional<RegisteredServer> loginServerOptional = server.getServer("login");
        this.loginServer = loginServerOptional.orElseThrow(() -> 
            new RuntimeException("找不到登录服务器！请确保配置了名为 'login' 的服务器！"));
    }

    @Subscribe
    public void onProxyInitialization(ProxyInitializeEvent event) {
        // 注册事件监听器
        server.getEventManager().register(this, new AuthListener(this));
        // 注册命令
        server.getCommandManager().register("login", new LoginCommand(authManager));
        server.getCommandManager().register("register", new RegisterCommand(authManager));
        server.getCommandManager().register("authreload", new ReloadCommand(this));
    }

    public AuthManager getAuthManager() {
        return authManager;
    }

    public RegisteredServer getLoginServer() {
        return loginServer;
    }

    public ProxyServer getServer() {
        return server;
    }

    public Logger getLogger() {
        return logger;
    }
} 