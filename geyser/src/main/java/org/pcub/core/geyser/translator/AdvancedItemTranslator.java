package org.pcub.core.geyser.translator;

import com.google.common.collect.SortedSetMultimap;
import net.kyori.adventure.key.Key;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
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
import org.pcub.core.geyser.cache.ItemHashCache;
import org.pcub.core.geyser.item.ItemSimilarityHandler;

import java.util.*;
import java.util.function.BiFunction;

import static org.pcub.core.common.PCUBCore.logger;

public class AdvancedItemTranslator {
    public static String CMD_ADDED_TAG = "pcubc_added";

    public static Map<MinecraftPredicate<?>, String> SPECIAL_PREDICATE_HOLDERS = new HashMap<>();
    // 特殊谓词，预先构造
    // public static Map<MinecraftPredicate<?>, Predicate<DataComponents>> SPECIAL_PREDICATES = new HashMap<>();
    // 提供相似度匹配处理器，每个物品实例对应一个处理器实例
    public static Map<MinecraftPredicate<?>, BiFunction<ItemPredicateContext, DataComponents, @Nullable ItemSimilarityHandler>> SIMILARITY_HANDLER_SUPPLIERS = new HashMap<>();

    /**
     * 将相似度匹配处理器和占位谓词（或所处的组合谓词）绑定
     * @param predicate 占位谓词，请确保其直接定义在映射项而非组合谓词（{@code and / or}）中，或连根将整个组合谓词传入（性能较差）
     * @param handlerSupplier 处理器实例构造器，每个物品实例构造一个处理器，当不满足需求（如所需组件不存在）时可传入 {@code null} 而不构造，以示此条件不成立
     * @param holder 匹配成功时，会使用其伪装物品的 CMD。需要和占位谓词所检查的 CMD 字符串一致，以欺骗 Geyser 匹配映射
     */
    public static void recordSimilarityPredicate(MinecraftPredicate<? super ItemPredicateContext> predicate,
                                BiFunction<ItemPredicateContext, DataComponents, @Nullable ItemSimilarityHandler> handlerSupplier, String holder) {
        SIMILARITY_HANDLER_SUPPLIERS.put(predicate, handlerSupplier);
        SPECIAL_PREDICATE_HOLDERS.put(predicate, holder);
    }



