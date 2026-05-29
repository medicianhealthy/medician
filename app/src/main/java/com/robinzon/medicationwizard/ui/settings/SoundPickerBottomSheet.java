package com.robinzon.medicationwizard.ui.settings;

import android.app.Dialog;
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

import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.robinzon.medicationwizard.R;

import java.util.ArrayList;
import java.util.List;

/**
 * A custom Material 3 BottomSheet for selecting a system reminder sound.
 * <p>
 * This dialog queries the Android {@link RingtoneManager} to list all available 
 * notification and alarm sounds on the device. It provides an interactive 
 * list with radio-button selection and automatic audio previews when a 
 * sound is tapped.
 * </p>
 * <p>
 * Performance: Optimized for quick expansion and centered display on larger screens.
 * </p>
 */
public class SoundPickerBottomSheet extends BottomSheetDialogFragment {

    /**
     * Standard lifecycle method to define the dialog's visual style.
     */
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setStyle(STYLE_NORMAL, R.style.CustomBottomSheetDialog);
    }

    /**
     * Standard lifecycle method to configure the dialog window.
     * <p>
     * Performance: Forces immediate full expansion and adheres to Material 3 standard anchoring.
     * </p>
     */
    @Override
    public void onStart() {
        super.onStart();
        Dialog dialog = getDialog();
        if (dialog != null) {
            View bottomSheet = dialog.findViewById(com.google.android.material.R.id.design_bottom_sheet);
            if (bottomSheet != null) {
                BottomSheetBehavior<View> behavior = BottomSheetBehavior.from(bottomSheet);
                behavior.setState(BottomSheetBehavior.STATE_EXPANDED);
                behavior.setSkipCollapsed(true);
            }
        }
    }

    private OnSoundSelectedListener listener;
    private String selectedSoundUri;
    private String selectedSoundName;
    private Ringtone lastRingtone;

    /**
     * Listener interface to notify the caller when a sound selection is confirmed.
     */
    public interface OnSoundSelectedListener {
        /**
         * @param name The human-readable name of the sound.
         * @param uri  The system URI string of the sound file.
         */
        void onSoundSelected(String name, String uri);
    }

    /**
     * Sets the selection listener.
     */
    public void setOnSoundSelectedListener(OnSoundSelectedListener listener) {
        this.listener = listener;
    }

    /**
     * Pre-selects a sound in the list by its URI.
     */
    public void setCurrentSoundUri(String uri) {
        this.selectedSoundUri = uri;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.bottom_sheet_sound_picker, container, false);
    }

    /**
     * Initializes the sound list and confirm button.
     * Maps the initial URI to a name for better UI display.
     */
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        RecyclerView recyclerView = view.findViewById(R.id.recycler_sounds);
        
        List<SoundItem> sounds = fetchAvailableSounds();
        
        // Match the current sound URI to its title
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

    /**
     * Low-level query to fetch all system-available notification and alarm sounds.
     *
     * @return A list of {@link SoundItem} objects.
     */
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

    /**
     * Plays a short audio sample of the selected sound.
     * Automatically stops any previous sample to avoid overlapping audio.
     *
     * @param uriString The URI of the sound to preview.
     */
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

    /**
     * Ensures audio preview stops if the user navigates away from the app.
     */
    @Override
    public void onPause() {
        super.onPause();
        if (lastRingtone != null) lastRingtone.stop();
    }

    /** Simple POJO for sound data. */
    private static class SoundItem {
        final String name;
        final String uri;
        SoundItem(String name, String uri) { this.name = name; this.uri = uri; }
    }

    /**
     * Inner RecyclerView Adapter for the sound list.
     */
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
                notifyDataSetChanged(); // Updates all radio buttons to show the new selection
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