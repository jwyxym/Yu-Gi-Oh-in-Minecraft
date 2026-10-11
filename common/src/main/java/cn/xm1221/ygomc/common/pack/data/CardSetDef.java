package cn.xm1221.ygomc.common.pack.data;

import cn.xm1221.ygomc.common.card.CardRarity;
import cn.xm1221.ygomc.common.pack.model.CardSetData;
import cn.xm1221.ygomc.common.pack.model.PrintRef;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * 一个系列收录了哪些卡、每张印成哪些稀有度。
 * {@code prints} 里出现的 {@code (卡号, 稀有度)} 覆盖 {@code cards} 里的同名项，用来写异画和少印。
 */
public record CardSetDef(
        Component name,
        String code,
        Optional<String> release,
        Map<CardRarity, List<Integer>> cards,
        List<PrintSpec> prints
) {

    public static final Codec<CardSetDef> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ComponentSerialization.CODEC.fieldOf("name").forGetter(CardSetDef::name),
            Codec.STRING.fieldOf("code").forGetter(CardSetDef::code),
            Codec.STRING.optionalFieldOf("release").forGetter(CardSetDef::release),
            Codec.unboundedMap(CardRarity.CODEC, Codec.INT.listOf()).optionalFieldOf("cards", Map.of())
                    .forGetter(CardSetDef::cards),
            PrintSpec.CODEC.listOf().optionalFieldOf("prints", List.of()).forGetter(CardSetDef::prints)
    ).apply(instance, CardSetDef::new));

    public CardSetDef {
        if (code == null || code.isBlank()) {
            throw new IllegalArgumentException("系列代码不能为空");
        }
        cards = Map.copyOf(cards);
        prints = List.copyOf(prints);
        if (cards.isEmpty() && prints.isEmpty()) {
            throw new IllegalArgumentException("系列 " + code + " 没有任何卡");
        }
    }

    /** 展开成开包用的池。同一 {@code (卡号, 稀有度, 异画)} 只留 {@code prints} 里的那条。 */
    public CardSetData toModel() {
        Set<String> detailed = new HashSet<>();
        List<PrintRef> pool = new ArrayList<>();
        for (PrintSpec print : prints) {
            detailed.add(print.code() + ":" + print.rarity().getSerializedName());
            pool.add(print.toRef());
        }
        for (Map.Entry<CardRarity, List<Integer>> entry : cards.entrySet()) {
            for (int code : entry.getValue()) {
                if (detailed.contains(code + ":" + entry.getKey().getSerializedName())) {
                    continue;
                }
                pool.add(PrintRef.of(code, entry.getKey()));
            }
        }
        return new CardSetData(pool);
    }
}
