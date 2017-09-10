package com.example.ac.desfirelearningtool;

import java.util.HashMap;

/**
 * Created by ldematte on 11/24/14.
 */
public class ByteArray {


    private static HashMap<String, Byte> hexMap = null;



    public static int ISO9797m2PadCount (byte [] in) {
        int count = in.length - 1;
        while (count > 0 && in[count] == 0) {
            count --;
        }
        if (in[count] != (byte) 0x80) {
            return -1;
        }
        return in.length - count;
    }

    public static byte[] appendCut(byte[] first, byte[] last) {
        byte[] ret;
        if (last == null || last.length == 0) {
            return first;
        }
        if (first == null || first.length == 0) {
            ret = new byte[last.length - 1];
            if (last.length == 1 && last[0] == 0x00)
                return new byte[0];
            System.arraycopy(last, 1, ret, 0, last.length - 1);
            return ret;
        }

        ret = new byte[first.length + last.length - 1];
        System.arraycopy(first, 0, ret, 0, first.length);
        System.arraycopy(last, 1, ret, first.length, last.length - 1);
        return ret;
    }

    public static byte[] appendCutCMAC ( byte[] data) {
        byte[] ret;
        if (data == null || data.length < 9) {
            return null;
        }

        ret = new byte[data.length - 9];
        System.arraycopy(data, 1, ret, 0, data.length-9);
        return ret;
    }


    public static String byteArrayToHexString(byte[] array) {
        int i, j, in;
        String[] hex = {"0", "1", "2", "3", "4", "5", "6", "7", "8", "9", "A", "B", "C", "D", "E", "F"};
        String out = "";

        for (j = 0; j < array.length; ++j) {
            in = (int)array[j] & 0xff;
            i = (in >> 4) & 0x0f;
            out += hex[i];
            i = in & 0x0f;
            out += hex[i];
            out += " ";
        }
        return out;
    }
    public static String byteToHexString(byte data) {
        return byteArrayToHexString(new byte[] {data});
    }

    public static String byteArrayToHexStringNoSpace(byte[] array) {
        int i, j, in;
        String[] hex = {"0", "1", "2", "3", "4", "5", "6", "7", "8", "9", "A", "B", "C", "D", "E", "F"};
        String out = "";

        for (j = 0; j < array.length; ++j) {
            in = (int)array[j] & 0xff;
            i = (in >> 4) & 0x0f;
            out += hex[i];
            i = in & 0x0f;
            out += hex[i];
        }
        return out;
    }

    public static String byteArrayToHexString(byte[] array, int arrayOffset, int size) {
        int i, j, in;
        String[] hex = {"0", "1", "2", "3", "4", "5", "6", "7", "8", "9", "A", "B", "C", "D", "E", "F"};
        String out = "";

        for (j = 0; j < size; ++j) {
            in = (int)array[j+arrayOffset] & 0xff;
            i = (in >> 4) & 0x0f;
            out += hex[i];
            i = in & 0x0f;
            out += hex[i];
            out += " ";
        }
        return out;
    }

    public static byte[] hexStringToByteArray(String s) {
        s = s.replaceAll("[^A-Fa-f0-9]", "");

        byte[] ret;
        if (s.length() % 2 != 0)
            return null;
        if (ByteArray.hexMap == null) {
            ByteArray.hexMap = new HashMap<String, Byte>();
            hexMap.put("l0", (byte)0x00);
            hexMap.put("l1", (byte)0x01);
            hexMap.put("l2", (byte)0x02);
            hexMap.put("l3", (byte)0x03);
            hexMap.put("l4", (byte)0x04);
            hexMap.put("l5", (byte)0x05);
            hexMap.put("l6", (byte)0x06);
            hexMap.put("l7", (byte)0x07);
            hexMap.put("l8", (byte)0x08);
            hexMap.put("l9", (byte)0x09);
            hexMap.put("lA", (byte)0x0a);
            hexMap.put("lB", (byte)0x0b);
            hexMap.put("lC", (byte)0x0c);
            hexMap.put("lD", (byte)0x0d);
            hexMap.put("lE", (byte)0x0e);
            hexMap.put("lF", (byte)0x0f);
            //lowercase
            hexMap.put("la", (byte)0x0a);
            hexMap.put("lb", (byte)0x0b);
            hexMap.put("lc", (byte)0x0c);
            hexMap.put("ld", (byte)0x0d);
            hexMap.put("le", (byte)0x0e);
            hexMap.put("lf", (byte)0x0f);

            hexMap.put("h0", (byte)0x00);
            hexMap.put("h1", (byte)0x10);
            hexMap.put("h2", (byte)0x20);
            hexMap.put("h3", (byte)0x30);
            hexMap.put("h4", (byte)0x40);
            hexMap.put("h5", (byte)0x50);
            hexMap.put("h6", (byte)0x60);
            hexMap.put("h7", (byte)0x70);
            hexMap.put("h8", (byte)0x80);
            hexMap.put("h9", (byte)0x90);
            hexMap.put("hA", (byte)0xa0);
            hexMap.put("hB", (byte)0xb0);
            hexMap.put("hC", (byte)0xc0);
            hexMap.put("hD", (byte)0xd0);
            hexMap.put("hE", (byte)0xe0);
            hexMap.put("hF", (byte)0xf0);
            //lowercase
            hexMap.put("ha", (byte)0xa0);
            hexMap.put("hb", (byte)0xb0);
            hexMap.put("hc", (byte)0xc0);
            hexMap.put("hd", (byte)0xd0);
            hexMap.put("he", (byte)0xe0);
            hexMap.put("hf", (byte)0xf0);
        }
        ret = new byte[s.length() / 2];
        for (int i = 0; i < ret.length; i++) {
            byte h = ByteArray.hexMap.get("h" + s.charAt(2 * i));
            byte l = ByteArray.hexMap.get("l" + s.charAt(2 * i + 1));
            ret[i] = (byte)(h ^ l);
        }
        return ret;
    }


