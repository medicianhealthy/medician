package com.robinzon.medicationwizard.ui.settings;

import android.database.Cursor;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RadioButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.robinzon.medicationwizard.R;

import java.util.ArrayList;
import java.util.List;

public class SoundPickerBottomSheet extends BottomSheetDialogFragment {

    private OnSoundSelectedListener listener;
    private String selectedSoundUri;
    private String selectedSoundName;
    private Ringtone lastRingtone;

    public interface OnSoundSelectedListener {
        void onSoundSelected(String name, String uri);
    }

    public void setOnSoundSelectedListener(OnSoundSelectedListener listener) {
        this.listener = listener;
    }

    public void setCurrentSoundUri(String uri) {
        this.selectedSoundUri = uri;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.bottom_sheet_sound_picker, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        RecyclerView recyclerView = view.findViewById(R.id.recycler_sounds);
        
        List<SoundItem> sounds = fetchAvailableSounds();
        
        // Find current name if we only have URI
        for (SoundItem s : sounds) {
            if (s.uri.equals(selectedSoundUri)) {
                selectedSoundName = s.name;
                break;
            }
        }

        recyclerView.setAdapter(new SoundAdapter(sounds));

        view.findViewById(R.id.btn_confirm_sound).setOnClickListener(v -> {
            if (listener != null && selectedSoundUri != null) {
                listener.onSoundSelected(selectedSoundName, selectedSoundUri);
            }
            dismiss();
        });
    }

    private List<SoundItem> fetchAvailableSounds() {
        List<SoundItem> list = new ArrayList<>();
        RingtoneManager manager = new RingtoneManager(requireContext());
        manager.setType(RingtoneManager.TYPE_NOTIFICATION | RingtoneManager.TYPE_ALARM);
        
        Cursor cursor = manager.getCursor();
        while (cursor.moveToNext()) {
            String title = cursor.getString(RingtoneManager.TITLE_COLUMN_INDEX);
            Uri uri = manager.getRingtoneUri(cursor.getPosition());
            list.add(new SoundItem(title, uri.toString()));
        }
        return list;
    }

    private void playPreview(String uriString) {
        if (lastRingtone != null && lastRingtone.isPlaying()) {
            lastRingtone.stop();
        }
        try {
            Uri uri = Uri.parse(uriString);
            lastRingtone = RingtoneManager.getRingtone(requireContext(), uri);
            lastRingtone.play();
        } catch (Exception ignored) {}
    }

    @Override
    public void onPause() {
        super.onPause();
        if (lastRingtone != null) lastRingtone.stop();
    }

    private static class SoundItem {
        final String name;
        final String uri;
        SoundItem(String name, String uri) { this.name = name; this.uri = uri; }
    }

    private class SoundAdapter extends RecyclerView.Adapter<SoundAdapter.ViewHolder> {
        private final List<SoundItem> data;

        SoundAdapter(List<SoundItem> data) { this.data = data; }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_sound_picker, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            SoundItem item = data.get(position);
            holder.name.setText(item.name);
            holder.radioButton.setChecked(item.uri.equals(selectedSoundUri));
            
            holder.itemView.setOnClickListener(v -> {
                selectedSoundUri = item.uri;
                selectedSoundName = item.name;
                playPreview(item.uri);
                notifyDataSetChanged(); // Refresh radio buttons
            });
        }

        @Override
        public int getItemCount() { return data.size(); }

        class ViewHolder extends RecyclerView.ViewHolder {
            final TextView name;
            final RadioButton radioButton;
            ViewHolder(View v) {
                super(v);
                name = v.findViewById(R.id.txt_sound_name);
                radioButton = v.findViewById(R.id.radio_selected);
            }
        }
    }
}