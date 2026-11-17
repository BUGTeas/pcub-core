package org.pcub.core.geyser.cache;

import it.unimi.dsi.fastutil.ints.Int2IntLinkedOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.geysermc.geyser.inventory.GeyserItemStack;
import org.geysermc.geyser.item.hashing.DataComponentHashers;
import org.geysermc.geyser.item.hashing.MinecraftHashEncoder;
import org.geysermc.geyser.item.hashing.MinecraftHasher;
import org.geysermc.geyser.item.hashing.RegistryHasher;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.mcprotocollib.protocol.data.game.item.HashedStack;
import org.geysermc.mcprotocollib.protocol.data.game.item.ItemStack;
import org.geysermc.mcprotocollib.protocol.data.game.item.component.CustomModelData;
import org.geysermc.mcprotocollib.protocol.data.game.item.component.DataComponentType;
import org.geysermc.mcprotocollib.protocol.data.game.item.component.DataComponentTypes;
import org.geysermc.mcprotocollib.protocol.data.game.item.component.DataComponents;
import org.pcub.core.geyser.translator.AdvancedItemTranslator;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.IntSupplier;

import static org.pcub.core.common.PCUBCore.logger;

public class ItemHashCache {
    public static ItemHashCache INSTANCE;
    private final static int TO_REMOVE = 0;
    private final static int NOTHING = -1;
    // <modded/added, original>
    private final Int2IntLinkedOpenHashMap cache;
    private final IntArrayList markDyedCache;






    public HashedStack getOrigin(@NonNull HashedStack hashedStack) {
        // 从缓存获取原始
        Map<DataComponentType<?>, Integer> addedComponents = hashedStack.addedComponents();
        Integer hashedBundleCont = addedComponents.get(DataComponentTypes.BUNDLE_CONTENTS);
        int originBundleCont = hashedBundleCont == null ? NOTHING : getOrigin(hashedBundleCont);
        Integer hashedCMD = addedComponents.get(DataComponentTypes.CUSTOM_MODEL_DATA);
        int originCMD = hashedCMD == null ? NOTHING : getOrigin(hashedCMD);

        if (originBundleCont == NOTHING && originCMD == NOTHING) {
            return null;
        }

        Map<DataComponentType<?>, Integer> newAddedComp = new HashMap<>(addedComponents);
        if (originBundleCont != NOTHING) {
            newAddedComp.put(DataComponentTypes.BUNDLE_CONTENTS, originBundleCont);
            logger().debug("从缓存 恢复原有收纳袋组件");
        }

        if (originCMD == TO_REMOVE) {
            newAddedComp.remove(DataComponentTypes.CUSTOM_MODEL_DATA);
            logger().debug("从缓存 移除增加的 CMD 组件");
        } else if (originCMD != NOTHING) {
            newAddedComp.put(DataComponentTypes.CUSTOM_MODEL_DATA, originCMD);
            logger().debug("从缓存 恢复原有 CMD 组件");
        }

        // 移除增加的染色组件
        if (originCMD != NOTHING) {
            boolean haveNewDyed;
            synchronized (markDyedCache) { // TODO: 实现线程安全的缓存读写
                haveNewDyed = markDyedCache.contains(hashedCMD.intValue());
            }
            if (haveNewDyed) {
                newAddedComp.remove(DataComponentTypes.DYED_COLOR);
                logger().debug("从缓存 移除增加的染色组件");
            }
        }

        return new HashedStack(hashedStack.id(), hashedStack.count(), newAddedComp, hashedStack.removedComponents());
    }

    public int getOrigin(int hashed) {
        synchronized (cache) { // TODO: 实现线程安全的缓存读写
            return cache.get(hashed);
        }
    }






    /**
     * 获取物品中，被处理过的组件的原始数据，并缓存其校验值
     * @param appliedItem 处理后的物品
     */
    public void put(GeyserSession session, @NonNull GeyserItemStack appliedItem) {
        if (appliedItem.getComponents() != null) {
            put(session, appliedItem.getComponents());
        }
    }

    /**
     * 获取物品中，被处理过的组件的原始数据，并缓存其校验值
     * @param appliedItem 处理后的物品
     */
    public void put(GeyserSession session, @NonNull ItemStack appliedItem) {
        if (appliedItem.getDataComponentsPatch() != null) {
            put(session, appliedItem.getDataComponentsPatch());
        }
    }

