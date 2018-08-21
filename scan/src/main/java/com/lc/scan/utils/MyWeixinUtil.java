package com.lc.scan.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import com.lc.scan.R;
import com.lc.scan.wxapi.WXEntryActivity;
import com.tencent.mm.opensdk.modelmsg.SendAuth;
import com.tencent.mm.opensdk.modelmsg.SendMessageToWX;
import com.tencent.mm.opensdk.modelmsg.WXImageObject;
import com.tencent.mm.opensdk.modelmsg.WXMediaMessage;
import com.tencent.mm.opensdk.modelmsg.WXTextObject;
import com.tencent.mm.opensdk.modelmsg.WXWebpageObject;
import com.tencent.mm.opensdk.openapi.IWXAPI;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.ConnectException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.MessageDigest;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

/**
 * Created by lichao on 2018/1/29.
 */

public class MyWeixinUtil {
    private final static String ORDER_URL = "https://api.mch.weixin.qq.com/pay/unifiedorder";
    private final static String APP_ID = "wx0b51c9523bffa013";
    private final static String MCH_ID = "1467414802";
    private final static String BODY = "test";
    private final static String IP = "192.168.1.1";
    private final static String TRADE_TYPE = "APP";
    private final static String KEY = "1111111111";//还没拿到

    public static void openWeixin(IWXAPI iwxapi){
        iwxapi.openWXApp();
    }

    public static void shareText(IWXAPI iwxapi, String text){
        WXTextObject textObj = new WXTextObject();
        textObj.text = text;

        WXMediaMessage msg = new WXMediaMessage();
        msg.mediaObject = textObj;
        msg.description = "msg from lc app";

        SendMessageToWX.Req req = new SendMessageToWX.Req();
        req.transaction = buildTransaction("text");
        req.message = msg;
        req.scene = SendMessageToWX.Req.WXSceneSession;  //分享到消息列表
        //req.scene = SendMessageToWX.Req.WXSceneTimeline;  //分享到朋友圈
        iwxapi.sendReq(req);
    }

    public static void shareImg(IWXAPI iwxapi, Bitmap bitmap){
        WXImageObject imgObj = new WXImageObject(bitmap);

        WXMediaMessage msg = new WXMediaMessage();
        msg.mediaObject = imgObj;

        //设置缩略图
        Bitmap thumbBmp = Bitmap.createScaledBitmap(bitmap, 150, 150, true);
        bitmap.recycle();
        msg.thumbData = bmpToByteArray(thumbBmp, true);

        SendMessageToWX.Req req = new SendMessageToWX.Req();
        req.transaction = buildTransaction("img");
        req.message = msg;
        req.scene = SendMessageToWX.Req.WXSceneSession;
        iwxapi.sendReq(req);
    }

    public static void shareUrl(IWXAPI iwxapi, String url, Context context){
        WXWebpageObject webPageObj = new WXWebpageObject();
        webPageObj.webpageUrl = url;

        WXMediaMessage msg = new WXMediaMessage(webPageObj);
        msg.title = "我的百度";
        msg.description = "就是百度";
        Bitmap bitmap = BitmapFactory.decodeResource(context.getResources(), R.mipmap.ic_launcher_round);
        msg.thumbData = bmpToByteArray(bitmap, true);

        SendMessageToWX.Req req = new SendMessageToWX.Req();
        req.transaction = buildTransaction("url");
        req.message = msg;
        req.scene = SendMessageToWX.Req.WXSceneSession;
        iwxapi.sendReq(req);
    }

    public static void getToken(IWXAPI iwxapi){
        SendAuth.Req req = new SendAuth.Req();
        req.scope = "snsapi_userinfo";             //授权域，要获取的内容，个人信息就是snsapi_userinfo
        req.state = "myTest";                      //会话密钥，微信的回调会原样返回
        iwxapi.sendReq(req);
    }

    public static void getAccessToken(String code){

    }

    public static void registerAppToWX(IWXAPI iwxapi){
        iwxapi.registerApp(WXEntryActivity.WECHAT_APP_ID);
    }

