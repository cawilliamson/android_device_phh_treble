package com.restlessos.partner.wallpapers;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

/** Grid of cards with a preview, a title and a subtitle. Used by both screens. */
public class CardAdapter extends RecyclerView.Adapter<CardAdapter.Holder> {

    public static class Card {
        public final Manifest.Item preview;
        public final String title;
        public final String subtitle;

        public Card(Manifest.Item preview, String title, String subtitle) {
            this.preview = preview;
            this.title = title;
            this.subtitle = subtitle;
        }
    }

    public interface OnCardClick {
        void onClick(int position);
    }

    static class Holder extends RecyclerView.ViewHolder {
        final ImageView image;
        final TextView title;
        final TextView subtitle;

        Holder(View view) {
            super(view);
            image = view.findViewById(R.id.image);
            title = view.findViewById(R.id.title);
            subtitle = view.findViewById(R.id.subtitle);
        }
    }

    private final PreviewLoader previews;
    private final OnCardClick onClick;
    private List<Card> cards = new ArrayList<>();

    public CardAdapter(PreviewLoader previews, OnCardClick onClick) {
        this.previews = previews;
        this.onClick = onClick;
    }

    public void setCards(List<Card> cards) {
        this.cards = cards;
        notifyDataSetChanged();
    }

    @Override
    public Holder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_wallpaper, parent, false);
        return new Holder(view);
    }

    @Override
    public void onBindViewHolder(Holder holder, int position) {
        Card card = cards.get(position);
        holder.title.setText(card.title);
        holder.subtitle.setText(card.subtitle);
        previews.load(card.preview, holder.image);
        holder.itemView.setOnClickListener(view -> onClick.onClick(holder.getBindingAdapterPosition()));
    }

    @Override
    public int getItemCount() {
        return cards.size();
    }
}
