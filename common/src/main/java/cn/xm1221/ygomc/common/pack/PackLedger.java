package cn.xm1221.ygomc.common.pack;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.LongArrayTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.saveddata.SavedData;

import java.security.SecureRandom;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * 开包用的存档：一份不下发的密钥，加上「这个批次的第几包已经开过」的账本。
 * 挂在主世界上，换维度也不会把同一包再开一次。
 */
public final class PackLedger extends SavedData {

    private static final String ID = "ygomc_pack_ledger";
    private static final SavedData.Factory<PackLedger> FACTORY = new SavedData.Factory<>(
            PackLedger::new, PackLedger::load, DataFixTypes.SAVED_DATA_COMMAND_STORAGE);

    private byte[] secret = new byte[0];
    private final Map<UUID, long[]> open = new HashMap<>();
    private final Map<UUID, Integer> size = new HashMap<>();
    /** 已经整盒开完的批次。只留标记，用来拒绝事后复制出来的包。 */
    private final Set<UUID> finished = new HashSet<>();

    public static PackLedger get(ServerPlayer player) {
        return get(player.serverLevel());
    }

    public static PackLedger get(ServerLevel level) {
        return level.getServer().overworld().getDataStorage().computeIfAbsent(FACTORY, ID);
    }

    public synchronized byte[] secret() {
        if (secret.length != 32) {
            secret = new byte[32];
            new SecureRandom().nextBytes(secret);
            setDirty();
        }
        return secret.clone();
    }

    /**
     * 记下这一包开过了。
     *
     * @return 这一包已经开过、或序号不合法时返回 {@code false}
     */
    public synchronized boolean claim(UUID lot, int index, int of) {
        if (finished.contains(lot) || index < 0 || index >= of) {
            return false;
        }
        Integer known = size.get(lot);
        if (known != null && known != of) {
            return false;
        }
        long[] bits = open.computeIfAbsent(lot, key -> new long[(of + 63) / 64]);
        int word = index >>> 6;
        long mask = 1L << (index & 63);
        if ((bits[word] & mask) != 0) {
            return false;
        }
        bits[word] |= mask;
        size.put(lot, of);
        if (complete(bits, of)) {
            open.remove(lot);
            size.remove(lot);
            finished.add(lot);
        }
        setDirty();
        return true;
    }

    /** 开封算到一半失败时把刚才占的位还回去，这包还能再开。 */
    public synchronized void release(UUID lot, int index, int of) {
        if (finished.remove(lot)) {
            long[] bits = new long[(of + 63) / 64];
            Arrays.fill(bits, -1L);
            clear(bits, index);
            open.put(lot, bits);
            size.put(lot, of);
            setDirty();
            return;
        }
        long[] bits = open.get(lot);
        if (bits == null) {
            return;
        }
        clear(bits, index);
        setDirty();
    }

    private static void clear(long[] bits, int index) {
        int word = index >>> 6;
        if (word < bits.length) {
            bits[word] &= ~(1L << (index & 63));
        }
    }

    private static boolean complete(long[] bits, int of) {
        for (int index = 0; index < of; index++) {
            int word = index >>> 6;
            long mask = 1L << (index & 63);
            if ((bits[word] & mask) == 0) {
                return false;
            }
        }
        return true;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        tag.putByteArray("secret", secret().clone());
        ListTag lots = new ListTag();
        for (Map.Entry<UUID, long[]> entry : open.entrySet()) {
            CompoundTag one = new CompoundTag();
            one.putUUID("lot", entry.getKey());
            one.putInt("of", size.get(entry.getKey()));
            one.put("bits", new LongArrayTag(entry.getValue()));
            lots.add(one);
        }
        tag.put("open", lots);
        ListTag done = new ListTag();
        for (UUID lot : finished) {
            CompoundTag one = new CompoundTag();
            one.putUUID("lot", lot);
            done.add(one);
        }
        tag.put("finished", done);
        return tag;
    }

    private static PackLedger load(CompoundTag tag, HolderLookup.Provider registries) {
        PackLedger ledger = new PackLedger();
        ledger.secret = tag.getByteArray("secret");
        for (Tag element : tag.getList("open", Tag.TAG_COMPOUND)) {
            CompoundTag one = (CompoundTag) element;
            UUID lot = one.getUUID("lot");
            ledger.size.put(lot, one.getInt("of"));
            ledger.open.put(lot, one.getLongArray("bits"));
        }
        for (Tag element : tag.getList("finished", Tag.TAG_COMPOUND)) {
            ledger.finished.add(((CompoundTag) element).getUUID("lot"));
        }
        return ledger;
    }
}
