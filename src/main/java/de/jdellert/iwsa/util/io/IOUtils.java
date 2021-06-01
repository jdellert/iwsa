package de.jdellert.iwsa.util.io;

import java.io.IOException;
import java.io.OutputStream;

public class IOUtils {

    public static final byte NEWLINE1 = (byte) '\n';
    public static final byte NEWLINE0 = (byte) ('\n' >> 8);
    public static final byte[] NEWLINE = new byte[]{NEWLINE0, NEWLINE1};

    public static void writeBytes(byte[] bytes, OutputStream out) throws IOException {
        for (byte b : bytes)
            out.write(b);
    }

    public static void writeAsBytes(String s, OutputStream out) throws IOException {
        writeAsBytes(s, NEWLINE0, NEWLINE1, out);
    }

    public static void writeAsBytes(String s, byte end0, byte end1, OutputStream out) throws IOException {
        byte[] sbytes = new byte[s.length() * 2 + 2];
        for (int j = 0; j < s.length(); j++) {
            int si = (int) s.charAt(j);
            sbytes[j * 2 + 1] = (byte) si;
            sbytes[j * 2] = (byte) (si >> 8);
        }
        sbytes[sbytes.length - 2] = end0;
        sbytes[sbytes.length - 1] = end1;
        out.write(sbytes);
    }

    public static void writeNewline(OutputStream out) throws IOException {
        out.write(NEWLINE);
    }

    public static void writeInt(int i, OutputStream out) throws IOException {
        writeIntTruncated(i, 4, out);
    }

    public static void writeIntTruncated(int i, int nBytes, OutputStream out) throws IOException {
        writeLongTruncated((long) i, nBytes, out);
    }

    public static void writeLong(long l, OutputStream out) throws IOException {
        writeLongTruncated(l, 8, out);
    }

    public static void writeLongTruncated(long l, int nBytes, OutputStream out) throws IOException {
        byte[] lbytes = new byte[nBytes];
        for (int k = nBytes - 1; k >= 0; k--) {
            lbytes[k] = (byte) l;
            l = l >> 8;
        }
        out.write(lbytes);
    }

    public static void writeDouble(double d, OutputStream out) throws IOException {
        writeLong(Double.doubleToRawLongBits(d), out);
    }

    /**
     * @param n A number
     * @return The number of bytes (up to 4) minimally required to store this number with a leading zero
     */
    public static int bytesNeededFor(int n) {
        int b = 1;
        if (n >= Math.pow(2, 7)) b++;
        if (n >= Math.pow(2, 15)) b++;
        if (n >= Math.pow(2, 23)) b++;
        return b;
    }
}
