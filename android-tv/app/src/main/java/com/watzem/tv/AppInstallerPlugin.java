package com.watzem.tv;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import androidx.core.content.FileProvider;
import com.getcapacitor.JSObject;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.annotation.CapacitorPlugin;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

@CapacitorPlugin(name = "AppInstaller")
public class AppInstallerPlugin extends Plugin {

    @PluginMethod
    public void startDownload(PluginCall call) {
        String downloadUrl = call.getString("url");
        if (downloadUrl == null || downloadUrl.isEmpty()) {
            call.reject("URL is required");
            return;
        }

        call.resolve();

        new Thread(() -> {
            HttpURLConnection connection = null;
            InputStream input = null;
            FileOutputStream output = null;
            try {
                Context context = getContext();
                String currentUrl = downloadUrl;

                // Follow redirects manually up to 5 times (for GitHub Releases -> Azure/AWS CDN)
                int redirects = 0;
                while (redirects < 5) {
                    URL u = new URL(currentUrl);
                    connection = (HttpURLConnection) u.openConnection();
                    connection.setInstanceFollowRedirects(true);
                    connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Android; WATZEM TV App)");
                    connection.setConnectTimeout(15000);
                    connection.setReadTimeout(30000);
                    connection.connect();

                    int code = connection.getResponseCode();
                    if (code == HttpURLConnection.HTTP_MOVED_TEMP || 
                        code == HttpURLConnection.HTTP_MOVED_PERM || 
                        code == HttpURLConnection.HTTP_SEE_OTHER ||
                        code == 307 || code == 308) {
                        String loc = connection.getHeaderField("Location");
                        if (loc != null) {
                            currentUrl = loc;
                            redirects++;
                            connection.disconnect();
                            continue;
                        }
                    }
                    break;
                }

                long fileLength = connection.getContentLengthLong();
                File updatesDir = new File(context.getExternalFilesDir(null), "updates");
                if (!updatesDir.exists()) {
                    updatesDir.mkdirs();
                }

                File apkFile = new File(updatesDir, "tv-update.apk");
                if (apkFile.exists()) {
                    apkFile.delete();
                }

                input = connection.getInputStream();
                output = new FileOutputStream(apkFile);

                byte[] buffer = new byte[16384];
                long total = 0;
                int count;
                long lastEmit = 0;

                while ((count = input.read(buffer)) != -1) {
                    total += count;
                    output.write(buffer, 0, count);

                    long now = System.currentTimeMillis();
                    if (now - lastEmit > 150 || (fileLength > 0 && total == fileLength)) {
                        lastEmit = now;
                        int progress = fileLength > 0 ? (int) (total * 100 / fileLength) : 0;
                        JSObject p = new JSObject();
                        p.put("progress", progress);
                        p.put("bytesDownloaded", total);
                        p.put("totalBytes", fileLength);
                        notifyListeners("downloadProgress", p);
                    }
                }

                output.flush();

                JSObject comp = new JSObject();
                comp.put("success", true);
                comp.put("filePath", apkFile.getAbsolutePath());
                notifyListeners("downloadComplete", comp);

                // Auto launch native package installer
                installApk(apkFile);

            } catch (Exception e) {
                JSObject err = new JSObject();
                err.put("error", e.getMessage() != null ? e.getMessage() : "Download failed");
                notifyListeners("downloadError", err);
            } finally {
                try {
                    if (output != null) output.close();
                    if (input != null) input.close();
                    if (connection != null) connection.disconnect();
                } catch (Exception ignored) {}
            }
        }).start();
    }

    @PluginMethod
    public void installApkFile(PluginCall call) {
        String path = call.getString("path");
        File file = path != null ? new File(path) : new File(getContext().getExternalFilesDir(null), "updates/tv-update.apk");
        if (file.exists()) {
            installApk(file);
            call.resolve();
        } else {
            call.reject("Update APK file not found");
        }
    }

    private void installApk(File apkFile) {
        getActivity().runOnUiThread(() -> {
            try {
                Context context = getContext();

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    if (!context.getPackageManager().canRequestPackageInstalls()) {
                        Intent permissionIntent = new Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES);
                        permissionIntent.setData(Uri.parse("package:" + context.getPackageName()));
                        permissionIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        context.startActivity(permissionIntent);
                    }
                }

                Uri apkUri = FileProvider.getUriForFile(
                    context,
                    context.getPackageName() + ".fileprovider",
                    apkFile
                );

                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setDataAndType(apkUri, "application/vnd.android.package-archive");
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(intent);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }
}
