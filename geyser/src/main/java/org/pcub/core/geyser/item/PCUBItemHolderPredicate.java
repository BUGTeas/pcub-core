package org.pcub.core.geyser.item;

import org.geysermc.geyser.api.predicate.MinecraftPredicate;
import org.geysermc.geyser.api.predicate.context.item.ItemPredicateContext;

import static org.pcub.core.common.PCUBCore.logger;

public record PCUBItemHolderPredicate(String holder) implements MinecraftPredicate<ItemPredicateContext> {

    /**
     * @param context 伪造为带有 CMD 占位符物品数据
     * @return 虚假的测试结果，用于欺骗 Geyser 应用映射
     */
    @Override
    public boolean test(ItemPredicateContext context) {
        String value = holder();
        int i = 0;
        while (true) {
            String target = context.customModelDataString(i++);
            if (target == null) {
                return false;
            }
            if (value.equals(target)) {
                return true;
            }
        }
    }

    @Override
    public MinecraftPredicate<ItemPredicateContext> negate() {
        throw new UnsupportedOperationException("特殊谓词不可反向");
    }
}
