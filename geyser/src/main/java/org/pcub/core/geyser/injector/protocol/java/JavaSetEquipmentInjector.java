package org.pcub.core.geyser.injector.protocol.java;

import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.geyser.translator.protocol.PacketTranslator;
import org.geysermc.mcprotocollib.network.packet.Packet;
import org.geysermc.mcprotocollib.protocol.data.game.entity.metadata.Equipment;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.entity.ClientboundSetEquipmentPacket;
import org.pcub.core.geyser.translator.AdvancedItemTranslator;

import static org.pcub.core.common.PCUBCore.logger;
import static org.pcub.core.geyser.translator.AdvancedItemTranslator.getItemName;

public class JavaSetEquipmentInjector extends PacketTranslator<ClientboundSetEquipmentPacket> {

    PacketTranslator<ClientboundSetEquipmentPacket> origin;

    @Override
    public void translate(GeyserSession session, ClientboundSetEquipmentPacket packet) {
        logger().debug("\t\tClientboundSetEquipmentPacket");

        for (Equipment equipment : packet.getEquipment()) {
            logger().debug(() -> getItemName(equipment.getItem()));
            if (equipment.getItem() != null) {
                AdvancedItemTranslator.apply(session, equipment.getItem(), false);
            }
        }

        origin.translate(session, packet);
    }

    @SuppressWarnings("unchecked")
    public JavaSetEquipmentInjector(PacketTranslator<? extends Packet> origin) {
        this.origin = (PacketTranslator<ClientboundSetEquipmentPacket>) origin;
    }
}
