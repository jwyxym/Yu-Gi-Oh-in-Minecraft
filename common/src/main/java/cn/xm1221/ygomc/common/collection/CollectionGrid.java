package cn.xm1221.ygomc.common.collection;

/**
 * 一组卡格的几何：几列、点到第几张、滚到哪一行。
 *
 * <p>不碰 Minecraft。点错格不会报错，只会取出另一张卡，所以命中要能单独断言。
 */
public final class CollectionGrid {

    public static final int CELL_W = 24;
    public static final int CELL_H = 34;
    public static final int GAP = 2;

    private final int x;
    private final int y;
    private final int cols;
    private final int rows;
    private final int cellW;
    private final int cellH;
    private final int gap;

    public CollectionGrid(int x, int y, int cols, int rows, int cellW, int cellH, int gap) {
        this.x = x;
        this.y = y;
        this.cols = Math.max(1, cols);
        this.rows = Math.max(1, rows);
        this.cellW = cellW;
        this.cellH = cellH;
        this.gap = gap;
    }

    /** 在给定像素范围内尽量铺满，至少 1×1。 */
    public static CollectionGrid fit(int x, int y, int width, int height, int cellW, int cellH, int gap) {
        int cols = Math.max(1, (width + gap) / (cellW + gap));
        int rows = Math.max(1, (height + gap) / (cellH + gap));
        return new CollectionGrid(x, y, cols, rows, cellW, cellH, gap);
    }

    public int x() {
        return x;
    }

    public int y() {
        return y;
    }

    public int cols() {
        return cols;
    }

    public int rows() {
        return rows;
    }

    public int cellW() {
        return cellW;
    }

    public int cellH() {
        return cellH;
    }

    public int capacity() {
        return cols * rows;
    }

    public int maxScrollRows(int total) {
        int need = (total + cols - 1) / cols;
        return Math.max(0, need - rows);
    }

    public int clampScroll(int scrollRows, int total) {
        return Math.max(0, Math.min(scrollRows, maxScrollRows(total)));
    }

    /**
     * @param scrollRows 已经滚掉的行数
     * @param total      列表总张数
     * @return 列表下标；点在缝里、点在空格上、或越界时返回 -1
     */
    public int hit(double mx, double my, int scrollRows, int total) {
        if (mx < x || my < y) {
            return -1;
        }
        int localCol = (int) ((mx - x) / (cellW + gap));
        int localRow = (int) ((my - y) / (cellH + gap));
        if (localCol < 0 || localRow < 0 || localCol >= cols || localRow >= rows) {
            return -1;
        }
        int cx = x + localCol * (cellW + gap);
        int cy = y + localRow * (cellH + gap);
        if (mx >= cx + cellW || my >= cy + cellH) {
            return -1;
        }
        int index = (scrollRows + localRow) * cols + localCol;
        if (index < 0 || index >= total) {
            return -1;
        }
        return index;
    }

    /** @param local 当前这一屏里的第几格，0 起 */
    public int cellX(int local) {
        return x + (local % cols) * (cellW + gap);
    }

    public int cellY(int local) {
        return y + (local / cols) * (cellH + gap);
    }
}
