package org.pcub.core.geyser.injector.protocol.java;

import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.geyser.translator.protocol.PacketTranslator;
import org.geysermc.mcprotocollib.network.packet.Packet;
import org.geysermc.mcprotocollib.protocol.data.game.entity.metadata.EntityMetadata;
import org.geysermc.mcprotocollib.protocol.data.game.entity.metadata.MetadataType;
import org.geysermc.mcprotocollib.protocol.data.game.entity.metadata.MetadataTypes;
import org.geysermc.mcprotocollib.protocol.data.game.item.ItemStack;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.entity.ClientboundSetEntityDataPacket;
import org.pcub.core.geyser.translator.AdvancedItemTranslator;

import static org.pcub.core.common.PCUBCore.logger;
import static org.pcub.core.geyser.translator.AdvancedItemTranslator.getItemName;

public class JavaSetEntityDataInjector extends PacketTranslator<ClientboundSetEntityDataPacket> {

    PacketTranslator<ClientboundSetEntityDataPacket> origin;

    @Override
    public void translate(GeyserSession session, ClientboundSetEntityDataPacket packet) {
        logger().debug(() -> "\t\tClientboundSetEntityDataPacket  " +
                session.getEntityCache().getEntityByJavaId(packet.getEntityId()).getEntityType().toString() +
                "(" + packet.getEntityId() + ")");

        for (EntityMetadata<?, ?> metadata : packet.getMetadata()) {
            MetadataType<?> type = metadata.getType();
            if (type == MetadataTypes.ITEM_STACK) {
                logger().debug(() -> "MetadataTypes.ITEM_STACK");

                ItemStack javaItem = ((EntityMetadata<ItemStack, MetadataType<ItemStack>>) metadata).getValue();
                logger().debug(() -> getItemName(javaItem));
                if (javaItem != null) {
                    AdvancedItemTranslator.apply(session, javaItem, false);
                }
            } else if (type == MetadataTypes.PARTICLE) {
                logger().debug(() -> "MetadataTypes.PARTICLE"); // TODO test only

//                Particle particle = ((EntityMetadata<Particle, MetadataType<Particle>>) metadata).getValue();
            } else if (type == MetadataTypes.PARTICLES) {
                logger().debug(() -> "MetadataTypes.PARTICLES S"); // TODO test only

//                List<Particle> particles = ((EntityMetadata<List<Particle>, MetadataType<List<Particle>>>) metadata).getValue();
            }
        }

        origin.translate(session, packet);
    }

    @SuppressWarnings("unchecked")
    public JavaSetEntityDataInjector(PacketTranslator<? extends Packet> origin) {
        this.origin = (PacketTranslator<ClientboundSetEntityDataPacket>) origin;
    }
}
