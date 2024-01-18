package com.g7.framework.javadoc.util;

import java.io.ByteArrayOutputStream;

public class MyBase64 {

    private static String base64EncodeStr = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/";
    private static char[] base64EncodeChars = base64EncodeStr.toCharArray();
    private static byte[] base64DecodeChars;

    static {
        base64DecodeChars = new byte[128];
        for (int i = 0; i < 128; i++) {
            base64DecodeChars[i] = (byte) -1;
        }

        for (int i = 'A'; i <= 'Z'; i++) {
            base64DecodeChars[i] = (byte) base64EncodeStr.indexOf((char) (i));
        }
        for (int i = 'a'; i <= 'z'; i++) {
            base64DecodeChars[i] = (byte) base64EncodeStr.indexOf((char) (i));
        }
        for (int i = '0'; i <= '9'; i++) {
            base64DecodeChars[i] = (byte) base64EncodeStr.indexOf((char) (i));
        }
        base64DecodeChars['+'] = (byte) base64EncodeStr.indexOf('+');
        base64DecodeChars['/'] = (byte) base64EncodeStr.indexOf('/');
    }

    private MyBase64() {
    }

    public static String encode(byte[] data) {
        StringBuffer sb = new StringBuffer();
        int len = data.length;
        int i = 0;
        int b1, b2, b3;

        while (i < len) {
            b1 = data[i++] & 0xff;
            if (i == len) {
                sb.append(base64EncodeChars[b1 >>> 2]);
                sb.append(base64EncodeChars[(b1 & 0x3) << 4]);
                sb.append("==");
                break;
            }
            b2 = data[i++] & 0xff;
            if (i == len) {
                sb.append(base64EncodeChars[b1 >>> 2]);
                sb.append(
                        base64EncodeChars[((b1 & 0x03) << 4) | ((b2 & 0xf0) >>> 4)]);
                sb.append(base64EncodeChars[(b2 & 0x0f) << 2]);
                sb.append("=");
                break;
            }
            b3 = data[i++] & 0xff;
            sb.append(base64EncodeChars[b1 >>> 2]);
            sb.append(
                    base64EncodeChars[((b1 & 0x03) << 4) | ((b2 & 0xf0) >>> 4)]);
            sb.append(
                    base64EncodeChars[((b2 & 0x0f) << 2) | ((b3 & 0xc0) >>> 6)]);
            sb.append(base64EncodeChars[b3 & 0x3f]);
        }
        return sb.toString();
    }

    public static byte[] decode(String str) {
        byte[] data = str.getBytes();
        int len = data.length;
        ByteArrayOutputStream buf = new ByteArrayOutputStream(len);
        int i = 0;
        int b1, b2, b3, b4;

        while (i < len) {

            /* b1 */
            do {
                b1 = base64DecodeChars[data[i++]];
            } while (i < len && b1 == -1);
            if (b1 == -1) {
                break;
            }

            /* b2 */
            do {
                b2 = base64DecodeChars[data[i++]];
            } while (i < len && b2 == -1);
            if (b2 == -1) {
                break;
            }
            buf.write((int) ((b1 << 2) | ((b2 & 0x30) >>> 4)));

            /* b3 */
            do {
                b3 = data[i++];
                if (b3 == 61) {
                    return buf.toByteArray();
                }
                b3 = base64DecodeChars[b3];
            } while (i < len && b3 == -1);
            if (b3 == -1) {
                break;
            }
            buf.write((int) (((b2 & 0x0f) << 4) | ((b3 & 0x3c) >>> 2)));

            /* b4 */
            do {
                b4 = data[i++];
                if (b4 == 61) {
                    return buf.toByteArray();
                }
                b4 = base64DecodeChars[b4];
            } while (i < len && b4 == -1);
            if (b4 == -1) {
                break;
            }
            buf.write((int) (((b3 & 0x03) << 6) | b4));
        }
        return buf.toByteArray();
    }

    public static void main(String[] args) {
        for (int i = 0; i < 128; i++) {
            System.out.print(base64DecodeChars[i] + " ");
            if (((i + 1) % 16) == 0) System.out.println();
        }

        String data = "";
        try {
            String result = MyBase64.encode(data.getBytes("utf-8"));

            System.out.println(data);
            System.out.println(result);
            System.out.println(new String(MyBase64.decode(result), "utf-8"));

            byte[] tmp = MyBase64.decode(result);
            for (int i = 0; i < tmp.length; i++) {
                String hex = Integer.toHexString(tmp[i]);
                System.out.print(hex + " ");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}