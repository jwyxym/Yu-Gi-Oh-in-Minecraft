package cn.xm1221.ygomc.common.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;

import java.util.HashMap;
import java.util.Map;

/** 开包界面贴图。画法和组卡界面相同。 */
public final class PackTextures {

    private static final Map<String, ResourceLocation> CACHE = new HashMap<>();

    private PackTextures() {
    }

    public static void background(GuiGraphics g, int x, int y, int w, int h) {
        tile(g, PackTextureSpec.BG, x, y, w, h);
    }

    public static void slot(GuiGraphics g, int x, int y, int w, int h) {
        stretch(g, PackTextureSpec.SLOT, x, y, w, h);
    }

    private static void stretch(GuiGraphics g, String id, int x, int y, int w, int h) {
        PackTextureSpec.Entry entry = PackTextureSpec.of(id);
        if (entry == null || w <= 0 || h <= 0) {
            return;
        }
        g.blit(location(id), x, y, w, h, 0.0F, 0.0F, entry.w(), entry.h(), entry.w(), entry.h());
    }

    private static void tile(GuiGraphics g, String id, int x, int y, int w, int h) {
        PackTextureSpec.Entry entry = PackTextureSpec.of(id);
        if (entry == null || w <= 0 || h <= 0) {
            return;
        }
        ResourceLocation loc = location(id);
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
                PackTextureSpec.NAMESPACE, PackTextureSpec.DIR + key + ".png"));
    }
}
