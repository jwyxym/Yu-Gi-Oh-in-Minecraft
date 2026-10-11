"""
生成本模组自绘的全部贴图：物品、决斗台方块、决斗场地格子、组卡界面。

卡面不在这里——卡图由渲染器从数据包的 pics.bin 解码；数据包里的卡背也不由本脚本提供。
`item/card.png` 是物品模型的兜底卡背，图案为原创，不仿 KONAMI 卡背。

尺寸必须与代码里的清单一致（`FieldTextureSpec`、`CollectionTextureSpec`），
blit 时按清单尺寸采样，画大了只会取到左上角一块。

- 物品 / 方块：16×16 逐像素绘制（字符网格 + 调色板），带 1px 暗描边、左上受光。
- GUI 格子：按 4 倍尺寸绘制后盒式缩小，格子会被拉伸到任意大小，笔画至少 2px。
- 牌垫 / 界面底：16×16 平铺，图案周期整除 16，四边自然接上。

输出是确定的（固定随机种子），重跑不会产生无意义的 git 改动。

用法:  python mkplaceholdertextures.py
"""
import math
import os
import random
import sys

try:
    from PIL import Image, ImageDraw
except ImportError:
    sys.exit("需要 Pillow：pip install Pillow")

REPO_ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
RES = os.path.join(REPO_ROOT, "common", "src", "main", "resources", "assets", "ygomc")
ITEM_DIR = os.path.join(RES, "textures", "item")
BLOCK_DIR = os.path.join(RES, "textures", "block")
DUEL_DIR = os.path.join(RES, "textures", "gui", "duel")
COLLECTION_DIR = os.path.join(RES, "textures", "gui", "collection")

S = 16
SS = 4          # GUI 超采样倍数


# ------------------------------------------------------------------ 工具

def grid(rows, palette):
    """字符网格 -> 16×16 RGBA。'.' 透明；其余字符查调色板。"""
    assert len(rows) == S, "行数 %d" % len(rows)
    img = Image.new("RGBA", (S, S), (0, 0, 0, 0))
    for y, row in enumerate(rows):
        assert len(row) == S, "第 %d 行长度 %d: %r" % (y, len(row), row)
        for x, ch in enumerate(row):
            if ch != ".":
                c = palette[ch]
                img.putpixel((x, y), c if len(c) == 4 else c + (255,))
    return img


def shade(c, f):
    """f>1 提亮，f<1 压暗。"""
    return tuple(max(0, min(255, int(v * f))) for v in c[:3])


def mix(a, b, t):
    return tuple(int(a[i] + (b[i] - a[i]) * t) for i in range(3))


def outline(img, color):
    """给不透明像素外围补一圈 4 邻域描边。"""
    src = img.copy()
    w, h = img.size
    for y in range(h):
        for x in range(w):
            if src.getpixel((x, y))[3]:
                continue
            for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
                nx, ny = x + dx, y + dy
                if 0 <= nx < w and 0 <= ny < h and src.getpixel((nx, ny))[3] > 128:
                    img.putpixel((x, y), color + (255,))
                    break
    return img


def save(img, path):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    img.save(path, "PNG", optimize=True)
    return path


# ------------------------------------------------------------------ 物品

def card_back():
    """兜底卡背：深蓝底、金框、中央椭圆环和青色光核。"""
    return grid([
        "................",
        "...kkkkkkkkkk...",
        "...kGGGGGGGGk...",
        "...kGnnnnnnhk...",
        "...kGnnyynnhk...",
        "...kGnyNNynhk...",
        "...kGyNwwNyhk...",
        "...kGyNwWNyhk...",
        "...kGyNWwNyhk...",
        "...kGyNwwNyhk...",
        "...kGnyNNynhk...",
        "...kGnnyynnhk...",
        "...kGnnnnnnhk...",
        "...kGhhhhhhhk...",
        "...kkkkkkkkkk...",
        "................",
    ], {
        "k": (18, 14, 30), "G": (236, 200, 104), "h": (148, 104, 40),
        "n": (28, 30, 66), "N": (50, 60, 124), "y": (214, 170, 70),
        "w": (120, 206, 250), "W": (226, 248, 255),
    })


