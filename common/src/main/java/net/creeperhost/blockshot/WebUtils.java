package net.creeperhost.blockshot;

import com.google.common.util.concurrent.AtomicDouble;
import com.mojang.blaze3d.platform.NativeImage;
import net.creeperhost.blockshot.lib.TrackableByteArrayEntity;
import net.creeperhost.blockshot.polylib.ModPackInfo;
import net.creeperhost.minetogether.session.MineTogetherSession;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHeaders;
import org.apache.http.StatusLine;
import org.apache.http.client.methods.*;
import org.apache.http.entity.FileEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.Nullable;

import java.io.*;
import java.nio.charset.StandardCharsets;

public class WebUtils {
    public static final Logger LOGGER = LogManager.getLogger();

    public static String delete(String url, @Nullable AtomicDouble progress) {
        return executeWebRequest(new HttpDelete(url), null, progress, true);
    }

    public static String get(String url, @Nullable AtomicDouble progress) {
        return executeWebRequest(new HttpGet(url), null, progress, true);
    }

    public static String put(String url, byte[] data, MediaType type, @Nullable AtomicDouble progress) {
        ModPackInfo.VersionInfo info = ModPackInfo.getInfo();
        String platform = "";
        String id = "";
        // this is not really the best place to put it, refactoring to allow headers before this would be best
        // but it's only used in this project, and put is only used in uploading media, so...
        if (!info.ftbPackID.isEmpty()) {
            id = info.ftbPackID.substring(1);
            platform = "FTB";
        } else if (!info.curseID.isEmpty()) {
            id = info.curseID;
            platform = "Curseforge";
        }

        HttpPut httpput = new HttpPut(url);
        if (!platform.isEmpty()) {
            httpput.setHeader("Modpack-Platform", platform);
            httpput.setHeader("Modpack-Id", id);
        }
        httpput.setEntity(new TrackableByteArrayEntity(data, progress));
        return executeWebRequest(httpput, type, null, true);
    }

    public static String post(String url, String data, MediaType type, @Nullable AtomicDouble progress) {
        byte[] postData = data.getBytes(StandardCharsets.UTF_8);
        HttpPost httppost = new HttpPost(url);
        httppost.setEntity(new TrackableByteArrayEntity(postData, progress));
        httppost.setHeader("charset", "utf-8");
        return executeWebRequest(httppost, type, null, true);
    }

    public static String post(String url, byte[] bytes, MediaType type, @Nullable AtomicDouble progress) {
        HttpPost httppost = new HttpPost(url);
        httppost.setEntity(new TrackableByteArrayEntity(bytes, progress));
        return executeWebRequest(httppost, type, null, true);
    }

    public static String post(String url, File file, MediaType type) {
        HttpPost httppost = new HttpPost(url);
        httppost.setEntity(new FileEntity(file));
        return executeWebRequest(httppost, type, null, true);
    }

    private static boolean scanned = false;

    public static NativeImage getImageFromUrl(String url) {
        HttpGet message = new HttpGet(url);
        try (CloseableHttpClient client = buildClient()) {

            CloseableHttpResponse response = client.execute(message);

            HttpEntity entity = response.getEntity();
            try (response; InputStream is = entity.getContent()) {
                return NativeImage.read(is);
            }
        } catch (Exception e) {
            LOGGER.error("Something went wrong while executing web request", e);
        }
        return null;
    }

    public static String executeWebRequest(HttpUriRequest message, @Nullable MediaType type, @Nullable AtomicDouble progress, boolean authHeaders) {
        try (CloseableHttpClient client = buildClient()) {
            if (authHeaders) {
                authHeaders(message);
            }
            if (type != null) {
                type.apply(message);
            }

            CloseableHttpResponse response = client.execute(message);
            StatusLine status = response.getStatusLine();

            String body = handleResponse(response, progress);

            if (status.getStatusCode() < 200 || status.getStatusCode() > 299) {
                LOGGER.error("Web Request failed. Returned response code: {}, Reason: {}, Body {}", status.getStatusCode(), status.getReasonPhrase(), body);
                return switch (status.getStatusCode()) {
                    case 401 -> "Unauthorized";
                    case 402 -> "Video is longer than allowed. Please upgrade to upload videos of this size";
                    case 409 -> "Client closed connection.";
                    case 413 -> "File is too large.";
                    case 415 -> "Unsupported media type.";
                    case 422 -> "Video is longer than allowed.";
                    case 500 -> "Unexpected error occurred.";
                    default -> "error";
                };
            }

            return body;
        } catch (IOException e) {
            LOGGER.error("Something went wrong while executing web request", e);
        }
        return "error";
    }

    private static String handleResponse(CloseableHttpResponse response, @Nullable AtomicDouble progress) throws IOException {
        HttpEntity entity = response.getEntity();
        long len = entity.getContentLength();
        try (response; InputStream is = entity.getContent()) {
            ByteArrayOutputStream resultBuffer = new ByteArrayOutputStream();
            byte[] buffer = new byte[512];
            long count = 0;
            int n;
            while (-1 != (n = is.read(buffer))) {
                resultBuffer.write(buffer, 0, n);
                count += n;
                if (progress != null) progress.set(len > 0 ? (count / (double) len) : Math.max(1.1, count));
            }
            String res = resultBuffer.toString();
            return res;
        }
    }

    public static CloseableHttpClient buildClient() {
        //TODO Cookies? They dont seem to be used by the current system so do we need them?
        HttpClientBuilder clientBuilder = HttpClients.custom()
                .setUserAgent("Mozilla/5.0 (Windows NT 10.0; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/57.0.2987.138 Safari/537.36 Vivaldi/1.8.770.56 BlockShot/1.0.0");
        return clientBuilder.build();
    }

    private static void authHeaders(HttpUriRequest message) throws IOException {
//        message.setHeader("Server-Id", Auth.getMojangServerId());
        message.setHeader("Authorization", "Bearer " + MineTogetherSession.getDefault().getToken());
        if (Config.INSTANCE.anonymous) {
            message.setHeader("Anonymous", "true"); //Used to trigger our servers to store additional meta data about your image to allow you to delete and list
        }
    }

    public enum MediaType {
        JPEG("Screencap-Type", "image/jpeg"),
        PNG("Screencap-Type", "image/png"),
        GIF("Screencap-Type", "image/gif", HttpHeaders.CONTENT_TYPE, "application/x-www-form-urlencoded"),
//        MOV("Screencap-Type", "video/quicktime", HttpHeaders.CONTENT_TYPE, "application/x-www-form-urlencoded"),
//        MP4("Screencap-Type", "video/mp4", HttpHeaders.CONTENT_TYPE, "application/x-www-form-urlencoded"),
        WEBM("Screencap-Type", "video/webm"),
//        AVI("Screencap-Type", "video/x-msvideo", HttpHeaders.CONTENT_TYPE, "application/x-www-form-urlencoded"),
//        MKV("Screencap-Type", "video/x-matroska", HttpHeaders.CONTENT_TYPE, "application/x-www-form-urlencoded"),
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
















