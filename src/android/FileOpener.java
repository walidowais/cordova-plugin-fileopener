package fr.smile.cordova.fileopener;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.lang.String;
import java.net.URLDecoder;
import java.util.ArrayList;

import android.annotation.TargetApi;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CallbackContext;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;

import android.os.Environment;
import android.content.pm.PackageManager;
import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.IntentFilter;
import android.database.Cursor;

import android.content.ActivityNotFoundException;
import android.support.v4.content.FileProvider;
import android.util.Log;
import android.webkit.CookieManager;

@TargetApi(9)
public class FileOpener extends CordovaPlugin {
    private static final String FILE_OPENER = "FileOpener";
    private static final ArrayList<String> SUPPORTED_MIME_TYPES;

    static {
        SUPPORTED_MIME_TYPES = new ArrayList<String>();
        SUPPORTED_MIME_TYPES.add("image/x.djvu");
        SUPPORTED_MIME_TYPES.add("application/pdf");
        SUPPORTED_MIME_TYPES.add("application/msword");
        SUPPORTED_MIME_TYPES.add("application/msword");
        SUPPORTED_MIME_TYPES.add("application/vnd.ms-powerpoint");
        SUPPORTED_MIME_TYPES.add("application/vnd.ms-powerpoint");
        SUPPORTED_MIME_TYPES.add("application/vnd.ms-excel");
        SUPPORTED_MIME_TYPES.add("audio/x-wav");
        SUPPORTED_MIME_TYPES.add("audio/mpeg3");
        SUPPORTED_MIME_TYPES.add("image/gif");
        SUPPORTED_MIME_TYPES.add("image/jpeg");
        SUPPORTED_MIME_TYPES.add("image/jpeg");
        SUPPORTED_MIME_TYPES.add("image/png");
        SUPPORTED_MIME_TYPES.add("text/plain");
        SUPPORTED_MIME_TYPES.add("image/tiff");
        SUPPORTED_MIME_TYPES.add("image/tiff");
        SUPPORTED_MIME_TYPES.add("video/*");
        SUPPORTED_MIME_TYPES.add("video/*");
        SUPPORTED_MIME_TYPES.add("video/*");
        SUPPORTED_MIME_TYPES.add("video/*");
        SUPPORTED_MIME_TYPES.add("video/*");
        SUPPORTED_MIME_TYPES.add("video/*");
        SUPPORTED_MIME_TYPES.add("application/vnd.oasis.opendocument.spreadsheet");
        SUPPORTED_MIME_TYPES.add("application/vnd.oasis.opendocument.text");
        SUPPORTED_MIME_TYPES.add("application/vnd.ms-powerpoint");
        SUPPORTED_MIME_TYPES.add("application/vnd.openxmlformats-officedocument.presentationml.presentation");
        SUPPORTED_MIME_TYPES.add("application/vnd.android.package-archive");
        SUPPORTED_MIME_TYPES.add("application/x-shockwave-flash");
        SUPPORTED_MIME_TYPES.add("application/zip");
        SUPPORTED_MIME_TYPES.add("application/x-rar-compressed");
    }

    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {
        Context context = cordova.getActivity().getApplicationContext();

        String mimeType = getMimeType(args, callbackContext);

        if ("canOpenFile".equals(action)) {
            if (mimeType != null) {
                JSONObject obj = new JSONObject();
                obj.put("mimeType", mimeType);
                obj.put("canBeOpen", this.canOpenFile(mimeType, context));
                callbackContext.success(obj);
            }
            return true;
        } else if ("openFile".equals(action)) {
            if (mimeType != null) {
                String fileURL = args.getString(0);
                if (fileURL.startsWith("file://")) {
                    // Local file uri (case of an already downloaded file)
                    Log.d(FILE_OPENER, "Opening file from local URI as it begins with file://");
                    File file = new File(fileURL.replaceFirst("^file:\\/\\/", ""));
                    Log.d(FILE_OPENER, "Local path: " + fileURL);
                    this.openFile(file, mimeType, context, callbackContext);
                } else {
                    try {
                        this.downloadAndOpenFile(context, fileURL, mimeType, callbackContext);
                    } catch (UnsupportedEncodingException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
            return true;
        } else {
            return false;
        }
    }

    private String getMimeType(final JSONArray args, CallbackContext callbackContext) throws JSONException {
        JSONObject obj = new JSONObject();
        if (args.length() > 0) {
            String url = args.getString(0);
            String mimeType = args.getString(1);
            if (supportsMimeType(mimeType)) {
                return mimeType;
            } else {
                obj.put("message", "This mime type: " + mimeType + " is not supported by the FileOpener plugin");
                callbackContext.error(obj);
                return null;
            }
        } else {
            obj.put("message", "Parameter is missing");
            callbackContext.error(obj);
            return null;
        }
    }

    private boolean supportsMimeType(String mimeType) {
        return SUPPORTED_MIME_TYPES.contains(mimeType);
    }

    private boolean canOpenFile(String mimeType, Context context) {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        final File tempFile = new File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), "test" + mimeType);
        intent.setDataAndType(Uri.fromFile(tempFile), mimeType);
        return context.getPackageManager().queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY).size() > 0;
    }

