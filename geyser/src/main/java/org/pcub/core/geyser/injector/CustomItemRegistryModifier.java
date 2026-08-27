package org.pcub.core.geyser.injector;

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

import java.lang.reflect.Field;
import java.util.Objects;
import java.util.SortedSet;

import static org.pcub.core.common.PCUBCore.logger;

public class CustomItemRegistryModifier {
    // 物品遍历执行（读取、修改）
    @FunctionalInterface
    public interface ItemFunction {
        void run(int protocolVersion, GeyserCustomMappingData customMapping, Modification modification);
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
        SortedSetMultimap<Key, GeyserCustomMappingData> customItemDefinitions = itemMapping.getCustomItemDefinitions();

        if (customItemDefinitions == null) {
            return;
        }

        for (Key key : customItemDefinitions.keys()) {
            SortedSet<GeyserCustomMappingData> customMappings = customItemDefinitions.get(key);
            for (GeyserCustomMappingData customMapping : customMappings) {
                ItemDefinition itemDefinition = customMapping.itemDefinition();
                String identifier = itemDefinition.getIdentifier();
                NbtMap componentData = itemDefinition.getComponentData();
                NbtMapBuilder compDataBuilder = null;
                Modification modification = new Modification();
                function.run(protocolVersion, customMapping, modification);

                if (modification.identifier != null) {
                    identifier = modification.identifier;
                    customIdMappings.put(itemDefinition.getRuntimeId(), identifier);
                    compDataBuilder = componentData.toBuilder().putString("name", identifier);
                }
                if (modification.componentsBuilder != null) {
                    compDataBuilder = (compDataBuilder != null ? compDataBuilder : componentData.toBuilder())
                            .putCompound("components", modification.componentsBuilder.build());
                }
                // 如果 components 被改变，则重新构造 ItemDefinition
                if (compDataBuilder != null) {
                    ItemDefinition newItemDefinition = new SimpleItemDefinition(
                            identifier, itemDefinition.getRuntimeId(),
                            itemDefinition.getVersion(), itemDefinition.isComponentBased(),
                            compDataBuilder.build());
                    // TODO: 需要测试
                    try {
                        Class<?> mappingClass = customMapping.getClass();
                        Field definitionField = mappingClass.getDeclaredField("itemDefinition");
                        definitionField.setAccessible(true);
                        definitionField.set(newItemDefinition, customMappings);

                        registry.put(itemDefinition.getRuntimeId(), newItemDefinition);
                    } catch (Exception e) {
                        logger().error("基岩端物品组件修改失败", e);
                    }
                }
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
                if (itemMapping.getCustomItemDefinitions() != null && !itemMapping.getCustomItemDefinitions().isEmpty())
                    logger().info("   " + itemMapping.getJavaItem().javaIdentifier());
                itemMapping.getCustomItemDefinitions().forEach((key, mappingData) -> {
                    NbtMap componentData = mappingData.itemDefinition().getComponentData();
                    // 测试输出
                    logger().info("      " + key + " " + componentData.toString().replaceAll("\n *", " "));
                });
            }
        });
    }
}
