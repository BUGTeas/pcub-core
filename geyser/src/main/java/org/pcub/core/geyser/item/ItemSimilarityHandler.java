package org.pcub.core.geyser.item;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.geysermc.geyser.api.predicate.MinecraftPredicate;

/**
 * 相似度匹配处理器，每个物品实例对应一个处理器实例
 */
public interface ItemSimilarityHandler {

    /**
     * 记录当前物品实例可触及的匹配项
     * @param predicate 匹配项的占位符谓词
     */
    void selectPredicate(MinecraftPredicate<?> predicate);

    /**
     * 得出与当前物品实例最相似的匹配项
     * @return 匹配项的占位符谓词
     */
    @Nullable
    MinecraftPredicate<?> getClosestPredicate();
}
