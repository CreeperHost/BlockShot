package net.creeperhost.blockshot;

import com.google.common.util.concurrent.AtomicDouble;
import net.creeperhost.blockshot.lib.ModPackInfo;
import net.creeperhost.minetogether.session.MineTogetherSession;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class WebUtils {
    private static final String USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/57.0.2987.138 Safari/537.36 Vivaldi/1.8.770.56 BlockShot/1.0.0";

    public static String delete(String url) {
        return executeWebRequest("DELETE", url, null, null, true);
    }

    public static String get(String url) {
        return executeWebRequest("GET", url, null, null, true);
    }

    public static String put(String url, byte[] data, MediaType type) {
        return put(url, data, type, null);
    }

    public static String put(String url, byte[] data, MediaType type, AtomicDouble progress) {
        ModPackInfo.VersionInfo info = ModPackInfo.getInfo();
        if (!info.ftbPackID.isEmpty()) {
            return executeWebRequest("PUT", url, data, type, true, progress, "FTB", info.ftbPackID);
        }
        if (!info.curseID.isEmpty()) {
            return executeWebRequest("PUT", url, data, type, true, progress, "Curseforge", info.curseID);
        }
        return executeWebRequest("PUT", url, data, type, true, progress);
    }

    public static String post(String url, String data, MediaType type, AtomicDouble progress) {
        byte[] postData = data.getBytes(StandardCharsets.UTF_8);
        return post(url, postData, type, progress);
    }

    public static String post(String url, byte[] data, MediaType type, AtomicDouble progress) {
        return executeWebRequest("POST", url, data, type, true, progress);
    }

    public static BufferedImage getImageFromUrl(String urlString) {
        try {
            HttpURLConnection conn = openConnection(urlString);
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(10000);
            conn.setReadTimeout(30000);
            try (InputStream inputStream = conn.getInputStream()) {
                return ImageIO.read(inputStream);
            }
        } catch (Throwable ignored) {
        }
        return null;
    }

    public static String executeWebRequest(String method, String urlString, byte[] body, MediaType type, boolean authHeaders) {
        return executeWebRequest(method, urlString, body, type, authHeaders, null);
    }

    public static String executeWebRequest(String method, String urlString, byte[] body, MediaType type, boolean authHeaders, AtomicDouble progress) {
        return executeWebRequest(method, urlString, body, type, authHeaders, progress, null, null);
    }

    public static String executeWebRequest(String method, String urlString, byte[] body, MediaType type, boolean authHeaders, AtomicDouble progress, String modpackPlatform, String modpackId) {
        try {
            HttpURLConnection conn = openConnection(urlString);
            conn.setRequestMethod(method);
            conn.setConnectTimeout(10000);
            conn.setReadTimeout(120000);
            conn.setUseCaches(false);

            if (authHeaders) {
                authHeaders(conn);
            }
            if (type != null) {
                type.apply(conn);
            }
            if (modpackPlatform != null && modpackId != null) {
                conn.setRequestProperty("Modpack-Platform", modpackPlatform);
                conn.setRequestProperty("Modpack-Id", modpackId);
            }
            if (body != null) {
                conn.setDoOutput(true);
                conn.setRequestProperty("Content-Length", Integer.toString(body.length));
                try (OutputStream outputStream = conn.getOutputStream()) {
                    int offset = 0;
                    while (offset < body.length) {
                        int length = Math.min(8192, body.length - offset);
                        outputStream.write(body, offset, length);
                        offset += length;
                        if (progress != null) {
                            progress.set(body.length > 0 ? (offset / (double) body.length) : 1);
                        }
                    }
                }
            }

            int responseCode = conn.getResponseCode();
            String response = readResponse(conn, responseCode);
            if (responseCode < 200 || responseCode > 299) {
                return errorForCode(responseCode);
            }
            return response;
        } catch (Throwable throwable) {
            throwable.printStackTrace();
        }
        return "error";
    }

    private static HttpURLConnection openConnection(String urlString) throws Exception {
        URL url = new URL(urlString);
        URI uri = new URI(url.getProtocol(), url.getUserInfo(), url.getHost(), url.getPort(), url.getPath(), url.getQuery(), url.getRef());
        HttpURLConnection conn = (HttpURLConnection) uri.toURL().openConnection();
        conn.setRequestProperty("User-Agent", USER_AGENT);
        return conn;
    }

    private static void authHeaders(HttpURLConnection conn) {
        try {
            conn.setRequestProperty("Authorization", "Bearer " + MineTogetherSession.getDefault().getToken());
        } catch (Throwable ignored) {
        }
        if (Config.INSTANCE.anonymous) {
            conn.setRequestProperty("Anonymous", "true");
        }
    }

    private static String readResponse(HttpURLConnection conn, int responseCode) throws Exception {
        InputStream responseStream = responseCode < 400 ? conn.getInputStream() : conn.getErrorStream();
        if (responseStream == null) {
            return "";
        }
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(responseStream, StandardCharsets.UTF_8))) {
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
            return response.toString();
        }
    }

    private static String errorForCode(int responseCode) {
        if (responseCode == 401) {
            return "Unauthorized";
        }
        if (responseCode == 402) {
            return "Video is longer than allowed. Please upgrade to upload videos of this size";
        }
        if (responseCode == 409) {
            return "Client closed connection.";
        }
        if (responseCode == 413) {
            return "File is too large.";
        }
        if (responseCode == 415) {
            return "Unsupported media type.";
        }
        if (responseCode == 422) {
            return "Video is longer than allowed.";
        }
        if (responseCode == 500) {
            return "Unexpected error occurred.";
        }
        return "error";
    }

    public enum MediaType {
        JPEG("Screencap-Type", "image/jpeg"),
        PNG("Screencap-Type", "image/png"),
        WEBM("Screencap-Type", "video/webm", "Content-Type", "video/webm"),
        JSON("Content-Type", "application/json");

        private final String[] headers;

        MediaType(String... headers) {
            this.headers = headers;
        }

        public void apply(HttpURLConnection conn) {
            for (int i = 0; i < headers.length; i += 2) {
                conn.setRequestProperty(headers[i], headers[i + 1]);
            }
        }
    }
}
