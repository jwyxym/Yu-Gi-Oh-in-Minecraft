package cn.xm1221.ygomc.common.client;

import java.util.List;

/**
 * 组卡 / 卡册界面的贴图清单。纯数据，绘制在 {@link CollectionTextures}。
 *
 * <p>和场地贴图同一条规矩：按 id 找文件，缺了就让原版画出紫黑格，代码不去判断文件在不在。
 * 换皮时覆盖同名 png，不用改这里的 id。
 */
public final class CollectionTextureSpec {

    private CollectionTextureSpec() {
    }

    public static final String NAMESPACE = "ygomc";
    public static final String DIR = "textures/gui/collection/";

    /** 界面底，16×16，平铺，四边要接得上。 */
    public static final String PANEL = "panel";

    /** 一张卡下面的卡槽，画在卡图后面。 */
    public static final String SLOT = "slot";

    /** 分区标题下的色条。 */
    public static final String BAR = "bar";

    public static final List<Entry> ENTRIES = List.of(
            new Entry(PANEL, 16, 16, "界面底，平铺"),
            new Entry(SLOT, 32, 48, "卡槽，画在卡图后面"),
            new Entry(BAR, 16, 8, "分区标题下的色条"));

    public record Entry(String id, int w, int h, String usage) {
        public String path() {
            return DIR + id + ".png";
        }
    }

    public static Entry of(String id) {
        for (Entry entry : ENTRIES) {
            if (entry.id().equals(id)) {
                return entry;
            }
        }
        return null;
    }
}
