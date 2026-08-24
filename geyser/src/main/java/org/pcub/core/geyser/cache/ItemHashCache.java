package org.pcub.core.geyser.cache;

import it.unimi.dsi.fastutil.ints.Int2IntLinkedOpenHashMap;
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
    // <modded/added, original>
    private Int2IntLinkedOpenHashMap cache;






    public HashedStack getOrigin(@NonNull HashedStack hashedStack) {
        // 从缓存获取原始
        Integer hashedBundleCont = hashedStack.addedComponents().get(DataComponentTypes.BUNDLE_CONTENTS);
        int originBundleCont = hashedBundleCont == null ? -1 : getOrigin(hashedBundleCont);
        Integer hashedCMD = hashedStack.addedComponents().get(DataComponentTypes.CUSTOM_MODEL_DATA);
        int originCMD = hashedCMD == null ? -1 : getOrigin(hashedCMD);

        if (originBundleCont == -1 && originCMD == -1) {
            return null;
        }

        logger().debug("从缓存恢复组件校验值");
        Map<DataComponentType<?>, Integer> newAddedComp = new HashMap<>(hashedStack.addedComponents());
        if (originBundleCont != -1) {
            newAddedComp.put(DataComponentTypes.BUNDLE_CONTENTS, originBundleCont);
        }
        if (originCMD == 0) {
            newAddedComp.remove(DataComponentTypes.CUSTOM_MODEL_DATA);
        } else if (originCMD != -1) {
            newAddedComp.put(DataComponentTypes.CUSTOM_MODEL_DATA, originCMD);
        }
        return new HashedStack(hashedStack.id(), hashedStack.count(), newAddedComp, hashedStack.removedComponents());
    }

    public int getOrigin(int hashed) {
        synchronized (cache) { // TODO: 实现线程安全的缓存读写
            return cache.get(hashed);
        }
    }






    /**
     * 获取物品中，被处理过的组件的原始数据，并将其hash缓存
     * @param appliedItem 已经处理过的物品
     */
    public void put(GeyserSession session, @NonNull GeyserItemStack appliedItem) {
        if (appliedItem.getComponents() != null) {
            put(session, appliedItem.getComponents());
        }
    }

    /**
     * 获取物品中，被处理过的组件的原始数据，并将其hash缓存
     * @param appliedItem 已经处理过的物品
     */
    public void put(GeyserSession session, @NonNull ItemStack appliedItem) {
        if (appliedItem.getDataComponentsPatch() != null) {
            put(session, appliedItem.getDataComponentsPatch());
        }
    }

    /**
     * 获取组件集中，被处理过的组件的原始数据，并将其hash缓存
     * @param appliedComponents 已经处理过的组件集
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
            put(session, DataComponentTypes.CUSTOM_MODEL_DATA, cmd, originCMD);
        }
    }






    /**
     * @param applied 已经处理过的数据
     * @param origin 原始数据 (为空则解析为 0)
     * @return 原有或新解析的原始值
     */
    public <T> void put(GeyserSession session, DataComponentType<T> componentType, T applied, T origin) {
        put(session, DataComponentHashers.hasher(componentType), applied, origin);
    }

    public <T> void put(GeyserSession session, MinecraftHasher<T> hasher, T applied, T origin) {
        MinecraftHashEncoder encoder = new MinecraftHashEncoder(session.getRegistryCache());
        put(hasher.hash(applied, encoder).asInt(),
                () -> origin == null ? 0 : hasher.hash(origin, encoder).asInt());
    }

    /**
     * @param hashed 需要获取对应原始值的新值
     * @param originIfNotExist 仅此前未存储过原始值时，才会解析
     */
    public void put(int hashed, IntSupplier originIfNotExist) {
        boolean originFound;
        int origin;
        synchronized (cache) { // TODO: 实现线程安全的缓存读写
            int originGet = cache.remove(hashed);
            if (originFound = originGet != cache.defaultReturnValue()) {
                cache.put(hashed, origin = originGet);
            } else {
                cache.put(hashed, origin = originIfNotExist.getAsInt());
                checkSize();
            }
        }
        logger().debug(() -> (originFound ? "排序 " : "新增 ") + hashed + " -> " + origin);
    }

    /**
     * @param hashed 需要获取对应原始值的新值
     * @param origin 新值对应的原始值
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
        logger().debug(() -> (originFound ? "排序 " : "新增 ") + hashed + " -> " + origin);
    }






    private void checkSize() {
        if (cache.size() > 512) {
            logger().warning("清理过量的 CMD 缓存");
            cache.removeFirstInt();
        }
    }

    public void reset() {
        cache = new Int2IntLinkedOpenHashMap();
        cache.defaultReturnValue(-1);
    }

    public static ItemHashCache mainInstance() {
        return INSTANCE = new ItemHashCache();
    }

    public ItemHashCache() {
        reset();
    }
}
