package org.pcub.core.geyser.injector.protocol.java;

import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.geyser.translator.protocol.PacketTranslator;
import org.geysermc.mcprotocollib.network.packet.Packet;
import org.geysermc.mcprotocollib.protocol.data.game.inventory.VillagerTrade;
import org.geysermc.mcprotocollib.protocol.data.game.item.component.DataComponents;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.inventory.ClientboundMerchantOffersPacket;
import org.pcub.core.geyser.translator.AdvancedItemTranslator;

import java.lang.reflect.Field;

import static org.pcub.core.common.PCUBCore.logger;
import static org.pcub.core.geyser.translator.AdvancedItemTranslator.getItemName;

public class JavaMerchantOffersInjector extends PacketTranslator<ClientboundMerchantOffersPacket> {

    PacketTranslator<ClientboundMerchantOffersPacket> origin;

    @Override
    public void translate(GeyserSession session, ClientboundMerchantOffersPacket packet) {
        logger().debug("\t\tClientboundMerchantOffersPacket");

        for (VillagerTrade offer : packet.getOffers()) {
            // 交易次数上限溢出修复
            int uses = offer.getUses();
            int maxUses = offer.getMaxUses();
            if ((maxUses - 2147483647) > uses) {
                try {
                    Field usesField = VillagerTrade.class.getDeclaredField("uses");
                    usesField.setAccessible(true);
                    Field maxUsesField = VillagerTrade.class.getDeclaredField("maxUses");
                    maxUsesField.setAccessible(true);
                        usesField.set(offer, uses + (maxUses - 2147483647) - uses);
                        logger().debug(() -> "修正交易次数上限溢出：uses = " + uses + " -> " + (uses + (maxUses - 2147483647) - uses));
                } catch (NoSuchFieldException | IllegalAccessException e) {
                    logger().warning("交易次数上限修复失败：" + e);
                }
            }

            logger().debug(() -> getItemName(offer.getItemCostA().itemId()) + "\t" +
                    (offer.getItemCostB() != null ? getItemName(offer.getItemCostB().itemId()) : "null") + "\t" +
                    getItemName(offer.getResult()));
            // 材料
            VillagerTrade.ItemCost costA = offer.getItemCostA();
            AdvancedItemTranslator.apply(session, costA.itemId(), costA.count(), new DataComponents(costA.components()), true);
            VillagerTrade.ItemCost costB = offer.getItemCostB();
            if (costB != null) {
                AdvancedItemTranslator.apply(session, costB.itemId(), costB.count(), new DataComponents(costB.components()), true);
            }
            // 目标
            AdvancedItemTranslator.apply(session, offer.getResult(), true);
        }

        origin.translate(session, packet);
    }

    @SuppressWarnings("unchecked")
    public JavaMerchantOffersInjector(PacketTranslator<? extends Packet> origin) {
        this.origin = (PacketTranslator<ClientboundMerchantOffersPacket>) origin;
    }
}
