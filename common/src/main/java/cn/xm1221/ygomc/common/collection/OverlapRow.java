package cn.xm1221.ygomc.common.collection;

/**
 * 一排会重叠的卡。张数再多也挤进给定宽度，不靠滚动。
 *
 * <p>后画的盖住先画的，点中的是最上面那张，所以命中从右往左测。
 */
public final class OverlapRow {

    private OverlapRow() {
    }

    /** @return 每张卡的左边缘。空列表给空数组。 */
    public static int[] xs(int x, int width, int cardW, int count) {
        if (count <= 0 || cardW <= 0 || width <= 0) {
            return new int[0];
        }
        int[] out = new int[count];
        if (count == 1) {
            out[0] = x;
            return out;
        }
        int span = Math.max(0, width - cardW);
        int step = Math.min(cardW, span / (count - 1));
        if (step < 1) {
            step = 1;
        }
        for (int i = 0; i < count; i++) {
            out[i] = x + i * step;
        }
        return out;
    }

    /**
     * @return 点中的下标；缝外或没有卡时是 -1。最右边的一张优先。
     */
    public static int hit(int[] xs, int cardW, int cardH, int y, double mx, double my) {
        if (xs == null || my < y || my >= y + cardH) {
            return -1;
        }
        for (int i = xs.length - 1; i >= 0; i--) {
            if (mx >= xs[i] && mx < xs[i] + cardW) {
                return i;
            }
        }
        return -1;
    }
}
