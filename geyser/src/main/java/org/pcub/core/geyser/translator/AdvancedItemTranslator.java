package org.pcub.core.geyser.translator;

import it.unimi.dsi.fastutil.Pair;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.cloudburstmc.protocol.bedrock.data.definitions.ItemDefinition;
import org.geysermc.geyser.api.item.custom.CustomItemOptions;
import org.geysermc.geyser.inventory.GeyserItemStack;
import org.geysermc.geyser.item.hashing.DataComponentHashers;
import org.geysermc.geyser.item.type.Item;
import org.geysermc.geyser.registry.Registries;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.mcprotocollib.protocol.data.game.item.ItemStack;
import org.geysermc.mcprotocollib.protocol.data.game.item.component.*;
import org.jspecify.annotations.Nullable;
import org.pcub.core.geyser.cache.ItemHashCache;

import java.util.*;

import static org.pcub.core.common.PCUBCore.logger;

public class AdvancedItemTranslator {
    public static String CMD_MODDED_TAG = "pcubc_modded";
    public static String CMD_ADDED_TAG = "pcubc_added";


    /**
     * @param javaItem 原始物品堆叠
     * @return 带有额外数据的新物品堆叠
     */
    public static @Nullable ItemStack applyFrom(GeyserSession session, @NonNull ItemStack javaItem, boolean readBundle) {
        if (javaItem.getDataComponentsPatch() == null) {
            return null;
        }
        DataComponents newComp = applyFrom(session, javaItem.getId(), javaItem.getDataComponentsPatch(), readBundle);
        return newComp != null ? new ItemStack(javaItem.getId(), javaItem.getAmount(), newComp) : null;
    }

    /**
     * @param components 原始组件集
     * @return 带有额外数据的新组件集
     */
    public static @Nullable DataComponents applyFrom(GeyserSession session, int javaId, @NonNull DataComponents components, boolean readBundle) {
        DataComponents newComp = components.clone();
        boolean processed = apply(session, javaId, newComp, readBundle);
        return processed ? newComp : null;
    }

    public static boolean apply(GeyserSession session, @NonNull ItemStack javaItem, boolean readBundle) {
        if (javaItem.getDataComponentsPatch() == null) {
            return false;
        }
        return apply(session, javaItem.getId(), javaItem.getDataComponentsPatch(), readBundle);
        /*try {
            if (components == null) {
                logger().info("创建组件对象");
                Field componentField = ItemStack.class.getDeclaredField("dataComponentsPatch");
                componentField.setAccessible(true);
                components = new DataComponents(new HashMap<>());
                componentField.set(javaItem, components);
            }
        } catch (Exception e) {
            logger().warning(e.getMessage());
        }*/
    }