    public static String getPrepayId(String notify_url, String out_trade_no, String total_fee){
        String random = getRandom();
        String stringA = "appid="+APP_ID+"&body="+BODY+"&mch_id="+MCH_ID+"&nonce_str="+random
                +"&notify_url="+notify_url+"&out_trade_no="+out_trade_no+"&spbill_create_ip="+IP
                +"&total_fee="+total_fee+"&trade_type="+TRADE_TYPE;
        String sign = MD5Encode(stringA + "&key=" + KEY, "UTF-8").toUpperCase();

        try {
            URL url = new URL(ORDER_URL);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();

            conn.setDoOutput(true);
            conn.setDoInput(true);
            conn.setUseCaches(false);
            // 设置请求方式（GET/POST）
            conn.setRequestMethod("POST");
            conn.setRequestProperty("content-type", "application/x-www-form-urlencoded");

            DataOutputStream out = new DataOutputStream(conn.getOutputStream());
            //TODO 请求格式什么样？
            String content = stringA + "&sign="+sign;
            out.writeBytes(content);
            out.flush();
            out.close();

            // 从输入流读取返回内容
            InputStream inputStream = conn.getInputStream();
            InputStreamReader inputStreamReader = new InputStreamReader(inputStream, "utf-8");
            BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
            String str = null;
            StringBuffer buffer = new StringBuffer();
            while ((str = bufferedReader.readLine()) != null) {
                buffer.append(str);
            }
            // 释放资源
            bufferedReader.close();
            inputStreamReader.close();
            inputStream.close();
            inputStream = null;
            out.close();
            out = null;
            conn.disconnect();

            //TODO 解析返回的xml字符串，返回的是xml吧？
            String return_code = map.get("return_code");
            String prepay_id = null;
            if (return_code.contains("SUCCESS")) {
                prepay_id = map.get("prepay_id");//获取到prepay_id
            }

            //TODO 只返回prepay_id吗？网上第三方的都返回什么
            return prepay_id;
        } catch (ConnectException ce) {
            System.out.println("连接超时：{}" + ce);
        } catch (Exception e) {
            System.out.println("https请求异常：{}" + e);
        }
        return "";
    }

    public static String createXmlParam(Map<String, String> map){
        StringBuilder sb = new StringBuilder();
        sb.append("<xml>");
        for (String in : map.keySet()) {
            if(null != map.get(in) && ""!=map.get(in)){
                sb.append("<").append(in).append(">").append(map.get(in)).append("</").append(in).append(">");
            }
        }
        sb.append("</xml>");
        System.out.println(sb.toString());
        return sb.toString();
    }

    public static void unregisterAppToWX(IWXAPI iwxapi){
        iwxapi.unregisterApp();
    }

    public static String getRandom(){
        final String base = "abcdefghijklmnopqrstuvwxyz0123456789";
        final Random random = new Random();
        final StringBuffer sb = new StringBuffer();
        for (int i = 0; i < 32; i++)
        {
            final int number = random.nextInt(base.length());
            sb.append(base.charAt(number));
        }
        return sb.toString();
    }

    public static String MD5Encode(String origin, String charsetname) {
        String resultString = null;
        try {
            resultString = new String(origin);
            MessageDigest md = MessageDigest.getInstance("MD5");
            if (charsetname == null || "".equals(charsetname)) {
                resultString = byteArrayToHexString(md.digest(resultString.getBytes()));
            } else {
                resultString = byteArrayToHexString(md.digest(resultString.getBytes(charsetname)));
            }
        } catch (Exception exception) {
        }
        return resultString;
    }

    private static String byteArrayToHexString(byte b[]) {
        StringBuffer resultSb = new StringBuffer();
        for (int i = 0; i < b.length; i++) {
            resultSb.append(byteToHexString(b[i]));
        }

        return resultSb.toString();
    }

    private static String byteToHexString(byte b) {
        int n = b;
        if (n < 0) {
            n += 256;
        }
        int d1 = n / 16;
        int d2 = n % 16;
        return hexDigits[d1] + hexDigits[d2];
    }

    private static final String hexDigits[] = { "0", "1", "2", "3", "4", "5", "6", "7", "8", "9", "a", "b", "c", "d", "e", "f" };

    private static String buildTransaction(final String type) {
        return (type == null) ? String.valueOf(System.currentTimeMillis()) : type + System.currentTimeMillis();
    }

    private static byte[] bmpToByteArray(final Bitmap bmp, final boolean needRecycle) {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        bmp.compress(Bitmap.CompressFormat.PNG, 100, output);
        if (needRecycle) {
            bmp.recycle();
        }

        byte[] result = output.toByteArray();
        try {
            output.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

        return result;
    }
}
