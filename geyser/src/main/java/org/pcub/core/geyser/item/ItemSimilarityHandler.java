package org.pcub.core.geyser.item;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.geysermc.geyser.api.predicate.MinecraftPredicate;
import org.geysermc.geyser.api.predicate.context.item.ItemPredicateContext;

import java.util.List;

/**
 * 相似度匹配处理器，每个物品实例对应一个处理器实例
 */
public interface ItemSimilarityHandler {

    /**
     * 记录当前物品实例可触及的映射项
     * @param predicate 映射项中适用于本处理器的占位谓词
     * @param holderPredicates 映射项中所有的占位谓词
     */
    void selectMapping(MinecraftPredicate<? super ItemPredicateContext> predicate, List<MinecraftPredicate<? super ItemPredicateContext>> holderPredicates);

    /**
     * 得出与当前物品实例最相似的映射项
     * @return 映射项中所有的占位谓词
     */
    @Nullable
    default List<MinecraftPredicate<? super ItemPredicateContext>> getClosestHolders() {
        return null;
    }
}
