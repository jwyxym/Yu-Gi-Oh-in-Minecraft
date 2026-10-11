package cn.xm1221.ygomc.common.pack.model;

/** 这一包按现有的池和回退顺序开不出来。调用方不应当消耗卡包。 */
public class PackRollException extends RuntimeException {

    public PackRollException(String message) {
        super(message);
    }
}
