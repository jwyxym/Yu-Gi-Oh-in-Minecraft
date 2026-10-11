package cn.xm1221.ygomc.common.pack.model;

import cn.xm1221.ygomc.common.card.CardRarity;

/**
 * 池里的一张印制，也是开包抽出来的一张卡。
 *
 * @param code    卡号
 * @param rarity  这一印的稀有度
 * @param variant 异画序号，0 为最初版
 * @param weight  同一稀有度池内的权重，默认 1
 */
public record PrintRef(int code, CardRarity rarity, int variant, int weight) {

    public PrintRef {
        if (rarity == null) {
            throw new IllegalArgumentException("印制的稀有度不能为空");
        }
        if (weight <= 0) {
            throw new IllegalArgumentException("印制权重必须为正，实得 " + weight);
        }
    }

    public static PrintRef of(int code, CardRarity rarity) {
        return new PrintRef(code, rarity, 0, 1);
    }
}
