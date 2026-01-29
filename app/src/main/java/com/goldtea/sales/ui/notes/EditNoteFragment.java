package com.goldtea.sales.ui.notes;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.goldtea.sales.R;
import com.goldtea.sales.data.firestore.FirestoreManager;
import com.goldtea.sales.data.model.Note;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class EditNoteFragment extends Fragment {

    private static final String ARG_NOTE_ID = "note_id";

    private EditText editTitle, editContent;
    private TextView dateText;
    private ImageButton btnBack, btnSave;
    private FirestoreManager firestoreManager;
    private Note currentNote;
    private String noteId;

    public static EditNoteFragment newInstance(@Nullable String noteId) {
        EditNoteFragment fragment = new EditNoteFragment();
        Bundle args = new Bundle();
        args.putString(ARG_NOTE_ID, noteId);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            noteId = getArguments().getString(ARG_NOTE_ID);
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_edit_note, container, false);

        initializeViews(view);
        loadNoteData();
        setupListeners();

        return view;
    }

    private void initializeViews(View view) {
        if (getContext() == null) return;
        firestoreManager = FirestoreManager.getInstance(getContext());
        editTitle = view.findViewById(R.id.editNoteTitle);
        editContent = view.findViewById(R.id.editNoteContent);
        dateText = view.findViewById(R.id.editNoteDate);
        btnBack = view.findViewById(R.id.btnBack);
        btnSave = view.findViewById(R.id.btnSave);

        // Clear header title when in full page mode if possible
        if (getActivity() != null) {
            TextView header = getActivity().findViewById(R.id.header_title);
            if (header != null) header.setText("");
        }
    }

    private void loadNoteData() {
        if (noteId == null) {
            currentNote = new Note();
            updateDateDisplay(new Date());
            return;
        }

        firestoreManager.getNoteById(noteId, new FirestoreManager.OnNoteLoadedListener() {
            @Override
            public void onNoteLoaded(Note note) {
                if (!isAdded()) return;
                currentNote = note;
                editTitle.setText(currentNote.getTitle());
                editContent.setText(currentNote.getContent());
                updateDateDisplay(currentNote.getUpdated_at() != null ? currentNote.getUpdated_at() : currentNote.getCreated_at());
            }

            @Override
            public void onError(String error) {
                if (!isAdded()) return;
                Toast.makeText(getContext(), "Error: " + error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateDateDisplay(Date date) {
        SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy HH:mm", Locale.getDefault());
        dateText.setText(sdf.format(date));
    }

    private void setupListeners() {
        btnBack.setOnClickListener(v -> {
            if (getActivity() != null) {
                getActivity().onBackPressed();
            }
        });

        btnSave.setOnClickListener(v -> saveNote());
    }

    private void saveNote() {
        String title = editTitle.getText().toString().trim();
        String content = editContent.getText().toString().trim();

        if (title.isEmpty()) {
            Toast.makeText(getContext(), "Title is required", Toast.LENGTH_SHORT).show();
            return;
        }

        if (currentNote == null) currentNote = new Note();
        currentNote.setTitle(title);
        currentNote.setContent(content);

        firestoreManager.addNote(currentNote, new FirestoreManager.OnNoteAddedListener() {
            @Override
            public void onSuccess() {
                Toast.makeText(getContext(), "Note saved", Toast.LENGTH_SHORT).show();
                if (getActivity() != null) {
                    getActivity().onBackPressed();
                }
            }

            @Override
            public void onError(String error) {
                Toast.makeText(getContext(), "Error: " + error, Toast.LENGTH_SHORT).show();
            }
        });
    }
}
