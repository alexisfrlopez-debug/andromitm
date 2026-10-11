package io.andromitm.app;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/** Liste des hôtes découverts avec sélection des cibles ARP. */
public class HostsAdapter extends RecyclerView.Adapter<HostsAdapter.VH> {
    private final List<Host> items = new ArrayList<>();
    private final Set<String> selected;

    public HostsAdapter(Set<String> selected) { this.selected = selected; }

    public void replace(List<Host> hosts) {
        items.clear();
        items.addAll(hosts);
        notifyDataSetChanged();
    }

    public List<Host> items() { return items; }

    public void persistTo(Prefs prefs) {
        prefs.setTargets(new java.util.HashSet<>(selected));
    }

    @NonNull @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_host, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int pos) {
        Host host = items.get(pos);
        h.title.setText(host.label());
        String sub = host.ip + (host.mac.isEmpty() ? "" : "  ·  " + host.mac);
        h.subtitle.setText(sub);
        h.badge.setVisibility(host.android ? View.VISIBLE : View.GONE);
        h.badge.setText(host.vendor != null && host.android ? host.vendor : "Android");
        h.check.setOnCheckedChangeListener(null);
        h.check.setChecked(selected.contains(host.ip));
        h.check.setOnCheckedChangeListener((b, checked) -> {
            if (checked) selected.add(host.ip); else selected.remove(host.ip);
        });
        h.itemView.setOnClickListener(v -> h.check.toggle());
    }

    @Override
    public int getItemCount() { return items.size(); }

    static class VH extends RecyclerView.ViewHolder {
        final TextView title, subtitle, badge;
        final CheckBox check;
        VH(View v) {
            super(v);
            title = v.findViewById(R.id.host_title);
            subtitle = v.findViewById(R.id.host_subtitle);
            badge = v.findViewById(R.id.host_badge);
            check = v.findViewById(R.id.host_check);
        }
    }
}
