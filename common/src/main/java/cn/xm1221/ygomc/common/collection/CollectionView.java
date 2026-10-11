package cn.xm1221.ygomc.common.collection;

import net.minecraft.network.RegistryFriendlyByteBuf;

import java.util.ArrayList;
import java.util.List;

/**
 * 服务端发给界面的一份快照。界面只画它，不自己清点物品栏——清点在服务端做，
 * 否则客户端报的张数和服务端扣的张数会对不上。
 */
public record CollectionView(
        boolean open,
        int token,
        int screen,
        int mode,
        boolean unlimited,
        List<Integer> main,
        List<Integer> extra,
        List<Integer> side,
        List<Counted> supply,
        List<Loose> loose,
        List<List<Integer>> pages) {

    public static final int VERSION = 2;

    public static final int SCREEN_DECK = 1;
    public static final int SCREEN_BINDER = 2;

    public static final int MODE_ITEM = 1;
    public static final int MODE_ENDER = 2;
    public static final int MODE_CREATIVE = 3;

    /** 一次动作能带的列表上限。不是卡册容量，是坏包的刹车。 */
    private static final int MAX_ENTRIES = 1_000_000;

    public record Counted(int code, int count) {
    }

    public record Loose(int slot, int code) {
    }

    public CollectionView {
        main = List.copyOf(main);
        extra = List.copyOf(extra);
        side = List.copyOf(side);
        supply = List.copyOf(supply);
        loose = List.copyOf(loose);
        ArrayList<List<Integer>> copied = new ArrayList<>(pages.size());
        for (List<Integer> page : pages) {
            copied.add(List.copyOf(page));
        }
        pages = List.copyOf(copied);
    }

    public static CollectionView closed() {
        return new CollectionView(false, 0, 0, 0, false,
                List.of(), List.of(), List.of(), List.of(), List.of(), List.of());
    }

    public static void write(RegistryFriendlyByteBuf buf, CollectionView view) {
        buf.writeVarInt(VERSION);
        buf.writeBoolean(view.open);
        buf.writeVarInt(view.token);
        buf.writeByte(view.screen);
        buf.writeByte(view.mode);
        buf.writeBoolean(view.unlimited);
        writeInts(buf, view.main);
        writeInts(buf, view.extra);
        writeInts(buf, view.side);
        buf.writeVarInt(view.supply.size());
        for (Counted counted : view.supply) {
            buf.writeVarInt(counted.code);
            buf.writeVarInt(counted.count);
        }
        buf.writeVarInt(view.loose.size());
        for (Loose card : view.loose) {
            buf.writeVarInt(card.slot);
            buf.writeVarInt(card.code);
        }
        buf.writeVarInt(view.pages.size());
        for (List<Integer> page : view.pages) {
            writeInts(buf, page);
        }
    }

    public static CollectionView read(RegistryFriendlyByteBuf buf) {
        int version = buf.readVarInt();
        if (version != VERSION) {
            throw new IllegalStateException(
                    "组卡界面版本不符：收到 " + version + "，本端是 " + VERSION + "。两端要一起换。");
        }
        boolean open = buf.readBoolean();
        int token = buf.readVarInt();
        int screen = buf.readByte();
        int mode = buf.readByte();
        boolean unlimited = buf.readBoolean();
        List<Integer> main = readInts(buf);
        List<Integer> extra = readInts(buf);
        List<Integer> side = readInts(buf);
        int supplyCount = readCount(buf);
        ArrayList<Counted> supply = new ArrayList<>(Math.min(supplyCount, 64));
        for (int i = 0; i < supplyCount; i++) {
            supply.add(new Counted(buf.readVarInt(), buf.readVarInt()));
        }
        int looseCount = readCount(buf);
        ArrayList<Loose> loose = new ArrayList<>(Math.min(looseCount, 64));
        for (int i = 0; i < looseCount; i++) {
            loose.add(new Loose(buf.readVarInt(), buf.readVarInt()));
        }
        int pageCount = readCount(buf);
        ArrayList<List<Integer>> pages = new ArrayList<>(pageCount);
        for (int i = 0; i < pageCount; i++) {
            pages.add(readInts(buf));
        }
        return new CollectionView(open, token, screen, mode, unlimited,
                main, extra, side, supply, loose, pages);
    }

    private static void writeInts(RegistryFriendlyByteBuf buf, List<Integer> values) {
        buf.writeVarInt(values.size());
        for (int value : values) {
            buf.writeVarInt(value);
        }
    }

    private static List<Integer> readInts(RegistryFriendlyByteBuf buf) {
        int count = readCount(buf);
        ArrayList<Integer> out = new ArrayList<>(Math.min(count, 64));
        for (int i = 0; i < count; i++) {
            out.add(buf.readVarInt());
        }
        return out;
    }

    private static int readCount(RegistryFriendlyByteBuf buf) {
        int count = buf.readVarInt();
        if (count < 0 || count > MAX_ENTRIES) {
            throw new IllegalStateException("组卡快照的列表长度不对：" + count);
        }
        return count;
    }
}