def duel_disk():
    """决斗盘：左下圆形计分器，向右上伸出带五个卡位的刀翼。"""
    img = Image.new("RGBA", (S, S), (0, 0, 0, 0))
    # 刀翼是一条 45° 斜带（x+y 落在 13..16）。暗缝取相邻两条 x-y 对角线，才连成一道完整的阶梯
    for y in range(S):
        for x in range(6, 15):
            s = x + y
            if not 13 <= s <= 16 or y < 1:
                continue
            if (x - y) % 5 in (2, 3) and x - y > 0:
                c = (64, 70, 92)
            elif s == 13:
                c = (226, 232, 244)
            elif s == 16:
                c = (100, 108, 130)
            else:
                c = (166, 176, 198)
            img.putpixel((x, y), c + (255,))
    cx, cy = 5.5, 10.5
    for y in range(S):
        for x in range(S):
            r = math.hypot(x + 0.5 - cx, y + 0.5 - cy)
            if r <= 4.6:
                light = (x + 0.5 - cx) + (y + 0.5 - cy) < -1.5
                if r > 3.3:
                    c = (196, 204, 220) if light else (124, 132, 152)
                elif r > 2.2:
                    c = (46, 50, 64)
                else:
                    c = (230, 74, 70) if r > 1.0 else (255, 196, 150)
                img.putpixel((x, y), c + (255,))
    return outline(img, (24, 26, 36))


DECK_BOX_ROWS = [
    "................",
    "...kkkkkkkkkk...",
    "...kLLLLLLLLk...",
    "...kDDDDDDDDk...",
    "...kDDDyYDDDk...",
    "...kkkkyYkkkk...",
    "...kBbbbbbbdk...",
    "...kBbeeeebdk...",
    "...kBbeEEebdk...",
    "...kBbeEEebdk...",
    "...kBbeEEebdk...",
    "...kBbeeeebdk...",
    "...kBbbbbbbdk...",
    "...kddddddddk...",
    "...kkkkkkkkkk...",
    "................",
]


def deck_box():
    """卡组盒：竖长的翻盖皮盒、金扣、盒身压印卡框。"""
    return grid(DECK_BOX_ROWS, {
        "k": (40, 24, 14), "L": (176, 124, 78), "l": (140, 94, 56),
        "D": (104, 68, 40), "y": (236, 198, 96), "Y": (166, 122, 44),
        "B": (156, 108, 66), "b": (126, 84, 50), "d": (90, 58, 34),
        "e": (104, 68, 40), "E": (176, 132, 64),
    })


def ender_deck_box():
    """末影卡盒：同一盒形，黑紫色，扣子换成末影之眼的青色。"""
    return grid(DECK_BOX_ROWS, {
        "k": (10, 6, 16), "L": (70, 50, 96), "l": (50, 34, 72),
        "D": (36, 24, 54), "y": (150, 255, 224), "Y": (32, 170, 140),
        "B": (60, 42, 84), "b": (42, 28, 62), "d": (28, 18, 42),
        "e": (22, 120, 104), "E": (190, 255, 236),
    })


def card_pack():
    """卡包：上下压齿封口、红色品牌条、金框卡窗、锡箔斜光。"""
    img = grid([
        "...sSsSsSsSsS...",
        "...kkkkkkkkkk...",
        "...kRRRRRRRRk...",
        "...krrrrrrrrk...",
        "...kFFFFFFFfk...",
        "...kFggggggfk...",
        "...kFgAAAAgfk...",
        "...kFgAeeAgfk...",
        "...kFgAeeAgfk...",
        "...kFgAAAAgfk...",
        "...kFggggggfk...",
        "...kFFFFFFFfk...",
        "...kfffffffFk...",
        "...kkkkkkkkkk...",
        "...sSsSsSsSsS...",
        "................",
    ], {
        "s": (120, 128, 150), "S": (196, 204, 222), "k": (22, 26, 44),
        "R": (220, 64, 60), "r": (150, 34, 40),
        "F": (126, 160, 214), "f": (78, 104, 160),
        "g": (232, 194, 92), "A": (36, 52, 104), "e": (255, 236, 150),
    })
    for y in range(4, 13):
        for x in range(4, 12):
            p = img.getpixel((x, y))
            if (x + y) in (14, 15) and p[:3] in ((126, 160, 214), (78, 104, 160)):
                img.putpixel((x, y), (214, 232, 255, 255))
    return img


