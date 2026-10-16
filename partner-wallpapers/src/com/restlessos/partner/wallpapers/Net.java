package com.restlessos.partner.wallpapers;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkCapabilities;
import android.net.Uri;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class Net {

    public static final String DIST_URL =
            "https://raw.githubusercontent.com/Ziednaga/treble_restlessos_wallpapers_stuff/dist/";

    public static boolean isOnline(Context context) {
        ConnectivityManager connectivity = context.getSystemService(ConnectivityManager.class);
        NetworkCapabilities capabilities =
                connectivity.getNetworkCapabilities(connectivity.getActiveNetwork());
        return capabilities != null
                && capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED);
    }

    public static String manifestUrl() {
        return DIST_URL + "manifest.json";
    }

    public static String previewUrl(Manifest.Item item) {
        return DIST_URL + item.preview;
    }

    public static String fullUrl(Manifest manifest, Manifest.Item item) {
        // encode each folder name on its own so the slashes stay
        StringBuilder url = new StringBuilder(manifest.baseUrl);
        String[] parts = item.path.split("/");
        for (int i = 0; i < parts.length; i++) {
            if (i > 0) {
                url.append('/');
            }
            url.append(Uri.encode(parts[i]));
        }
        return url.toString();
    }

    public interface Progress {
        void update(long done, long total);
    }

    public static void download(String url, File file) throws IOException {
        download(url, file, null);
    }

    /** Downloads url into file. The file is only replaced once the download is complete. */
    public static void download(String url, File file, Progress progress) throws IOException {
        File partial = new File(file.getPath() + ".part");
        HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
        connection.setConnectTimeout(15000);
        connection.setReadTimeout(30000);
        try {
            if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
                throw new IOException("HTTP " + connection.getResponseCode() + " for " + url);
            }
            try (InputStream in = connection.getInputStream();
                 OutputStream out = new FileOutputStream(partial)) {
                copy(in, out, connection.getContentLengthLong(), progress);
            }
        } finally {
            connection.disconnect();
        }
        if (!partial.renameTo(file)) {
            throw new IOException("could not rename " + partial);
        }
    }

    public static String sha256(File file) throws IOException {
        MessageDigest digest;
        try {
            digest = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            throw new IOException(e);
        }
        try (InputStream in = new FileInputStream(file)) {
            byte[] buffer = new byte[64 * 1024];
            int count;
            while ((count = in.read(buffer)) != -1) {
                digest.update(buffer, 0, count);
            }
        }
        StringBuilder hex = new StringBuilder();
        for (byte b : digest.digest()) {
            hex.append(String.format("%02x", b));
        }
        return hex.toString();
    }

    private static void copy(InputStream in, OutputStream out, long total, Progress progress)
            throws IOException {
        byte[] buffer = new byte[64 * 1024];
        long done = 0;
        int count;
        while ((count = in.read(buffer)) != -1) {
            out.write(buffer, 0, count);
            done += count;
            if (progress != null) {
                progress.update(done, total);
            }
        }
    }
}
