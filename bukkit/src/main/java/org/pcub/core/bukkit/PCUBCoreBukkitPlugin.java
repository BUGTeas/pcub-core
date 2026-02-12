package org.pcub.core.bukkit;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import org.geysermc.geyser.api.GeyserApi;
import org.geysermc.geyser.api.event.EventRegistrar;
import org.geysermc.geyser.api.event.lifecycle.GeyserPostInitializeEvent;
import org.pcub.core.common.PCUBCore;
import org.pcub.core.common.PCUBCoreLogger;
import org.pcub.core.geyser.GeyserHandler;

public class PCUBCoreBukkitPlugin extends JavaPlugin implements EventRegistrar {
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
            GeyserApi geyserApi = null;
            try {
                geyserApi = GeyserApi.api();
            } catch (Exception e) {
                logger.error("Geyser 接口加载失败", e);
            }
            if (geyserApi != null) {
                GeyserApi finalGeyserApi = geyserApi;
                geyserApi.eventBus().subscribe(this, GeyserPostInitializeEvent.class, event -> {
                    logger.info("");
                    logger.info("PCUB 核心组件 v" + getDescription().getVersion() + ": 加载 Geyser 功能");
                    logger.info("--------------------------------------------------");
                    GeyserHandler.init(finalGeyserApi);
                });
            }
        } else {
            logger.warning("在未安装 Geyser 插件的服务端中运行，功能将受到限制。");
        }
    }
}