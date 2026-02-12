package org.pcub.core.geyser.injector;

import it.unimi.dsi.fastutil.Pair;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import org.cloudburstmc.nbt.NbtMap;
import org.cloudburstmc.nbt.NbtMapBuilder;
import org.cloudburstmc.protocol.bedrock.data.definitions.ItemDefinition;
import org.cloudburstmc.protocol.bedrock.data.definitions.SimpleItemDefinition;
import org.geysermc.geyser.api.item.custom.CustomItemOptions;
import org.geysermc.geyser.item.type.Item;
import org.geysermc.geyser.registry.Registries;
import org.geysermc.geyser.registry.type.ItemMapping;

import java.util.List;
import java.util.Objects;

import static org.pcub.core.common.PCUBCore.logger;

public class CustomItemRegistryModifier {
    // 物品遍历执行（读取、修改）
    @FunctionalInterface
    public interface ItemFunction {
        void run(int protocolVersion, CustomItemOptions customItemOptions, ItemDefinition itemDefinition, Modification modification);
    }

    // 所有修改通过此类发出请求
    public static class Modification {
        private String identifier = null;
        private NbtMapBuilder componentsBuilder = null;
        // 设置物品ID（含命名空间）
        public void setIdentifier(String identifier) {
            this.identifier = identifier;
        }
        // 设置物品组件
        /// [数据格式说明](https://minecraft.wiki/w/Bedrock_Edition_protocol#Item_Component)
        public void setComponentsBuilder(NbtMapBuilder builder) {
            componentsBuilder = builder;
        }
    }

    public static void forItemMapping(int protocolVersion, ItemMapping itemMapping,
                                      Int2ObjectMap<ItemDefinition> registry, Int2ObjectMap<String> customIdMappings, ItemFunction function) {
        List<Pair<CustomItemOptions, ItemDefinition>> customItemOptions = itemMapping.getCustomItemOptions();

        for (int i = 0; i < customItemOptions.size(); i++) {
            Pair<CustomItemOptions, ItemDefinition> pair = customItemOptions.get(i);
            String identifier = pair.value().getIdentifier();
            NbtMap componentData = pair.value().getComponentData();
            NbtMapBuilder compDataBuilder = null;
            Modification modification = new Modification();
            function.run(protocolVersion, pair.key(), pair.value(), modification);

            if (modification.identifier != null) {
                identifier = modification.identifier;
                customIdMappings.put(pair.value().getRuntimeId(), identifier);
                compDataBuilder = componentData.toBuilder().putString("name", identifier);
            }
            if (modification.componentsBuilder != null) {
                compDataBuilder = (compDataBuilder != null ? compDataBuilder : componentData.toBuilder())
                        .putCompound("components", modification.componentsBuilder.build());
            }
            // 如果 components 被改变，则重新构造 ItemDefinition
            if (compDataBuilder != null) {
                ItemDefinition itemDefinition = new SimpleItemDefinition(
                        identifier, pair.value().getRuntimeId(),
                        pair.value().getVersion(), pair.value().isComponentBased(),
                        compDataBuilder.build());
                customItemOptions.set(i, Pair.of(pair.key(), itemDefinition));
                registry.put(pair.value().getRuntimeId(), itemDefinition);
            }
        }
    }

    public static void forJavaItem(Item javaItem, ItemFunction consumer) {
        Registries.ITEMS.get().forEach((protocolVersion, itemMappings) -> {
            forItemMapping(protocolVersion, itemMappings.getMapping(javaItem),
                    itemMappings.getItemDefinitions(), itemMappings.getCustomIdMappings(), consumer);
        });
    }

    public static void forJavaIdentifier(String javaIdentifier, ItemFunction consumer) {
        Registries.ITEMS.get().forEach((protocolVersion, itemMappings) -> {
            forItemMapping(protocolVersion, Objects.requireNonNull(itemMappings.getMapping(javaIdentifier)),
                    itemMappings.getItemDefinitions(), itemMappings.getCustomIdMappings(), consumer);
        });
    }

    public static void forEach(ItemFunction consumer) {
        Registries.ITEMS.get().forEach((protocolVersion, itemMappings) -> {
            for (ItemMapping itemMapping : itemMappings.getItems()) {
                forItemMapping(protocolVersion, itemMapping, itemMappings.getItemDefinitions(), itemMappings.getCustomIdMappings(), consumer);
            }
        });
    }

    public static void dump(){
        Registries.ITEMS.get().forEach((version, itemMappings) -> {
            logger().info(version.toString());
            for (ItemMapping itemMapping : itemMappings.getItems()){
                if (!itemMapping.getCustomItemOptions().isEmpty())
                    logger().info("   " + itemMapping.getJavaItem().javaIdentifier());
                itemMapping.getCustomItemOptions().forEach(pair -> {
                    NbtMap componentData = pair.value().getComponentData();
                    // 测试输出
                    logger().info("      " + componentData.toString().replaceAll("\n *", " "));
                });
            }
        });
    }
}