    public static byte[] rotateLT(byte[] input) {
        byte bytes[] = new byte[input.length];
        System.arraycopy(input,0,bytes,0,input.length);
        byte help = bytes[0];
        for (int i = 0; i < bytes.length - 1; i++)
            bytes[i] = bytes[i + 1];
        bytes[bytes.length - 1] = help;
        return bytes;
    }

    public static byte[] rotateRT(byte[] input) {
        byte bytes[] = new byte[input.length];
        System.arraycopy(input,0,bytes,0,input.length);
        byte help = bytes[bytes.length - 1];
        for (int i = bytes.length - 1; i > 0; i--)
            bytes[i] = bytes[i - 1];
        bytes[0] = help;
        return bytes;
    }

    public static byte[] xor(byte[] a, byte[] b) {
        byte[] ret = new byte[a.length < b.length ? b.length : a.length];
        for (int i = 0; i < a.length && i < b.length; i++)
            ret[i] = (byte)(a[i] ^ b[i]);
        return ret;
    }

    public static ByteArray from(byte b) {
        return new ByteArray().append(b);
    }

    public static byte[] fromInt(int n, int bytes) {
        return fromInt(n, false, bytes);
    }

    public static byte[] fromInt(int n, boolean littleEndian, int bytes) {
        byte[] ret = new byte[bytes];
        fromInt(ret, 0, n, littleEndian, bytes);
        return ret;
    }

    private static void fromInt(byte[] dst, int from, int n, boolean littleEndian, int bytes) {
        for (int i = 0; i < bytes; ++i) {
            // Java is big Endian. Do we want little Endian?
            int index;
            if (littleEndian) {
                index = bytes - i - 1;
            } else {
                index = i;
            }

            dst[from + index] = (byte)(n >> 8 * i);
        }
    }


    private final static int INITIAL_SIZE = 100;
    private byte[] buffer;
    private int count = 0;

    public ByteArray() {
        buffer = new byte[INITIAL_SIZE];
    }

    public ByteArray(int size) {
        buffer = new byte[size];
    }

    public ByteArray clear() {
        count = 0;
        return this;
    }

    public int length() {
        return count;
    }

    private void checkResize(int n) {
        if (count + n >= buffer.length) {
            byte[] tmp = new byte[count + n + INITIAL_SIZE];
            System.arraycopy(buffer, 0, tmp, 0, count);
            buffer = tmp;
        }
    }

    public ByteArray append(byte b) {
        checkResize(1);
        buffer[count] = b;
        ++count;
        return this;
    }

    public ByteArray append(byte[] b) {
        return append(b, 0, b.length);
    }

    public ByteArray append(byte[] a, int start, int n) {
        checkResize(n);
        System.arraycopy(a, start, buffer, count, n);
        count += n;
        return this;
    }

    public ByteArray append(int n, int bytes) {
        checkResize(bytes);
        fromInt(buffer, count, n, false, bytes);
        count += bytes;
        return this;
    }

    public byte[] toArray() {
        byte[] data = new byte[count];
        System.arraycopy(buffer, 0, data, 0, count);
        return data;
    }

    public byte[] rawData() {
        return buffer;
    }
}
