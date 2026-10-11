package cn.xm1221.ygomc.common.client;

import java.util.function.BooleanSupplier;

/**
 * 物品提示要知道玩家有没有按着 Shift，而那个查询只能在客户端做。
 * 服务端上这个开关保持关闭，概率行不出现。
 */
public final class PackTips {

    public static BooleanSupplier shiftDown = () -> false;

    private PackTips() {
    }
}
