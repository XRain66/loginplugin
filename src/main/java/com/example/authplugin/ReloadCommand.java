package com.example.authplugin;

import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import net.kyori.adventure.text.Component;

public class ReloadCommand implements SimpleCommand {
    private final AuthPlugin plugin;

    public ReloadCommand(AuthPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void execute(Invocation invocation) {
        // 只允许控制台或有权限的玩家使用
        if (invocation.source() instanceof Player) {
            Player player = (Player) invocation.source();
            if (!player.hasPermission("authplugin.reload")) {
                player.sendMessage(Component.text("§c你没有权限使用此命令！"));
                return;
            }
        }

        // 重载配置
        plugin.getAuthManager().reloadConfig();
        invocation.source().sendMessage(Component.text("§a配置重载成功！"));
    }
} 