    private void openFile(File file, String mimeType, Context context, CallbackContext callbackContext) throws JSONException {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        Uri apkURI = FileProvider.getUriForFile(
                                 context,
                                 context.getApplicationContext()
                                 .getPackageName() + ".provider", file);
        intent.setDataAndType(apkURI, mimeType);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

        JSONObject obj = new JSONObject();

        try {
            context.startActivity(intent);
            obj.put("message", "successfull downloading and openning");
            callbackContext.success(obj);
        } catch (ActivityNotFoundException e) {
            obj.put("message", "Failed to open the file, no reader found");
            obj.put("ActivityNotFoundException", e.getMessage());
            callbackContext.error(obj);
        }
    }

    private void downloadAndOpenFile(final Context context, final String fileUrl, final String mimeType, final CallbackContext callbackContext) throws UnsupportedEncodingException {
        final String filename = URLDecoder.decode(fileUrl.substring(fileUrl.lastIndexOf("/") + 1), "UTF-8");
        final File tempFile = new File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), filename);

        if (tempFile.exists()) {
            try {
                openFile(tempFile, mimeType, context, callbackContext);
            } catch (JSONException e) {
                Log.d(FILE_OPENER, "downloadAndOpenFile", e);
            }
            return;
        }

        DownloadManager.Request request = new DownloadManager.Request(Uri.parse(fileUrl));
        request.setDestinationInExternalFilesDir(context, Environment.DIRECTORY_DOWNLOADS, filename);
        final DownloadManager downloadManager = (DownloadManager) context.getSystemService(Context.DOWNLOAD_SERVICE);
        BroadcastReceiver onComplete = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                context.unregisterReceiver(this);

                long downloadId = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1);
                Cursor cursor = downloadManager.query(new DownloadManager.Query().setFilterById(downloadId));

                if (cursor.moveToFirst()) {
                    int status = cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_STATUS));
                    if (status == DownloadManager.STATUS_SUCCESSFUL) {
                        try {
                            openFile(tempFile, mimeType, context, callbackContext);
                        } catch (JSONException e) {
                            Log.d(FILE_OPENER, "downloadAndOpenFile", e);
                        }
                    } else if (status == DownloadManager.STATUS_FAILED) {
                        manageDownloadStatusFailed(cursor, callbackContext);
                    }
                }
                cursor.close();
            }
        };
        context.registerReceiver(onComplete, new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE));

        CookieManager cookieManager = CookieManager.getInstance();
        String cookieForUrl = cookieManager.getCookie(fileUrl);
        request.addRequestHeader("Cookie", cookieForUrl);

        downloadManager.enqueue(request);
    }

    private void manageDownloadStatusFailed(Cursor cursor, CallbackContext callbackContext) {
        int reason = cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_REASON));
        String failedReason = "";

        switch (reason) {
            case DownloadManager.ERROR_CANNOT_RESUME:
                failedReason = "ERROR_CANNOT_RESUME";
                break;
            case DownloadManager.ERROR_DEVICE_NOT_FOUND:
                failedReason = "ERROR_DEVICE_NOT_FOUND";
                break;
            case DownloadManager.ERROR_FILE_ALREADY_EXISTS:
                failedReason = "ERROR_FILE_ALREADY_EXISTS";
                break;
            case DownloadManager.ERROR_FILE_ERROR:
                failedReason = "ERROR_FILE_ERROR";
                break;
            case DownloadManager.ERROR_HTTP_DATA_ERROR:
                failedReason = "ERROR_HTTP_DATA_ERROR";
                break;
            case DownloadManager.ERROR_INSUFFICIENT_SPACE:
                failedReason = "ERROR_INSUFFICIENT_SPACE";
                break;
            case DownloadManager.ERROR_TOO_MANY_REDIRECTS:
                failedReason = "ERROR_TOO_MANY_REDIRECTS";
                break;
            case DownloadManager.ERROR_UNHANDLED_HTTP_CODE:
                failedReason = "ERROR_UNHANDLED_HTTP_CODE";
                break;
            case DownloadManager.ERROR_UNKNOWN:
                failedReason = "ERROR_UNKNOWN";
                break;
            case 400:
                failedReason = "BAD_REQUEST";
                break;
            case 401:
                failedReason = "UNAUTHORIZED";
                break;
            case 404:
                failedReason = "NOT_FOUND";
                break;
            case 500:
                failedReason = "INTERNAL_SERVER_ERROR";
                break;
        }

        JSONObject obj = new JSONObject();
        try {
            obj.put("message", "Download failed for reason: #" + reason + " " + failedReason);
            callbackContext.error(obj);
        } catch (JSONException e) {
            Log.d(FILE_OPENER, "downloadAndOpenFile", e);
        }
    }
}
