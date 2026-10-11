package cn.xm1221.ygomc.common.collection;

import cn.xm1221.ygomc.common.card.CardRarity;
import cn.xm1221.ygomc.common.card.CardRef;
import cn.xm1221.ygomc.common.card.DeckData;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

import java.util.ArrayList;
import java.util.List;

/**
 * 卡组盒里实际装着的卡：主 / 额外 / 副，每张都是放进去的那张 {@link CardRef}。
 *
 * <p>{@link DeckData} 只留卡号，给决斗和 {@code .ydk} 用。盒子要能把卡原样取出来，
 * 所以稀有度留在这里。两边一起写：卡号清单从这里推导，不另维护一份会漂移的列表。
 */
public record DeckCards(List<CardRef> main, List<CardRef> extra, List<CardRef> side) {

    public static final DeckCards EMPTY = new DeckCards(List.of(), List.of(), List.of());

    public static final Codec<DeckCards> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            CardRef.CODEC.listOf().fieldOf("main").forGetter(DeckCards::main),
            CardRef.CODEC.listOf().fieldOf("extra").forGetter(DeckCards::extra),
            CardRef.CODEC.listOf().fieldOf("side").forGetter(DeckCards::side)
    ).apply(instance, DeckCards::new));

    public static final StreamCodec<ByteBuf, DeckCards> STREAM_CODEC = StreamCodec.composite(
            CardRef.STREAM_CODEC.apply(ByteBufCodecs.list()), DeckCards::main,
            CardRef.STREAM_CODEC.apply(ByteBufCodecs.list()), DeckCards::extra,
            CardRef.STREAM_CODEC.apply(ByteBufCodecs.list()), DeckCards::side,
            DeckCards::new);

    public DeckCards {
        main = List.copyOf(main);
        extra = List.copyOf(extra);
        side = List.copyOf(side);
    }

    public DeckData toDeckData() {
        return new DeckData(codes(main), codes(extra), codes(side));
    }

    /** 旧盒子只有卡号、没有稀有度时，取出来按普通卡还给玩家。 */
    public static DeckCards fromCodes(DeckData data) {
        return new DeckCards(lift(data.main()), lift(data.extra()), lift(data.side()));
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        write(tag, "main", main);
        write(tag, "extra", extra);
        write(tag, "side", side);
        return tag;
    }

    public static DeckCards load(CompoundTag tag) {
        return new DeckCards(read(tag, "main"), read(tag, "extra"), read(tag, "side"));
    }

    private static List<Integer> codes(List<CardRef> refs) {
        ArrayList<Integer> out = new ArrayList<>(refs.size());
        for (CardRef ref : refs) {
            out.add(ref.cardCode());
        }
        return out;
    }

    private static List<CardRef> lift(List<Integer> codes) {
        ArrayList<CardRef> out = new ArrayList<>(codes.size());
        for (int code : codes) {
            out.add(CardRef.of(code, CardRarity.COMMON));
        }
        return out;
    }

    private static void write(CompoundTag tag, String key, List<CardRef> refs) {
        int[] codes = new int[refs.size()];
        byte[] rarity = new byte[refs.size()];
        int[] variant = new int[refs.size()];
        for (int i = 0; i < refs.size(); i++) {
            CardRef ref = refs.get(i);
            codes[i] = ref.cardCode();
            rarity[i] = (byte) ref.rarity().ordinal();
            variant[i] = ref.variant();
        }
        tag.putIntArray(key, codes);
        tag.putByteArray(key + "_rarity", rarity);
        tag.putIntArray(key + "_variant", variant);
    }

    private static List<CardRef> read(CompoundTag tag, String key) {
        int[] codes = tag.getIntArray(key);
        byte[] rarity = tag.getByteArray(key + "_rarity");
        int[] variant = tag.getIntArray(key + "_variant");
        CardRarity[] values = CardRarity.values();
        ArrayList<CardRef> out = new ArrayList<>(codes.length);
        for (int i = 0; i < codes.length; i++) {
            int ord = i < rarity.length ? (rarity[i] & 0xFF) : 0;
            CardRarity rare = ord >= 0 && ord < values.length ? values[ord] : CardRarity.COMMON;
            int ver = i < variant.length ? variant[i] : CardRef.DEFAULT_VARIANT;
            out.add(new CardRef(codes[i], rare, ver));
        }
        return out;
    }
}