def booster_box():
    """卡盒（整盒零售）：掀起的盒盖、露出一排包顶、正面印着品牌条与卡窗。"""
    return grid([
        "................",
        "..kkkkkkkkkkkk..",
        "..kLLLLLLLLLLk..",
        "..kLlllgglllLk..",
        "..kkkkkkkkkkkk..",
        "..kPpQqPpQqPpk..",
        "..kpoqzpoqzpok..",
        "..kkkkkkkkkkkk..",
        "..kRRRRRRRRRRk..",
        "..krrrrrrrrrrk..",
        "..kFFFFFFFFFfk..",
        "..kFFFgggFFFfk..",
        "..kFFFgegFFFfk..",
        "..kFFFgggFFFfk..",
        "..kffffffffffk..",
        "..kkkkkkkkkkkk..",
    ], {
        "k": (22, 26, 44), "L": (78, 104, 160), "l": (56, 76, 126),
        "g": (232, 194, 92), "e": (255, 240, 170),
        "P": (196, 204, 222), "p": (126, 160, 214), "o": (90, 120, 176),
        "Q": (236, 120, 110), "q": (190, 70, 64), "z": (140, 44, 44),
        "R": (220, 64, 60), "r": (150, 34, 40),
        "F": (126, 160, 214), "f": (78, 104, 160),
    })


BINDER_ROWS = [
    "................",
    ".kkkkkkkkkkkkkk.",
    ".kSskCCCCCCCCCdk",
    ".kSskCccccccccdk",
    "mMMskCcllllllcdk",
    ".kSskCclLLLLlcdk",
    ".kSskCclLxxLlcdk",
    ".kSskCclLxxLlcdk",
    "mMMskCclLLLLlcdk",
    ".kSskCcllllllcdk",
    ".kSskCccccccccdk",
    ".kSskCccccccccdk",
    "mMMskCccccccccdk",
    ".kSskddddddddddk",
    ".kkkkkkkkkkkkkk.",
    "................",
]


def binder(cover, spine, label, mark, ring):
    """卡册的共用版式：左侧书脊穿三枚环，封面中间一枚标签。"""
    return grid(BINDER_ROWS, {
        "k": shade(cover, 0.28), "S": shade(spine, 1.15), "s": shade(spine, 0.8),
        "C": shade(cover, 1.25), "c": cover, "d": shade(cover, 0.7),
        "l": shade(label, 0.62), "L": label, "x": mark,
        "M": ring, "m": shade(ring, 0.58),
    })


def card_binder():
    return binder(cover=(164, 52, 58), spine=(104, 30, 36), label=(232, 214, 176),
                  mark=(168, 52, 58), ring=(210, 214, 224))


def creative_binder():
    return binder(cover=(120, 66, 188), spine=(76, 38, 128), label=(246, 238, 255),
                  mark=(236, 196, 90), ring=(240, 204, 104))


def ender_binder():
    return binder(cover=(46, 26, 70), spine=(28, 14, 44), label=(30, 150, 128),
                  mark=(200, 255, 236), ring=(120, 232, 196))


# ------------------------------------------------------------------ 方块

FELT = (38, 98, 68)
WOOD = (122, 80, 46)


