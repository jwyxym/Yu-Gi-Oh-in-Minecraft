package cn.xm1221.ygomc.common.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;

import java.util.HashMap;
import java.util.Map;

/**
 * 组卡界面贴图的绘制。加载方式和 {@link FieldTextures} 相同：不检查文件在不在。
 */
public final class CollectionTextures {

    private static final Map<String, ResourceLocation> CACHE = new HashMap<>();

    private CollectionTextures() {
    }

    public static void panel(GuiGraphics g, int x, int y, int w, int h) {
        tile(g, CollectionTextureSpec.PANEL, x, y, w, h);
    }

    public static void slot(GuiGraphics g, int x, int y, int w, int h) {
        stretch(g, CollectionTextureSpec.SLOT, x, y, w, h);
    }

    public static void bar(GuiGraphics g, int x, int y, int w, int h) {
        stretch(g, CollectionTextureSpec.BAR, x, y, w, h);
    }

    private static void stretch(GuiGraphics g, String id, int x, int y, int w, int h) {
        CollectionTextureSpec.Entry entry = CollectionTextureSpec.of(id);
        if (entry == null || w <= 0 || h <= 0) {
            return;
        }
        g.blit(location(entry.id()), x, y, w, h, 0.0F, 0.0F, entry.w(), entry.h(), entry.w(), entry.h());
    }

    private static void tile(GuiGraphics g, String id, int x, int y, int w, int h) {
        CollectionTextureSpec.Entry entry = CollectionTextureSpec.of(id);
        if (entry == null || w <= 0 || h <= 0) {
            return;
        }
        ResourceLocation loc = location(entry.id());
        int bottom = y + h;
        int right = x + w;
        for (int py = y; py < bottom; py += entry.h()) {
            int th = Math.min(entry.h(), bottom - py);
            for (int px = x; px < right; px += entry.w()) {
                int tw = Math.min(entry.w(), right - px);
                g.blit(loc, px, py, tw, th, 0.0F, 0.0F, tw, th, entry.w(), entry.h());
            }
        }
    }

    private static ResourceLocation location(String id) {
        return CACHE.computeIfAbsent(id, key -> ResourceLocation.fromNamespaceAndPath(
                CollectionTextureSpec.NAMESPACE, CollectionTextureSpec.DIR + key + ".png"));
    }
}