    /**
     * 如果物品类型和组件集中的数据满足特定条件，则为其写入额外数据（仅存在于 Geyser 和客户端间，如作为介质以实现特殊条件物品映射的 CMD）
     * @param javaId 物品的数字 ID
     * @param components 目标组件集
     * @param readBundle 是否应用在收纳袋内的物品
     * @return 是否满足条件并写入
     */
    public static boolean apply(GeyserSession session, int javaId, @NonNull DataComponents components, boolean readBundle) {
        // 遍历收纳袋
        if (readBundle/* && session.getTagCache().is(ItemTag.BUNDLES, javaId)*/) {
            List<ItemStack> bundleContents = components.get(DataComponentTypes.BUNDLE_CONTENTS);
            if (bundleContents != null) {
                boolean modded = false;
                int originHash = DataComponentHashers.hash(session, DataComponentTypes.BUNDLE_CONTENTS, bundleContents).asInt();
                for (ItemStack bundleItem : bundleContents) {
                    if (apply(session, bundleItem, true)) {
                        modded = true;
                    }
                }
                if (modded) {
                    logger().debug("收纳袋校验缓存");
                    ItemHashCache.INSTANCE.put(
                            DataComponentHashers.hash(session, DataComponentTypes.BUNDLE_CONTENTS, bundleContents).asInt(),
                            originHash);
                }
            }
        }

        List<Pair<CustomItemOptions, ItemDefinition>> customMappings = session.getItemMappings().getMapping(javaId).getCustomItemOptions();
        if (customMappings.isEmpty()) {
            return false;
        }

        CustomModelData cmd = components.get(DataComponentTypes.CUSTOM_MODEL_DATA);
        // 如果原先有定义则跳过（原对应 CMD 优先级最高）
        if (cmd != null && !cmd.floats().isEmpty()) {
            return false;
        }
        if (cmd != null && (cmd.strings().contains(CMD_MODDED_TAG) || cmd.strings().contains(CMD_ADDED_TAG))) {
            logger().warning("部分物品包含用于本扩展内部功能的标签，可能会导致物品操作异常（尤其是创造模式）");
        }

        // 获取药水颜色
        int potionColor = -1;
        PotionContents potionContents = components.get(DataComponentTypes.POTION_CONTENTS);
        if (potionContents != null) {
            potionColor = potionContents.getCustomColor();
            if (potionColor == -1) {
                // TODO: 自动颜色
            }
        }

        // 遍历已知映射配置
        int newValue = -1;
        for (Pair<CustomItemOptions, ItemDefinition> mappingTypes : customMappings) {
            OptionalInt customModelDataOption = mappingTypes.key().customModelData();
            if (customModelDataOption.isEmpty()) {
                continue;
            }
            int optionData = customModelDataOption.getAsInt();
            // 匹配相同药水颜色
            if (potionColor == optionData) {
                logger().debug("匹配到相同药水颜色");
                newValue = optionData;
            }
            // TODO：头颅映射
        }

        // 若无相同颜色则匹配邻近药水颜色
        if (potionColor != -1 && newValue == -1) {
            logger().debug("匹配到邻近药水颜色");
            // 特定颜色
            int potionR = potionColor / 65536;
            int potionG = potionColor / 256 % 256;
            int potionB = potionColor % 256;
            // 颜色，偏向的色调为最小值1，其次的色调会更大，以增加其距离，不存在的色调为最大值2
            int maxValue = Math.max(Math.max(potionR, potionG), potionB);
            double potionPercentR = 2 - (double) potionR / maxValue;
            double potionPercentG = 2 - (double) potionG / maxValue;
            double potionPercentB = 2 - (double) potionB / maxValue;
            // 重新遍历
            double colorDistanceClose = -1;
            for (Pair<CustomItemOptions, ItemDefinition> mappingTypes : customMappings) {
                OptionalInt customModelDataOption = mappingTypes.key().customModelData();
                if (customModelDataOption.isEmpty()) {
                    continue;
                }
                int optionData = customModelDataOption.getAsInt();
                // 当前项的颜色
                int optionR = optionData / 65536;
                int optionG = optionData / 256 % 256;
                int optionB = optionData % 256;
                // 基于 RGB 的欧氏距离比较（根据颜色占比加权）
                double colorDistance = Math.pow((optionR - potionR) * potionPercentR, 2) +
                        Math.pow((optionG - potionG) * potionPercentG, 2) +
                        Math.pow((optionB - potionB) * potionPercentB, 2);
                if (colorDistanceClose == -1 || colorDistanceClose > colorDistance) {
                    colorDistanceClose = colorDistance;
                    newValue = optionData;
                }
            }
        }

        // 根据遍历得到的最近值，应用组件数据
        if (newValue == -1) {
            return false;
        }
        CustomModelData.CustomModelDataBuilder cmdBuilder;
        if (cmd != null) {
            List<Float> cmdFloats = List.of((float) newValue); //todo: 需兼容 1.21.4+ 仅限无任何现有值的情况，否则需改为插入新值（后期预留）
            List<String> cmdStrings = new ArrayList<>(cmd.strings());
            cmdStrings.add(CMD_MODDED_TAG); // 附加标记
            cmdBuilder = cmd.toBuilder().floats(cmdFloats).strings(cmdStrings);
        } else {
            // 新建组件
            // todo: 需兼容 1.21.4+
            cmdBuilder = CustomModelData.builder().colors(List.of()).flags(List.of())
                    .floats(List.of((float) newValue))
                    .strings(List.of(CMD_ADDED_TAG)); // 新建标记
        }
        CustomModelData newCMD = cmdBuilder.build();
        components.put(DataComponentTypes.CUSTOM_MODEL_DATA, newCMD);
        // 将原始组件转为 hash 供服务器物品校验
        ItemHashCache.INSTANCE.put(session, DataComponentTypes.CUSTOM_MODEL_DATA, newCMD, cmd);

        return true;
    }






