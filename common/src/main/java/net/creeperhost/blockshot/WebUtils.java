package net.creeperhost.blockshot;

import com.google.common.util.concurrent.AtomicDouble;
import com.mojang.blaze3d.platform.NativeImage;
import net.creeperhost.blockshot.polylib.ModPackInfo;
import net.covers1624.quack.net.httpapi.EngineRequest;
import net.covers1624.quack.net.httpapi.EngineResponse;
import net.covers1624.quack.net.httpapi.HttpEngine;
import net.covers1624.quack.net.httpapi.WebBody;
import net.covers1624.quack.net.httpapi.java11.Java11HttpEngine;
import net.creeperhost.minetogether.session.MineTogetherSession;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.Nullable;

import java.io.*;
import java.nio.charset.StandardCharsets;

public class WebUtils {
    public static final Logger LOGGER = LogManager.getLogger();
    private static final HttpEngine WEB_ENGINE = Java11HttpEngine.create();
    private static final String CONTENT_TYPE = "Content-Type";
    private static final String USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/57.0.2987.138 Safari/537.36 Vivaldi/1.8.770.56 BlockShot/1.0.0";

    public static String delete(String url, @Nullable AtomicDouble progress) {
        return executeWebRequest(buildRequest("DELETE", url, null), null, progress, true);
    }

    public static String get(String url, @Nullable AtomicDouble progress) {
        return executeWebRequest(buildRequest("GET", url, null), null, progress, true);
    }

    public static String put(String url, byte[] data, MediaType type, @Nullable AtomicDouble progress) {
        ModPackInfo.VersionInfo info = ModPackInfo.getInfo();
        String platform = "";
        String id = "";
        // this is not really the best place to put it, refactoring to allow headers before this would be best
        // but it's only used in this project, and put is only used in uploading media, so...
        if (!info.ftbPackID.isEmpty()) {
            id = info.ftbPackID;
            platform = "FTB";
        } else if (!info.curseID.isEmpty()) {
            id = info.curseID;
            platform = "Curseforge";
        }

        EngineRequest request = buildRequest("PUT", url, track(WebBody.bytes(data, type.contentType()), progress));
        if (!platform.isEmpty()) {
            setHeader(request, "Modpack-Platform", platform);
            setHeader(request, "Modpack-Id", id);
        }
        return executeWebRequest(request, type, null, true);
    }

    public static String post(String url, String data, MediaType type, @Nullable AtomicDouble progress) {
        byte[] postData = data.getBytes(StandardCharsets.UTF_8);
        EngineRequest request = buildRequest("POST", url, track(WebBody.bytes(postData, type.contentType()), progress));
        setHeader(request, "charset", "utf-8");
        return executeWebRequest(request, type, null, true);
    }

    public static String post(String url, byte[] bytes, MediaType type, @Nullable AtomicDouble progress) {
        return executeWebRequest(buildRequest("POST", url, track(WebBody.bytes(bytes, type.contentType()), progress)), type, null, true);
    }

    public static String post(String url, File file, MediaType type) {
        return executeWebRequest(buildRequest("POST", url, WebBody.path(file.toPath(), type.contentType())), type, null, true);
    }

    public static NativeImage getImageFromUrl(String url) {
        try (EngineResponse response = buildRequest("GET", url, null).execute()) {
            WebBody body = response.body();
            if (body == null) return null;
            try (InputStream is = body.open()) {
                return NativeImage.read(is);
            }
        } catch (Exception e) {
            LOGGER.error("Something went wrong while executing web request", e);
        }
        return null;
    }

