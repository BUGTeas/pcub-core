package org.pcub.core.geyser.injector.protocol.java;

import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.geyser.translator.protocol.PacketTranslator;
import org.geysermc.mcprotocollib.network.packet.Packet;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.inventory.ClientboundContainerSetSlotPacket;
import org.pcub.core.geyser.translator.AdvancedItemTranslator;

import static org.pcub.core.common.PCUBCore.logger;
import static org.pcub.core.geyser.translator.AdvancedItemTranslator.getItemName;

public class JavaContainerSetSlotInjector extends PacketTranslator<ClientboundContainerSetSlotPacket> {

    PacketTranslator<ClientboundContainerSetSlotPacket> origin;

    @Override
    public void translate(GeyserSession session, ClientboundContainerSetSlotPacket packet) {
        logger().debug(() -> "\t\tClientboundContainerSetSlotPacket  %s %s".formatted(packet.getSlot(), getItemName(packet.getItem())));

        if (packet.getItem() != null) {
            AdvancedItemTranslator.apply(session, packet.getItem(), true);
        }

        origin.translate(session, packet);
    }

    @SuppressWarnings("unchecked")
    public JavaContainerSetSlotInjector(PacketTranslator<? extends Packet> origin) {
        this.origin = (PacketTranslator<ClientboundContainerSetSlotPacket>) origin;
    }
}
