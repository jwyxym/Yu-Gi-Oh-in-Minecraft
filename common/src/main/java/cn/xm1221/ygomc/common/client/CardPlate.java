package cn.xm1221.ygomc.common.client;

import java.util.ArrayList;
import java.util.List;

/**
 * 一张卡的薄板：正面、牌背、四条纸边。不依赖 Minecraft，顶点顺序就是渲染时提交的顺序。
 *
 * <p>每个面从外侧看都是逆时针。侧面走会剔除背面的渲染类型，绕序反了厚度就整片消失。
 * 牌背的 u 沿 -X 增大，从背后看图案才是正的，不是左右镜像。
 */
public final class CardPlate {

    /** 厚度 = 卡高的 1/50。再厚就像砖，再薄从侧面看不见。 */
    public static final float THICKNESS = 0.9F / 50.0F;

    /** 纸边色。不能是纯白，否则侧面等于没上色。 */
    public static final int EDGE_ARGB = 0xFFF3E8D4;

    private static final float ASPECT_W = 200.0F;
    private static final float ASPECT_H = 290.0F;
    private static final int WHITE = 0xFFFFFFFF;

    private final float height;
    private final float width;
    private final float thickness;
    private final float aspectW;
    private final float aspectH;
    private final List<Quad> quads;

    private CardPlate(float height, float width, float thickness, float aspectW, float aspectH, List<Quad> quads) {
        this.height = height;
        this.width = width;
        this.thickness = thickness;
        this.aspectW = aspectW;
        this.aspectH = aspectH;
        this.quads = List.copyOf(quads);
    }

    public static CardPlate of(float height, float thickness) {
        return of(height, thickness, ASPECT_W, ASPECT_H);
    }

    public static CardPlate of(float height, float thickness, float aspectW, float aspectH) {
        if (!(height > 0.0F) || !(thickness > 0.0F) || !(aspectW > 0.0F) || !(aspectH > 0.0F)) {
            throw new IllegalArgumentException(
                    "卡高、厚度、长宽比都必须是正数: h=" + height + " t=" + thickness
                            + " aw=" + aspectW + " ah=" + aspectH);
        }
        float h = height;
        float w = h * aspectW / aspectH;
        float x0 = 0.5F - w / 2.0F;
        float x1 = 0.5F + w / 2.0F;
        float y0 = 0.5F - h / 2.0F;
        float y1 = 0.5F + h / 2.0F;
        float zf = 0.5F + thickness / 2.0F;
        float zb = 0.5F - thickness / 2.0F;

        ArrayList<Quad> faces = new ArrayList<>(6);
        // 正面朝 +Z。u 沿 +X 增，v 沿 -Y 增。绕序从 +Z 外侧看是逆时针。
        faces.add(new Quad(Material.FRONT, WHITE, List.of(
                corner(x0, y1, zf, 0.0F, 0.0F, 0.0F, 0.0F, 1.0F),
                corner(x0, y0, zf, 0.0F, 1.0F, 0.0F, 0.0F, 1.0F),
                corner(x1, y0, zf, 1.0F, 1.0F, 0.0F, 0.0F, 1.0F),
                corner(x1, y1, zf, 1.0F, 0.0F, 0.0F, 0.0F, 1.0F))));
        // 牌背朝 -Z。u 沿 -X 增，从背后看才不镜像。
        faces.add(new Quad(Material.BACK, WHITE, List.of(
                corner(x1, y1, zb, 0.0F, 0.0F, 0.0F, 0.0F, -1.0F),
                corner(x1, y0, zb, 0.0F, 1.0F, 0.0F, 0.0F, -1.0F),
                corner(x0, y0, zb, 1.0F, 1.0F, 0.0F, 0.0F, -1.0F),
                corner(x0, y1, zb, 1.0F, 0.0F, 0.0F, 0.0F, -1.0F))));
        // 上、下、右、左。绕序都是从外侧看的逆时针。
        faces.add(new Quad(Material.EDGE, EDGE_ARGB, List.of(
                corner(x0, y1, zf, 0.0F, 0.0F, 0.0F, 1.0F, 0.0F),
                corner(x1, y1, zf, 1.0F, 0.0F, 0.0F, 1.0F, 0.0F),
                corner(x1, y1, zb, 1.0F, 1.0F, 0.0F, 1.0F, 0.0F),
                corner(x0, y1, zb, 0.0F, 1.0F, 0.0F, 1.0F, 0.0F))));
        faces.add(new Quad(Material.EDGE, EDGE_ARGB, List.of(
                corner(x0, y0, zf, 0.0F, 0.0F, 0.0F, -1.0F, 0.0F),
                corner(x0, y0, zb, 0.0F, 1.0F, 0.0F, -1.0F, 0.0F),
                corner(x1, y0, zb, 1.0F, 1.0F, 0.0F, -1.0F, 0.0F),
                corner(x1, y0, zf, 1.0F, 0.0F, 0.0F, -1.0F, 0.0F))));
        faces.add(new Quad(Material.EDGE, EDGE_ARGB, List.of(
                corner(x1, y1, zf, 0.0F, 0.0F, 1.0F, 0.0F, 0.0F),
                corner(x1, y0, zf, 0.0F, 1.0F, 1.0F, 0.0F, 0.0F),
                corner(x1, y0, zb, 1.0F, 1.0F, 1.0F, 0.0F, 0.0F),
                corner(x1, y1, zb, 1.0F, 0.0F, 1.0F, 0.0F, 0.0F))));
        faces.add(new Quad(Material.EDGE, EDGE_ARGB, List.of(
                corner(x0, y1, zf, 0.0F, 0.0F, -1.0F, 0.0F, 0.0F),
                corner(x0, y1, zb, 1.0F, 0.0F, -1.0F, 0.0F, 0.0F),
                corner(x0, y0, zb, 1.0F, 1.0F, -1.0F, 0.0F, 0.0F),
                corner(x0, y0, zf, 0.0F, 1.0F, -1.0F, 0.0F, 0.0F))));
        return new CardPlate(h, w, thickness, aspectW, aspectH, faces);
    }

    public float height() {
        return height;
    }

    public float width() {
        return width;
    }

    public float thickness() {
        return thickness;
    }

    public float aspectW() {
        return aspectW;
    }

    public float aspectH() {
        return aspectH;
    }

    public List<Quad> quads() {
        return quads;
    }

    public int count(Material material) {
        int n = 0;
        for (Quad quad : quads) {
            if (quad.material == material) {
                n++;
            }
        }
        return n;
    }

    /** @return 该材质的第一张面；没有时 {@code null} */
    public Quad face(Material material) {
        for (Quad quad : quads) {
            if (quad.material == material) {
                return quad;
            }
        }
        return null;
    }

    private static Corner corner(float x, float y, float z, float u, float v, float nx, float ny, float nz) {
        return new Corner(x, y, z, u, v, nx, ny, nz);
    }

    public enum Material {
        FRONT, BACK, EDGE
    }

    public record Corner(float x, float y, float z, float u, float v, float nx, float ny, float nz) {
    }

    public record Quad(Material material, int colorArgb, List<Corner> corners) {
    }
}
