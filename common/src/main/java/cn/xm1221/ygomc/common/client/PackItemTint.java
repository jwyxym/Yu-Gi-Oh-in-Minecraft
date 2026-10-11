package cn.xm1221.ygomc.common.client;

import cn.xm1221.ygomc.common.card.CardComponents;
import cn.xm1221.ygomc.common.pack.data.BoosterBoxDef;
import cn.xm1221.ygomc.common.pack.data.BoosterDef;
import net.minecraft.client.color.item.ItemColor;
import net.minecraft.core.Holder;
import net.minecraft.world.item.ItemStack;

/**
 * 卡包和盒装卡包的第二层贴图按 {@code display.color} 染色。
 * 没有定义时用原来的红色，创造栏里的空物品看起来和以前一样。
 */
public final class PackItemTint {

    private static final int DEFAULT = 0xFFDC403C;

    public static final ItemColor COLOR = (ItemStack stack, int tintIndex) -> {
        if (tintIndex != 1) {
            return 0xFFFFFFFF;
        }
        Holder<BoosterDef> booster = stack.get(CardComponents.BOOSTER.get());
        if (booster != null && booster.isBound()) {
            return 0xFF000000 | booster.value().display().color();
        }
        Holder<BoosterBoxDef> box = stack.get(CardComponents.BOOSTER_BOX.get());
        if (box != null && box.isBound()) {
            return 0xFF000000 | box.value().display().color();
        }
        return DEFAULT;
    };

    private PackItemTint() {
    }
}
