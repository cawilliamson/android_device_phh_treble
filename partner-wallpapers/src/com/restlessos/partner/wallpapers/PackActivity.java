package com.restlessos.partner.wallpapers;

import android.app.WallpaperManager;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.android.material.snackbar.Snackbar;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/** Second screen: the wallpapers of one pack, with a variant selector on top. */
public class PackActivity extends AppCompatActivity {

    public static final String EXTRA_PACK_ID = "pack_id";

    private static final String PICKER_PACKAGE = "com.android.wallpaper";

    private Manifest manifest;
    private Manifest.Pack pack;
    private Manifest.Variant variant;

    private PreviewLoader previews;
    private CardAdapter adapter;
    private LinearProgressIndicator progress;
    private boolean downloading;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        try {
            manifest = PacksActivity.readManifest(this);
        } catch (Exception e) {
            finish();
            return;
        }
        pack = manifest.findPack(getIntent().getStringExtra(EXTRA_PACK_ID));
        if (pack == null) {
            finish();
            return;
        }
        variant = pack.variants.get(0);

        setContentView(R.layout.activity_pack);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setTitle(pack.name);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        toolbar.setNavigationOnClickListener(view -> finish());

        progress = findViewById(R.id.progress);
        previews = new PreviewLoader(this);
        adapter = new CardAdapter(previews, position -> apply(variant.items.get(position)));

        RecyclerView grid = findViewById(R.id.grid);
        grid.setLayoutManager(new GridLayoutManager(this, 3));
        grid.setAdapter(adapter);

        setUpVariants();
        showVariant();
    }

    @Override
    protected void onDestroy() {
        if (previews != null) {
            previews.shutdown();
        }
        super.onDestroy();
    }

    private void setUpVariants() {
        if (pack.variants.size() < 2) {
            findViewById(R.id.variants_scroll).setVisibility(View.GONE);
            return;
        }
        ChipGroup group = findViewById(R.id.variants);
        for (Manifest.Variant v : pack.variants) {
            Chip chip = (Chip) getLayoutInflater().inflate(R.layout.chip_variant, group, false);
            chip.setId(View.generateViewId());
            chip.setText(v.name.isEmpty() ? getString(R.string.variant_default) : v.name);
            chip.setOnClickListener(view -> {
                variant = v;
                showVariant();
            });
            group.addView(chip);
            if (v == variant) {
                chip.setChecked(true);
            }
        }
    }

    private void showVariant() {
        List<CardAdapter.Card> cards = new ArrayList<>();
        for (Manifest.Item item : variant.items) {
            String size = getString(R.string.size_mb, item.size / (1024f * 1024f));
            cards.add(new CardAdapter.Card(item, item.name, size));
        }
        adapter.setCards(cards);
    }

    private void apply(Manifest.Item item) {
        if (downloading) {
            return;
        }
        if (!Net.isOnline(this)) {
            showMessage(R.string.offline_title);
            return;
        }
        downloading = true;
        progress.setProgressCompat(0, false);
        progress.setVisibility(View.VISIBLE);

        new Thread(() -> {
            File file = null;
            try {
                file = download(item);
            } catch (Exception e) {
                // shown as a message below
            }
            File result = file;
            runOnUiThread(() -> {
                downloading = false;
                progress.setVisibility(View.INVISIBLE);
                if (result == null) {
                    showMessage(Net.isOnline(this) ? R.string.download_failed : R.string.offline_title);
                } else if (!isDestroyed()) {
                    openPicker(WallpaperProvider.uriFor(result));
                }
            });
        }).start();
    }

    private void showMessage(int text) {
        Snackbar.make(findViewById(R.id.grid), text, Snackbar.LENGTH_LONG).show();
    }

    /** Downloads the full image, checks its sha256 and returns it. */
    private File download(Manifest.Item item) throws Exception {
        File folder = WallpaperProvider.folder(this);
        folder.mkdirs();

        String extension = item.path.substring(item.path.lastIndexOf('.'));
        File file = new File(folder, item.sha256 + extension);
        if (file.exists() && Net.sha256(file).equals(item.sha256)) {
            return file;
        }

        // the picker keeps its own copy, so older downloads are not needed
        for (File old : folder.listFiles()) {
            old.delete();
        }

        Net.download(Net.fullUrl(manifest, item), file, (done, total) -> {
            int percent = (int) (done * 100 / item.size);
            runOnUiThread(() -> progress.setProgressCompat(percent, true));
        });
        if (!Net.sha256(file).equals(item.sha256)) {
            file.delete();
            throw new Exception("sha256 mismatch for " + item.path);
        }
        return file;
    }

    private void openPicker(Uri uri) {
        Intent intent = new Intent(WallpaperManager.ACTION_CROP_AND_SET_WALLPAPER);
        intent.setDataAndType(uri, getContentResolver().getType(uri));
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        intent.setPackage(PICKER_PACKAGE);
        try {
            startActivity(intent);
        } catch (ActivityNotFoundException e) {
            // no ThemePicker, let the framework pick a cropper
            startActivity(WallpaperManager.getInstance(this).getCropAndSetWallpaperIntent(uri)
                    .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION));
        }
    }
}
