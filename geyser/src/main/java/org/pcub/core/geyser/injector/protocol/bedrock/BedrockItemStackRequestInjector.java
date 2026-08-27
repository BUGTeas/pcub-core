package org.pcub.core.geyser.injector.protocol.bedrock;

import org.cloudburstmc.protocol.bedrock.data.inventory.ContainerSlotType;
import org.cloudburstmc.protocol.bedrock.data.inventory.FullContainerName;
import org.cloudburstmc.protocol.bedrock.data.inventory.itemstack.request.ItemStackRequest;
import org.cloudburstmc.protocol.bedrock.data.inventory.itemstack.request.ItemStackRequestSlotData;
import org.cloudburstmc.protocol.bedrock.data.inventory.itemstack.request.action.*;
import org.cloudburstmc.protocol.bedrock.packet.BedrockPacket;
import org.geysermc.geyser.inventory.GeyserItemStack;
import org.geysermc.geyser.inventory.Inventory;
import org.geysermc.geyser.inventory.InventoryHolder;
import org.geysermc.geyser.inventory.PlayerInventory;
import org.geysermc.geyser.item.hashing.RegistryHasher;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.geyser.session.cache.BundleCache;
import org.geysermc.geyser.translator.inventory.BundleInventoryTranslator;
import org.geysermc.geyser.translator.inventory.PlayerInventoryTranslator;
import org.geysermc.geyser.translator.protocol.PacketTranslator;
import org.cloudburstmc.protocol.bedrock.packet.ItemStackRequestPacket;
import org.geysermc.mcprotocollib.protocol.data.game.entity.player.GameMode;
import org.geysermc.mcprotocollib.protocol.data.game.item.ItemStack;
import org.pcub.core.geyser.cache.ItemHashCache;
import org.pcub.core.geyser.translator.AdvancedItemTranslator;

import java.util.List;

import static org.pcub.core.common.PCUBCore.logger;
import static org.pcub.core.geyser.translator.AdvancedItemTranslator.getItemName;

public class BedrockItemStackRequestInjector extends PacketTranslator<ItemStackRequestPacket> {

    PacketTranslator<ItemStackRequestPacket> origin;

