package org.pcub.core.geyser.injector;

import com.google.common.collect.MultimapBuilder;
import com.google.common.collect.SetMultimap;
import com.google.common.collect.SortedSetMultimap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import net.kyori.adventure.key.Key;
import org.cloudburstmc.nbt.NbtMap;
import org.cloudburstmc.nbt.NbtMapBuilder;
import org.cloudburstmc.protocol.bedrock.data.definitions.ItemDefinition;
import org.cloudburstmc.protocol.bedrock.data.definitions.SimpleItemDefinition;
import org.geysermc.geyser.item.GeyserCustomMappingData;
import org.geysermc.geyser.item.type.Item;
import org.geysermc.geyser.registry.Registries;
import org.geysermc.geyser.registry.type.ItemMapping;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

import static org.pcub.core.common.PCUBCore.logger;

public class CustomItemRegistryModifier {
    // 物品遍历执行（读取、修改）
    @FunctionalInterface
    public interface ItemConsumer {
        void accept(int protocolVersion, GeyserCustomMappingData customMapping, Modification modification);
    }

    // 所有修改通过此类发出请求
    public static class Modification {
        private final NbtMap componentData;
        private NbtMapBuilder componentsBuilder = null;

        /// [数据格式说明](https://minecraft.wiki/w/Bedrock_Edition_protocol#Item_Component)
        // 获取物品组件
        public NbtMap getComponents() {
            return componentData.getCompound("components");
        }
        // 设置物品组件
        public void modifyComponents(Function<NbtMap, NbtMapBuilder> function) {
            componentsBuilder = function.apply(getComponents());
        }
        public void setComponentsBuilder(NbtMapBuilder builder) {
            componentsBuilder = builder;
        }

        public Modification(NbtMap componentData) {
            this.componentData = componentData;
        }
    }

    private static class DefinitionReplacer {
        SetMultimap<Key, GeyserCustomMappingData> oldItemDefinitions = null;
        Map<GeyserCustomMappingData, GeyserCustomMappingData> newItemDefinitions = null; // <old, new>

        private void put(Key key, GeyserCustomMappingData oldMapping, GeyserCustomMappingData newMapping) {
            if (oldItemDefinitions == null) {
                oldItemDefinitions = MultimapBuilder.hashKeys().hashSetValues().build();
                newItemDefinitions = new HashMap<>(); // <old, new>
            }
            oldItemDefinitions.put(key, oldMapping);
            newItemDefinitions.put(oldMapping, newMapping);
        }

        private void applyTo(SetMultimap<Key, GeyserCustomMappingData> customItemDefinitions) {
            if (oldItemDefinitions != null) {
                oldItemDefinitions.forEach((key, customMapping) -> {
                    customItemDefinitions.remove(key, customMapping);
                    customItemDefinitions.put(key, newItemDefinitions.get(customMapping));
                });
            }
        }
    }

    public static void forItemMapping(int protocolVersion, ItemMapping itemMapping,
                                      Int2ObjectMap<ItemDefinition> registry, ItemConsumer consumer) {
        SetMultimap<Key, GeyserCustomMappingData> customItemDefinitions = itemMapping.getCustomItemDefinitions();
        if (customItemDefinitions == null) {
            return;
        }

        DefinitionReplacer definitionReplacer = new DefinitionReplacer();
        customItemDefinitions.forEach((key, customMapping) -> {
            ItemDefinition itemDefinition = customMapping.itemDefinition();
            NbtMap componentData = itemDefinition.getComponentData();
            Modification modification = new Modification(componentData);
            consumer.accept(protocolVersion, customMapping, modification);

            // 如果 components 被改变，则重新构造 ItemDefinition
            if (modification.componentsBuilder != null) {
                ItemDefinition newItemDefinition = new SimpleItemDefinition(
                        itemDefinition.getIdentifier(), itemDefinition.getRuntimeId(), itemDefinition.getVersion(),
                        itemDefinition.isComponentBased(), componentData.toBuilder()
                        .putCompound("components", modification.componentsBuilder.build()).build());

                registry.put(itemDefinition.getRuntimeId(), newItemDefinition);
                definitionReplacer.put(key, customMapping, new GeyserCustomMappingData(
                        customMapping.definition(), newItemDefinition, customMapping.integerId()));
            }
        });
        definitionReplacer.applyTo(customItemDefinitions);
    }

    public static void forJavaItem(Item javaItem, ItemConsumer consumer) {
        Registries.ITEMS.get().forEach((protocolVersion, itemMappings) -> {
            forItemMapping(protocolVersion, itemMappings.getMapping(javaItem),
                    itemMappings.getItemDefinitions(), consumer);
        });
    }

    public static void forJavaIdentifier(String javaIdentifier, ItemConsumer consumer) {
        Registries.ITEMS.get().forEach((protocolVersion, itemMappings) -> {
            forItemMapping(protocolVersion, Objects.requireNonNull(itemMappings.getMapping(javaIdentifier)),
                    itemMappings.getItemDefinitions(), consumer);
        });
    }

    public static void forEach(ItemConsumer consumer) {
        Registries.ITEMS.get().forEach((protocolVersion, itemMappings) -> {
            for (ItemMapping itemMapping : itemMappings.getItems()) {
                forItemMapping(protocolVersion, itemMapping, itemMappings.getItemDefinitions(), consumer);
            }
        });
    }

    public static void dump(){
        StringBuilder info = new StringBuilder();
        try {
            Registries.ITEMS.get().forEach((version, itemMappings) -> {
                info.append('\n').append(version.toString());
                for (ItemMapping itemMapping : itemMappings.getItems()) {
                    SortedSetMultimap<Key, GeyserCustomMappingData> customItemDefinitions = itemMapping.getCustomItemDefinitions();
                    if (customItemDefinitions == null || customItemDefinitions.isEmpty()) {
                        continue;
                    }
                    info.append("\n  ").append(itemMapping.getJavaItem().javaIdentifier());
                    customItemDefinitions.keySet().forEach(key -> {
                        info.append("\n    ").append(key);
                        customItemDefinitions.get(key).forEach(customMapping -> {
                            // 测试输出
                            info.append("\n      ").append(customMapping.itemDefinition().getComponentData()
                                    .toString().replaceAll("\n *", " "));
                        });
                    });
                }
            });
        } finally {
            logger().debug(info.toString());
        }
    }
}
