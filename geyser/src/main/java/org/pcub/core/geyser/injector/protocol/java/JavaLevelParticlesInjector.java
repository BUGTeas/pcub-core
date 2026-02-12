package org.pcub.core.geyser.injector.protocol.java;

import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.geyser.translator.protocol.PacketTranslator;
import org.geysermc.mcprotocollib.network.packet.Packet;
import org.geysermc.mcprotocollib.protocol.data.game.item.ItemStack;
import org.geysermc.mcprotocollib.protocol.data.game.level.particle.ItemParticleData;
import org.geysermc.mcprotocollib.protocol.data.game.level.particle.ParticleType;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.level.ClientboundLevelParticlesPacket;
import org.pcub.core.geyser.translator.AdvancedItemTranslator;

import static org.pcub.core.common.PCUBCore.logger;
import static org.pcub.core.geyser.translator.AdvancedItemTranslator.getItemName;

public class JavaLevelParticlesInjector extends PacketTranslator<ClientboundLevelParticlesPacket> {

    PacketTranslator<ClientboundLevelParticlesPacket> origin;

    @Override
    public void translate(GeyserSession session, ClientboundLevelParticlesPacket packet) {
        logger().debug(() -> "\t\tClientboundLevelParticlesPacket");

        if (packet.getParticle().getType() == ParticleType.ITEM) {
            ItemStack javaItem = ((ItemParticleData) packet.getParticle().getData()).getItemStack();
            logger().debug(() -> "ITEM " + getItemName(javaItem));
            if (javaItem != null) {
                AdvancedItemTranslator.apply(session, javaItem, false);
            }
        }

        origin.translate(session, packet);
    }

    @SuppressWarnings("unchecked")
    public JavaLevelParticlesInjector(PacketTranslator<? extends Packet> origin) {
        this.origin = (PacketTranslator<ClientboundLevelParticlesPacket>) origin;
    }
}
