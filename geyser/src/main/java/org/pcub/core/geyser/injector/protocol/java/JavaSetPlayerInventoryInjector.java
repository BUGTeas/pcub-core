package org.pcub.core.geyser.injector.protocol.java;

import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.geyser.translator.protocol.PacketTranslator;
import org.geysermc.mcprotocollib.network.packet.Packet;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.inventory.ClientboundSetPlayerInventoryPacket;

import static org.pcub.core.common.PCUBCore.logger;

public class JavaSetPlayerInventoryInjector extends PacketTranslator<ClientboundSetPlayerInventoryPacket> {

    PacketTranslator<ClientboundSetPlayerInventoryPacket> origin;

    @Override
    public void translate(GeyserSession session, ClientboundSetPlayerInventoryPacket packet) {
        logger().info("\n\n\n\n\n\nClientboundSetPlayerInventoryPacket\n\n\n\n"); // TODO test only

        origin.translate(session, packet);
    }

    @SuppressWarnings("unchecked")
    public JavaSetPlayerInventoryInjector(PacketTranslator<? extends Packet> origin) {
        this.origin = (PacketTranslator<ClientboundSetPlayerInventoryPacket>) origin;
    }
}
