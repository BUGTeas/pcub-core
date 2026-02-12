package org.pcub.core.geyser.injector;

import org.cloudburstmc.protocol.bedrock.packet.BedrockPacket;
import org.cloudburstmc.protocol.bedrock.packet.ItemStackRequestPacket;
import org.geysermc.geyser.registry.PacketTranslatorRegistry;
import org.geysermc.geyser.registry.Registries;
import org.geysermc.mcprotocollib.network.packet.Packet;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.entity.ClientboundSetEntityDataPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.entity.ClientboundSetEquipmentPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.inventory.*;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.level.ClientboundLevelParticlesPacket;
import org.pcub.core.geyser.injector.protocol.bedrock.BedrockItemStackRequestInjector;
import org.pcub.core.geyser.injector.protocol.java.*;

public class Injector4Geyser {
    public static void registerProtocol() {
        Class<? extends Packet> packet;
        PacketTranslatorRegistry<Packet> transJava = Registries.JAVA_PACKET_TRANSLATORS;
        transJava.register(packet = ClientboundSetPlayerInventoryPacket.class, new JavaSetPlayerInventoryInjector(transJava.get(packet)));
        transJava.register(packet = ClientboundContainerSetContentPacket.class, new JavaContainerSetContentInjector(transJava.get(packet)));
        transJava.register(packet = ClientboundContainerSetSlotPacket.class, new JavaContainerSetSlotInjector(transJava.get(packet)));
        transJava.register(packet = ClientboundSetCursorItemPacket.class, new JavaSetCursorItemInjector(transJava.get(packet)));
        transJava.register(packet = ClientboundSetEquipmentPacket.class, new JavaSetEquipmentInjector(transJava.get(packet)));
        transJava.register(packet = ClientboundMerchantOffersPacket.class, new JavaMerchantOffersInjector(transJava.get(packet)));
        transJava.register(packet = ClientboundSetEntityDataPacket.class, new JavaSetEntityDataInjector(transJava.get(packet)));
        transJava.register(packet = ClientboundLevelParticlesPacket.class, new JavaLevelParticlesInjector(transJava.get(packet)));

        PacketTranslatorRegistry<BedrockPacket> transBedrock = Registries.BEDROCK_PACKET_TRANSLATORS;
        transBedrock.register(ItemStackRequestPacket.class, new BedrockItemStackRequestInjector(transBedrock.get(ItemStackRequestPacket.class)));
    }
}