    public static @Nullable List<ItemStack> restoreFrom(@NonNull List<ItemStack> javaItems) {
        List<ItemStack> originJavaItems = new ArrayList<>();
        boolean updated = false;
        for (ItemStack item : javaItems) {
            ItemStack originItem = AdvancedItemTranslator.restoreFrom(item);
            originJavaItems.add(originItem != null ? originItem : item);
            if (originItem != null) {
                updated = true;
            }
        }
        return updated ? originJavaItems : null;
    }

    public static @Nullable ItemStack restoreFrom(@NonNull ItemStack javaItem) {
        DataComponents components = javaItem.getDataComponentsPatch();
        if (components == null) {
            return null;
        }
        DataComponents newComp = restoreFrom(components);
        return newComp == null ? null : new ItemStack(javaItem.getId(), javaItem.getAmount(), newComp);
    }

    public static @Nullable DataComponents restoreFrom(@NonNull DataComponents components) {
        List<ItemStack> bundleContents = components.get(DataComponentTypes.BUNDLE_CONTENTS);
        List<ItemStack> newBundleCont = (bundleContents != null && !bundleContents.isEmpty()) ? restoreFrom(bundleContents) : null;

        CustomModelData cmd = components.get(DataComponentTypes.CUSTOM_MODEL_DATA);
        CustomModelData newCMD = cmd != null ? restoreFrom(cmd) : null;

        if (newBundleCont != null || newCMD != cmd) {
            DataComponents newComp = components.clone();
            if (newBundleCont != null) {
                newComp.put(DataComponentTypes.BUNDLE_CONTENTS, newBundleCont);
            }
            if (newCMD != cmd) {
                if (newCMD != null) {
                    newComp.put(DataComponentTypes.CUSTOM_MODEL_DATA, newCMD);
                } else {
                    newComp.remove(DataComponentTypes.CUSTOM_MODEL_DATA);
                }
            }
            return newComp;
        }
        return null;
    }

    /// 需要删除则返回 null，需要更新则返回新对象，否则返回原对象
    public static @Nullable CustomModelData restoreFrom(@NonNull CustomModelData cmd) {
        logger().debug("从标签恢复组件");
        boolean modded = cmd.strings().contains(CMD_MODDED_TAG) &&
                !cmd.floats().isEmpty();
        boolean added = cmd.strings().contains(CMD_ADDED_TAG);

        if (modded) {
            List<Float> floats = new ArrayList<>(cmd.floats());
            floats.remove(0);
            List<String> strings = new ArrayList<>(cmd.strings());
            strings.remove(CMD_MODDED_TAG);
            return cmd.toBuilder().floats(floats).strings(strings).build();
        } else if (added) {
            return null;
        }
        return cmd;
    }






    public static String getItemName(ItemStack itemStack) {
        if (itemStack == null) return null;
        return getItemName(itemStack.getId());
    }

    public static String getItemName(GeyserItemStack itemStack) {
        if (itemStack == null) return null;
        return getItemName(itemStack.getJavaId());
    }

    public static String getItemName(int id) {
        List<Item> itemList = Registries.JAVA_ITEMS.get();
        if (id < 0 || id >= itemList.size()) {
            return null;
        }
        Item item = itemList.get(id);
        if (item == null) return id + "";
        return item.javaIdentifier().replaceAll("^minecraft:", "mc:") + "(" + id + ")";
    }
}
