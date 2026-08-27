package org.pcub.core.geyser.item;

import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import org.geysermc.geyser.api.predicate.item.CustomModelDataPredicate;
import org.geysermc.mcprotocollib.protocol.data.game.item.component.DataComponentTypes;
import org.geysermc.mcprotocollib.protocol.data.game.item.component.DataComponents;
import org.geysermc.mcprotocollib.protocol.data.game.item.component.PotionContents;

import java.util.regex.Pattern;

import static org.pcub.core.common.PCUBCore.logger;
// TODO: 分离接口
public class PotionColorMapping {
    public static Pattern CMD_POTION_PATTERN = Pattern.compile("^pcubc_potion_color_([0-9]+)$");

    public static int getCloserPotionColor(int potionColor, Int2ObjectOpenHashMap<?> potionMappings) {
        // 匹配相同药水颜色
        if (potionMappings.containsKey(potionColor)) {
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
        for (int option : potionMappings.keySet()) {
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

    public static Object2IntOpenHashMap<CustomModelDataPredicate.StringPredicate> predicate2Color = new Object2IntOpenHashMap<>(); // <color, predicate>
    static {
        predicate2Color.defaultReturnValue(-1);
    }

    // 当前物品数据
    private int potionColor = -1;
    // 距离匹配用
    private final Int2ObjectOpenHashMap<CustomModelDataPredicate.StringPredicate> potionMappings = new Int2ObjectOpenHashMap<>(); // <color, predicate>

    public CustomModelDataPredicate.StringPredicate getClosestPredicate() {
        // logger().debug("已候选 %s 个药水颜色条件".formatted(potionMappings.size()));
        if (potionColor == -1 || potionMappings.isEmpty()) {
            return null;
        }
        int key = getCloserPotionColor(potionColor, potionMappings);
        if (key == -1) {
            return null;
        }
        return potionMappings.get(key);
    }

    public void recordPredicate(CustomModelDataPredicate.StringPredicate predicate) {
        int color = predicate2Color.getInt(predicate);
        if (color != -1) {
            potionMappings.computeIfAbsent(color, x -> predicate);
        }
    }

    public PotionColorMapping(DataComponents components) {
        PotionContents potionContents = components.get(DataComponentTypes.POTION_CONTENTS);
        if (potionContents != null) {
            potionColor = potionContents.getCustomColor();
            if (potionColor == -1) {
                // TODO: 自动颜色
            }
        }
    }
}
