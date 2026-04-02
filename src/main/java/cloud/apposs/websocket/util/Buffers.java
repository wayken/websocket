package cloud.apposs.websocket.util;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;

/**
 * ByteBuffer 字节数据操作工具类，基于{@link ByteBuffer}实现
 */
public final class Buffers {
    /**
     * 将字节数组包装成 {@link ByteBuffer}，如果输入为 null 或空数组，则返回一个容量为0的字节数据
     * @param  buffer 字节数组
     * @return 包装后的字节数据
     */
    public static ByteBuffer wrappedBuffer(byte[] buffer) {
        if (buffer == null || buffer.length == 0) {
            return ByteBuffer.allocate(0);
        }
        return ByteBuffer.wrap(buffer);
    }

    public static ByteBuffer wrappedBuffer(String buffer, String charset) {
        return wrappedBuffer(buffer == null ? null : buffer.getBytes(Charset.forName(charset)));
    }

    /**
     * 将多个 {@link ByteBuffer} 的剩余数据合并包装成一个新的字节数据（直接拷贝合并），
     * 每个 buffer 的 position~limit 之间的数据都会被包含，原 buffer 的 position 不受影响
     *
     * @param  buffers 一个或多个字节数据
     * @return 包含所有数据的新字节数据
     */
    public static ByteBuffer wrappedBuffer(ByteBuffer... buffers) {
        if (buffers == null || buffers.length == 0) {
            return ByteBuffer.allocate(0);
        }
        // 计算总容量
        int totalLen = 0;
        for (ByteBuffer buf : buffers) {
            if (buf != null) {
                totalLen += buf.remaining();
            }
        }
        if (totalLen == 0) {
            return ByteBuffer.allocate(0);
        }
        ByteBuffer result = ByteBuffer.allocate(totalLen);
        for (ByteBuffer buf : buffers) {
            if (buf != null && buf.hasRemaining()) {
                result.put(buf.duplicate());
            }
        }
        result.flip();
        return result;
    }

    /**
     * 将字节数组包装成输入流
     *
     * @param  buffer 字节数据
     * @return 包含数据的输入流
     */
    public static InputStream wrappedInputStream(byte[] buffer) {
        if (buffer == null || buffer.length == 0) {
            return new ByteArrayInputStream(new byte[0]);
        }
        return new ByteArrayInputStream(buffer);
    }

    /**
     * 深拷贝一个 {@link ByteBuffer}，返回独立的新字节数据
     *
     * @param  buffer 源字节数据
     * @return 拷贝后的新字节数据（position=0，limit=原 remaining）
     */
    public static ByteBuffer copiedBuffer(ByteBuffer buffer) {
        if (buffer == null || !buffer.hasRemaining()) {
            return ByteBuffer.allocate(0);
        }
        ByteBuffer copiedBuffer = ByteBuffer.allocate(buffer.remaining());
        copiedBuffer.put(buffer.duplicate());
        copiedBuffer.flip();
        return copiedBuffer;
    }

    /**
     * 将多个字节数组合并拷贝成一个新的 {@link ByteBuffer}。
     *
     * @param  buffers 一个或多个字节数组
     * @return 合并后的新字节数据（position=0，limit=所有数组总长度）
     */
    public static ByteBuffer copiedBuffer(byte[]... buffers) {
        if (buffers == null || buffers.length == 0) {
            return ByteBuffer.allocate(0);
        }
        int totalLen = 0;
        for (byte[] buffer : buffers) {
            if (buffer != null) {
                totalLen += buffer.length;
            }
        }
        if (totalLen == 0) {
            return ByteBuffer.allocate(0);
        }
        ByteBuffer result = ByteBuffer.allocate(totalLen);
        for (byte[] buffer : buffers) {
            if (buffer != null && buffer.length > 0) {
                result.put(buffer);
            }
        }
        result.flip();
        return result;
    }

