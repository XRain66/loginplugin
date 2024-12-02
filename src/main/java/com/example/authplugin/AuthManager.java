package com.example.authplugin;

import com.velocitypowered.api.proxy.Player;
import net.kyori.adventure.text.Component;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import java.util.Optional;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class AuthManager {
    private final Set<UUID> authenticatedPlayers = new HashSet<>();
    private static final String CORRECT_PASSWORD = "your_password_here";
    private final AuthPlugin plugin;

    public AuthManager(AuthPlugin plugin) {
        this.plugin = plugin;
    }

    public boolean isAuthenticated(Player player) {
        return player.isOnlineMode() || authenticatedPlayers.contains(player.getUniqueId());
    }

    public boolean authenticate(Player player, String password) {
        if (password.equals(CORRECT_PASSWORD)) {
            authenticatedPlayers.add(player.getUniqueId());
            player.sendMessage(Component.text("§a登录成功！"));
            
            // 获取大厅服务器
            Optional<RegisteredServer> lobbyServer = plugin.getServer().getServer("survival");
            if (lobbyServer.isPresent()) {
                // 创建连接请求并发送
                player.createConnectionRequest(lobbyServer.get()).fireAndForget();
                player.sendMessage(Component.text("§a正在将你传送到生存服..."));
            } else {
                player.sendMessage(Component.text("§c错误：找不到生存服，请联系管理员！"));
            }
            
            return true;
        }
        return false;
    }

    public void removePlayer(Player player) {
        authenticatedPlayers.remove(player.getUniqueId());
    }
} 