    /**
     * 获取组件集当中，被处理过的组件的原始数据，并缓存其校验值
     * @param appliedComponents 处理后的组件集
     */
    public void put(GeyserSession session, @NonNull DataComponents appliedComponents) {
        List<ItemStack> bundleContents = appliedComponents.get(DataComponentTypes.BUNDLE_CONTENTS);
        if (bundleContents != null && !bundleContents.isEmpty()) {
            List<ItemStack> originBundleCont = AdvancedItemTranslator.restoreFrom(bundleContents);
            if (originBundleCont != null) {
                put(session, RegistryHasher.ITEM_STACK.list(), bundleContents, originBundleCont);
            }
        }

        CustomModelData cmd = appliedComponents.get(DataComponentTypes.CUSTOM_MODEL_DATA);
        CustomModelData originCMD = cmd != null ? AdvancedItemTranslator.restoreFrom(cmd) : null;
        if (originCMD != cmd) {
            int hashed = put(session, DataComponentTypes.CUSTOM_MODEL_DATA, cmd, originCMD);
            if (cmd.strings().contains(AdvancedItemTranslator.CMD_DYED_TAG)) {
                markDyed(hashed);
            }
        }
    }






    /**
     * @param applied 处理后的组件
     * @param origin 原始组件 (为空则表示新建，还原时删除)
     * @return 处理后的组件的校验值
     */
    public <T> int put(GeyserSession session, DataComponentType<T> componentType, T applied, T origin) {
        return put(session, DataComponentHashers.hasher(componentType), applied, origin);
    }

    public <T> int put(GeyserSession session, MinecraftHasher<T> hasher, T applied, T origin) {
        MinecraftHashEncoder encoder = new MinecraftHashEncoder(session.javaRegistries());
        int appliedHash = hasher.hash(applied, encoder).asInt();
        if (origin == null) {
            put(appliedHash, 0);
        } else {
            computeIfAbsent(appliedHash, () -> hasher.hash(origin, encoder).asInt());
        }
        return appliedHash;
    }

    /**
     * @param hashed 处理后的组件的校验值
     * @param originSupplier 解析原始组件的校验值，仅此前未存储时解析 (返回 0 则表示新建，还原时删除)
     */
    public void computeIfAbsent(int hashed, IntSupplier originSupplier) {
        boolean originFound;
        int origin;
        synchronized (cache) { // TODO: 实现线程安全的缓存读写
            int originGet = cache.remove(hashed);
            if (originFound = originGet != cache.defaultReturnValue()) {
                cache.put(hashed, origin = originGet);
            } else {
                cache.put(hashed, origin = originSupplier.getAsInt());
                checkSize();
            }
        }
        logger().debug(() -> (originFound ? "续期 " : "新增 ") + hashed + " -> " + (origin == 0 ? "删除" : origin));
    }

    /**
     * @param hashed 处理后的组件的校验值
     * @param origin 原始组件的校验值 (为 0 则表示新建，还原时删除)
     */
    public void put(int hashed, int origin) {
        boolean originFound;
        synchronized (cache) { // TODO: 实现线程安全的缓存读写
            originFound = cache.remove(hashed) != cache.defaultReturnValue();
            cache.put(hashed, origin);
            if (!originFound) {
                checkSize();
            }
        }
        logger().debug(() -> (originFound ? "续期 " : "新增 ") + hashed + " -> " + (origin == 0 ? "删除" : origin));
    }






    /**
     * 标记添加有自定义染色的物品的 CMD 组件，还原校验值时会删除移除对应物品的染色组件
     * @param hashed 处理后的 CMD 组件的校验值
     */
    public void markDyed(int hashed) {
        boolean originFound;
        synchronized (markDyedCache) { // TODO: 实现线程安全的缓存读写
            originFound = markDyedCache.contains(hashed);
            if (originFound) {
                markDyedCache.rem(hashed);
            }
            markDyedCache.add(hashed);
            if (markDyedCache.size() > 512) {
                logger().warning("清理过量的染色标记缓存");
                markDyedCache.removeFirst();
            }
        }
        logger().debug(() -> (originFound ? "续期" : "新增") + "染色标记 " + hashed);
    }






    private void checkSize() {
        if (cache.size() > 512) {
            logger().warning("清理过量的 CMD 缓存");
            cache.removeFirstInt();
        }
    }

    public static void mainInstance() {
        INSTANCE = new ItemHashCache();
    }

    public ItemHashCache() {
        cache = new Int2IntLinkedOpenHashMap();
        cache.defaultReturnValue(NOTHING);
        markDyedCache = new IntArrayList();
    }
}
