package com.example.authplugin;

import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import net.kyori.adventure.text.Component;

public class LoginCommand implements SimpleCommand {
    private final AuthManager authManager;

    public LoginCommand(AuthManager authManager) {
        this.authManager = authManager;
    }

    @Override
    public void execute(Invocation invocation) {
        if (!(invocation.source() instanceof Player)) {
            invocation.source().sendMessage(Component.text("§c只有玩家才能使用此命令！"));
            return;
        }

        Player player = (Player) invocation.source();
        String[] args = invocation.arguments();

        if (args.length != 1) {
            player.sendMessage(Component.text("§c使用方法: /login <密码>"));
            return;
        }

        if (authManager.isAuthenticated(player)) {
            player.sendMessage(Component.text("§c你已经登录了！"));
            return;
        }

        String password = args[0];
        if (authManager.authenticate(player, password)) {
            player.sendMessage(Component.text("§a登录成功！"));
        } else {
            player.sendMessage(Component.text("§c密码错误！"));
        }
    }
} 