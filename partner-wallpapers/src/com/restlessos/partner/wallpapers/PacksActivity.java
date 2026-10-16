package com.restlessos.partner.wallpapers;

import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;

import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

/** First screen: every wallpaper pack with its cover. */
public class PacksActivity extends AppCompatActivity {

    private PreviewLoader previews;
    private CardAdapter adapter;
    private View loading;
    private View offline;
    private Manifest manifest;

    // reloads the list once the connection is back
    private final ConnectivityManager.NetworkCallback networkCallback =
            new ConnectivityManager.NetworkCallback() {
                @Override
                public void onCapabilitiesChanged(Network network, NetworkCapabilities capabilities) {
                    if (capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)) {
                        runOnUiThread(() -> {
                            if (offline.getVisibility() == View.VISIBLE) {
                                load();
                            }
                        });
                    }
                }
            };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_packs);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setTitle(R.string.app_name);
        setSupportActionBar(toolbar);

        loading = findViewById(R.id.loading);
        offline = findViewById(R.id.offline);
        findViewById(R.id.retry).setOnClickListener(view -> load());

        previews = new PreviewLoader(this);
        adapter = new CardAdapter(previews, this::openPack);

        RecyclerView grid = findViewById(R.id.grid);
        grid.setLayoutManager(new GridLayoutManager(this, 2));
        grid.setAdapter(adapter);

        getSystemService(ConnectivityManager.class).registerDefaultNetworkCallback(networkCallback);
        load();
    }

    @Override
    protected void onDestroy() {
        getSystemService(ConnectivityManager.class).unregisterNetworkCallback(networkCallback);
        previews.shutdown();
        super.onDestroy();
    }

    /** The last downloaded manifest, also read by PackActivity. */
    public static File manifestFile(Context context) {
        return new File(context.getFilesDir(), "manifest.json");
    }

    public static Manifest readManifest(Context context) throws Exception {
        return Manifest.parse(new String(Files.readAllBytes(manifestFile(context).toPath())));
    }

    private void load() {
        offline.setVisibility(View.GONE);
        if (!Net.isOnline(this)) {
            show(null);
            return;
        }
        loading.setVisibility(View.VISIBLE);
        new Thread(this::loadManifest).start();
    }

    private void loadManifest() {
        Manifest result = null;
        try {
            Net.download(Net.manifestUrl(), manifestFile(this));
            result = readManifest(this);
        } catch (Exception e) {
            // shown as the offline screen
        }

        Manifest loaded = result;
        runOnUiThread(() -> show(loaded));
    }

    private void show(Manifest loaded) {
        if (isDestroyed()) {
            return;
        }
        loading.setVisibility(View.GONE);
        if (loaded == null) {
            offline.setVisibility(View.VISIBLE);
            return;
        }
        manifest = loaded;

        List<CardAdapter.Card> cards = new ArrayList<>();
        for (Manifest.Pack pack : manifest.packs) {
            Manifest.Item cover = pack.variants.get(0).items.get(0);
            cards.add(new CardAdapter.Card(cover, pack.name, pack.author));
        }
        adapter.setCards(cards);
    }

    private void openPack(int position) {
        Intent intent = new Intent(this, PackActivity.class);
        intent.putExtra(PackActivity.EXTRA_PACK_ID, manifest.packs.get(position).id);
        startActivity(intent);
    }
}