    public static String executeWebRequest(EngineRequest message, @Nullable MediaType type, @Nullable AtomicDouble progress, boolean authHeaders) {
        try {
            if (authHeaders) {
                authHeaders(message);
            }
            if (type != null) {
                type.apply(message);
            }

            try (EngineResponse response = message.execute()) {
                int statusCode = response.statusCode();
                String body = handleResponse(response, progress);

                if (statusCode < 200 || statusCode > 299) {
                    LOGGER.error("Web Request failed. Returned response code: {}, Reason: {}, Body {}", statusCode, response.message(), body);
                    return switch (statusCode) {
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
            }
        } catch (IOException e) {
            LOGGER.error("Something went wrong while executing web request", e);
        }
        return "error";
    }

    private static String handleResponse(EngineResponse response, @Nullable AtomicDouble progress) throws IOException {
        WebBody body = response.body();
        if (body == null) return "";
        long len = body.length();
        try (InputStream is = body.open()) {
            ByteArrayOutputStream resultBuffer = new ByteArrayOutputStream();
            byte[] buffer = new byte[512];
            long count = 0;
            int n;
            while (-1 != (n = is.read(buffer))) {
                resultBuffer.write(buffer, 0, n);
                count += n;
                if (progress != null) progress.set(len > 0 ? (count / (double) len) : Math.max(1.1, count));
            }
            return resultBuffer.toString(StandardCharsets.UTF_8);
        }
    }

    private static EngineRequest buildRequest(String method, String url, @Nullable WebBody body) {
        return WEB_ENGINE.newRequest()
                .method(method, body)
                .url(url)
                .header("User-Agent", USER_AGENT);
    }

    private static WebBody track(WebBody body, @Nullable AtomicDouble progress) {
        if (progress == null) return body;
        return new ProgressWebBody(body, progress);
    }

    private static void setHeader(EngineRequest message, String key, String value) {
        message.removeHeader(key);
        message.header(key, value);
    }

    private static void authHeaders(EngineRequest message) throws IOException {
//        message.setHeader("Server-Id", Auth.getMojangServerId());
        setHeader(message, "Authorization", "Bearer " + MineTogetherSession.getDefault().getToken());
        if (Config.INSTANCE.anonymous) {
            setHeader(message, "Anonymous", "true"); //Used to trigger our servers to store additional meta data about your image to allow you to delete and list
        }
    }

    private record ProgressWebBody(WebBody body, AtomicDouble progress) implements WebBody {
        @Override
        public InputStream open() throws IOException {
            return new FilterInputStream(body.open()) {
                private long count = 0;

                @Override
                public int read() throws IOException {
                    int result = super.read();
                    if (result != -1) update(1);
                    return result;
                }

                @Override
                public int read(byte[] b, int off, int len) throws IOException {
                    int result = super.read(b, off, len);
                    if (result > 0) update(result);
                    return result;
                }

                private void update(int read) {
                    count += read;
                    long len = body.length();
                    progress.set(len > 0 ? (count / (double) len) : Math.max(1.1, count));
                }
            };
        }

        @Override
        public boolean multiOpenAllowed() {
            return body.multiOpenAllowed();
        }

        @Override
        public long length() {
            return body.length();
        }

        @Nullable
        @Override
        public String contentType() {
            return body.contentType();
        }
    }

    public enum MediaType {
        JPEG("Screencap-Type", "image/jpeg"),
        PNG("Screencap-Type", "image/png"),
        GIF("Screencap-Type", "image/gif", CONTENT_TYPE, "application/x-www-form-urlencoded"),
//        MOV("Screencap-Type", "video/quicktime", CONTENT_TYPE, "application/x-www-form-urlencoded"),
//        MP4("Screencap-Type", "video/mp4", CONTENT_TYPE, "application/x-www-form-urlencoded"),
        WEBM("Screencap-Type", "video/webm"),
//        AVI("Screencap-Type", "video/x-msvideo", CONTENT_TYPE, "application/x-www-form-urlencoded"),
//        MKV("Screencap-Type", "video/x-matroska", CONTENT_TYPE, "application/x-www-form-urlencoded"),
        JSON(CONTENT_TYPE, "application/json");

        private final String[] headers;

        MediaType(String... headers) {
            this.headers = headers;
        }

        public void apply(EngineRequest message) {
            for (int i = 0; i < headers.length; i += 2) {
                setHeader(message, headers[i], headers[i + 1]);
            }
        }

        public String contentType() {
            for (int i = 0; i < headers.length; i += 2) {
                if (headers[i].equalsIgnoreCase(CONTENT_TYPE)) {
                    return headers[i + 1];
                }
            }
            for (int i = 0; i < headers.length; i += 2) {
                if (headers[i].equalsIgnoreCase("Screencap-Type")) {
                    return headers[i + 1];
                }
            }
            return "application/octet-stream";
        }
    }
}














