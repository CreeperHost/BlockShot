package net.creeperhost.blockshot;

import com.google.common.util.concurrent.AtomicDouble;
import net.creeperhost.blockshot.lib.TrackableByteArrayEntity;
import net.minecraft.client.Minecraft;
import org.apache.commons.compress.utils.IOUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHeaders;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.entity.BasicHttpEntity;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.entity.FileEntity;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.cookie.BasicClientCookie;
import org.apache.http.message.AbstractHttpMessage;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.Nullable;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.Map;

public class WebUtils {
    public static final Logger LOGGER = LogManager.getLogger();

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
            conn.setRequestProperty("Server-Id", Auth.checkAndGetServerID());
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
            LOGGER.error("Web request error", throwable);
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

    public static String postWebResponse(String urlString, Map<String, String> postDataMap, @Nullable AtomicDouble progress) {
        return postWebResponse(urlString, mapToFormString(postDataMap), progress);
    }

    public static String methodWebResponse(String urlString, String postDataString, String method, boolean isJson, boolean silent, boolean gif, @Nullable AtomicDouble progress) {
        //Turns out getting upload progress is basically impossible with this setup.
        try {
            byte[] postData = postDataString.getBytes(StandardCharsets.UTF_8);
            int postDataLength = postData.length;

            URL url = new URL(urlString);

            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/57.0.2987.138 Safari/537.36 Vivaldi/1.8.770.56 BlockShot/1.0.0");

            //Used only to verify you against Mojang using hasJoined
            conn.setRequestProperty("Server-Id", Auth.checkAndGetServerID());
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
            conn.setConnectTimeout(30000);
            conn.setUseCaches(false);
            conn.setDoOutput(true);
            try {
                DataOutputStream wr = new DataOutputStream(conn.getOutputStream());
                wr.write(postData);
//                for (int i = 0; i < postData.length; i++) {
//                    wr.write(postData[i]);
//                    if (progress != null) {
//                        progress.set(i / (double)postData.length);
//                    }
//                }
            } catch (Throwable ignored) { }

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

    //Testing
    public static String methodVideoWebResponse(String urlString, File file, String method) {
        try {
//            String postDataString;
//            try (FileInputStream is = new FileInputStream(file)) {
//                postDataString = Base64.getEncoder().encodeToString(is.readAllBytes());
//            }


//            byte[] postData = postDataString.getBytes(StandardCharsets.UTF_8);
//            int postDataLength = postData.length;

            URL url = new URL(urlString);

            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/57.0.2987.138 Safari/537.36 Vivaldi/1.8.770.56 BlockShot/1.0.0");

            //Used only to verify you against Mojang using hasJoined
            conn.setRequestProperty("Server-Id", Auth.checkAndGetServerID());
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

//            conn.setRequestProperty("Screencap-Type", "image/gif");
//            conn.setRequestProperty("Screencap-Type", "image/jpeg");

//            conn.setRequestProperty("Screencap-Type", "video/x-flv");
//            conn.setRequestProperty("Screencap-Type", "video/mp4");
//            conn.setRequestProperty("Screencap-Type", "application/x-mpegURL");
//            conn.setRequestProperty("Screencap-Type", "video/MP2T");
//            conn.setRequestProperty("Screencap-Type", "video/3gpp");
            conn.setRequestProperty("Screencap-Type", "video/quicktime");
//            conn.setRequestProperty("Screencap-Type", "video/x-msvideo");
//            conn.setRequestProperty("Screencap-Type", "video/x-ms-wmv");

            conn.setRequestProperty("Content-Type", "application/json");
//            conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
//            conn.setRequestProperty("Content-Type", "multipart/form-data");

//            conn.setRequestProperty("charset", "utf-8");
            conn.setRequestProperty("Content-Length", Long.toString(file.length()));
//            conn.setRequestProperty("Content-Length", Integer.toString(postDataString.length()));
            conn.setConnectTimeout(30000);
            conn.setUseCaches(false);
            conn.setDoOutput(true);

//            try {
//                DataOutputStream wr = new DataOutputStream(conn.getOutputStream());
//                wr.write(postData);
//
//            } catch (Throwable ignored) {
//                BlockShot.LOGGER.error(ignored);
//            }

            try (FileInputStream is = new FileInputStream(file)) {
                DataOutputStream wr = new DataOutputStream(conn.getOutputStream());
                wr.write(is.readAllBytes());
//                IOUtils.copy(is, wr);
            } catch (Throwable ignored) {
                LOGGER.error(ignored);
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

    public static String postWebResponse(String urlString, String postDataString, @Nullable AtomicDouble progress) {
        return methodWebResponse(urlString, postDataString, "POST", false, false, false, progress);
    }

    public static String putWebResponse(String urlString, String body, boolean isJson, boolean isSilent, @Nullable AtomicDouble progress) {
        return methodWebResponse(urlString, body, "PUT", isJson, isSilent, false, progress);
    }

//    public static String putWebResponse(String urlString, String body, boolean isJson, boolean isSilent, boolean isAnimated, @Nullable AtomicDouble progress) {
//        return methodWebResponse(urlString, body, "PUT", isJson, isSilent, true, progress);
//    }

    public static String upload(String data, MediaType type, @Nullable AtomicDouble progress) {
        byte[] postData = data.getBytes(StandardCharsets.UTF_8);
        HttpPost httppost = new HttpPost("https://blockshot.ch/upload");
        httppost.setEntity(new TrackableByteArrayEntity(postData, progress));
        httppost.setHeader("charset", "utf-8");
        return executeWebRequest(httppost, type);
    }

    public static String upload(byte[] bytes, MediaType type, @Nullable AtomicDouble progress) {
        HttpPost httppost = new HttpPost("https://blockshot.ch/upload");
        httppost.setEntity(new TrackableByteArrayEntity(bytes, progress));
        return executeWebRequest(httppost, type);
    }

    public static String upload(File file, MediaType type) {
        HttpPost httppost = new HttpPost("https://blockshot.ch/upload");
        httppost.setEntity(new FileEntity(file));
        return executeWebRequest(httppost, type);
    }

    private static String executeWebRequest(HttpUriRequest message, MediaType type) {
        try (CloseableHttpClient client = buildClient()) {
            applyHeaders(message, type);
            CloseableHttpResponse response = client.execute(message);
            BufferedReader reader = new BufferedReader(new InputStreamReader(response.getEntity().getContent()));
            try (response; reader) {
                StringBuilder result = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    result.append(line);
                }
                String res = result.toString();
                return res.isEmpty() ? "error" : res;
            }

        } catch (IOException e) {
            LOGGER.error("Something went wrong while executing web request", e);
        }
        return "error";
    }

    private static CloseableHttpClient buildClient() {
        //TODO Cookies? They dont seem to be used by the current system so do we need them?
        HttpClientBuilder clientBuilder = HttpClients.custom()
                .setUserAgent("Mozilla/5.0 (Windows NT 10.0; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/57.0.2987.138 Safari/537.36 Vivaldi/1.8.770.56 BlockShot/1.0.0");
        return clientBuilder.build();
    }

    private static void applyHeaders(HttpUriRequest message, MediaType type) {
        message.setHeader("Server-Id", Auth.checkAndGetServerID());
        message.setHeader("Minecraft-Name", Minecraft.getInstance().getUser().getName());
        if (!Config.INSTANCE.anonymous) {
            message.setHeader("Minecraft-Uuid", Minecraft.getInstance().getUser().getUuid()); //Used to trigger our servers to store additional meta data about your image to allow you to delete and list
        }
        type.apply(message);
    }

    public enum MediaType {
        JPEG("Screencap-Type", "image/jpeg", HttpHeaders.CONTENT_TYPE, "application/x-www-form-urlencoded"),
        PNG("Screencap-Type", "image/png", HttpHeaders.CONTENT_TYPE, "application/x-www-form-urlencoded"),
        GIF("Screencap-Type", "image/gif", HttpHeaders.CONTENT_TYPE, "application/x-www-form-urlencoded"),
        MOV("Screencap-Type", "video/quicktime", HttpHeaders.CONTENT_TYPE, "application/x-www-form-urlencoded"),
        MP4("Screencap-Type", "video/mp4", HttpHeaders.CONTENT_TYPE, "application/x-www-form-urlencoded"),
        WEBM("Screencap-Type", "video/webm", HttpHeaders.CONTENT_TYPE, "application/x-www-form-urlencoded"),
        AVI("Screencap-Type", "video/x-msvideo", HttpHeaders.CONTENT_TYPE, "application/x-www-form-urlencoded"),
        MKV("Screencap-Type", "video/x-matroska", HttpHeaders.CONTENT_TYPE, "application/x-www-form-urlencoded"),
        JSON(HttpHeaders.CONTENT_TYPE, "application/json");

        private final String[] headers;

        MediaType(String... headers) {
            this.headers = headers;
        }

        public void apply(HttpUriRequest message) {
            for (int i = 0; i < headers.length; i += 2) {
                message.setHeader(headers[i], headers[i + 1]);
            }
        }
    }
}
















