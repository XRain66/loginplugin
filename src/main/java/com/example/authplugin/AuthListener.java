package com.example.authplugin;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.command.CommandExecuteEvent;
import com.velocitypowered.api.event.connection.DisconnectEvent;
import com.velocitypowered.api.event.player.PlayerChatEvent;
import com.velocitypowered.api.event.player.ServerPreConnectEvent;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import net.kyori.adventure.text.Component;

public class AuthListener {
    private final AuthPlugin plugin;

    public AuthListener(AuthPlugin plugin) {
        this.plugin = plugin;
    }

    @Subscribe
    public void onServerPreConnect(ServerPreConnectEvent event) {
        Player player = event.getPlayer();
        RegisteredServer targetServer = event.getResult().getServer().orElse(null);
        
        if (!plugin.getAuthManager().isAuthenticated(player)) {
            // 如果玩家未登录，强制连接到登录服务器
            if (targetServer != plugin.getLoginServer()) {
                event.setResult(ServerPreConnectEvent.ServerResult.allowed(plugin.getLoginServer()));
                player.sendMessage(Component.text("§c请先登录后才能进入其他服务器！"));
                player.sendMessage(Component.text("§e使用 /login <密码> 登录"));
            }
        } else {
            // 如果玩家已登录，但仍在登录服务器，则允许切换到其他服务器
            if (targetServer == plugin.getLoginServer()) {
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
        // 如果不是玩家执行的命令，直接返回
        if (!(event.getCommandSource() instanceof Player)) {
            return;
        }

        Player player = (Player) event.getCommandSource();
        String command = event.getCommand().toLowerCase();

        // 如果玩家未登录且不是login命令，则取消命令执行
        if (!plugin.getAuthManager().isAuthenticated(player) && !command.startsWith("login")) {
            event.setResult(CommandExecuteEvent.CommandResult.denied());
            player.sendMessage(Component.text("§c请先登录后再使用命令！"));
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
} 