def duel_table_top():
    """台面：木框、金线、绿绒上画着两侧各三格的卡位。"""
    rnd = random.Random(7)
    img = Image.new("RGBA", (S, S))
    for y in range(S):
        for x in range(S):
            edge = x in (0, S - 1) or y in (0, S - 1)
            if edge:
                c = shade(WOOD, 1.1 if (x + y) % 3 else 0.9)
            elif x in (1, S - 2) or y in (1, S - 2):
                c = (210, 170, 78)
            else:
                c = shade(FELT, 0.94 + rnd.random() * 0.12)
            img.putpixel((x, y), c + (255,))
    line = shade(FELT, 1.55)
    for x0 in (3, 7, 11):
        for y0 in (3, 10):
            for y in range(y0, y0 + 3):
                for x in range(x0, x0 + 2):
                    img.putpixel((x, y), line + (255,))
    for x in range(3, 13):
        img.putpixel((x, 7), shade(FELT, 0.7) + (255,))
        img.putpixel((x, 8), shade(FELT, 1.25) + (255,))
    return img


def duel_table_side():
    """台侧：顶沿金线、深色裙板、竖向木纹。"""
    rnd = random.Random(11)
    img = Image.new("RGBA", (S, S))
    for y in range(S):
        for x in range(S):
            if y == 0:
                c = (210, 170, 78)
            elif y == 1:
                c = shade(FELT, 0.8)
            elif y == 2:
                c = shade(WOOD, 0.55)
            elif x in (0, S - 1) or y == S - 1:
                c = shade(WOOD, 0.62)
            elif 4 <= y <= 12 and 3 <= x <= 12:
                inner = x in (3, 12) or y in (4, 12)
                c = shade(WOOD, 0.62 if inner else 0.8 + rnd.random() * 0.06)
            else:
                grain = 0.92 + (0.1 if x % 4 == 1 else 0) + rnd.random() * 0.05
                c = shade(WOOD, grain)
            img.putpixel((x, y), c + (255,))
    img.putpixel((7, 8), (232, 194, 92, 255))
    img.putpixel((8, 8), (166, 122, 44, 255))
    return img


def duel_table_bottom():
    """台底：横向木板。"""
    rnd = random.Random(13)
    img = Image.new("RGBA", (S, S))
    for y in range(S):
        for x in range(S):
            if y % 4 == 3:
                c = shade(WOOD, 0.55)
            else:
                c = shade(WOOD, 0.78 + rnd.random() * 0.08)
            img.putpixel((x, y), c + (255,))
    return img


# ------------------------------------------------------------------ 决斗场地

ZONES = {
    "zone_monster": {"rim": (232, 156, 76), "fill": (78, 40, 20), "emblem": "monster"},
    "zone_spell": {"rim": (70, 206, 176), "fill": (14, 60, 54), "emblem": "spell"},
    "zone_field": {"rim": (120, 170, 240), "fill": (20, 36, 76), "emblem": "field"},
    "zone_pile": {"rim": (176, 188, 206), "fill": (30, 34, 46), "emblem": "pile"},
}
GOLD = (236, 200, 104)


def rgba(c, a):
    return c[:3] + (a,)


