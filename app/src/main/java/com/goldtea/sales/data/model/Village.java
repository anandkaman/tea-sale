package com.goldtea.sales.data.model;


import java.util.Date;

/**
 * Village data model matching MongoDB schema
 */
public class Village {
    // Firestore auto-generates IDs
    private String name;
    private String day;
    private Date added_on;
    
    public Village() {
        // Firestore auto-generates IDs
        this.added_on = new Date();
    }
    
    // Getters and Setters
    // ID getters/setters removed for Firestore
    
    
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    
    public String getDay() { return day; }
    public void setDay(String day) { this.day = day; }
    
    public Date getAdded_on() { return added_on; }
    public void setAdded_on(Date added_on) { this.added_on = added_on; }
}
