package de.jdellert.iwsa.util.io;

import java.io.IOException;
import java.io.InputStream;

/**
 * A buffered wrapper around another InputStream with convenience methods for reading bytes.
 */
public class BufferedByteReader {

    // Default buffer size
    private static final int BUFFER_SIZE = 4096;
    // Mask to extract the last 8 bytes of an int
    private static final int LAST8 = 0b011111111;

    // The input file
    private final InputStream in;
    // The buffer
    private final byte[] buffer;
    // Current index in the buffer
    private int i;
    // Current buffer size
    private int length;

    /**
     * Wrap a BufferedByteReader around an InputStream
     *
     * @param in An input file
     * @throws IOException
     */
    public BufferedByteReader(InputStream in) throws IOException {
        this(in, BUFFER_SIZE);
    }

    /**
     * Wrap a BufferedByteReader around an InputStream
     *
     * @param in         An input file
     * @param bufferSize The buffer size (default: 4096)
     * @throws IOException
     */
    public BufferedByteReader(InputStream in, int bufferSize) throws IOException {
        this.in = in;
        this.buffer = new byte[bufferSize];
        this.i = 0;
        this.length = in.read(buffer);
    }

    /**
     * Fill the buffer with new bytes from the file.
     *
     * @throws IOException
     */
    private void refill() throws IOException {
        int old = length - i;
        System.arraycopy(buffer, i, buffer, 0, old);
        length = old + in.read(buffer, old, buffer.length - old);
        i = 0;
    }

    /**
     * @return True if there is another byte to read
     * @throws IOException
     */
    public boolean hasNext() throws IOException {
        return hasNext(1);
    }

    /**
     * @param b The number of bytes to look for
     * @return True if there are another b bytes to read
     * @throws IOException
     */
    public boolean hasNext(int b) throws IOException {
        if (length < 0)
            return false;
        if (i + b > length)
            refill();
        return i + b <= length;
    }

    /**
     * Get the next byte without moving the pointer.
     *
     * @return The next byte
     * @throws IOException
     */
    public byte peek() throws IOException {
        if (hasNext())
            return buffer[i];
        else
            throw new IOException("Reached end of file");
    }

    /**
     * @param b A byte
     * @return True if the next byte is equal to b
     * @throws IOException
     */
    public boolean startsWith(byte b) throws IOException {
        return hasNext() && buffer[i] == b;
    }

    /**
     * @param b A byte sequence
     * @return True if the next bytes equal the byte sequence
     * @throws IOException
     */
    public boolean startsWith(byte[] b) throws IOException {
        if (hasNext(b.length)) {
            for (int j = 0; j < b.length; j++) {
                if (b[j] != buffer[i + j])
                    return false;
            }
            return true;
        } else
            return false;
    }

    /**
     * @return True if the next bytes equal a newline
     * @throws IOException
     */
    public boolean startsWithNewline() throws IOException {
        return startsWith(IOUtils.NEWLINE);
    }

    /**
     * Move the pointer s bytes without returning the bytes.
     *
     * @param s How many bytes to skip
     * @return False if the end of the file was reached while skipping
     * @throws IOException
     */
    public boolean skip(int s) throws IOException {
        for (int i = 0; i < s; i++) {
            if (hasNext())
                this.i++;
        }

//        while (s > BUFFER_SIZE && length > 0) {
//            if (!skip(BUFFER_SIZE))
//                return false;
//        }
//        if (hasNext(s)) {
//            i += s;
//            return true;
//        }
        return false;
    }

    public int skipRemaining() throws IOException {
        int len = length - i;
        i = length;
        while (hasNext()) {
            len += length;
            i = length;
        }
        return len;
    }

    /**
     * Get the next byte and move the pointer forward.
     *
     * @return The next byte
     * @throws IOException
     */
    public byte pop() throws IOException {
        byte res = peek();
        i++;
        return res;
    }

    /**
     * Get the next b bytes and move the pointer forward.
     *
     * @param b How many bytes to pop
     * @return The next b bytes
     * @throws IOException
     */
    public byte[] pop(int b) throws IOException {
        if (hasNext(b)) {
            byte[] res = new byte[b];
            System.arraycopy(buffer, i, res, 0, b);
            i += b;
            return res;
        } else
            throw new IOException("Reached end of file");
    }

    /**
     * Get the next four bytes and shift them into an int. Moves the pointer forward.
     *
     * @return The next integer
     * @throws IOException If the end of the file is reached while fetching the bytes
     */
    public int popToInt() throws IOException {
        return popToInt(4);
    }

    /**
     * Get the next b bytes and shift them into an int. If less than four bytes are requested,
     * the int comes with leading zeroes. If more than four bytes are requested, the first b-4 bytes
     * will be shifted out of range and won't be returned at all.
     * Moves the pointer forward.
     *
     * @param b The number of bytes to shift into an int.
     * @return An int with the requested bytes inside
     * @throws IOException If the end of the file is reached while fetching the bytes
     */
    public int popToInt(int b) throws IOException {
        if (hasNext(b)) {
            int res = 0;
            for (int j = 0; j < b; j++)
                res = (res << 8) | buffer[i + j] & LAST8;
            i += b;
            return res;
        } else
            throw new IOException("Reached end of file");
    }

    /**
     * Get the next eight bytes and shift them into a long. Moves the pointer forward.
     *
     * @return The next long integer
     * @throws IOException If the end of the file is reached while fetching the bytes
     */
    public long popToLong() throws IOException {
        return popToLong(8);
    }

    /**
     * Get the next b bytes and shift them into a long. If less than eight bytes are requested,
     * the long comes with leading zeroes. If more than eight bytes are requested, the first b-8 bytes
     * will be shifted out of range and won't be returned at all.
     * Moves the pointer forward.
     *
     * @param b The number of bytes to shift into a long.
     * @return A long with the requested bytes inside
     * @throws IOException If the end of the file is reached while fetching the bytes
     */
    public long popToLong(int b) throws IOException {
        if (hasNext(b)) {
            long res = 0;
            for (int j = 0; j < b; j++)
                res = (res << 8) | buffer[i + j] & LAST8;
            i += b;
            return res;
        } else
            throw new IOException("Reached end of file");
    }

    /**
     * Get the next eight bytes and parse them into a double. Moves the pointer forward.
     *
     * @return The next double
     * @throws IOException If the end of the file is reached while fetching the bytes
     */
    public double popToDouble() throws IOException {
        return Double.longBitsToDouble(popToLong());
    }

    /**
     * Get the next two bytes and parse them into a char. Moves the pointer forward.
     *
     * @return The next 16-bit char
     * @throws IOException If the end of the file is reached while fetching the bytes
     */
    public char popToChar() throws IOException {
        return (char) popToInt(2);
    }

    /**
     * Get the next chars until a newline and parse them into a string. Moves the pointer forward (also consumes the
     * newline!).
     *
     * @return The next String until a newline
     * @throws IOException If the end of the file is reached while fetching the bytes
     */
    public String nextString() throws IOException {
        return nextString('\n');
    }

    /**
     * Get the next chars until a certain char and parse them into a string. Moves the pointer forward (also consumes
     * separator char!).
     *
     * @param sep The separator char
     * @return The next String until sep
     * @throws IOException If the end of the file is reached while fetching the bytes
     */
    public String nextString(char sep) throws IOException {
        StringBuilder s = new StringBuilder();
        for (char c = popToChar(); c != sep; c = popToChar()) {
            s.append(c);
        }
        return s.toString();
    }

    public void close() throws IOException {
        in.close();
        i = -1;
        length = -1;
    }

}
