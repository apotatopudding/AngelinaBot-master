package top.strelitzia.test;

import org.apache.http.HttpEntity;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;

public class weXin {

    private static final String cookie = "pgv_pvid=7614483630; tvfe_boss_uuid=89bd61123819d240; pac_uid=0_e9879e566c6fb; iip=0; RK=NOnxHyhFT5; ptcz=813fbf6565f49e79599365caf805dbbaf9ddec33ae99d5d09c43cbece7132f26; ua_id=EKU7Z2W9iRxaOT06AAAAABLidVrSMkTRUVlggJNAsR0=; wxuin=62440887129542; uuid=484f78d6f356099503cccb1b2e7c1e58; rand_info=CAESILRY5/ruEBUCFSCPM6F1YwYSVujTe7yh0eB3y69wrJ4s; slave_bizuin=3910405839; data_bizuin=3910405839; bizuin=3910405839; data_ticket=kqMd9B5CpH6upv5IOhrG7alBou/++CmNEmOwCRmlrM+X3Dj/XsJvEWndSQbLfbTT; slave_sid=OVJ2bmp3V2RMSnNkWE1QMHdWbmRVOTdGN3lBbUlnZHk3SVdWU01WX20xbWFEZ2VVZ1lYSE9qaTdQZUZoMTlHMzAyMWkxUkdaMjJuUWllbmV4b1c4VHNyck5McVlZdW81N1FIalZfblloT0Y0a1ZoYkNWMXJBU3M2WkFIdkFRak5Ha1pZbXo1dThpcmpFNko3; slave_user=gh_1462ec66a03d; xid=dd163858e621357207c6e8513a2f8723; mm_lang=zh_CN; _clck=3910405839|1|f4o|0";
    private static final String user_agent = "Mozilla/5.0 (Windows NT 10.0; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/80.0.3987.87 Safari/537.36 SE 2.X MetaSr 1.0";
    private static final String fakeid = "MzU3MjYzNjc0Nw==";
    private static final String token = "367049712";

    private static URIBuilder builder() throws URISyntaxException {
        URIBuilder builder = new URIBuilder("https://mp.weixin.qq.com/cgi-bin/appmsg");
        String beginNum = "0";
        builder.setParameter("action", "list_ex");
        builder.setParameter("begin", beginNum);
        builder.setParameter("count", "5");
        builder.setParameter("fakeid", fakeid);
        builder.setParameter("type", "9");
        builder.setParameter("token", token);
        builder.setParameter("lang", "zh_CN");
        builder.setParameter("f", "json");
        builder.setParameter("ajax", "1");
        return builder;
    }

    public static void main(String[] args) throws IOException, URISyntaxException {
        System.out.println();
    }

    public static String getToken() throws IOException {
        CloseableHttpClient httpClient = HttpClients.createDefault();
        CloseableHttpResponse response;
        HttpGet httpGet = new HttpGet("https://mp.weixin.qq.com/cgi-bin/bizlogin?action=validate&lang=zh_CN&account=515133662%40qq.com&token=");
        response = httpClient.execute(httpGet);
        HttpEntity httpEntity = response.getEntity();
        FileOutputStream out = new FileOutputStream("F://123.html");
        out.write(EntityUtils.toString(httpEntity).getBytes(StandardCharsets.UTF_8));
        return EntityUtils.toString(httpEntity);
    }

    public static String tokenFind() throws URISyntaxException, IOException {
        URIBuilder builder = new URIBuilder("https://api.weixin.qq.com/cgi-bin/token");
        builder.setParameter("grant_type","client_credential");
        builder.setParameter("appid","wxeda705fa8a02588e");
        builder.setParameter("secret","21439ed766513a8595cf0f6e8e017e9f");
        CloseableHttpClient httpclient = HttpClients.createDefault();
        CloseableHttpResponse response;
        HttpGet httpget = new HttpGet(builder.build());
        response = httpclient.execute(httpget);
        HttpEntity httpEntity = response.getEntity();
        JSONObject json = new JSONObject(EntityUtils.toString(httpEntity, "utf-8"));
        String token = json.getString("access_token");
        return token;
    }

    public static String weiXin() throws IOException, URISyntaxException {
        CloseableHttpClient httpclient = HttpClients.createDefault();
        CloseableHttpResponse response;
        HttpGet httpget = new HttpGet(builder().build());
        httpget.addHeader("cookie",cookie);
        httpget.addHeader("user_agent",user_agent);
        response = httpclient.execute(httpget);
        //判断响应状态为200，进行处理
        if (response.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
            HttpEntity httpEntity = response.getEntity();//获取响应文本体
            JSONObject json = new JSONObject(EntityUtils.toString(httpEntity, "utf-8"));
            JSONArray app_msg_list = json.getJSONArray("app_msg_list");
            String url = app_msg_list.getJSONObject(1).getString("link");
            return url;
        } else {
            //如果返回状态不是200，比如404（页面不存在）等，根据情况做处理，这里略
            System.out.println("返回状态不是200");
            System.out.println(EntityUtils.toString(response.getEntity(), "utf-8"));
            return null;
        }
    }
    public static String zhiHu() throws IOException {
        CloseableHttpClient httpclient = HttpClients.createDefault();
        CloseableHttpResponse response;
        HttpGet httpget = new HttpGet("https://www.zhihu.com/api/v4/columns/c_1261258401923026944/items");
        response = httpclient.execute(httpget);
        if (response.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
            //5.获取响应内容
            HttpEntity httpEntity = response.getEntity();
            JSONObject json = new JSONObject(EntityUtils.toString(httpEntity, "utf-8"));
            JSONArray data = json.getJSONArray("data");
            String zhiHuUrl = data.getJSONObject(0).getString("url");
            return zhiHuUrl;
        } else {
            //如果返回状态不是200，比如404（页面不存在）等，根据情况做处理，这里略
            System.out.println("返回状态不是200");
            System.out.println(EntityUtils.toString(response.getEntity(), "utf-8"));
            return null;
        }
    }

    public static void zhuanLan() throws IOException {
        CloseableHttpClient httpclient = HttpClients.createDefault();
        CloseableHttpResponse response;
        HttpGet httpget = new HttpGet("https://zhuanlan.zhihu.com/p/562366493");
        response = httpclient.execute(httpget);
        if (response.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
            //5.获取响应内容
            HttpEntity httpEntity = response.getEntity();
            byte[] bytes = EntityUtils.toString(httpEntity, "utf-8").getBytes(StandardCharsets.UTF_8);
            FileOutputStream out = new FileOutputStream("F://123.html");
            out.write(bytes);
        } else {
            //如果返回状态不是200，比如404（页面不存在）等，根据情况做处理，这里略
            System.out.println("返回状态不是200");
            System.out.println(EntityUtils.toString(response.getEntity(), "utf-8"));
        }
    }

}