    public static CustomModelData applyNewCMD(CustomModelData origin, Collection<? extends MinecraftPredicate<?>> closestPredicates) {
        List<String> cmdStrings = new ArrayList<>(origin != null ? origin.strings() :
            List.of(CMD_ADDED_TAG)); // 新建标记
        for (var predicate : closestPredicates) {
            if (predicate instanceof CustomModelDataPredicate.StringPredicate stringPredicate) {
                cmdStrings.add(stringPredicate.index(), stringPredicate.string());
            } else {
                cmdStrings.add(SPECIAL_PREDICATE_HOLDERS.get(predicate));
            }
        }
        return (origin != null ? origin.toBuilder() :
                // 新建组件
                CustomModelData.builder().colors(List.of()).flags(List.of()).floats(List.of())
        ).strings(cmdStrings).build();
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
            for (var holder : SPECIAL_PREDICATE_HOLDERS.values()) {
                if (cmd.strings().contains(holder)) {
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

        ItemPredicateContext geyserContext = null;
        // 已经实例化的处理器，伴随此次映射检查，下一次检查会重新创建
        Map<BiFunction<ItemPredicateContext, DataComponents, ItemSimilarityHandler>, ItemSimilarityHandler> loadedHandlers = new HashMap<>();

        for (GeyserCustomMappingData customMapping : customMappings) {
            boolean needsOnlyOneMatch = customMapping.definition().predicateStrategy() == PredicateStrategy.OR;
            boolean allMatch = true;

            Map<ItemSimilarityHandler, MinecraftPredicate<? super ItemPredicateContext>> simPredicates = null;

            List<MinecraftPredicate<? super ItemPredicateContext>> predicates = customMapping.definition().predicates();

            for (var predicate : predicates) {
                // 特殊条件收集
                var simHandlerSupplier = SIMILARITY_HANDLER_SUPPLIERS.get(predicate);
                if (simHandlerSupplier != null) {
                    // 相似度匹配

                    // 首先排除固定索引值超出物品现有 CMD 列表大小的的文本谓词（专用谓词不需要索引）
                    if (predicate instanceof CustomModelDataPredicate.StringPredicate stringPredicate &&
                            stringPredicate.index() > (cmd != null ? cmd.strings().size() : 0)) {
                        if (needsOnlyOneMatch) {
                            // 或条件 跳过此谓词
                            continue;
                        } else {
                            // 与条件 直接跳过此映射
                            allMatch = false; // 若已存入临时区，则取消
                            break;
                        }
                    }

                    // 创建或获取当前物品对应实例
                    if (geyserContext == null) {
                        geyserContext = GeyserItemPredicateContext.create(session, amount, fullComponents);
                    }
                    ItemPredicateContext finalGeyserContext = geyserContext;
                    ItemSimilarityHandler simHandler = loadedHandlers.computeIfAbsent(simHandlerSupplier,
                            x -> simHandlerSupplier.apply(finalGeyserContext, fullComponents));

                    // 当物品数据不满足某些自定条件时（如所需数据不存在）可能会返回 null 而非处理器实例
                    if (simHandler == null) {
                        if (needsOnlyOneMatch) {
                            // 或条件 跳过此谓词
                            continue;
                        } else {
                            // 与条件 直接跳过此映射
                            allMatch = false; // 若已存入临时区，则取消
                            break;
                        }
                    }

                    if (simPredicates == null) {
                        simPredicates = new HashMap<>();
                    }

                    // 先暂存，稍后检查通过才添加
                    // 如果一处理器具有多个匹配项，或条件只需匹配其一，与条件则完全不能匹配
                    MinecraftPredicate<?> stagedPredicate = simPredicates.putIfAbsent(simHandler, predicate);
                    if (!needsOnlyOneMatch && stagedPredicate != null) {
                        // 与条件 已有匹配项，直接跳过此映射（提前检测以减少开销，后续检查全部谓词时也会跳过）
                        allMatch = false; // 若已存入临时区，则取消
                        break;
                    }
                }
                // TODO: 特殊谓词匹配
            }

            if (!allMatch || simPredicates == null) {
                continue;
            }

            DataComponents fakeFullComponents = fullComponents.clone();
            fakeFullComponents.put(DataComponentTypes.CUSTOM_MODEL_DATA, applyNewCMD(cmd, simPredicates.values()));
            ItemPredicateContext fakeContext = GeyserItemPredicateContext.create(session, amount, fakeFullComponents);

            // 与条件 需检查全部谓词
            if (!needsOnlyOneMatch) {
                for (var predicate : predicates) {
                    if (!predicate.test(fakeContext)) {
                        // 与条件 其一不满足，直接跳过此映射
                        allMatch = false;
                        break;
                    }
                }
            }

            if (allMatch) {
                simPredicates.forEach((handlerInstance, predicate) -> {
                    // 单个相似度匹配也可能会和其它条件组合，故或条件需在此检查
                    if (!needsOnlyOneMatch || predicate.test(fakeContext)) {
                        handlerInstance.selectPredicate(predicate);
                    }
                });
            }
        }

        // TODO: 实现有效的多个特殊映射排序，并把映射项顺序列入
        Set<MinecraftPredicate<?>> closestPredicates = null;
        for (var handler : loadedHandlers.values()) {
            MinecraftPredicate<?> predicate = handler.getClosestPredicate();
            if (predicate != null) {
                if (closestPredicates == null) {
                    closestPredicates = new HashSet<>();
                }
                closestPredicates.add(predicate);
            }
        }

        // 根据遍历得到的最近值，应用组件数据
        if (closestPredicates == null) {
            return false;
        }
        CustomModelData newCMD = applyNewCMD(cmd, closestPredicates);
        components.put(DataComponentTypes.CUSTOM_MODEL_DATA, newCMD);
        // 将原始组件转为 hash 供服务器物品校验
        ItemHashCache.INSTANCE.put(session, DataComponentTypes.CUSTOM_MODEL_DATA, newCMD, cmd);

        logger().debug(() -> "伪CMD" + Arrays.toString(newCMD.strings().toArray()));
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
        for (var holder : SPECIAL_PREDICATE_HOLDERS.values()) {
            if (cmd.strings().contains(holder)) {
                logger().debug("恢复原有 CMD 组件");
                List<String> strings = new ArrayList<>(cmd.strings());
                strings.remove(holder);
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
        if (id > 0 && id < itemList.size()) {
            Item item = itemList.get(id);
            if (item != null) {
                return item.javaIdentifier().replaceAll("^minecraft:", "mc:") + "(" + id + ")";
            }
        }
        return "?(" + id + ")";
    }
}
