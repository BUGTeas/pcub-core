package org.pcub.core.geyser;

import org.geysermc.event.subscribe.Subscribe;
import org.geysermc.geyser.api.GeyserApi;
import org.geysermc.geyser.api.event.lifecycle.GeyserPostInitializeEvent;
import org.geysermc.geyser.api.event.lifecycle.GeyserShutdownEvent;
import org.geysermc.geyser.api.extension.Extension;
import org.pcub.core.common.PCUBCore;
import org.pcub.core.common.PCUBCoreLogger;

public class PCUBCoreExtension implements Extension {
    @Subscribe
    public void onPostInitialize(GeyserPostInitializeEvent event) {
        this.logger().info("");
        this.logger().info("PCUB 核心组件 (Geyser 扩展模式) 版本: " + description().version());
        this.logger().info("--------------------------------------------------");

        PCUBCoreLogger logger = new ExtensionLogger(this);
        PCUBCore.setCommon(
                logger
        );
        PCUBCore.load();

        logger.info("扩展模式下将只加载 Geyser 功能");
        GeyserHandler.init(GeyserApi.api());
    }
    @Subscribe
    public void onShutdown(GeyserShutdownEvent event) {
//        CustomItemRegistryModifier.dump();
    }
}
