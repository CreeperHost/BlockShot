package net.creeperhost.blockshot;

import net.minecraft.client.Minecraft;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

public class WebUtils {
    private static List<String> cookies;

    public static String getWebResponse(String urlString) {
        return getWebResponse(urlString, 0, false);
    }

    public static String getWebResponse(String urlString, int timeout) {
        return getWebResponse(urlString, timeout, false);
    }

    public static String getWebResponse(String urlString, int timeout, boolean print) {
        try {
            if (timeout == 0) timeout = 120000;

            URL url = new URL(urlString);
            URI uri = new URI(url.getProtocol(), url.getUserInfo(), url.getHost(), url.getPort(), url.getPath(), url.getQuery(), url.getRef());
            url = uri.toURL();
            // lul
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            //            conn.setConnectTimeout(10);
            conn.setReadTimeout(timeout);
            conn.setRequestMethod("GET");

            if (cookies != null) {
                for (String cookie : cookies) {
                    conn.addRequestProperty("Cookie", cookie.split(";", 2)[0]);
                }
            }
            conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/57.0.2987.138 Safari/537.36 Vivaldi/1.8.770.56 MineTogether/0.0.0");
            //Used only to verify you against Mojang using hasJoined
            conn.setRequestProperty("Server-Id", BlockShot.getServerIDAndVerify());
            conn.setRequestProperty("Minecraft-Name", Minecraft.getInstance().getUser().getName());
            if (!Config.INSTANCE.anonymous) {
                //Used to trigger our servers to store additional meta data about your image to allow you to delete and list
                conn.setRequestProperty("Minecraft-Uuid", Minecraft.getInstance().getUser().getUuid());
            }
            BufferedReader rd = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            String line;
            StringBuilder respData = new StringBuilder();
            while ((line = rd.readLine()) != null) {
                respData.append(line);
                respData.append("\n");
            }

            List<String> setCookies = conn.getHeaderFields().get("Set-Cookie");

            if (setCookies != null) {
                cookies = setCookies;
            }

            rd.close();
            return respData.toString();
        } catch (Throwable throwable) {
//            BlockShot.logger.error(throwable);
        }
        return "error";
    }

    private static String mapToFormString(Map<String, String> map) {
        StringBuilder postDataStringBuilder = new StringBuilder();

        String postDataString;

        try {
            for (Map.Entry<String, String> entry : map.entrySet()) {
                postDataStringBuilder.append(URLEncoder.encode(entry.getKey(), "UTF-8")).append("=").append(URLEncoder.encode(entry.getValue(), "UTF-8")).append("&");
            }
        } catch (Exception ignored) {
        } finally {
            postDataString = postDataStringBuilder.toString();
        }
        return postDataString;
    }

    public static String postWebResponse(String urlString, Map<String, String> postDataMap) {
        return postWebResponse(urlString, mapToFormString(postDataMap));
    }

    public static String methodWebResponse(String urlString, String postDataString, String method, boolean isJson, boolean silent, boolean gif) {
        try {
            postDataString.substring(0, postDataString.length() - 1);

            byte[] postData = postDataString.getBytes(StandardCharsets.UTF_8);
            int postDataLength = postData.length;

            URL url = new URL(urlString);

            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/57.0.2987.138 Safari/537.36 Vivaldi/1.8.770.56 BlockShot/1.0.0");

            //Used only to verify you against Mojang using hasJoined
            conn.setRequestProperty("Server-Id", BlockShot.getServerIDAndVerify());
            conn.setRequestProperty("Minecraft-Name", Minecraft.getInstance().getUser().getName());
            if (!Config.INSTANCE.anonymous) {
                //Used to trigger our servers to store additional meta data about your image to allow you to delete and list
                conn.setRequestProperty("Minecraft-Uuid", Minecraft.getInstance().getUser().getUuid());
            }
            conn.setRequestMethod(method);
            if (cookies != null) {
                for (String cookie : cookies) {
                    conn.addRequestProperty("Cookie", cookie.split(";", 2)[0]);
                }
            }
            if (gif) {
                conn.setRequestProperty("Screencap-Type", "image/gif");
            } else {
                conn.setRequestProperty("Screencap-Type", "image/jpeg");
            }
            conn.setRequestProperty("Content-Type", isJson ? "application/json" : "application/x-www-form-urlencoded");
            conn.setRequestProperty("charset", "utf-8");
            conn.setRequestProperty("Content-Length", Integer.toString(postDataLength));
            conn.setConnectTimeout(5000);
            conn.setUseCaches(false);
            conn.setDoOutput(true);
            try {
                DataOutputStream wr = new DataOutputStream(conn.getOutputStream());
                wr.write(postData);
            } catch (Throwable ignored) {
            }

            BufferedReader rd = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            String line;
            StringBuilder respData = new StringBuilder();
            while ((line = rd.readLine()) != null) {
                respData.append(line);
            }

            List<String> setCookies = conn.getHeaderFields().get("Set-Cookie");

            if (setCookies != null) {
                cookies = setCookies;
            }

            rd.close();
            return respData.toString();
        } catch (Throwable e) {
            e.printStackTrace();
        }
        return "error";
    }

    public static String postWebResponse(String urlString, String postDataString) {
        return methodWebResponse(urlString, postDataString, "POST", false, false, false);
    }

    public static String putWebResponse(String urlString, String body, boolean isJson, boolean isSilent) {
        return methodWebResponse(urlString, body, "PUT", isJson, isSilent, false);
    }

    public static String putWebResponse(String urlString, String body, boolean isJson, boolean isSilent, boolean isAnimated) {
        return methodWebResponse(urlString, body, "PUT", isJson, isSilent, true);
    }
}
