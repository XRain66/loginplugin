package com.example.authplugin;

import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import net.kyori.adventure.text.Component;

public class RegisterCommand implements SimpleCommand {
    private final AuthManager authManager;

    public RegisterCommand(AuthManager authManager) {
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
            player.sendMessage(Component.text("§c用法: /register <密码>"));
            return;
        }

        if (authManager.isRegistered(player)) {
            player.sendMessage(Component.text("§c你已经注册过了！"));
            return;
        }

        authManager.register(player, args[0]);
    }
} 