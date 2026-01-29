package com.goldtea.sales.ui.notes;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.goldtea.sales.R;
import com.goldtea.sales.data.model.Note;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class NotesAdapter extends RecyclerView.Adapter<NotesAdapter.NoteViewHolder> {

    private List<Note> notes = new ArrayList<>();
    private List<Note> notesFull = new ArrayList<>();
    private final OnNoteClickListener listener;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());

    public interface OnNoteClickListener {
        void onNoteClick(Note note);
        void onNoteLongClick(Note note);
    }

    public NotesAdapter(OnNoteClickListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public NoteViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_note, parent, false);
        return new NoteViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull NoteViewHolder holder, int position) {
        Note note = notes.get(position);
        holder.bind(note);
    }

    @Override
    public int getItemCount() {
        return notes.size();
    }

    public void setNotes(List<Note> notes) {
        this.notes = new ArrayList<>(notes);
        this.notesFull = new ArrayList<>(notes);
        notifyDataSetChanged();
    }

    public void addNotes(List<Note> newNotes) {
        int startPos = this.notes.size();
        this.notes.addAll(newNotes);
        this.notesFull.addAll(newNotes);
        notifyItemRangeInserted(startPos, newNotes.size());
    }

    public void clear() {
        this.notes.clear();
        this.notesFull.clear();
        notifyDataSetChanged();
    }

    public void filter(String text) {
        notes.clear();
        if (text.isEmpty()) {
            notes.addAll(notesFull);
        } else {
            String query = text.toLowerCase().trim();
            for (Note note : notesFull) {
                if (note.getTitle().toLowerCase().contains(query) || 
                    (note.getContent() != null && note.getContent().toLowerCase().contains(query))) {
                    notes.add(note);
                }
            }
        }
        notifyDataSetChanged();
    }

    class NoteViewHolder extends RecyclerView.ViewHolder {
        TextView titleText, dateText, snippetText;

        NoteViewHolder(@NonNull View itemView) {
            super(itemView);
            titleText = itemView.findViewById(R.id.noteTitleText);
            dateText = itemView.findViewById(R.id.noteDateText);
            snippetText = itemView.findViewById(R.id.noteSnippetText);

            itemView.setOnClickListener(v -> {
                int pos = getAdapterPosition();
                if (pos != RecyclerView.NO_POSITION) {
                    listener.onNoteClick(notes.get(pos));
                }
            });

            itemView.setOnLongClickListener(v -> {
                int pos = getAdapterPosition();
                if (pos != RecyclerView.NO_POSITION) {
                    listener.onNoteLongClick(notes.get(pos));
                    return true;
                }
                return false;
            });
        }

        void bind(Note note) {
            titleText.setText(note.getTitle());
            dateText.setText(dateFormat.format(note.getUpdated_at() != null ? note.getUpdated_at() : note.getCreated_at()));
            
            String content = note.getContent();
            if (content != null && !content.isEmpty()) {
                snippetText.setText(content);
                snippetText.setVisibility(View.VISIBLE);
            } else {
                snippetText.setVisibility(View.GONE);
            }
        }
    }
}
