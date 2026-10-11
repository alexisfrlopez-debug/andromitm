package io.andromitm.app;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/** Journal d'événements en direct (colonne inversée : le plus récent en haut). */
public class EventsAdapter extends RecyclerView.Adapter<EventsAdapter.VH> {
    public static class Entry {
        public final long ts;
        public final String level, text;
        public Entry(String level, String text) { this.ts = System.currentTimeMillis(); this.level = level; this.text = text; }
    }

    private final List<Entry> items = new ArrayList<>();
    private final SimpleDateFormat fmt = new SimpleDateFormat("HH:mm:ss", Locale.getDefault());
    private static final int MAX = 400;

    public synchronized void add(String level, String text) {
        items.add(0, new Entry(level, text));
        if (items.size() > MAX) items.remove(items.size() - 1);
        notifyDataSetChanged();
    }

    public synchronized void clear() { items.clear(); notifyDataSetChanged(); }

    @NonNull @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new VH(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_event, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int pos) {
        Entry e = items.get(pos);
        h.time.setText(fmt.format(new Date(e.ts)));
        h.text.setText(e.text);
        int color = h.itemView.getContext().getColor(
            "error".equals(e.level) ? R.color.log_error :
            "warn".equals(e.level) ? R.color.log_warn : R.color.log_info);
        h.text.setTextColor(color);
    }

    @Override
    public int getItemCount() { return items.size(); }

    static class VH extends RecyclerView.ViewHolder {
        final TextView time, text;
        VH(View v) {
            super(v);
            time = v.findViewById(R.id.ev_time);
            text = v.findViewById(R.id.ev_text);
        }
    }
}
