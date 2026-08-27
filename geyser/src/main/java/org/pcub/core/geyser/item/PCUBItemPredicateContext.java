package org.pcub.core.geyser.item;

import it.unimi.dsi.fastutil.ints.IntComparators;
import it.unimi.dsi.fastutil.ints.IntList;
import org.geysermc.geyser.api.predicate.context.item.ChargedProjectile;
import org.geysermc.geyser.api.predicate.context.item.ItemPredicateContext;
import org.geysermc.geyser.api.util.Identifier;
import org.geysermc.mcprotocollib.protocol.data.game.item.component.DataComponents;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.List;

import static org.pcub.core.common.PCUBCore.logger;

public class PCUBItemPredicateContext implements ItemPredicateContext {
    private final ItemPredicateContext geyserContext;
    private IntList offsetIndexes;

    @Override
    public int count() {
        return geyserContext.count();
    }

    @Override
    public int maxStackSize() {
        return geyserContext.maxStackSize();
    }

    @Override
    public int damage() {
        return geyserContext.damage();
    }

    @Override
    public int maxDamage() {
        return geyserContext.maxDamage();
    }

    @Override
    public boolean hasFishingRodCast() {
        return geyserContext.hasFishingRodCast();
    }

    @Override
    public boolean unbreakable() {
        return geyserContext.unbreakable();
    }

    @Override
    public float bundleFullness() {
        return geyserContext.bundleFullness();
    }

    @Override
    public @Nullable Identifier trimMaterial() {
        return geyserContext.trimMaterial();
    }

    @Override
    public @NonNull List<ChargedProjectile> chargedProjectiles() {
        return geyserContext.chargedProjectiles();
    }

    @Override
    public @NonNull List<Identifier> components() {
        return geyserContext.components();
    }

    @Override
    public boolean customModelDataFlag(int index) {
        return geyserContext.customModelDataFlag(index);
    }

    @Override
    public @Nullable String customModelDataString(int index) {
        // 假设已经插入了特殊条件所需的标签
        for (int offset = offsetIndexes.size(); offset >= 1; offset --) {
            if (offsetIndexes.getInt(offset - 1) < index) {
                return geyserContext.customModelDataString(index - offset);
            }
        }
        return geyserContext.customModelDataString(index);
    }

    @Override
    public float customModelDataFloat(int index) {
        return geyserContext.customModelDataFloat(index);
    }

    @Override
    public @NonNull Identifier dimension() {
        return geyserContext.dimension();
    }

    public void sortAndSetOffsetIndexes(IntList offsetIndexes) {
        offsetIndexes.sort(IntComparators.NATURAL_COMPARATOR);
        this.offsetIndexes = offsetIndexes;
    }

    public PCUBItemPredicateContext(ItemPredicateContext geyserContext, DataComponents components) {
        this.geyserContext = geyserContext;
    }
}
