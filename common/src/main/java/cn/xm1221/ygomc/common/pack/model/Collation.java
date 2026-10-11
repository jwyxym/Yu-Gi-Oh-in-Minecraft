package cn.xm1221.ygomc.common.pack.model;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Random;
import java.util.UUID;

/**
 * 由存档密钥和批次号算出一盒每一包用哪种抽法。
 * 同一密钥、同一批次、同一盒得到同一计划；密钥不出服务端，客户端算不出计划。
 */
public final class Collation {

    private Collation() {
    }

    /**
     * @param secret  存档密钥
     * @param lot     这一盒的批次
     * @param boxId   盒装卡包的 id，用来把不同商品的批次隔开
     * @return 长度等于盒内包数。配比覆盖到的位置是抽法 id，其余位置是 {@code null}，开封时按权重抽
     */
    public static String[] plan(byte[] secret, UUID lot, String boxId, BoxSpec box) {
        String[] plan = new String[box.packs()];
        int index = 0;
        for (String pullId : box.fixedPulls()) {
            plan[index++] = pullId;
        }
        shuffle(plan, random(secret, lot, boxId));
        return plan;
    }

    private static Random random(byte[] secret, UUID lot, String boxId) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret, "HmacSHA256"));
            ByteBuffer lotBytes = ByteBuffer.allocate(16);
            lotBytes.putLong(lot.getMostSignificantBits());
            lotBytes.putLong(lot.getLeastSignificantBits());
            mac.update(lotBytes.array());
            mac.update(boxId.getBytes(StandardCharsets.UTF_8));
            byte[] digest = mac.doFinal();
            long seed = ByteBuffer.wrap(digest).getLong();
            return new Random(seed);
        } catch (Exception e) {
            throw new IllegalStateException("算不出盒装计划", e);
        }
    }

    /** Fisher–Yates。洗的是整盒，固定抽法不会总堆在前面。 */
    private static void shuffle(String[] plan, Random random) {
        for (int i = plan.length - 1; i > 0; i--) {
            int j = random.nextInt(i + 1);
            String swap = plan[i];
            plan[i] = plan[j];
            plan[j] = swap;
        }
    }

    /** 统计计划里某个抽法出现了几次。{@code null} 位置不计入。 */
    public static int count(String[] plan, String pullId) {
        int count = 0;
        for (String id : plan) {
            if (pullId.equals(id)) {
                count++;
            }
        }
        return count;
    }

    public static boolean same(String[] left, String[] right) {
        return Arrays.equals(left, right);
    }
}
