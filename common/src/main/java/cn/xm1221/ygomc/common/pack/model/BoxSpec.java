package cn.xm1221.ygomc.common.pack.model;

import java.util.ArrayList;
import java.util.List;

/**
 * 一盒怎么装。{@code collation} 的顺序就是展开配比时的顺序，不要用无序映射。
 *
 * @param packs     盒内包数
 * @param collation 各抽法的固定包数；独立盒传空列表
 * @param extras    每盒附赠，开盒时直接给
 */
public record BoxSpec(int packs, List<CollationEntry> collation, List<PrintRef> extras) {

    public BoxSpec {
        if (packs <= 0) {
            throw new IllegalArgumentException("盒内包数必须为正，实得 " + packs);
        }
        collation = collation == null ? List.of() : List.copyOf(collation);
        extras = extras == null ? List.of() : List.copyOf(extras);
    }

    public static BoxSpec independent(int packs) {
        return new BoxSpec(packs, List.of(), List.of());
    }

    /** 配比引用了不存在的抽法，或固定包数超过盒内包数。 */
    public void check(Booster booster) {
        int fixed = 0;
        for (CollationEntry entry : collation) {
            if (entry.count() <= 0) {
                throw new IllegalArgumentException("配比 " + entry.pullId() + " 的包数必须为正");
            }
            if (booster.byId(entry.pullId()) == null) {
                throw new IllegalArgumentException("配比引用了不存在的抽法 " + entry.pullId());
            }
            fixed += entry.count();
        }
        if (fixed > packs) {
            throw new IllegalArgumentException("配比共 " + fixed + " 包，超过盒内的 " + packs + " 包");
        }
    }

    /** 按声明顺序展开成固定抽法 id，长度等于各配比之和。 */
    public List<String> fixedPulls() {
        List<String> pulls = new ArrayList<>();
        for (CollationEntry entry : collation) {
            for (int i = 0; i < entry.count(); i++) {
                pulls.add(entry.pullId());
            }
        }
        return pulls;
    }
}
