package cn.xm1221.ygomc.common.client;

import java.util.List;

/** 开包界面的贴图。缺文件时原版画紫黑格，这里不去判断文件在不在。 */
public final class PackTextureSpec {

    public static final String NAMESPACE = "ygomc";
    public static final String DIR = "textures/gui/pack/";
    public static final String BG = "bg";
    public static final String SLOT = "card_slot";

    public static final List<Entry> ENTRIES = List.of(
            new Entry(BG, 16, 16, "界面底，平铺"),
            new Entry(SLOT, 32, 48, "一张卡后面的槽"));

    private PackTextureSpec() {
    }

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
