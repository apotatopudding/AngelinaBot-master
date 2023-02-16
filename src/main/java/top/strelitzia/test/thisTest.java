package top.strelitzia.test;

import java.io.IOException;

public class thisTest {
    public static void main(String[] args) throws IOException {
        String s = "a";
        Long l = Long.valueOf(s);
        System.out.println(l);
    }

    private static String change(String s){
        //用来把数据库的带下划线字符转成java首字母大写字符
        String[] sqlit = s.split("_");
        for (int i = 0; i < sqlit.length-1; i++) {
            StringBuilder sb = new StringBuilder(s);
            int substring = s.lastIndexOf("_");
            String c = s.substring(substring+1,substring+2).toUpperCase();
            sb.replace(substring+1,substring+2,c)
                    .replace(substring,substring+1,"");
            s = sb.toString();
        }
        return s;
    }
}
