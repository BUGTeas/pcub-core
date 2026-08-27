package org.pcub.core.geyser.translator;

import com.google.common.collect.SortedSetMultimap;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import it.unimi.dsi.fastutil.objects.Object2BooleanMap;
import it.unimi.dsi.fastutil.objects.Object2BooleanOpenHashMap;
import net.kyori.adventure.key.Key;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.geysermc.geyser.api.predicate.MinecraftPredicate;
import org.geysermc.geyser.api.predicate.PredicateStrategy;
import org.geysermc.geyser.api.predicate.context.item.ItemPredicateContext;
import org.geysermc.geyser.api.predicate.item.CustomModelDataPredicate;
import org.geysermc.geyser.inventory.GeyserItemStack;
import org.geysermc.geyser.item.GeyserCustomMappingData;
import org.geysermc.geyser.item.custom.GeyserItemPredicateContext;
import org.geysermc.geyser.item.hashing.DataComponentHashers;
import org.geysermc.geyser.item.type.Item;
import org.geysermc.geyser.registry.Registries;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.mcprotocollib.protocol.data.game.item.ItemStack;
import org.geysermc.mcprotocollib.protocol.data.game.item.component.*;
import org.jspecify.annotations.Nullable;
import org.pcub.core.geyser.cache.ItemHashCache;
import org.pcub.core.geyser.item.PCUBItemPredicateContext;
import org.pcub.core.geyser.item.PotionColorMapping;

import java.util.*;
import java.util.function.Function;

import static org.pcub.core.common.PCUBCore.logger;

public class AdvancedItemTranslator {
    public static String CMD_ADDED_TAG = "pcubc_added";

    public static Map<CustomModelDataPredicate.StringPredicate, Function<DataComponents, PotionColorMapping>> advancePredicateHandlers = new HashMap<>();

    public static boolean stringPredicateEquals(CustomModelDataPredicate.@NonNull StringPredicate p1, CustomModelDataPredicate.@NonNull StringPredicate p2) {
        if (p1 == p2) {
            return true;
        }
        String string1 = p1.string();
        return (string1 != null && string1.equals(p2.string()) || p1.index() == p2.index());
    }

    /**
     * @param javaItem 原始物品堆叠
     * @return 带有额外数据的新物品堆叠
     */
    public static @Nullable ItemStack applyFrom(GeyserSession session, @NonNull ItemStack javaItem, boolean readBundle) {
        if (javaItem.getDataComponentsPatch() == null) {
            return null;
        }
        DataComponents newComp = applyFrom(session, javaItem.getId(), javaItem.getAmount(), javaItem.getDataComponentsPatch(), readBundle);
        return newComp != null ? new ItemStack(javaItem.getId(), javaItem.getAmount(), newComp) : null;
    }

    /**
     * @param components 原始组件集
     * @return 带有额外数据的新组件集
     */
    public static @Nullable DataComponents applyFrom(GeyserSession session, int javaId, int amount, @NonNull DataComponents components, boolean readBundle) {
        DataComponents newComp = components.clone();
        boolean processed = apply(session, javaId, amount, newComp, readBundle);
        return processed ? newComp : null;
    }

    public static boolean apply(GeyserSession session, @NonNull ItemStack javaItem, boolean readBundle) {
        if (javaItem.getDataComponentsPatch() == null) {
            return false;
        }
        return apply(session, javaItem.getId(), javaItem.getAmount(), javaItem.getDataComponentsPatch(), readBundle);
    }

