package com.restlessos.partner.wallpapers;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * manifest.json from the dist branch of the wallpapers repo,
 * written by .github/scripts/build_dist.py.
 */
public class Manifest {

    public static class Item {
        public String name;
        public String path;
        public String sha256;
        public String preview;
        public long size;
    }

    public static class Variant {
        public String name;
        public List<Item> items = new ArrayList<>();
    }

    public static class Pack {
        public String id;
        public String name;
        public String author;
        public List<Variant> variants = new ArrayList<>();
    }

    public String baseUrl;
    public List<Pack> packs = new ArrayList<>();

    public static Manifest parse(String json) throws JSONException {
        JSONObject root = new JSONObject(json);
        Manifest manifest = new Manifest();
        manifest.baseUrl = root.getString("base_url");

        JSONArray packs = root.getJSONArray("packs");
        for (int i = 0; i < packs.length(); i++) {
            JSONObject packJson = packs.getJSONObject(i);
            Pack pack = new Pack();
            pack.id = packJson.getString("id");
            pack.name = packJson.getString("name");
            pack.author = packJson.getString("author");

            JSONArray variants = packJson.getJSONArray("variants");
            for (int j = 0; j < variants.length(); j++) {
                JSONObject variantJson = variants.getJSONObject(j);
                Variant variant = new Variant();
                variant.name = variantJson.getString("name");

                JSONArray items = variantJson.getJSONArray("items");
                for (int k = 0; k < items.length(); k++) {
                    JSONObject itemJson = items.getJSONObject(k);
                    Item item = new Item();
                    item.name = itemJson.getString("name");
                    item.path = itemJson.getString("path");
                    item.sha256 = itemJson.getString("sha256");
                    item.preview = itemJson.getString("preview");
                    item.size = itemJson.getLong("size");
                    variant.items.add(item);
                }
                pack.variants.add(variant);
            }
            manifest.packs.add(pack);
        }
        return manifest;
    }

    public Pack findPack(String id) {
        for (Pack pack : packs) {
            if (pack.id.equals(id)) {
                return pack;
            }
        }
        return null;
    }
}
