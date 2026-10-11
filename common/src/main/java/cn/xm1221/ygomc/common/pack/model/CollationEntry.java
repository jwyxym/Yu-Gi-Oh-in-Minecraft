package cn.xm1221.ygomc.common.pack.model;

/** 配比表的一行：这种抽法在一盒里有固定的几包。 */
public record CollationEntry(String pullId, int count) {
}
