package com.example.authplugin;

import com.velocitypowered.api.proxy.Player;
import net.kyori.adventure.text.Component;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import java.io.*;
import java.util.*;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import ninja.leaping.configurate.yaml.YAMLConfigurationLoader;
import ninja.leaping.configurate.ConfigurationNode;
import ninja.leaping.configurate.objectmapping.ObjectMappingException;
import com.google.common.reflect.TypeToken;
import org.slf4j.Logger;

public class AuthManager {
    private final Set<UUID> authenticatedPlayers = new HashSet<>();
    private final Map<UUID, String> playerPasswords = new HashMap<>();
    private final AuthPlugin plugin;
    private final File passwordFile;
    private final File configFile;
    private List<String> allowedOfflinePlayers;
    private String denyMessage;
    private final Logger logger;

    public AuthManager(AuthPlugin plugin) {
        this.plugin = plugin;
        this.logger = plugin.getLogger();
        this.passwordFile = new File("plugins/auth-plugin/passwords.txt");
        this.configFile = new File("plugins/auth-plugin/config.yml");
        this.allowedOfflinePlayers = new ArrayList<>();
        this.denyMessage = "§c对不起，该用户不允许离线登录！请联系管理员";
        loadConfig();
        loadPasswords();
    }

    private void loadConfig() {
        try {
            // 确保目录存在
            configFile.getParentFile().mkdirs();
            
            // 如果配置文件不存在，从资源中复制
            if (!configFile.exists()) {
                try (InputStream in = plugin.getClass().getResourceAsStream("/config.yml")) {
                    Files.copy(in, configFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                }
            }

            // 加载配置
            ConfigurationNode root = YAMLConfigurationLoader.builder()
                .setFile(configFile)
                .build()
                .load();

            List<String> players = new ArrayList<>();
            for (ConfigurationNode node : root.getNode("allowed-offline-players").getChildrenList()) {
                players.add(node.getString());
            }
            allowedOfflinePlayers = players;
            
            denyMessage = root.getNode("deny-message").getString(denyMessage);
        } catch (Exception e) {
            logger.error("无法加载配置文件", e);
        }
    }

    public boolean canPlayerJoin(Player player) {
        // 如果是正版登录，直接允许
        if (player.isOnlineMode()) {
            logger.info("正版玩家 " + player.getUsername() + " 正在连接...");
            return true;
        }
        
        // 检查离线玩家是否在允许列表中
        boolean allowed = allowedOfflinePlayers.contains(player.getUsername());
        if (!allowed) {
            logger.info("离线玩家 " + player.getUsername() + " 尝试连接但不在白名单中");
        } else {
            logger.info("白名单离线玩家 " + player.getUsername() + " 正在连接...");
        }
        return allowed;
    }

    public String getDenyMessage() {
        return denyMessage;
    }

    private void loadPasswords() {
        try {
            // 确保目录存在
            passwordFile.getParentFile().mkdirs();
            
            // 如果文件不存在，创建它
            if (!passwordFile.exists()) {
                passwordFile.createNewFile();
                return;
            }

            // 读取密码文件
            try (BufferedReader reader = new BufferedReader(new FileReader(passwordFile))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    String[] parts = line.split(":");
                    if (parts.length == 3) {
                        UUID uuid = UUID.fromString(parts[0]);
                        playerPasswords.put(uuid, parts[2]);
                    }
                }
            }
        } catch (IOException e) {
            logger.error("无法加载密码文件", e);
        }
    }

    private void savePasswords() {
        try (PrintWriter writer = new PrintWriter(new FileWriter(passwordFile))) {
            for (Map.Entry<UUID, String> entry : playerPasswords.entrySet()) {
                Player player = plugin.getServer().getPlayer(entry.getKey()).orElse(null);
                if (player != null) {
                    // 格式：UUID:玩家名:密码
                    writer.println(entry.getKey() + ":" + player.getUsername() + ":" + entry.getValue());
                }
            }
        } catch (IOException e) {
            logger.error("无法保存密码文件", e);
        }
    }

    public boolean isAuthenticated(Player player) {
        return player.isOnlineMode() || authenticatedPlayers.contains(player.getUniqueId());
    }

    public boolean isRegistered(Player player) {
        return playerPasswords.containsKey(player.getUniqueId());
    }

    public boolean register(Player player, String password) {
        if (isRegistered(player)) {
            player.sendMessage(Component.text("§c你已经注册过了！"));
            return false;
        }
        
        playerPasswords.put(player.getUniqueId(), password);
        authenticatedPlayers.add(player.getUniqueId());
        savePasswords();  // 保存到文件
        
        player.sendMessage(Component.text("§a注册成功！"));
        
        // 获取生存服务器
        Optional<RegisteredServer> survivalServer = plugin.getServer().getServer("survival");
        if (survivalServer.isPresent()) {
            // 创建连接请求并���送
            player.createConnectionRequest(survivalServer.get()).fireAndForget();
            player.sendMessage(Component.text("§a正在将你传送到生存服务器..."));
        } else {
            player.sendMessage(Component.text("§c错误：找不到生存服务器，请联系管理员！"));
        }
        
        return true;
    }

    public boolean authenticate(Player player, String password) {
        if (!isRegistered(player)) {
            player.sendMessage(Component.text("§c你还没有注册！请使用 /register <密码> 注册"));
            return false;
        }

        if (password.equals(playerPasswords.get(player.getUniqueId()))) {
            authenticatedPlayers.add(player.getUniqueId());
            player.sendMessage(Component.text("§a登录成功！"));
            
            // 获取生存服务器
            Optional<RegisteredServer> survivalServer = plugin.getServer().getServer("survival");
            if (survivalServer.isPresent()) {
                // 创建连接请求并发送
                player.createConnectionRequest(survivalServer.get()).fireAndForget();
                player.sendMessage(Component.text("§a正在将你传送到生存服务器..."));
            } else {
                player.sendMessage(Component.text("§c错误：找不���生存服务器，请联系管理员！"));
            }
            
            return true;
        }
        
        player.sendMessage(Component.text("§c密码错误！"));
        return false;
    }

    public void removePlayer(Player player) {
        authenticatedPlayers.remove(player.getUniqueId());
    }

    public void reloadConfig() {
        loadConfig();
        logger.info("配置已重载");
    }
} 