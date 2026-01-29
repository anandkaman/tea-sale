package com.goldtea.sales.data.model;

import com.google.firebase.firestore.PropertyName;

import java.util.Date;

/**
 * Pricing data model - supports different prices for Mix and Barik tea types
 */
public class Pricing {
    private String _package;  // Using _package because 'package' is a Java keyword (100gm, 250gm, 500gm, 1kg)
    private String tea_type;  // "Mix" or "Barik"
    private int rate;
    private Date updated_on;

    public Pricing() {
        this.updated_on = new Date();
    }

    // Getters and Setters
    @PropertyName("package")
    public String getPackage() { return _package; }
    
    @PropertyName("package")
    public void setPackage(String packageName) { this._package = packageName; }

    public String getTea_type() { return tea_type; }
    public void setTea_type(String tea_type) { this.tea_type = tea_type; }

    public int getRate() { return rate; }
    public void setRate(int rate) { this.rate = rate; }

    public Date getUpdated_on() { return updated_on; }
    public void setUpdated_on(Date updated_on) { this.updated_on = updated_on; }

    // Helper to get unique key for pricing (tea_type + package)
    public String getPricingKey() {
        return (tea_type != null ? tea_type : "Mix") + "_" + _package;
    }
}
