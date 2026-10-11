package cn.xm1221.ygomc.common.client;

import cn.xm1221.ygomc.common.card.CardRarity;
import cn.xm1221.ygomc.common.card.RarityEntry;
import cn.xm1221.ygomc.common.card.RarityLayer;
import cn.xm1221.ygomc.common.card.RarityLayerType;
import net.minecraft.resources.ResourceLocation;

/**
 * 五档稀有度各自叠哪些闪面。超稀及以上才有层；普通和稀有是白卡。
 * 贴图是本模组画的箔膜，不是实卡扫描。
 */
public final class RarityLooks {

    private static final ResourceLocation HOLO = foil("holo");
    private static final ResourceLocation SPARKLE = foil("sparkle");
    private static final ResourceLocation EMBOSS = foil("emboss");

    private RarityLooks() {
    }

    public static RarityEntry of(CardRarity rarity) {
        return switch (rarity) {
            case SUPER -> RarityEntry.of(rarity, layer(HOLO, RarityLayerType.FOIL));
            case ULTRA -> RarityEntry.of(rarity,
                    layer(HOLO, RarityLayerType.FOIL),
                    layer(SPARKLE, RarityLayerType.OVERLAY));
            case SECRET -> RarityEntry.of(rarity,
                    layer(HOLO, RarityLayerType.FOIL),
                    layer(EMBOSS, RarityLayerType.OVERLAY));
            default -> RarityEntry.of(rarity);
        };
    }

    private static RarityLayer layer(ResourceLocation texture, RarityLayerType type) {
        return new RarityLayer(texture, type);
    }

    private static ResourceLocation foil(String name) {
        return ResourceLocation.fromNamespaceAndPath("ygomc", "textures/card/foil/" + name + ".png");
    }
}
