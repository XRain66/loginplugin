package com.example.authplugin;

import com.velocitypowered.api.event.PostOrder;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.command.CommandExecuteEvent;
import com.velocitypowered.api.event.connection.DisconnectEvent;
import com.velocitypowered.api.event.connection.LoginEvent;
import com.velocitypowered.api.event.connection.PreLoginEvent;
import com.velocitypowered.api.event.player.PlayerChatEvent;
import com.velocitypowered.api.event.player.ServerPreConnectEvent;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import net.kyori.adventure.text.Component;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

public class AuthListener {
    private final AuthPlugin plugin;
    private final AuthManager authManager;

    public AuthListener(AuthPlugin plugin) {
        this.plugin = plugin;
        this.authManager = plugin.getAuthManager();
    }

    @Subscribe
    public void onServerPreConnect(ServerPreConnectEvent event) {
        Player player = event.getPlayer();
        RegisteredServer targetServer = event.getResult().getServer().orElse(null);
        
        if (!plugin.getAuthManager().isAuthenticated(player)) {
            // 如果玩家未登录，强制连接到登录服务器
            Optional<RegisteredServer> loginServer = plugin.getServer().getServer("login");
            if (loginServer.isPresent() && targetServer != loginServer.get()) {
                event.setResult(ServerPreConnectEvent.ServerResult.allowed(loginServer.get()));
                player.sendMessage(Component.text("§c请先登录后才能进入其他服务器！"));
                player.sendMessage(Component.text("§e使用 /login <密码> 登录"));
            }
        } else {
            // 如果玩家已登录，但仍在登录服务器，则允许切换到其他服务器
            Optional<RegisteredServer> loginServer = plugin.getServer().getServer("login");
            if (loginServer.isPresent() && targetServer == loginServer.get()) {
                player.sendMessage(Component.text("§a你已经登录，可以切换到其他服务器了！"));
            }
        }
    }

    @Subscribe
    public void onDisconnect(DisconnectEvent event) {
        plugin.getAuthManager().removePlayer(event.getPlayer());
    }

    @Subscribe
    public void onCommand(CommandExecuteEvent event) {
        if (!(event.getCommandSource() instanceof Player)) {
            return;
        }

        Player player = (Player) event.getCommandSource();
        String command = event.getCommand().toLowerCase();

        // 如果玩家未登录且不是login或register命令，则取消命令执行
        if (!plugin.getAuthManager().isAuthenticated(player) && 
            !command.startsWith("login") && 
            !command.startsWith("register")) {
            event.setResult(CommandExecuteEvent.CommandResult.denied());
            if (plugin.getAuthManager().isRegistered(player)) {
                player.sendMessage(Component.text("§c请先使用 /login <密码> 登录！"));
            } else {
                player.sendMessage(Component.text("§c请先使用 /register <密码> 注册！"));
            }
        }
    }

    @Subscribe
    public void onPlayerChat(PlayerChatEvent event) {
        Player player = event.getPlayer();
        if (!plugin.getAuthManager().isAuthenticated(player)) {
            event.setResult(PlayerChatEvent.ChatResult.denied());
            player.sendMessage(Component.text("§c请先登录后再聊天！"));
        }
    }

    @Subscribe(order = PostOrder.FIRST)
    public void onPreLogin(PreLoginEvent event) {
        // 不要让 Velocity 直接拒绝非正版玩家
        event.setResult(PreLoginEvent.PreLoginComponentResult.forceOfflineMode());
    }

    @Subscribe(order = PostOrder.FIRST)
    public void onLogin(LoginEvent event) {
        Player player = event.getPlayer();
        
        // 检查是否是正版玩家
        if (player.isOnlineMode()) {
            // 正版玩家自动通过验证
            plugin.getAuthManager().authenticatePlayer(player.getUniqueId());
            player.sendMessage(Component.text("§a欢迎正版玩家 " + player.getUsername()));
            
            // 直接传送到生存服务器
            Optional<RegisteredServer> survivalServer = plugin.getServer().getServer("survival");
            if (survivalServer.isPresent()) {
                player.createConnectionRequest(survivalServer.get()).fireAndForget();
            }
        } else {
            // 非正版玩家需要登录验证
            player.sendMessage(Component.text("§e请使用 /login <密码> 登录"));
            player.sendMessage(Component.text("§e如果没有账号，请使用 /register <密码> 注册"));
            
            // 强制传送到登录服务器
            Optional<RegisteredServer> loginServer = plugin.getServer().getServer("login");
            if (loginServer.isPresent()) {
                player.createConnectionRequest(loginServer.get()).fireAndForget();
            }
        }
    }
} 