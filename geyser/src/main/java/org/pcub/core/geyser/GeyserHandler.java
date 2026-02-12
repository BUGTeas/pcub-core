package org.pcub.core.geyser;

import org.cloudburstmc.nbt.NbtMap;
import org.geysermc.api.util.ApiVersion;
import org.geysermc.geyser.Constants;
import org.geysermc.geyser.GeyserImpl;
import org.geysermc.geyser.api.GeyserApi;
import org.geysermc.geyser.item.Items;
import org.pcub.core.geyser.cache.ItemHashCache;
import org.pcub.core.geyser.injector.Injector4Geyser;
import org.pcub.core.geyser.listener.EventListener4Geyser;
import org.pcub.core.geyser.injector.CustomItemRegistryModifier;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;

import static org.pcub.core.common.PCUBCore.logger;

public class GeyserHandler {
    public static final String EXPECT_GEYSER_VER = "2.9.0-b989 (git-master-c0c7b51)";
    public static final int[] EXPECT_GEYSER_API_VER = {2,9,0};

    public static void init(GeyserApi api) {
        ApiVersion geyserApiVer = api.geyserApiVersion();
        logger().info("Geyser 版本: " + geyserApiVer.toString());
        logger().info("本组件适配版本: " + EXPECT_GEYSER_VER);
        if (EXPECT_GEYSER_API_VER[0] > geyserApiVer.human() ||
                EXPECT_GEYSER_API_VER[1] > geyserApiVer.major() ||
                EXPECT_GEYSER_API_VER[2] > geyserApiVer.minor()) {
            logger().warning("正在运行的 Geyser 版本低于本组件适配的版本");
        } else if (EXPECT_GEYSER_API_VER[0] < geyserApiVer.human() ||
                EXPECT_GEYSER_API_VER[1] < geyserApiVer.major() ||
                EXPECT_GEYSER_API_VER[2] < geyserApiVer.minor()) {
            logger().warning("部分功能依赖对 Geyser 底层的干预，可能会因极小的版本变动而造成异常。");
        }

        ItemHashCache.mainInstance();

        logger().info("模型映射文件顺序:");
        try {
            for (Path path : Files.walk(GeyserImpl.getInstance().getBootstrap().getConfigFolder().resolve("custom_mappings"))
                    .filter(child -> child.toString().endsWith(".json"))
                    .toArray(Path[]::new)) {
                logger().info(" " + path);
            }
        } catch (Exception e) {
            logger().error("列出模型映射文件时出现问题", e);
        }

        logger().info("修改基岩端堆叠上限... (需配合服务端物品组件/插件)");
        CustomItemRegistryModifier.forEach(
                (protocolVersion, customItemOptions, itemDefinition, modification) -> {
                    String identifier = itemDefinition.getIdentifier();
                    String prefix = Constants.GEYSER_CUSTOM_NAMESPACE + ":";

                    if (identifier.startsWith(prefix + "temp_stack_")) {
                        // 去除名称前缀
                        modification.setIdentifier(prefix + identifier.substring((prefix + "temp_stack_").length()));
                        // 设置叠放上限
                        NbtMap components = itemDefinition.getComponentData().getCompound("components");
                        modification.setComponentsBuilder(components.toBuilder().putCompound("item_properties",
                                components.getCompound("item_properties").toBuilder()
                                        .putInt("max_stack_size", 64).build()));
                    }
                });
        Set.of(Items.POTION, Items.SPLASH_POTION).forEach(
                javaItem -> CustomItemRegistryModifier.forJavaItem(javaItem,
                        (protocolVersion, customItemOptions, itemDefinition, modification) -> {
                            // 设置叠放上限
                            NbtMap components = itemDefinition.getComponentData().getCompound("components");
                            modification.setComponentsBuilder(components.toBuilder().putCompound("item_properties",
                                    components.getCompound("item_properties").toBuilder()
                                            .putInt("max_stack_size", 64).build()));
                        }));

        logger().info("介入到收包监听器...");
        Injector4Geyser.registerProtocol();

        logger().info("注册事件监听器...");
        new EventListener4Geyser(api);
    }
}
