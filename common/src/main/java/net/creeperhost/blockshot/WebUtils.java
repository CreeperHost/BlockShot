package net.creeperhost.blockshot;

import com.google.common.util.concurrent.AtomicDouble;
import net.creeperhost.blockshot.lib.TrackableByteArrayEntity;
import net.minecraft.client.Minecraft;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHeaders;
import org.apache.http.StatusLine;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.entity.FileEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.Nullable;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

public class WebUtils {
    public static final Logger LOGGER = LogManager.getLogger();

    public static String get(String url, @Nullable AtomicDouble progress) {
        return executeWebRequest(new HttpGet(url), null, progress, true);
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

            if (status.getStatusCode() != 200) {
                LOGGER.error("Web Request failed. Returned response code: {}, Reason: {}", status.getStatusCode(), status.getReasonPhrase());
                return "error";
            }

            return handleResponse(response, progress);
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
            if (res.isEmpty()) {
                //For now this is fine but if we ever need to do a request that expects an empty response then this will have to move.
                LOGGER.error("Error executing web request, Empty response");
                return "error";
            }
            return res;
        }
    }

    public static CloseableHttpClient buildClient() {
        //TODO Cookies? They dont seem to be used by the current system so do we need them?
        HttpClientBuilder clientBuilder = HttpClients.custom()
                .setUserAgent("Mozilla/5.0 (Windows NT 10.0; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/57.0.2987.138 Safari/537.36 Vivaldi/1.8.770.56 BlockShot/1.0.0");
        return clientBuilder.build();
    }

    private static void authHeaders(HttpUriRequest message) {
//        message.setHeader("Server-Id", Auth.getMojangServerId());
        message.setHeader("Authorization", "Bearer " + Auth.getCreeperHostAuth());
        message.setHeader("Minecraft-Name", Minecraft.getInstance().getUser().getName());
        if (!Config.INSTANCE.anonymous) {
            message.setHeader("Minecraft-Uuid", Minecraft.getInstance().getUser().getUuid()); //Used to trigger our servers to store additional meta data about your image to allow you to delete and list
        }
    }

    public enum MediaType {
//        JPEG("Screencap-Type", "image/jpeg", HttpHeaders.CONTENT_TYPE, "application/x-www-form-urlencoded"),
        PNG("Screencap-Type", "image/png", HttpHeaders.CONTENT_TYPE, "application/x-www-form-urlencoded"),
        GIF("Screencap-Type", "image/gif", HttpHeaders.CONTENT_TYPE, "application/x-www-form-urlencoded"),
//        MOV("Screencap-Type", "video/quicktime", HttpHeaders.CONTENT_TYPE, "application/x-www-form-urlencoded"),
//        MP4("Screencap-Type", "video/mp4", HttpHeaders.CONTENT_TYPE, "application/x-www-form-urlencoded"),
        WEBM("Screencap-Type", "video/webm", HttpHeaders.CONTENT_TYPE, "application/x-www-form-urlencoded"),
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
















