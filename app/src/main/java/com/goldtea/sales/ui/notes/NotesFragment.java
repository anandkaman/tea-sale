package com.goldtea.sales.ui.notes;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.goldtea.sales.R;
import com.goldtea.sales.data.firestore.FirestoreManager;
import com.goldtea.sales.data.model.Note;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;

import java.util.ArrayList;
import java.util.List;

public class NotesFragment extends Fragment implements NotesAdapter.OnNoteClickListener {

    private RecyclerView notesRecyclerView;
    private SwipeRefreshLayout swipeRefreshLayout;
    private NotesAdapter notesAdapter;
    private LinearLayout emptyNotesLayout;
    private FloatingActionButton addNoteFab;
    private TextView noteCountText;
    private FirestoreManager firestoreManager;

    // Pagination variables
    private boolean isLoading = false;
    private boolean isLastPage = false;
    private com.google.firebase.firestore.DocumentSnapshot lastVisibleSnapshot = null;
    private static final int PAGE_SIZE = 30;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_notes, container, false);
        
        initializeViews(view);
        setupRecyclerView();
        setupListeners(view);
        loadNotes(false);
        
        // Restore header title when coming back
        if (getActivity() != null) {
            android.widget.TextView header = getActivity().findViewById(R.id.header_title);
            if (header != null) header.setText("GOLD Tea");
        }

        return view;
    }

    private void initializeViews(View view) {
        if (getContext() == null) return;
        firestoreManager = FirestoreManager.getInstance(getContext());
        notesRecyclerView = view.findViewById(R.id.notesRecyclerView);
        swipeRefreshLayout = view.findViewById(R.id.swipeRefreshLayout);
        emptyNotesLayout = view.findViewById(R.id.emptyNotesLayout);
        addNoteFab = view.findViewById(R.id.addNoteFab);
        noteCountText = view.findViewById(R.id.noteCountText);
    }

    private void setupRecyclerView() {
        notesAdapter = new NotesAdapter(this);
        notesRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        notesRecyclerView.setAdapter(notesAdapter);

        // Infinite scroll listener
        notesRecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                LinearLayoutManager layoutManager = (LinearLayoutManager) recyclerView.getLayoutManager();
                if (layoutManager != null && !isLoading && !isLastPage) {
                    int visibleItemCount = layoutManager.getChildCount();
                    int totalItemCount = layoutManager.getItemCount();
                    int firstVisibleItemPosition = layoutManager.findFirstVisibleItemPosition();

                    if ((visibleItemCount + firstVisibleItemPosition) >= totalItemCount
                            && firstVisibleItemPosition >= 0
                            && totalItemCount >= PAGE_SIZE) {
                        loadMoreNotes();
                    }
                }
            }
        });
    }

    private void resetPagination() {
        lastVisibleSnapshot = null;
        isLastPage = false;
        notesAdapter.clear();
    }

    private void setupListeners(View view) {
        addNoteFab.setOnClickListener(v -> navigateToEdit(null));
        
        view.findViewById(R.id.btnSearchNotes).setOnClickListener(v -> showSearchDialog());

        swipeRefreshLayout.setOnRefreshListener(() -> {
            resetPagination();
            loadNotes(true);
        });
    }

    private void showSearchDialog() {
        if (getContext() == null) return;
        
        android.widget.EditText searchInput = new android.widget.EditText(getContext());
        searchInput.setHint("Search notes...");
        int padding = (int) (16 * getResources().getDisplayMetrics().density);
        searchInput.setPadding(padding, padding, padding, padding);

        new androidx.appcompat.app.AlertDialog.Builder(getContext())
                .setTitle("Search")
                .setView(searchInput)
                .setPositiveButton("Search", (dialog, which) -> {
                    String query = searchInput.getText().toString();
                    notesAdapter.filter(query);
                })
                .setNegativeButton("Clear", (dialog, which) -> notesAdapter.filter(""))
                .show();

        // Optional: Real-time filtering as they type
        searchInput.addTextChangedListener(new android.text.TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                notesAdapter.filter(s.toString());
            }
            @Override
            public void afterTextChanged(android.text.Editable s) {}
        });
    }

    private void loadNotes(boolean isRefresh) {
        if (!isAdded() || isLoading) return;
        isLoading = true;
        if (isRefresh) swipeRefreshLayout.setRefreshing(true);
        
        firestoreManager.getNotesPaginated(PAGE_SIZE, null, new FirestoreManager.OnNotesPaginatedListener() {
            @Override
            public void onNotesLoaded(List<Note> notes, com.google.firebase.firestore.DocumentSnapshot lastVisible) {
                if (!isAdded()) return;
                isLoading = false;
                swipeRefreshLayout.setRefreshing(false);
                
                notesAdapter.setNotes(notes);
                lastVisibleSnapshot = lastVisible;
                isLastPage = notes.size() < PAGE_SIZE;
                
                updateEmptyState(notes.isEmpty());
                noteCountText.setText(notes.size() + " notes");
            }

            @Override
            public void onNotesLoaded(List<Note> notes) {
                // Not used
            }

            @Override
            public void onError(String error) {
                if (!isAdded()) return;
                isLoading = false;
                swipeRefreshLayout.setRefreshing(false);
                Toast.makeText(getContext(), "Error: " + error, Toast.LENGTH_SHORT).show();
            }
        }, isRefresh);
    }

    private void loadMoreNotes() {
        if (isLoading || isLastPage || !isAdded()) return;
        isLoading = true;

        firestoreManager.getNotesPaginated(PAGE_SIZE, lastVisibleSnapshot, new FirestoreManager.OnNotesPaginatedListener() {
            @Override
            public void onNotesLoaded(List<Note> notes, com.google.firebase.firestore.DocumentSnapshot lastVisible) {
                if (!isAdded()) return;
                isLoading = false;
                
                notesAdapter.addNotes(notes);
                lastVisibleSnapshot = lastVisible;
                isLastPage = notes.size() < PAGE_SIZE;
                
                // Update count based on full adapter list
                noteCountText.setText(notesAdapter.getItemCount() + " notes");
            }

            @Override
            public void onNotesLoaded(List<Note> notes) {
                // Not used
            }

            @Override
            public void onError(String error) {
                if (!isAdded()) return;
                isLoading = false;
            }
        });
    }

    private void updateEmptyState(boolean isEmpty) {
        if (isEmpty) {
            notesRecyclerView.setVisibility(View.GONE);
            emptyNotesLayout.setVisibility(View.VISIBLE);
        } else {
            notesRecyclerView.setVisibility(View.VISIBLE);
            emptyNotesLayout.setVisibility(View.GONE);
        }
    }

    private void navigateToEdit(@Nullable String noteId) {
        if (getActivity() != null) {
            getActivity().getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, EditNoteFragment.newInstance(noteId))
                    .addToBackStack(null)
                    .commit();
        }
    }

    @Override
    public void onNoteClick(Note note) {
        navigateToEdit(note.getNote_id());
    }

    @Override
    public void onNoteLongClick(Note note) {
        if (getContext() == null) return;

        new AlertDialog.Builder(getContext())
                .setTitle("Delete Note")
                .setMessage("Are you sure you want to delete this note?")
                .setPositiveButton("Delete", (dialog, which) -> {
                    deleteNote(note);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void deleteNote(Note note) {
        firestoreManager.deleteNote(note.getNote_id(), new FirestoreManager.OnNoteDeletedListener() {
            @Override
            public void onSuccess() {
                if (!isAdded()) return;
                Toast.makeText(getContext(), "Note deleted", Toast.LENGTH_SHORT).show();
                resetPagination();
                loadNotes(true);
            }

            @Override
            public void onError(String error) {
                if (!isAdded()) return;
                Toast.makeText(getContext(), "Error deleting note: " + error, Toast.LENGTH_SHORT).show();
            }
        });
    }
}
