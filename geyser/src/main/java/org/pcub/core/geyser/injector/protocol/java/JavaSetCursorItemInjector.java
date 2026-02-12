package org.pcub.core.geyser.injector.protocol.java;

import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.geyser.translator.protocol.PacketTranslator;
import org.geysermc.mcprotocollib.network.packet.Packet;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.inventory.ClientboundSetCursorItemPacket;
import org.pcub.core.geyser.translator.AdvancedItemTranslator;

import static org.pcub.core.common.PCUBCore.logger;
import static org.pcub.core.geyser.translator.AdvancedItemTranslator.getItemName;

public class JavaSetCursorItemInjector extends PacketTranslator<ClientboundSetCursorItemPacket> {

    PacketTranslator<ClientboundSetCursorItemPacket> origin;

    @Override
    public void translate(GeyserSession session, ClientboundSetCursorItemPacket packet) {
        logger().debug(() -> "\t\tClientboundSetCursorItemPacket  " + getItemName(packet.getContents()));

        if (packet.getContents() != null) {
            AdvancedItemTranslator.apply(session, packet.getContents(), true);
        }

        origin.translate(session, packet);
    }

    @SuppressWarnings("unchecked")
    public JavaSetCursorItemInjector(PacketTranslator<? extends Packet> origin) {
        this.origin = (PacketTranslator<ClientboundSetCursorItemPacket>) origin;
    }
}