    /**
     * 从 {@link ByteBuffer} 中拷贝 index 开始、长度为 length 的数据到新的独立字节数据，
     * 返回的 buffer 与原 buffer 不共享底层内存，互不影响。
     *
     * @param  buffer 源字节数据
     * @param  index  切片起始绝对索引（相对于 buffer 底层数组）
     * @param  length 切片长度
     * @return 独立拷贝的新字节数据（position=0，limit=length）
     * @throws IndexOutOfBoundsException 如果 index 或 length 超出 buffer 容量范围
     */
    public static ByteBuffer sliceBuffer(ByteBuffer buffer, int index, int length) {
        if (buffer == null) {
            return ByteBuffer.allocate(0);
        }
        if (index < 0 || length < 0 || index + length > buffer.capacity()) {
            throw new IndexOutOfBoundsException(
                    "index: " + index + ", length: " + length + ", capacity: " + buffer.capacity());
        }
        byte[] copy = new byte[length];
        // 使用 duplicate 避免修改原 buffer 的 position/limit
        ByteBuffer dup = buffer.duplicate();
        dup.position(index);
        dup.get(copy);
        return ByteBuffer.wrap(copy);
    }

    /**
     * 在 {@link ByteBuffer} 中查找子序列 searchValue 第一次出现的绝对索引位置（KMP算法），
     * 不修改原 buffer 的 position/limit/mark
     *
     * @param  buffer      源字节数据，搜索范围为 position~limit
     * @param  searchValue 要查找的子序列，搜索范围为 position~limit
     * @return searchValue 在 buffer 中第一次出现的绝对索引，未找到返回 -1
     */
    public static int findBufferIndex(ByteBuffer buffer, ByteBuffer searchValue) {
        if (buffer == null || searchValue == null) {
            return -1;
        }
        int bufLen = buffer.remaining();
        int patLen = searchValue.remaining();
        if (patLen == 0) {
            return buffer.position();
        }
        if (bufLen < patLen) {
            return -1;
        }
        int bufStart = buffer.position();
        int patStart = searchValue.position();

        // 构建 KMP 失配表
        int[] fail = new int[patLen];
        fail[0] = 0;
        for (int i = 1; i < patLen; i++) {
            int j = fail[i - 1];
            while (j > 0 && searchValue.get(patStart + i) != searchValue.get(patStart + j)) {
                j = fail[j - 1];
            }
            if (searchValue.get(patStart + i) == searchValue.get(patStart + j)) {
                j++;
            }
            fail[i] = j;
        }

        // KMP 搜索
        int j = 0;
        for (int i = 0; i < bufLen; i++) {
            byte b = buffer.get(bufStart + i);
            while (j > 0 && b != searchValue.get(patStart + j)) {
                j = fail[j - 1];
            }
            if (b == searchValue.get(patStart + j)) {
                j++;
            }
            if (j == patLen) {
                return bufStart + i - patLen + 1;
            }
        }
        return -1;
    }

    public static int bytesBefore(ByteBuffer buffer, byte value) {
        return bytesBefore(buffer, buffer.position(), buffer.remaining(), value);
    }

    public static int bytesBefore(ByteBuffer buffer, int length, byte value) {
        return bytesBefore(buffer, buffer.position(), length, value);
    }

    /**
     * 在 {@link ByteBuffer} 的指定区间内查找第一个匹配字节的位置，不修改原 buffer 的 position/limit/mark
     *
     * @param  buffer 源字节数据
     * @param  index  搜索起始索引（绝对索引，相对于 buffer 底层数组）
     * @param  length 搜索长度
     * @param  value  要查找的字节值
     * @return 从 index 起到第一个匹配字节的字节数（即相对偏移量），未找到返回 -1
     * @throws IndexOutOfBoundsException 如果 index 或 length 超出 buffer 容量范围
     */
    public static int bytesBefore(ByteBuffer buffer, int index, int length, byte value) {
        if (buffer == null) {
            return -1;
        }
        if (index < 0 || length < 0 || index + length > buffer.capacity()) {
            throw new IndexOutOfBoundsException(
                    "index: " + index + ", length: " + length + ", capacity: " + buffer.capacity());
        }
        for (int i = index; i < index + length; i++) {
            if (buffer.get(i) == value) {
                return i - index;
            }
        }
        return -1;
    }
}
