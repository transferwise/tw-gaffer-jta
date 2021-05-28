package com.transferwise.common.gaffer.util;

public class Encoder {

  public static byte[] longToBytes(long value) {
    byte[] array = new byte[8];

    array[7] = (byte) (value & 0xff);
    array[6] = (byte) ((value >> 8) & 0xff);
    array[5] = (byte) ((value >> 16) & 0xff);
    array[4] = (byte) ((value >> 24) & 0xff);
    array[3] = (byte) ((value >> 32) & 0xff);
    array[2] = (byte) ((value >> 40) & 0xff);
    array[1] = (byte) ((value >> 48) & 0xff);
    array[0] = (byte) ((value >> 56) & 0xff);

    return array;
  }

  public static byte[] intToBytes(int anInt) {
    byte[] array = new byte[4];

    array[3] = (byte) (anInt & 0xff);
    array[2] = (byte) ((anInt >> 8) & 0xff);
    array[1] = (byte) ((anInt >> 16) & 0xff);
    array[0] = (byte) ((anInt >> 24) & 0xff);

    return array;
  }
}
