package com.restlessos.partner.wallpapers;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.Looper;
import android.util.LruCache;
import android.widget.ImageView;

import java.io.File;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Loads previews into image views. Previews are kept on disk in the cache
 * folder, so each one is downloaded only once.
 */
public class PreviewLoader {

    private final File folder;
    private final ExecutorService executor = Executors.newFixedThreadPool(4);
    private final Handler mainThread = new Handler(Looper.getMainLooper());
    private final LruCache<String, Bitmap> memory = new LruCache<>(32 * 1024 * 1024) {
        @Override
        protected int sizeOf(String key, Bitmap bitmap) {
            return bitmap.getByteCount();
        }
    };

    public PreviewLoader(Context context) {
        folder = new File(context.getCacheDir(), "previews");
        folder.mkdirs();
    }

    public void load(Manifest.Item item, ImageView view) {
        String key = item.preview;
        view.setTag(key);

        Bitmap cached = memory.get(key);
        if (cached != null) {
            view.setImageBitmap(cached);
            return;
        }

        view.setImageDrawable(null);
        executor.execute(() -> {
            Bitmap bitmap = readOrDownload(item);
            if (bitmap == null) {
                return;
            }
            memory.put(key, bitmap);
            mainThread.post(() -> {
                // the view may have been reused for another item meanwhile
                if (key.equals(view.getTag())) {
                    view.setImageBitmap(bitmap);
                }
            });
        });
    }

    public void shutdown() {
        executor.shutdownNow();
    }

    private Bitmap readOrDownload(Manifest.Item item) {
        File file = new File(folder, new File(item.preview).getName());
        try {
            if (!file.exists()) {
                Net.download(Net.previewUrl(item), file);
            }
        } catch (Exception e) {
            return null;
        }
        return BitmapFactory.decodeFile(file.getPath());
    }
}
