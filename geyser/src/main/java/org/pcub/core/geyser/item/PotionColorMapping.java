package org.pcub.core.geyser.item;

import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntSet;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.geysermc.geyser.api.predicate.MinecraftPredicate;
import org.geysermc.geyser.api.predicate.context.item.ItemPredicateContext;
import org.geysermc.geyser.api.predicate.item.CustomModelDataPredicate;
import org.geysermc.geyser.level.EffectType;
import org.geysermc.mcprotocollib.protocol.data.game.item.component.DataComponentTypes;
import org.geysermc.mcprotocollib.protocol.data.game.item.component.DataComponents;
import org.geysermc.mcprotocollib.protocol.data.game.item.component.MobEffectInstance;
import org.geysermc.mcprotocollib.protocol.data.game.item.component.PotionContents;
import org.pcub.core.common.PCUBColor;
import org.pcub.core.geyser.item.predicate.PCUBItemHolderPredicate;
import org.pcub.core.geyser.translator.AdvancedItemTranslator;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.UnaryOperator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.pcub.core.common.PCUBCore.logger;

public class PotionColorMapping implements ItemSimilarityHandler {
    public static String CMD_POTION_PREFIX = "pcubc_potion_color_";
    public static Pattern CMD_POTION_PATTERN = Pattern.compile("^%s([0-9]+)$".formatted(CMD_POTION_PREFIX));

    public static BiFunction<ItemPredicateContext, DataComponents, @Nullable ItemSimilarityHandler> SIMILARITY_HANDLER_SUPPLIER = (geyserContext, components) -> {
        PotionContents potionContents = components.get(DataComponentTypes.POTION_CONTENTS);
        int potionColor = potionContents == null ? -1 : getPotionColor(potionContents);
        return potionColor == -1 ? null : new PotionColorMapping(potionColor);
    };

    public static int getPotionColor(PotionContents potionContents) {
        int customColor = potionContents.getCustomColor();
        if (customColor != -1) {
            logger().debug(() -> "药水颜色: 自定义 " + PCUBColor.fromInt(customColor));
            return customColor;
        }
        if (!potionContents.getCustomEffects().isEmpty()) {
            Potion vanilla = Potion.getByJavaId(potionContents.getPotionId());
            List<MobEffectInstance> effects = vanilla == null ? potionContents.getCustomEffects() : new ArrayList<>(Arrays.asList(vanilla.getJavaEffects()));
            if (vanilla != null) {
                effects.addAll(potionContents.getCustomEffects());
            }
            int totalR = 0, totalG = 0, totalB = 0;
            int total = 0;
            for (MobEffectInstance effect : effects) {
                int amplifier = effect.getDetails().getAmplifier() + 1;
                total += amplifier;
                PCUBColor effectColor = PCUBColor.fromInt(EffectType.fromJavaEffect(effect.getEffect()).getColor());
                totalR += amplifier * effectColor.red();
                totalG += amplifier * effectColor.green();
                totalB += amplifier * effectColor.blue();
            }
            PCUBColor potionColor = new PCUBColor(totalR / total, totalG / total, totalB / total);
            logger().debug("药水颜色: 混合 " + potionColor);
            return potionColor.toInt();
        }
        return -1;
    }

    public static int getCloserPotionColor(int potionColor, IntSet possibleColors) {
        // 匹配相同药水颜色
        if (possibleColors.contains(potionColor)) {
            logger().debug("匹配药水颜色 相同");
            return potionColor;
        }

        // 若无相同颜色则匹配邻近药水颜色
        int newValue = -1;
        logger().debug("匹配药水颜色 邻近");
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
        for (int option : possibleColors) {
            // 当前项的颜色
            int optionR = option / 65536;
            int optionG = option / 256 % 256;
            int optionB = option % 256;
            // 基于 RGB 的欧氏距离比较（根据颜色占比加权）
            double colorDistance = Math.pow((optionR - potionR) * potionPercentR, 2) +
                    Math.pow((optionG - potionG) * potionPercentG, 2) +
                    Math.pow((optionB - potionB) * potionPercentB, 2);
            if (colorDistanceClose == -1 || colorDistanceClose > colorDistance) {
                colorDistanceClose = colorDistance;
                newValue = option;
            }
        }
        return newValue;
    }

    public static Object2IntOpenHashMap<MinecraftPredicate<?>> HOLDER_TO_COLOR = new Object2IntOpenHashMap<>(); // <color, predicate>
    static {
        HOLDER_TO_COLOR.defaultReturnValue(-1);
    }

    public static void recordHolder(int color, MinecraftPredicate<? super ItemPredicateContext> holder, String holderString) {
        HOLDER_TO_COLOR.put(holder, color);
        AdvancedItemTranslator.recordSimilarityPredicate(SIMILARITY_HANDLER_SUPPLIER, holder, holderString);
    }

    public static boolean recordHolder(MinecraftPredicate<? super ItemPredicateContext> holder) {
        if (!(holder instanceof CustomModelDataPredicate.StringPredicate stringPredicate)) {
            return false;
        }
        String holderString = stringPredicate.string();
        if (holderString == null || stringPredicate.negated()) {
            return false;
        }
        Matcher matcher = PotionColorMapping.CMD_POTION_PATTERN.matcher(holderString);
        if (!matcher.find()) {
            return false;
        }
        recordHolder(Integer.parseInt(matcher.group(1)), holder, holderString);
        return true;
    }

    public static MinecraftPredicate<? super ItemPredicateContext> createHolder(int color, UnaryOperator<MinecraftPredicate<? super ItemPredicateContext>> compound) {
        String holderString = CMD_POTION_PREFIX + color;
        PCUBItemHolderPredicate holderPredicate = new PCUBItemHolderPredicate(holderString);
        MinecraftPredicate<? super ItemPredicateContext> predicate = compound == null ? holderPredicate : compound.apply(holderPredicate);
        recordHolder(color, predicate, holderString);
        return predicate;
    }

    public static MinecraftPredicate<? super ItemPredicateContext> createHolder(int color) {
        return createHolder(color, null);
    }






    // 当前物品数据
    private final int potionColor;
    // 距离匹配用
    private final Int2ObjectOpenHashMap<List<MinecraftPredicate<? super ItemPredicateContext>>> potionMappings = new Int2ObjectOpenHashMap<>(); // <color, predicate>

    @Override
    public List<MinecraftPredicate<? super ItemPredicateContext>> getClosestHolders() {
        // logger().debug("已候选 %s 个药水颜色条件".formatted(potionMappings.size()));
        if (potionColor == -1 || potionMappings.isEmpty()) {
            return null;
        }
        int key = getCloserPotionColor(potionColor, potionMappings.keySet());
        if (key == -1) {
            return null;
        }
        return potionMappings.get(key);
    }

    @Override
    public void selectMapping(MinecraftPredicate<? super ItemPredicateContext> predicate, List<MinecraftPredicate<? super ItemPredicateContext>> holderPredicates) {
        int color = HOLDER_TO_COLOR.getInt(predicate);
        if (color != -1) {
            potionMappings.computeIfAbsent(color, x -> holderPredicates);
        }
    }

    private PotionColorMapping(int potionColor) {
        this.potionColor = potionColor;
    }
}
