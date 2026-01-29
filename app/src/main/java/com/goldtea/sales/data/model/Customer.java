package com.goldtea.sales.data.model;


import java.util.Date;

/**
 * Customer data model matching MongoDB schema
 */
public class Customer {
    // Firestore auto-generates IDs
    private String village;
    private String customer_name;
    private Date added_on;
    
    public Customer() {
        // Firestore auto-generates IDs
        this.added_on = new Date();
    }
    
    // Getters and Setters
    // ID getters/setters removed for Firestore
    
    
    public String getVillage() { return village; }
    public void setVillage(String village) { this.village = village; }
    
    public String getCustomer_name() { return customer_name; }
    public void setCustomer_name(String customer_name) { this.customer_name = customer_name; }
    
    public Date getAdded_on() { return added_on; }
    public void setAdded_on(Date added_on) { this.added_on = added_on; }
}
