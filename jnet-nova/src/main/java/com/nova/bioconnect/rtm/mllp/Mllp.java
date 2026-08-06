package com.nova.bioconnect.rtm.mllp;

/**
 * MLLP（Minimal Lower Layer Protocol）控制字符常量。
 * <p>
 * 起始块 {@code 0x0B}，结束块 {@code 0x1C} 紧跟回车 {@code 0x0D}。
 */
public final class Mllp {

    private Mllp() {
    }

    /** 起始块字符。 */
    public static final byte START_BYTE = 0x0B;
    /** 结束块字符。 */
    public static final byte END_BYTE = 0x1C;
    /** 回车字符。 */
    public static final byte CR_BYTE = 0x0D;

    /** 结束帧字节序列：{@code 0x1C 0x0D}。 */
    public static final byte[] END_FRAME_BYTES = new byte[]{END_BYTE, CR_BYTE};
}
