package org.pcub.core.geyser;

import org.geysermc.api.util.ApiVersion;
import org.geysermc.geyser.GeyserImpl;
import org.geysermc.geyser.api.GeyserApi;
import org.geysermc.geyser.api.event.EventRegistrar;
import org.geysermc.geyser.api.event.lifecycle.GeyserPostInitializeEvent;
import org.geysermc.geyser.api.item.custom.v2.CustomItemDefinition;
import org.geysermc.geyser.api.predicate.MinecraftPredicate;
import org.geysermc.geyser.api.predicate.context.item.ItemPredicateContext;
import org.pcub.core.geyser.cache.ItemHashCache;
import org.pcub.core.geyser.injector.Injector4Geyser;
import org.pcub.core.geyser.item.PotionColorMapping;
import org.pcub.core.geyser.listener.EventListener4Geyser;
import org.pcub.core.geyser.injector.CustomItemRegistryModifier;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.pcub.core.common.PCUBCore.logger;

public class GeyserHandler implements EventRegistrar {
    public static final String EXPECT_GEYSER_VER = "2.11.2-b1232 (git-master-32e7fe1)";
    public static final int[] EXPECT_GEYSER_API_VER = {2,11,2};

    // 供非 Geyser 扩展使用
    public static void subscribeInit(Runnable callback) {
        GeyserApi geyserApi = GeyserApi.api();
        geyserApi.eventBus().subscribe(new GeyserHandler(), GeyserPostInitializeEvent.class, event -> {
            callback.run();
            init(geyserApi);
        });
    }

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

        logger().info("注册药水颜色映射...");
        CustomItemRegistryModifier.forEach((protocolVersion, customMapping, modification) -> {
            CustomItemDefinition mappingDefinition = customMapping.definition();
            for (MinecraftPredicate<? super ItemPredicateContext> predicate : mappingDefinition.predicates()) {
                PotionColorMapping.recordPredicate(predicate);
            }
            // modification.modifyComponents(comp -> comp.toBuilder()
            //         .putCompound("item_properties",
            //                 comp.getCompound("item_properties").toBuilder()
            //                         .putString("pcub_core_test", "test")
            //                         .build()));
        });

        logger().info("介入到收包监听器...");
        Injector4Geyser.registerProtocol();

        logger().info("注册事件监听器...");
        new EventListener4Geyser(api);
    }
}