    /**
     * 如果物品类型和组件集中的数据满足特定条件，则为其写入额外数据（仅存在于 Geyser 和客户端间，如作为介质以实现特殊条件物品映射的 CMD）
     * @param javaId 物品的数字 ID
     * @param components 目标组件集
     * @param readBundle 是否应用在收纳袋内的物品
     * @return 是否满足条件并写入
     */
    public static boolean apply(GeyserSession session, int javaId, int amount, @NonNull DataComponents components, boolean readBundle) {
        // 遍历收纳袋
        if (readBundle/* && session.getTagCache().is(ItemTag.BUNDLES, javaId)*/) {
            List<ItemStack> bundleContents = components.get(DataComponentTypes.BUNDLE_CONTENTS);
            if (bundleContents != null) {
                boolean modded = false;
                int originHash = DataComponentHashers.hash(session.getRegistryCache(), DataComponentTypes.BUNDLE_CONTENTS, bundleContents).asInt();
                for (ItemStack bundleItem : bundleContents) {
                    if (apply(session, bundleItem, true)) {
                        modded = true;
                    }
                }
                if (modded) {
                    logger().debug("收纳袋校验缓存");
                    ItemHashCache.INSTANCE.put(
                            DataComponentHashers.hash(session.getRegistryCache(), DataComponentTypes.BUNDLE_CONTENTS, bundleContents).asInt(),
                            originHash);
                }
            }
        }

        CustomModelData cmd = components.get(DataComponentTypes.CUSTOM_MODEL_DATA);

        // 检查非法标签
        if (cmd != null) {
            boolean includeTags = cmd.strings().contains(CMD_ADDED_TAG);
            for (var advancePredicate : advancePredicateHandlers.keySet()) {
                if (cmd.strings().contains(advancePredicate.string())) {
                    includeTags = true;
                    break;
                }
            }
            if (includeTags) {
                logger().warning("部分物品包含用于本扩展内部功能的标签，可能会导致物品操作异常（尤其是创造模式）");
            }
        }

        SortedSetMultimap<Key, GeyserCustomMappingData> customItemDefinitions = session.getItemMappings().getMapping(javaId).getCustomItemDefinitions();
        if (customItemDefinitions == null) {
            return false;
        }

        // 获取完整组件及模型映射列表
        DataComponents fullComponents = Registries.JAVA_ITEMS.get().get(javaId).gatherComponents(session.getComponentCache(), components);
        Key itemModel = fullComponents.get(DataComponentTypes.ITEM_MODEL);
        if (itemModel == null) {
            return false;
        }
        SortedSet<GeyserCustomMappingData> customMappings = customItemDefinitions.get(itemModel);
        if (customMappings.isEmpty()) {
            return false;
        }

        ItemPredicateContext geyserContext = GeyserItemPredicateContext.create(session, amount, fullComponents);
        PCUBItemPredicateContext context = new PCUBItemPredicateContext(geyserContext, fullComponents);
        // 已经实例化的处理器，伴随此次映射检查，下一次检查会重新创建
        Map<Function<DataComponents, PotionColorMapping>, PotionColorMapping> loadedHandlers = new HashMap<>();

        Object2BooleanMap<MinecraftPredicate<?>> calculatedPredicates = new Object2BooleanOpenHashMap<>();
        for (GeyserCustomMappingData customMapping : customMappings) {
            List<MinecraftPredicate<? super ItemPredicateContext>> predicates = customMapping.definition().predicates();

            IntList offsetIndexes = new IntArrayList(); // 用于后期实现多个特殊条件
            boolean needsOnlyOneMatch = customMapping.definition().predicateStrategy() == PredicateStrategy.OR;
            boolean allMatch = true;

            boolean hasAdvancePredicate = false;
            // 临时区
            Map<PotionColorMapping, CustomModelDataPredicate.StringPredicate> stagedAdvancedPredicates = needsOnlyOneMatch ? null : new HashMap<>();

            List<MinecraftPredicate<? super ItemPredicateContext>> nativePredicates = needsOnlyOneMatch ? null : new ArrayList<>();
            for (MinecraftPredicate<? super ItemPredicateContext> predicate : predicates) {
                // 特殊条件收集
                if (predicate instanceof CustomModelDataPredicate.StringPredicate stringPredicate) {
                    var handler = advancePredicateHandlers.get(stringPredicate);
                    if (handler != null) {
                        if (!needsOnlyOneMatch && stringPredicate.index() > (cmd != null ? cmd.strings().size() : 0)){
                            // 与条件 超过物品现有 CMD 列表大小，直接跳过此映射
                            allMatch = false; // 若已存入临时区，则取消
                            break;
                        }

                        // 创建或获取当前物品对应实例
                        var handlerInstance = loadedHandlers.computeIfAbsent(handler, x -> handler.apply(fullComponents));

                        // 临时区仅与条件可用
                        CustomModelDataPredicate.StringPredicate stagedAdvancedPredicate = needsOnlyOneMatch ? null :
                                stagedAdvancedPredicates.get(handlerInstance);
                        if (stagedAdvancedPredicate != null) {
                            if (stringPredicateEquals(stagedAdvancedPredicate, stringPredicate)) {
                                continue; // 相同条件都能满足
                            }
                            // 不能匹配相同处理器的多个不同条件，直接跳过此映射
                            allMatch = false; // 若已存入临时区，则取消
                            break;
                        }

                        if (needsOnlyOneMatch) {
                            // 或条件 直接添加
                            handlerInstance.recordPredicate(stringPredicate);
                        } else {
                            // 与条件 存入临时区，所有条件通过后才添加
                            stagedAdvancedPredicates.put(handlerInstance, stringPredicate);
                            hasAdvancePredicate = true;
                        }

                        offsetIndexes.add(stringPredicate.index());
                        continue; // 特殊条件不进行原生检查
                    }
                }
                if (!needsOnlyOneMatch) {
                    nativePredicates.add(predicate);
                }
            }

            if (!allMatch || !hasAdvancePredicate) {
                continue;
            }

            // 原生条件检查
            context.sortAndSetOffsetIndexes(offsetIndexes);
            for (var predicate : nativePredicates) {
                if (!calculatedPredicates.computeIfAbsent(predicate, x -> predicate.test(context))) {
                    allMatch = false; // 与条件其一不满足，直接跳过此映射
                    break;
                }
            }

            if (allMatch) {
                for (var handlerInstance : stagedAdvancedPredicates.keySet()) {
                    handlerInstance.recordPredicate(stagedAdvancedPredicates.get(handlerInstance));
                }
            }
        }

        CustomModelDataPredicate.StringPredicate result = null;
        // TODO: 实现有效的多个特殊映射排序
        for (var handler : loadedHandlers.keySet()) {
            result = loadedHandlers.get(handler).getClosestPredicate();
        }

        // 根据遍历得到的最近值，应用组件数据
        if (loadedHandlers.isEmpty() || result == null) {
            return false;
        }
        List<String> cmdStrings = new ArrayList<>(cmd != null ? cmd.strings() :
                List.of(CMD_ADDED_TAG)); // 新建标记
        cmdStrings.add(result.index(), result.string());
        CustomModelData newCMD = (cmd != null ? cmd.toBuilder() :
                // 新建组件
                CustomModelData.builder().colors(List.of()).flags(List.of()).floats(List.of())
        ).strings(cmdStrings).build();
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
        if (cmd.strings().contains(CMD_ADDED_TAG)) {
            logger().debug("移除增加的 CMD 组件");
            return null;
        }
        for (var advancePredicate : advancePredicateHandlers.keySet()) {
            String predicateStr = advancePredicate.string();
            if (cmd.strings().contains(predicateStr)) {
                logger().debug("恢复原有 CMD 组件");
                List<String> strings = new ArrayList<>(cmd.strings());
                strings.remove(predicateStr);
                return cmd.toBuilder().strings(strings).build();
            }
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
