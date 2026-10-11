package cn.xm1221.ygomc.common.pack.data;

import cn.xm1221.ygomc.common.card.CardRarity;
import cn.xm1221.ygomc.common.pack.model.PrintRef;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/** 一条印制。{@code weight} 默认 1，{@code variant} 默认 0。 */
public record PrintSpec(int code, CardRarity rarity, int variant, int weight) {

    public static final Codec<PrintSpec> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.INT.fieldOf("code").forGetter(PrintSpec::code),
            CardRarity.CODEC.fieldOf("rarity").forGetter(PrintSpec::rarity),
            Codec.INT.optionalFieldOf("variant", 0).forGetter(PrintSpec::variant),
            Codec.intRange(1, 1_000_000).optionalFieldOf("weight", 1).forGetter(PrintSpec::weight)
    ).apply(instance, PrintSpec::new));

    public PrintRef toRef() {
        return new PrintRef(code, rarity, variant, weight);
    }
}
