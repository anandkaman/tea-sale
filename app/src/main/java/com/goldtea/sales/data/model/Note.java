package com.goldtea.sales.data.model;

import java.util.Date;
import java.util.UUID;

public class Note {
    private String note_id;
    private String title;
    private String content;
    private Date created_at;
    private Date updated_at;

    public Note() {
        this.note_id = UUID.randomUUID().toString();
        this.created_at = new Date();
        this.updated_at = new Date();
    }

    public Note(String title, String content) {
        this();
        this.title = title;
        this.content = content;
    }

    // Getters and Setters
    public String getNote_id() { return note_id; }
    public void setNote_id(String note_id) { this.note_id = note_id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public Date getCreated_at() { return created_at; }
    public void setCreated_at(Date created_at) { this.created_at = created_at; }

    public Date getUpdated_at() { return updated_at; }
    public void setUpdated_at(Date updated_at) { this.updated_at = updated_at; }
}
