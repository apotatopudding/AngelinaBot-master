package top.strelitzia.test;

import java.io.*;

public class thisTest {

    public final static String TYPE_JPEG = "jpeg";
    public final static String TYPE_PNG = "png";
    public final static String TYPE_GIF = "gif";
    public final static String TYPE_WEBP = "webp";
    public final static String TYPE_BMP = "bmp";
    public final static String TYPE_ICO = "ico";

    public static void main(String[] args) throws Exception {
        System.out.println(readType(new File("F:\\1.jpg")));
    }

    /**
     * 读取文件字节识别图片扩展名
     *
     * @param file
     * @return
     * @throws IOException
     */
    public static String readType(byte[] file) throws IOException {
        ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(file);
        return readType(byteArrayInputStream);
    }

    /**
     * 读取本地文件识别图片扩展名
     *
     * @param file
     * @return
     * @throws IOException
     */
    public static String readType(File file) throws IOException {

        FileInputStream fis = null;
        try {
            if (!file.exists() || file.isDirectory() || file.length() < 8) {
                throw new IOException("the file [" + file.getAbsolutePath() + "] is not image !");
            }

            fis = new FileInputStream(file);
            return readType(fis);

        } catch (IOException e) {
            throw e;
        } finally {
            try {
                if (fis != null)
                    fis.close();
            } catch (Exception e) {
            }
        }

    }

    /**
     * 输入流识别图片扩展名
     *
     * @param is
     * @return
     * @throws IOException
     */
    public static String readType(InputStream is) throws IOException {

        byte[] bufHeaders = readInputStreamAt(is, 0, 8);
        if (isJPEGHeader(bufHeaders)) {
            /* jpeg需要判定头和尾，但是目前不需要这严谨，为了节约资源注释掉结尾判定
            long skiplength = f.length() - 2 - 8; // 第一次读取时已经读了8个byte,因此需要减掉
            byte[] bufFooters = readInputStreamAt(fis, skiplength, 2);
            if (isJPEGFooter(bufFooters)) {
                return "jpeg";
            }*/
            return TYPE_JPEG;
        }
        if (isPNG(bufHeaders)) {
            return TYPE_PNG;
        }
        if (isGIF(bufHeaders)) {

            return TYPE_GIF;
        }
        if (isWEBP(bufHeaders)) {
            return TYPE_WEBP;
        }
        if (isBMP(bufHeaders)) {
            return TYPE_BMP;
        }
        if (isICON(bufHeaders)) {
            return TYPE_ICO;
        }
        throw new IOException("the image's format is unkown!");
    }


    /**
     * 标示一致性比较
     *
     * @param buf     待检测标示
     * @param markBuf 标识符字节数组
     * @return 返回false标示标示不匹配
     */
    private static boolean compare(byte[] buf, byte[] markBuf) {
        for (int i = 0; i < markBuf.length; i++) {
            byte b = markBuf[i];
            byte a = buf[i];

            if (a != b) {
                return false;
            }
        }
        return true;
    }

    /**
     * @param fis        输入流对象
     * @param skiplength 跳过位置长度
     * @param length     要读取的长度
     * @return 字节数组
     * @throws IOException
     */
    private static byte[] readInputStreamAt(InputStream fis, long skiplength, int length) throws IOException {
        byte[] buf = new byte[length];
        fis.skip(skiplength); //
        fis.read(buf, 0, length);
        return buf;
    }

    // BMP图片文件的前两个字节
    private static boolean isBMP(byte[] buf) {
        byte[] markBuf = "BM".getBytes();
        return compare(buf, markBuf);
    }

    private static boolean isICON(byte[] buf) {
        byte[] markBuf = {0, 0, 1, 0, 1, 0, 32, 32};
        return compare(buf, markBuf);
    }

    // WebP图片识别符
    private static boolean isWEBP(byte[] buf) {
        byte[] markBuf = "RIFF".getBytes();
        return compare(buf, markBuf);
    }

    private static boolean isGIF(byte[] buf) {
        // GIF识别符
        byte[] markBuf = "GIF89a".getBytes();
        if (compare(buf, markBuf)) {
            return true;
        }
        // GIF识别符
        markBuf = "GIF87a".getBytes();
        if (compare(buf, markBuf)) {
            return true;
        }
        return false;
    }

    // PNG识别符
    private static boolean isPNG(byte[] buf) {
        byte[] markBuf = {(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A};
        // new String(buf).indexOf("PNG")>0 //也可以使用这种方式
        return compare(buf, markBuf);
    }

    // JPEG开始符
    private static boolean isJPEGHeader(byte[] buf) {
        byte[] markBuf = {(byte) 0xff, (byte) 0xd8};
        return compare(buf, markBuf);
    }

    // JPEG结束符
    private static boolean isJPEGFooter(byte[] buf) {
        byte[] markBuf = {(byte) 0xff, (byte) 0xd9};
        return compare(buf, markBuf);
    }
}
