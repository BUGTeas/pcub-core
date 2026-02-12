package org.pcub.core.geyser.listener;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import org.geysermc.geyser.inventory.InventoryHolder;
import org.geysermc.geyser.session.DownstreamSession;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.mcprotocollib.network.event.session.PacketSendingEvent;
import org.geysermc.mcprotocollib.network.event.session.SessionAdapter;
import org.geysermc.mcprotocollib.network.packet.Packet;
import org.geysermc.mcprotocollib.protocol.data.game.inventory.ContainerAction;
import org.geysermc.mcprotocollib.protocol.data.game.inventory.ContainerActionType;
import org.geysermc.mcprotocollib.protocol.data.game.item.HashedStack;
import org.geysermc.mcprotocollib.protocol.data.game.item.ItemStack;
import org.geysermc.mcprotocollib.protocol.packet.common.serverbound.ServerboundKeepAlivePacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.serverbound.ServerboundClientTickEndPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.serverbound.inventory.ServerboundContainerClickPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.serverbound.inventory.ServerboundSetCreativeModeSlotPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.serverbound.level.ServerboundPlayerInputPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.serverbound.player.ServerboundMovePlayerPosPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.serverbound.player.ServerboundMovePlayerPosRotPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.serverbound.player.ServerboundMovePlayerRotPacket;
import org.pcub.core.geyser.cache.ItemHashCache;
import org.pcub.core.geyser.translator.AdvancedItemTranslator;

import java.lang.reflect.Field;

import static org.pcub.core.common.PCUBCore.logger;

public class DownstreamListener4Geyser extends SessionAdapter {
    GeyserSession session;

    public DownstreamListener4Geyser(GeyserSession session) {
        this.session = session;
        try {
            Field downstreamField = GeyserSession.class.getDeclaredField("downstream");
            downstreamField.setAccessible(true);
            DownstreamSession downstream = (DownstreamSession) downstreamField.get(session);
            downstream.getSession().addListener(this);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            logger().error("服务端会话获取失败", e);
        }
    }

    @Override
    public void packetSending(PacketSendingEvent event) {
        Packet packet = event.getPacket();
        if (logger().isDebug() && !(
                packet instanceof ServerboundClientTickEndPacket ||
                packet instanceof ServerboundKeepAlivePacket ||
                packet instanceof ServerboundPlayerInputPacket ||
                packet instanceof ServerboundMovePlayerPosPacket ||
                packet instanceof ServerboundMovePlayerPosRotPacket ||
                packet instanceof ServerboundMovePlayerRotPacket)) {
            logger().debug("\t\t" + packet.getClass().getName().replaceAll("^org.geysermc.mcprotocollib.protocol.packet.", "mcpl:"));
        }


        if (packet instanceof ServerboundSetCreativeModeSlotPacket setCreative) {
            // 创造背包物品操作
            ItemStack javaItem = setCreative.getClickedItem();
            if (javaItem != null) {
                logger().debug(AdvancedItemTranslator.getItemName(javaItem));
                ItemStack newItem = AdvancedItemTranslator.restoreFrom(javaItem);
                if (newItem != null) {
                    event.setPacket(new ServerboundSetCreativeModeSlotPacket(setCreative.getSlot(), newItem));
                }
            }
        }


        else if (packet instanceof ServerboundContainerClickPacket containerClick) {
            ItemHashCache hashCache = ItemHashCache.INSTANCE;

            // 物品操作
            ContainerActionType action = containerClick.getAction();
            ContainerAction param = containerClick.getParam();

            logger().debug(() -> action + " " + param.getId());

            HashedStack carried = containerClick.getCarriedItem(), newCarried = carried;
            Int2ObjectMap<HashedStack> newChanged = new Int2ObjectOpenHashMap<>(containerClick.getChangedSlots());
            boolean updated = false;
            InventoryHolder<?> holder = session.getInventoryHolder();
            if (carried != null) {
                if (logger().isDebug()) {
                    logger().debug("carried " + containerClick.getSlot() + "\t" + AdvancedItemTranslator.getItemName(carried.id()));
//                    GeyserItemStack storedItem = null;
//                    if (action == ContainerActionType.CLICK_ITEM && param == ClickItemAction.LEFT_CLICK) {
//                        // 原槽位物品被移走，所以从指针获取，但还是看运气，因为发包为异步任务
//                        storedItem = session.getPlayerInventory().getCursor();
//                    } else {
//                        int slot = containerClick.getSlot();
//                        if (holder != null && slot < holder.inventory().getSize() && slot >= 0) {
//                            storedItem = holder.inventory().getItem(containerClick.getSlot());
//                        }
//                    }
//                    logger().debug(" origin " + (storedItem == null ? null : AdvancedItemTranslator.getItemName(storedItem)));
                }

                HashedStack newer = hashCache.getOrigin(carried);
                if (newer != null) {
                    newCarried = newer;
                    updated = true;
                }
            }
            for (int slot : newChanged.keySet()) {
                HashedStack changed = newChanged.get(slot);
                if (changed == null) {
                    continue;
                }
                if (logger().isDebug()) {
                    logger().debug("changed " + slot + "\t" + AdvancedItemTranslator.getItemName(changed.id()));
//                    GeyserItemStack storedItem = (holder == null || slot >= holder.inventory().getSize() && slot < 0) ? null :
//                            holder.inventory().getItem(slot);
//                    logger().debug(" origin " + (storedItem == null ? null : AdvancedItemTranslator.getItemName(storedItem)));
                }

                HashedStack newer = hashCache.getOrigin(changed);
                if (newer != null) {
                    newChanged.put(slot, newer);
                    updated = true;
                }
            }
            if (updated) {
                event.setPacket(new ServerboundContainerClickPacket(
                        containerClick.getContainerId(), containerClick.getStateId(), containerClick.getSlot(),
                        action, param, newCarried, newChanged));
            }
        }
    }
}