def emblem(d, kind, cx, cy, size, color, width):
    r = size
    if kind == "monster":
        # 盾形纹章 + 竖剑
        top, bottom = cy - r, cy + r * 1.15
        shield = [(cx - r * 0.85, top), (cx + r * 0.85, top), (cx + r * 0.85, cy + r * 0.1),
                  (cx, bottom), (cx - r * 0.85, cy + r * 0.1)]
        d.line(shield + [shield[0]], fill=color, width=width, joint="curve")
        d.line([(cx, top + r * 0.3), (cx, bottom - r * 0.35)], fill=color, width=width)
        d.line([(cx - r * 0.4, cy - r * 0.15), (cx + r * 0.4, cy - r * 0.15)], fill=color, width=width)
    elif kind == "spell":
        # 法阵：外圆 + 六芒星
        d.ellipse([cx - r, cy - r, cx + r, cy + r], outline=color, width=width)
        pts = [(cx + r * 0.86 * math.sin(math.radians(a)), cy - r * 0.86 * math.cos(math.radians(a)))
               for a in range(0, 360, 60)]
        for tri in (pts[0::2], pts[1::2]):
            d.line(tri + [tri[0]], fill=color, width=width, joint="curve")
    elif kind == "field":
        # 罗盘：双圆 + 四向尖芒
        d.ellipse([cx - r * 0.55, cy - r * 0.55, cx + r * 0.55, cy + r * 0.55], outline=color, width=width)
        for a in range(0, 360, 90):
            s, c = math.sin(math.radians(a)), math.cos(math.radians(a))
            tip = (cx + s * r * 1.2, cy - c * r * 1.2)
            l = (cx + c * r * 0.22, cy + s * r * 0.22)
            rr = (cx - c * r * 0.22, cy - s * r * 0.22)
            d.polygon([tip, l, rr], fill=color)
    elif kind == "pile":
        # 两张错开的牌框，示意「一叠」
        w, h = r * 1.05, r * 1.45
        for off in (-r * 0.22, r * 0.22):
            d.rounded_rectangle([cx - w / 2 + off, cy - h / 2 - off, cx + w / 2 + off, cy + h / 2 - off],
                                radius=r * 0.12, outline=color, width=width)


