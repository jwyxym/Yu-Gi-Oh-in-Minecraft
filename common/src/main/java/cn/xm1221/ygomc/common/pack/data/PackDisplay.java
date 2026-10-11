package cn.xm1221.ygomc.common.pack.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.world.item.Rarity;

/**
 * 卡包和盒装卡包共用的显示信息。颜色是 {@code #RRGGBB}，给染色层用。
 */
public record PackDisplay(Component name, int color, Rarity rarity) {

    public static final Codec<PackDisplay> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ComponentSerialization.CODEC.fieldOf("name").forGetter(PackDisplay::name),
            Codec.STRING.optionalFieldOf("color", "#FFFFFF").xmap(PackDisplay::parse, PackDisplay::format)
                    .forGetter(PackDisplay::color),
            Rarity.CODEC.optionalFieldOf("rarity", Rarity.COMMON).forGetter(PackDisplay::rarity)
    ).apply(instance, PackDisplay::new));

    public static int parse(String text) {
        String hex = text.startsWith("#") ? text.substring(1) : text;
        if (hex.length() != 6) {
            throw new IllegalArgumentException("颜色要写成 #RRGGBB，实得 " + text);
        }
        return Integer.parseInt(hex, 16);
    }

    public static String format(int color) {
        return String.format("#%06X", color & 0xFFFFFF);
    }
}
