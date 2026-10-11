package cn.xm1221.ygomc.common.duel;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;
import java.util.UUID;

/**
 * 便携决斗盘：向另一名玩家发起对局邀请（PLAN §5.2 的「便携入口」）。
 *
 * <h2>为什么交互挂在 {@code interactLivingEntity} 而不是 {@code use}</h2>
 * {@code use} 只在「空手/持物对空气或方块右键」时触发，拿不到「你对准了哪个玩家」。
 * 而决斗盘的语义就是<b>指定一个对手</b>，所以必须用
 * {@link Item#interactLivingEntity}——它带着被点击的实体一起来。
 * 这也是 YgoDuelingMod 验证过的做法（{@code DuelDiskItem} 用同一族回调）。
 *
 * <h2>为什么逻辑入口必须显式判 {@code isClientSide}</h2>
 * 这个回调在<b>两端都会跑</b>。如果客户端也去走一遍房间状态机，就会得到
 * 「客户端自己以为开了房、服务端完全不知道」的分裂状态。
 * 约定：客户端只负责发动作（这里就是原版靠 {@code InteractionResult} 自动回包的
 * 交互），房间的建立与校验一律在服务端；这里先早早返回，
 * 让服务端成为唯一的权威。
 */
public class DuelDiskItem extends Item {

    public DuelDiskItem(Properties properties) {
        super(properties);
    }

    /**
     * 手持决斗盘右键一名玩家：邀请他。如果右键的正是邀请你的那个人，则改成接受。
     */
    @Override
    public InteractionResult interactLivingEntity(ItemStack stack, Player player, LivingEntity target,
                                                 InteractionHand hand) {
        if (!(target instanceof ServerPlayer to) || !(player instanceof ServerPlayer from)) {
            return player.level().isClientSide() && target instanceof Player
                    ? InteractionResult.SUCCESS : InteractionResult.PASS;
        }
        UUID pending = DuelLobby.pendingInviter(from);
        String message = pending != null && pending.equals(to.getUUID())
                ? DuelLobby.accept(from)
                : DuelLobby.invite(from, to);
        from.displayClientMessage(Component.literal(message), false);
        return InteractionResult.SUCCESS;
    }

    /**
     * 对着空气右键：
     * <ul>
     *   <li><b>自己正有一局</b> → 客户端把对局界面拉回来（决斗盘就是「回到牌桌前」的入口）；</li>
     *   <li>没有对局 → 和本地贪心对手开一局（单人可测）。</li>
     * </ul>
     *
     * <p>「拉回界面」只能在客户端做：界面是客户端的东西。但客户端不能因此改变
     * 房间状态（那会造出「客户端以为开了房」的分裂状态），所以客户端那侧只是
     * 调 {@link DuelDiskHooks}，服务端仍旧是唯一权威。
     *
     * <p>注意返回类型必须是 {@link InteractionResultHolder}，这是
     * {@code Item#use} 在 1.21 的签名（它要把「用完这格物品变成什么」一并带回去）；
     * 写成 {@link InteractionResult} 会编译不过。
     */
    /**
     * 对着空气右键：对局中把界面拉回来；有人邀请你就接受；否则说明怎么约战。
     */
    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide()) {
            DuelDiskHooks.reopenScreen();
            return InteractionResultHolder.success(stack);
        }
        if (!(player instanceof ServerPlayer server)) {
            return InteractionResultHolder.pass(stack);
        }
        if (DuelRoom.isDueling(server)) {
            return InteractionResultHolder.success(stack);
        }
        if (DuelLobby.pendingInviter(server) != null) {
            server.displayClientMessage(Component.literal(DuelLobby.accept(server)), false);
            return InteractionResultHolder.success(stack);
        }
        server.displayClientMessage(Component.translatable("ygomc.lobby.how"), false);
        return InteractionResultHolder.success(stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        tooltip.add(Component.translatable("ygomc.lobby.disk_tip"));
    }
}
