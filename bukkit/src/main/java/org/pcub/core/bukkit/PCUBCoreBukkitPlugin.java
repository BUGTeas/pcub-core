package org.pcub.core.bukkit;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import org.pcub.core.common.PCUBCore;
import org.pcub.core.common.PCUBCoreLogger;
import org.pcub.core.geyser.GeyserHandler;

import static org.pcub.core.common.PCUBCore.logger;

public class PCUBCoreBukkitPlugin extends JavaPlugin {
    @Override
    public void onEnable() {
        getLogger().info("");
        getLogger().info("PCUB 核心组件 (服务端插件模式) 版本: " + getDescription().getVersion());
        getLogger().info("--------------------------------------------------");

        PCUBCoreLogger logger = new BukkitLogger(this);
        PCUBCore.setCommon(
                logger
        );
        PCUBCore.load();

        logger.info("检查 Geyser 功能可用性...");
        if (Bukkit.getPluginManager().getPlugin("Geyser-Spigot") != null) {
            try {
                GeyserHandler.subscribeInit(() -> {
                    logger.info("");
                    logger.info("PCUB 核心组件 v" + getDescription().getVersion() + ": 加载 Geyser 功能");
                    logger.info("--------------------------------------------------");
                });
            } catch (Exception e) {
                logger().error("Geyser 接口加载失败", e);
            }
        } else {
            logger.warning("在未安装 Geyser 插件的服务端中运行，功能将受到限制。");
        }
    }
}