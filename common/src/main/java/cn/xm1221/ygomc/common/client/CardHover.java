package cn.xm1221.ygomc.common.client;

import net.minecraft.Util;

/**
 * 光标在同一张卡上停够一段时间，才弹出大图。划过时不闪。
 */
final class CardHover {

    static final long DELAY_MS = 450;

    private int code = -1;
    private long since;

    void at(int code) {
        if (code != this.code) {
            this.code = code;
            since = Util.getMillis();
        }
    }

    void clear() {
        code = -1;
    }

    /** @return 还没停够，或当前没有卡时返回 -1 */
    int ready() {
        if (code < 0 || Util.getMillis() - since < DELAY_MS) {
            return -1;
        }
        return code;
    }
}