    @Override
    public void translate(GeyserSession session, ItemStackRequestPacket packet) {
        logger().debug(() -> "\n\nBEDROCK  ItemStackRequestPacket");

        PlayerInventory playerInv = session.getPlayerInventory();
        InventoryHolder<?> holder = session.getInventoryHolder();

        if (holder != null && logger().isDebug()) {
            logger().debug(
                    holder.inventory().getClass().getName().replaceAll("^org.geysermc.geyser.", ".") + "  " +
                    holder.translator().getClass().getName().replaceAll("^org.geysermc.geyser.", "."));
        }
        // 提前存下物品组件的校验值，以备发包时使用 - 仅限非创造模式背包
        if (holder != null && !(session.getGameMode() == GameMode.CREATIVE && holder.translator() instanceof PlayerInventoryTranslator)) {
            int reqCount = 0;
            for (ItemStackRequest request : packet.getRequests()) {
                for (ItemStackRequestAction action : request.getActions()) {
                    // 获取槽位
                    ItemStackRequestSlotData source = null, destination = null;
                    int count = -1;
                    TransferItemStackRequestAction transferAction = null;
                    switch (action.getType()) {
                        case TAKE, PLACE:
                            transferAction = (TransferItemStackRequestAction) action;
                            source = transferAction.getSource();
                            destination = transferAction.getDestination();
                            count = transferAction.getCount();
                            break;
                        case SWAP: {
                            SwapAction swapAction = (SwapAction) action;
                            source = swapAction.getSource();
                            destination = swapAction.getDestination();
                            break;
                        }
                        case DROP: {
                            DropAction dropAction = (DropAction) action;
                            source = dropAction.getSource();
                            count = dropAction.getCount();
                            break;
                        }
                    }

                    if (logger().isDebug()) {
                        logger().debug((reqCount ++) + " " + action.getType().name() + (count != -1 ? " x" + count : ""));
                    }

                    ItemHashCache hashCache = ItemHashCache.INSTANCE;

                    GeyserItemStack sourceItem = source != null ? getJavaItem(holder, playerInv, source) : null;
                    GeyserItemStack destItem = destination != null ? getJavaItem(holder, playerInv, destination) : null;

                    if (transferAction != null &&
                            source != null && source.getContainerName().getContainer() == ContainerSlotType.DYNAMIC_CONTAINER &&
                            destination != null && destination.getContainerName().getContainer() == ContainerSlotType.DYNAMIC_CONTAINER) {
                        if (logger().isDebug()) {
                            logger().debug(" ~ " + source.getSlot() + " " + getItemName(sourceItem) + " -> " + destination.getSlot() + " " + getItemName(destItem));
                            getBundleData(source, holder.inventory(), playerInv);
                            getBundleData(destination, holder.inventory(), playerInv);
                        }
                        // 跳过基岩端独有的，由客户端发起的收纳袋排序操作
                        continue;
                    }

                    if (source != null) {
                        ItemStackRequestSlotData finalSource = source;
                        logger().debug(() -> " <- " +
                                finalSource.getContainerName().getContainer() + " " + finalSource.getSlot() + " " + getItemName(sourceItem));
                        // 检查是否为收纳袋
                        BundleCache.BundleData bundleData = transferAction != null ? getBundleData(source, holder.inventory(), playerInv) : null;

                        if (bundleData != null) {
                            // 模拟取出物品后的数据
                            if (source.getSlot() >= 0 && source.getSlot() < bundleData.contents().size()) {
                                List<ItemStack> bundleContents = bundleData.toComponent();
                                int slot = BundleCache.platformConvertSlot(bundleContents.size(), source.getSlot()); // 转为 Java 版槽位 (反序)
                                hashCache.put(session, bundleContents.remove(slot)); // 取出，并存入校验值
                                List<ItemStack> originBundleCont = AdvancedItemTranslator.restoreFrom(bundleContents);
                                if (originBundleCont != null) {
                                    hashCache.put(session, RegistryHasher.ITEM_STACK.list(), bundleContents, originBundleCont); // 存入校验值
                                }
                            }
                        } else if (sourceItem != null) {
                            hashCache.put(session, sourceItem); // 存入校验值
                        }
                    }

                    if (destination != null) {
                        ItemStackRequestSlotData finalDest = destination;
                        logger().debug(() -> " -> " +
                                finalDest.getContainerName().getContainer() + " " + finalDest.getSlot() + " " + getItemName(destItem));
                        // 检查是否为收纳袋
                        BundleCache.BundleData bundleData = transferAction != null ? getBundleData(destination, holder.inventory(), playerInv) : null;

                        if (bundleData != null) {
                            // 模拟存入物品后的数据
                            if (sourceItem != null && !sourceItem.isEmpty()) {
                                int sourceCount = destination.getContainerName().getDynamicId() == playerInv.getCursor().getBundleId() ?
                                        sourceItem.getAmount() : count;
                                // 确保客户端请求放入数量不超出容量才尝试缓存
                                if (BundleInventoryTranslator.capacityForItemStack(
                                        BundleInventoryTranslator.calculateBundleWeight(bundleData.contents()), sourceItem) >= sourceCount) {

                                    List<ItemStack> bundleContents = bundleData.toComponent();
                                    // TODO: 需特别留意，如果袋中已有的相同物品，Geyser 不会将其合并，任由其与服务端数据不同步而被刷新 (截止至 2.9.2-b1013)
                                    bundleContents.addFirst(sourceItem.getItemStack(sourceCount));

                                    List<ItemStack> originBundleCont = AdvancedItemTranslator.restoreFrom(bundleContents);
                                    if (originBundleCont != null) {
                                        hashCache.put(session, RegistryHasher.ITEM_STACK.list(), bundleContents, originBundleCont); // 存入校验值
                                    }
                                }
                            }
                        } else if (destItem != null) {
                            hashCache.put(session, destItem); // 存入校验值
                        }
                    }
                }
            }
        }

        origin.translate(session, packet);
    }

    public BundleCache.BundleData getBundleData(ItemStackRequestSlotData slotData, Inventory inventory, PlayerInventory playerInv) {
        FullContainerName containerName = slotData.getContainerName();
        if (containerName.getContainer() != ContainerSlotType.DYNAMIC_CONTAINER) {
            return null;
        }
        int bundleId = containerName.getDynamicId();
        if (bundleId == playerInv.getCursor().getBundleId()) {
            logger().debug("    bundle: cursor");
            return playerInv.getCursor().getBundleData();
        }
        for (int javaSlot = 0; javaSlot < inventory.getSize(); javaSlot++) {
            GeyserItemStack bundle = inventory.getItem(javaSlot);
            if (bundle.getBundleId() != bundleId) {
                continue;
            }
            logger().debug("    bundle: " + javaSlot);
            return bundle.getBundleData();
        }
        return null;
    }

    public GeyserItemStack getJavaItem(InventoryHolder<?> holder, PlayerInventory playerInv, ItemStackRequestSlotData slotData) {
//        boolean inCursor = slotData.getContainerName().getContainer() == ContainerSlotType.CURSOR;
//        int slot = holder.translator().bedrockSlotToJava(slotData);
//        return inCursor ? playerInv.getCursor() : holder.inventory().getItem(slot);
        return switch (slotData.getContainerName().getContainer()) {
            case CURSOR -> playerInv.getCursor();
    //                            case INVENTORY -> playerInv.getItem(
    //                                    InventoryTranslator.PLAYER_INVENTORY_TRANSLATOR.bedrockSlotToJava(source));
            default -> holder.inventory().getItem(
                    holder.translator().bedrockSlotToJava(slotData));
        };
    }

    @SuppressWarnings("unchecked")
    public BedrockItemStackRequestInjector(PacketTranslator<? extends BedrockPacket> origin) {
        this.origin = (PacketTranslator<ItemStackRequestPacket>) origin;
    }
}