def zone(spec, w, h):
    W, H = w * SS, h * SS
    img = Image.new("RGBA", (W, H), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    rim, fill = spec["rim"], spec["fill"]
    pile = spec["emblem"] == "pile"
    base_alpha = 70 if pile else 120

    # 底：自上而下渐暗的半透明色，让牌垫透出来
    for y in range(H):
        t = y / H
        c = mix(shade(fill, 1.35), shade(fill, 0.8), t)
        d.line([(0, y), (W, y)], fill=rgba(c, base_alpha))
    mask = Image.new("L", (W, H), 0)
    ImageDraw.Draw(mask).rounded_rectangle([2 * SS, 2 * SS, W - 2 * SS - 1, H - 2 * SS - 1],
                                           radius=6 * SS, fill=255)
    clear = Image.new("RGBA", (W, H), (0, 0, 0, 0))
    img = Image.composite(img, clear, mask)
    d = ImageDraw.Draw(img)

    # 外框：亮边 + 内侧暗线
    d.rounded_rectangle([2 * SS, 2 * SS, W - 2 * SS - 1, H - 2 * SS - 1], radius=6 * SS,
                        outline=rgba(rim, 235), width=3 * SS)
    d.rounded_rectangle([5 * SS, 5 * SS, W - 5 * SS - 1, H - 5 * SS - 1], radius=4 * SS,
                        outline=rgba(shade(rim, 0.35), 200), width=1 * SS)
    # 内框细线
    d.rounded_rectangle([8 * SS, 8 * SS, W - 8 * SS - 1, H - 8 * SS - 1], radius=3 * SS,
                        outline=rgba(rim, 70 if pile else 90), width=1 * SS)

    # 四角金色折角
    if not pile:
        L, t, m = 9 * SS, 2 * SS, 8 * SS
        for (x, y, sx, sy) in ((m, m, 1, 1), (W - m, m, -1, 1), (m, H - m, 1, -1), (W - m, H - m, -1, -1)):
            for (lx, ly) in ((L, t), (t, L)):
                x1, y1 = x + sx * lx, y + sy * ly
                d.rectangle([min(x, x1), min(y, y1), max(x, x1), max(y, y1)], fill=rgba(GOLD, 220))

    # 中央纹章
    size = min(W, H) * (0.2 if pile else 0.24)
    emblem(d, spec["emblem"], W / 2, H / 2, size, rgba(rim, 80 if pile else 120), int(2.5 * SS))

    return img.resize((w, h), Image.BOX)


def mat():
    """牌垫：绿绒底 + 周期 8 的菱形织纹 + 细噪点。"""
    rnd = random.Random(3)
    base = (40, 90, 66)
    img = Image.new("RGBA", (S, S))
    for y in range(S):
        for x in range(S):
            f = 0.95 + rnd.random() * 0.08
            if (x + y) % 8 == 0 or (x - y) % 8 == 0:
                f += 0.1
            img.putpixel((x, y), shade(base, f) + (255,))
    return img


# ------------------------------------------------------------------ 组卡界面

PANEL = (24, 30, 50)


def collection_panel():
    """界面底：深蓝、周期 8 的暗菱格、细噪点。"""
    rnd = random.Random(5)
    img = Image.new("RGBA", (S, S))
    for y in range(S):
        for x in range(S):
            f = 0.96 + rnd.random() * 0.06
            if (x + y) % 8 == 0 or (x - y) % 8 == 0:
                f += 0.14
            img.putpixel((x, y), shade(PANEL, f) + (255,))
    return img


def collection_slot():
    """卡槽：金色双线框、深色半透明里子、四角高光。中央会被卡图盖住。"""
    w, h = 32, 48
    W, H = w * SS, h * SS
    img = Image.new("RGBA", (W, H), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    d.rounded_rectangle([0, 0, W - 1, H - 1], radius=3 * SS, fill=(10, 12, 22, 190))
    d.rounded_rectangle([0, 0, W - 1, H - 1], radius=3 * SS, outline=(196, 158, 76, 255), width=SS)
    d.rounded_rectangle([2 * SS, 2 * SS, W - 2 * SS - 1, H - 2 * SS - 1], radius=2 * SS,
                        outline=(96, 72, 34, 255), width=SS)
    for (x, y) in ((3, 3), (w - 4, 3), (3, h - 4), (w - 4, h - 4)):
        d.rectangle([x * SS, y * SS, (x + 1) * SS - 1, (y + 1) * SS - 1], fill=(250, 222, 140, 255))
    emblem(d, "pile", W / 2, H / 2, W * 0.22, (90, 100, 130, 110), int(1.5 * SS))
    return img.resize((w, h), Image.BOX)


def collection_bar():
    """分区标题条。会被横向拉宽，所以每一行同色，只在竖直方向做变化。"""
    img = Image.new("RGBA", (16, 8))
    rows = [(226, 190, 100), (70, 92, 140), (58, 78, 124), (52, 70, 114),
            (46, 62, 104), (40, 54, 94), (100, 78, 36), (196, 158, 76)]
    for y, c in enumerate(rows):
        for x in range(16):
            img.putpixel((x, y), c + (255,))
    return img


# ------------------------------------------------------------------ 入口

def main():
    outputs = [
        (ITEM_DIR, {
            "card": card_back(),
            "duel_disk": duel_disk(),
            "deck_box": deck_box(),
            "ender_deck_box": ender_deck_box(),
            "card_pack": card_pack(),
            "booster_box": booster_box(),
            "card_binder": card_binder(),
            "creative_binder": creative_binder(),
            "ender_binder": ender_binder(),
        }),
        (BLOCK_DIR, {
            "duel_table_top": duel_table_top(),
            "duel_table_side": duel_table_side(),
            "duel_table_bottom": duel_table_bottom(),
        }),
        (DUEL_DIR, {
            "zone_monster": zone(ZONES["zone_monster"], 64, 96),
            "zone_spell": zone(ZONES["zone_spell"], 64, 96),
            "zone_field": zone(ZONES["zone_field"], 64, 96),
            "zone_pile": zone(ZONES["zone_pile"], 64, 72),
            "mat": mat(),
        }),
        (COLLECTION_DIR, {
            "panel": collection_panel(),
            "slot": collection_slot(),
            "bar": collection_bar(),
        }),
    ]
    for directory, images in outputs:
        print("->", directory)
        for name, img in images.items():
            p = save(img, os.path.join(directory, name + ".png"))
            print("   %-18s %3dx%-3d %6d B" % (name + ".png", img.size[0], img.size[1], os.path.getsize(p)))


if __name__ == "__main__":
    main()
