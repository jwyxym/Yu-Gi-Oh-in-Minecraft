package cn.xm1221.ygomc.common.net;

import cn.xm1221.ygomc.common.card.CardRarity;
import cn.xm1221.ygomc.common.pack.model.PrintRef;
import dev.architectury.networking.NetworkManager;
import io.netty.buffer.Unpooled;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * 开包结果（服务器 → 客户端）。卡在发包之前已经进了背包，这个包只负责展示。
 */
@SuppressWarnings("removal")
public final class PackNet {

    public static final ResourceLocation OPENED =
            ResourceLocation.fromNamespaceAndPath("ygomc", "pack_opened");

    private static Consumer<List<PrintRef>> handler;

    private PackNet() {
    }

    /** 只在客户端调用。 */
    public static void registerClient(Consumer<List<PrintRef>> opened) {
        handler = opened;
        NetworkManager.registerReceiver(NetworkManager.Side.S2C, OPENED, (buf, ctx) -> {
            List<PrintRef> cards = read(buf);
            Consumer<List<PrintRef>> got = handler;
            if (got != null) {
                ctx.queue(() -> got.accept(cards));
            }
        });
    }

    public static void send(ServerPlayer player, List<PrintRef> cards) {
        RegistryFriendlyByteBuf buf = new RegistryFriendlyByteBuf(Unpooled.buffer(), player.registryAccess());
        buf.writeVarInt(cards.size());
        for (PrintRef card : cards) {
            buf.writeVarInt(card.code());
            buf.writeVarInt(card.rarity().id());
            buf.writeVarInt(card.variant());
        }
        NetworkManager.sendToPlayer(player, OPENED, buf);
    }

    private static List<PrintRef> read(RegistryFriendlyByteBuf buf) {
        int count = buf.readVarInt();
        List<PrintRef> cards = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            int code = buf.readVarInt();
            CardRarity rarity = CardRarity.byId(buf.readVarInt());
            int variant = buf.readVarInt();
            cards.add(new PrintRef(code, rarity, variant, 1));
        }
        return cards;
    }
}
