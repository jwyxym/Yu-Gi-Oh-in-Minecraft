package cn.xm1221.ygomc.common.pack.model;

import cn.xm1221.ygomc.common.card.CardRarity;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/**
 * 一种抽法里的一个卡位：连抽 {@code count} 张，每张先按 {@code pick} 定稀有度。
 */
public record Slot(int count, RarityPick pick) {

    public static final Codec<Slot> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.intRange(1, 60).fieldOf("count").forGetter(Slot::count),
            RarityPick.CODEC.fieldOf("rarity").forGetter(Slot::pick)
    ).apply(instance, Slot::new));

    public Slot {
        if (count <= 0) {
            throw new IllegalArgumentException("卡位张数必须为正，实得 " + count);
        }
        if (pick == null) {
            throw new IllegalArgumentException("卡位的稀有度分布不能为空");
        }
    }

    public static Slot of(int count, CardRarity rarity) {
        return new Slot(count, RarityPick.one(rarity));
    }
}
