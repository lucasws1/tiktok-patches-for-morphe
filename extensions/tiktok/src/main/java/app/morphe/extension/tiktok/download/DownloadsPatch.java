/*
 * Forked from:
 * https://github.com/ReVanced/revanced-patches/blob/377d4e15016296b45d809697f7f69bce74badd3a/extensions/tiktok/src/main/java/app/revanced/extension/tiktok/download/DownloadsPatch.java
 */

package app.morphe.extension.tiktok.download;

import app.morphe.extension.shared.Logger;
import app.morphe.extension.shared.settings.BaseSettings;
import app.morphe.extension.tiktok.settings.Settings;
import com.ss.android.ugc.aweme.base.model.UrlModel;
import com.ss.android.ugc.aweme.feed.model.Video;

import java.util.List;

@SuppressWarnings("unused")
public class DownloadsPatch {
    private static volatile String lastLoggedPath;
    private static volatile Boolean lastLoggedRemoveWatermark;

    public static String getDownloadPath() {
        String path = Settings.DOWNLOAD_PATH.get();
        if (BaseSettings.DEBUG.get() && (lastLoggedPath == null || !lastLoggedPath.equals(path))) {
            lastLoggedPath = path;
            Logger.printInfo(() -> "[Morphe Downloads] download_path=\"" + path + "\"");
        }
        return path;
    }

    public static boolean shouldRemoveWatermark() {
        boolean removeWatermark = Settings.DOWNLOAD_WATERMARK.get();
        if (BaseSettings.DEBUG.get() && (lastLoggedRemoveWatermark == null || lastLoggedRemoveWatermark != removeWatermark)) {
            lastLoggedRemoveWatermark = removeWatermark;
            Logger.printInfo(() -> "[Morphe Downloads] remove_watermark=" + removeWatermark);
        }
        return removeWatermark;
    }

    public static void patchVideoObject(Video video) {
        if (video == null) return;

        try {
            UrlModel clean = video.downloadNoWatermarkAddr;

            // TikTok may remove the non-watermark URL for some videos.
            if (!isUsableDownloadModel(clean)) {
                UrlModel replacement = null;
                String replacementName = null;

                if (isUsableDownloadModel(video.h264PlayAddr)) {
                    replacement = video.h264PlayAddr;
                    replacementName = "h264PlayAddr";
                } else if (isUsableDownloadModel(video.playAddr)) {
                    replacement = video.playAddr;
                    replacementName = "playAddr";
                }

                if (replacement != null) {
                    video.downloadNoWatermarkAddr = replacement;
                    if (BaseSettings.DEBUG.get()) {
                        String fallbackSource = replacementName;
                        String cleanSummary = describeUrlModel(clean);
                        String replacementSummary = describeUrlModel(replacement);
                        Logger.printInfo(() -> "[Morphe Downloads] replaced unusable no-watermark model"
                                + " clean=" + cleanSummary
                                + " source=" + fallbackSource
                                + " replacement=" + replacementSummary);
                    }
                } else if (BaseSettings.DEBUG.get()) {
                    String cleanSummary = describeUrlModel(clean);
                    String h264Summary = describeUrlModel(video.h264PlayAddr);
                    String playSummary = describeUrlModel(video.playAddr);
                    Logger.printInfo(() -> "[Morphe Downloads] no usable download fallback"
                            + " clean=" + cleanSummary
                            + " h264=" + h264Summary
                            + " play=" + playSummary);
                }
            }
        } catch (Throwable ex) {
            if (BaseSettings.DEBUG.get()) {
                Logger.printException(() -> "[Morphe Downloads] patchVideoObject failure", ex);
            }
        }
    }

    private static boolean isUsableDownloadModel(UrlModel model) {
        if (model == null) {
            return false;
        }

        String uri = getUriSafe(model);
        if (uri == null || uri.trim().isEmpty() || "null".equalsIgnoreCase(uri.trim())) {
            return false;
        }

        return hasUsableUrl(model);
    }

    private static boolean hasUsableUrl(UrlModel model) {
        List<String> urls = getUrlListSafe(model);
        if (urls == null || urls.isEmpty()) {
            return false;
        }

        for (String url : urls) {
            if (url != null && !url.trim().isEmpty() && !"null".equalsIgnoreCase(url.trim())) {
                return true;
            }
        }

        return false;
    }

    private static String describeUrlModel(UrlModel model) {
        if (model == null) {
            return "null";
        }

        List<String> urls = getUrlListSafe(model);
        int urlCount = urls == null ? -1 : urls.size();
        return "{class=" + model.getClass().getName()
                + ",uri=" + getUriSafe(model)
                + ",urlKey=" + getUrlKeySafe(model)
                + ",size=" + getSizeSafe(model)
                + ",urlCount=" + urlCount
                + ",firstUrl=" + redactUrl(firstUrl(urls)) + "}";
    }

    private static List<String> getUrlListSafe(UrlModel model) {
        try {
            return model.getUrlList();
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static String getUriSafe(UrlModel model) {
        try {
            return model.getUri();
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static String getUrlKeySafe(UrlModel model) {
        try {
            return model.getUrlKey();
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static long getSizeSafe(UrlModel model) {
        try {
            return model.getSize();
        } catch (Throwable ignored) {
            return -1;
        }
    }

    private static String firstUrl(List<String> urls) {
        if (urls == null || urls.isEmpty()) {
            return null;
        }

        return urls.get(0);
    }

    private static String redactUrl(String url) {
        if (url == null) {
            return null;
        }

        int queryIndex = url.indexOf('?');
        String withoutQuery = queryIndex >= 0 ? url.substring(0, queryIndex) : url;
        return withoutQuery.length() <= 96 ? withoutQuery : withoutQuery.substring(0, 96) + "...";
    }
}
