package cn.xm1221.ygomc.common.pack.model;

import java.util.Random;

/**
 * 开包用的随机源。模型只依赖这一个方法，离线断言传入固定种子，
 * 服务端再换成独立于世界随机的实现。
 */
@FunctionalInterface
public interface RollSource {

    /** @return {@code [0, bound)}；{@code bound} 必须为正 */
    int nextInt(int bound);

    /** @return 用 {@link Random} 包出来的源，种子固定则序列固定 */
    static RollSource seeded(long seed) {
        Random random = new Random(seed);
        return random::nextInt;
    }
}
