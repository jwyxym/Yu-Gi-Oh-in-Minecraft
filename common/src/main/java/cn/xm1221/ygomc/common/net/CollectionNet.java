package cn.xm1221.ygomc.common.net;

import cn.xm1221.ygomc.common.collection.CollectionServer;
import cn.xm1221.ygomc.common.collection.CollectionView;
import dev.architectury.networking.NetworkManager;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

import java.util.function.Consumer;

/**
 * 组卡 / 卡册的两份包。跟对局帧分开：对局那条线的版本号只管牌桌，
 * 这里再塞字段不会把正在打的局拆掉。
 *
 * <p>沿用和 {@link YgomcNet} 同一套旧注册接口，理由也一样：这条链路还没在
 * 真实客户端上跑过，不在这一次把传输层一起换掉。
 */
@SuppressWarnings("removal")
public final class CollectionNet {

    public static final ResourceLocation VIEW =
            ResourceLocation.fromNamespaceAndPath("ygomc", "collection_view");
    public static final ResourceLocation ACT =
            ResourceLocation.fromNamespaceAndPath("ygomc", "collection_act");
    public static final ResourceLocation IMPORT =
            ResourceLocation.fromNamespaceAndPath("ygomc", "collection_import");

    public static final int ADD_MAIN = 1;
    public static final int ADD_EXTRA = 2;
    public static final int ADD_SIDE = 3;
    public static final int TAKE_MAIN = 4;
    public static final int TAKE_EXTRA = 5;
    public static final int TAKE_SIDE = 6;
    /** 参数是物品栏槽位。 */
    public static final int BINDER_PUT = 7;
    /** 卡册里的下标；创造卡册时参数是卡号。 */
    public static final int BINDER_TAKE = 8;
    public static final int CLOSE = 9;
    /** 卡册里把第 {@code arg} 张挪到第 {@code arg2} 张。 */
    public static final int BINDER_MOVE = 10;
    /** {@code arg} 是 {@link cn.xm1221.ygomc.common.collection.BinderArrange} 的排序方式。 */
    public static final int BINDER_SORT = 11;
    /** 在末尾加一页空白。最后一页已经是空的则不动。 */
    public static final int BINDER_ADD_PAGE = 12;

    private static Consumer<CollectionView> viewHandler;

    private CollectionNet() {
    }

    public static void registerCommon() {
        NetworkManager.registerReceiver(NetworkManager.Side.C2S, ACT, CollectionNet::onAct);
        NetworkManager.registerReceiver(NetworkManager.Side.C2S, IMPORT, CollectionNet::onImport);
    }

    /** 只在客户端调用。处理函数已经在主线程上。 */
    public static void registerClient(Consumer<CollectionView> handler) {
        viewHandler = handler;
        NetworkManager.registerReceiver(NetworkManager.Side.S2C, VIEW, (buf, ctx) -> {
            CollectionView view = CollectionView.read(buf);
            Consumer<CollectionView> got = viewHandler;
            if (got != null) {
                ctx.queue(() -> got.accept(view));
            }
        });
    }

    public static void send(ServerPlayer player, CollectionView view) {
        var server = player.getServer();
        if (server == null) {
            return;
        }
        RegistryFriendlyByteBuf buf = new RegistryFriendlyByteBuf(
                io.netty.buffer.Unpooled.buffer(), player.registryAccess());
        CollectionView.write(buf, view);
        Runnable go = () -> {
            if (player.hasDisconnected()) {
                return;
            }
            NetworkManager.sendToPlayer(player, VIEW, buf);
        };
        if (server.isSameThread()) {
            go.run();
        } else {
            server.execute(go);
        }
    }

    public static void sendAct(int token, int action, int arg, int arg2,
                               net.minecraft.core.RegistryAccess registries) {
        RegistryFriendlyByteBuf buf = new RegistryFriendlyByteBuf(
                io.netty.buffer.Unpooled.buffer(), registries);
        buf.writeVarInt(token);
        buf.writeByte(action);
        buf.writeVarInt(arg);
        buf.writeVarInt(arg2);
        buf.writeVarInt(0);
        NetworkManager.sendToServer(ACT, buf);
    }

    public static void sendAct(int token, int action, int arg, int arg2, int arg3,
                               net.minecraft.core.RegistryAccess registries) {
        RegistryFriendlyByteBuf buf = new RegistryFriendlyByteBuf(
                io.netty.buffer.Unpooled.buffer(), registries);
        buf.writeVarInt(token);
        buf.writeByte(action);
        buf.writeVarInt(arg);
        buf.writeVarInt(arg2);
        buf.writeVarInt(arg3);
        NetworkManager.sendToServer(ACT, buf);
    }

    public static void sendImport(int token, String text, net.minecraft.core.RegistryAccess registries) {
        RegistryFriendlyByteBuf buf = new RegistryFriendlyByteBuf(
                io.netty.buffer.Unpooled.buffer(), registries);
        buf.writeVarInt(token);
        buf.writeUtf(text == null ? "" : text);
        NetworkManager.sendToServer(IMPORT, buf);
    }

    private static void onImport(RegistryFriendlyByteBuf buf, NetworkManager.PacketContext ctx) {
        int token = buf.readVarInt();
        String text = buf.readUtf();
        ctx.queue(() -> {
            if (ctx.getPlayer() instanceof ServerPlayer player) {
                CollectionServer.importYdk(player, token, text);
            }
        });
    }

    private static void onAct(RegistryFriendlyByteBuf buf, NetworkManager.PacketContext ctx) {
        int token = buf.readVarInt();
        int action = buf.readByte();
        int arg = buf.readVarInt();
        int arg2 = buf.readVarInt();
        int arg3 = buf.readVarInt();
        ctx.queue(() -> {
            if (ctx.getPlayer() instanceof ServerPlayer player) {
                CollectionServer.act(player, token, action, arg, arg2, arg3);
            }
        });
    }
}
