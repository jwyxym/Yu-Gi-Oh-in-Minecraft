package cn.xm1221.ygomc.common.pack;

import cn.xm1221.ygomc.common.card.CardComponents;
import cn.xm1221.ygomc.common.client.PackTips;
import cn.xm1221.ygomc.common.pack.data.BoosterBoxDef;
import cn.xm1221.ygomc.common.pack.model.CollationEntry;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

/** 盒装卡包：右键拆成若干包，潜行右键则直接开成卡。 */
public class BoosterBoxItem extends Item {

    public BoosterBoxItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide || !(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResultHolder.success(stack);
        }
        boolean opened = PackOpening.openBox(serverPlayer, stack, player.isShiftKeyDown());
        return opened ? InteractionResultHolder.success(stack) : InteractionResultHolder.fail(stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        Holder<BoosterBoxDef> holder = stack.get(CardComponents.BOOSTER_BOX.get());
        if (holder == null || !holder.isBound()) {
            return;
        }
        BoosterBoxDef box = holder.value();
        tooltip.add(Component.translatable("ygomc.box.packs", box.packs()));
        tooltip.add(Component.translatable("ygomc.box.mode." + box.mode().getSerializedName()));
        if (box.mode() == BoosterBoxDef.Mode.COLLATED) {
            for (CollationEntry entry : box.spec().fixedPulls().stream()
                    .distinct()
                    .map(id -> new CollationEntry(id, count(box, id)))
                    .toList()) {
                tooltip.add(Component.translatable("ygomc.box.guarantee", entry.pullId(), entry.count()));
            }
        }
        if (PackTips.shiftDown.getAsBoolean() && box.booster().isBound()) {
            tooltip.addAll(PackOpening.oddsLines(box.booster().value().mechanics()));
        }
    }

    private static int count(BoosterBoxDef box, String pullId) {
        int count = 0;
        Integer fixed = box.collation().get(pullId);
        if (fixed != null) {
            count = fixed;
        }
        return count;
    }
}
