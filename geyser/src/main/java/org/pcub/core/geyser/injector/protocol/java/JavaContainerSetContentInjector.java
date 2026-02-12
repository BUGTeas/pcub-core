package org.pcub.core.geyser.injector.protocol.java;

import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.geyser.translator.protocol.PacketTranslator;
import org.geysermc.mcprotocollib.network.packet.Packet;
import org.geysermc.mcprotocollib.protocol.data.game.item.ItemStack;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.inventory.ClientboundContainerSetContentPacket;
import org.pcub.core.geyser.translator.AdvancedItemTranslator;

import static org.pcub.core.common.PCUBCore.logger;
import static org.pcub.core.geyser.translator.AdvancedItemTranslator.getItemName;

public class JavaContainerSetContentInjector extends PacketTranslator<ClientboundContainerSetContentPacket> {

    PacketTranslator<ClientboundContainerSetContentPacket> origin;

    @Override
    public void translate(GeyserSession session, ClientboundContainerSetContentPacket packet) {
        logger().debug(() -> {
            StringBuilder str = new StringBuilder("\t\tClientboundContainerSetContentPacket  " + getItemName(packet.getCarriedItem()));
            for (int i = 0; i < packet.getItems().length; i++) {
                if (i % 9 == 0) {
                    str.append("\n");
                }
                ItemStack ii = packet.getItems()[i];
                str.append("\t").append(ii != null ? ii.getId() : "!");
            }
            return str.toString();
        });

        // 槽位
        for (ItemStack item : packet.getItems()) {
            if (item != null) {
                AdvancedItemTranslator.apply(session, item, true);
            }
        }
        // 指针
        if (packet.getCarriedItem() != null) {
            AdvancedItemTranslator.apply(session, packet.getCarriedItem(), true);
        }

        origin.translate(session, packet);
    }

    @SuppressWarnings("unchecked")
    public JavaContainerSetContentInjector(PacketTranslator<? extends Packet> origin) {
        this.origin = (PacketTranslator<ClientboundContainerSetContentPacket>) origin;
    }
}
