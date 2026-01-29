package com.goldtea.sales.data.model;


import java.util.Date;

/**
 * Sale data model matching MongoDB schema exactly
 * Using POJOs instead of Realm for simplicity
 */
public class Sale {
    // Firestore auto-generates IDs  // MongoDB ObjectId
    private String sale_id;
    private Date date;
    private String day;
    private String village;
    private String customer_name;
    private String brand;
    private String tea_type;
    private String packaging;
    private double rate;
    private double quantity;
    private double total_amount;
    private String payment_status;
    private double amount_paid;
    private double balance;
    private Date created_at;
    private Date updated_at;
    
    public Sale() {
        // Generate a unique ID by default
        this.sale_id = java.util.UUID.randomUUID().toString();
        this.created_at = new Date();
        this.updated_at = new Date();
    }
    
    // Getters and Setters
    // ID getters/setters removed for Firestore
    
    
    public String getSale_id() { return sale_id; }
    public void setSale_id(String sale_id) { this.sale_id = sale_id; }
    
    public Date getDate() { return date; }
    public void setDate(Date date) { this.date = date; }
    
    public String getDay() { return day; }
    public void setDay(String day) { this.day = day; }
    
    public String getVillage() { return village; }
    public void setVillage(String village) { this.village = village; }
    
    public String getCustomer_name() { return customer_name; }
    public void setCustomer_name(String customer_name) { this.customer_name = customer_name; }
    
    public String getBrand() { return brand; }
    public void setBrand(String brand) { this.brand = brand; }
    
    public String getTea_type() { return tea_type; }
    public void setTea_type(String tea_type) { this.tea_type = tea_type; }
    
    public String getPackaging() { return packaging; }
    public void setPackaging(String packaging) { this.packaging = packaging; }
    
    public double getRate() { return rate; }
    public void setRate(double rate) { this.rate = rate; }
    
    public double getQuantity() { return quantity; }
    public void setQuantity(double quantity) { this.quantity = quantity; }
    
    public double getTotal_amount() { return total_amount; }
    public void setTotal_amount(double total_amount) { this.total_amount = total_amount; }
    
    public String getPayment_status() { return payment_status; }
    public void setPayment_status(String payment_status) { this.payment_status = payment_status; }
    
    public double getAmount_paid() { return amount_paid; }
    public void setAmount_paid(double amount_paid) { this.amount_paid = amount_paid; }
    
    public double getBalance() { return balance; }
    public void setBalance(double balance) { this.balance = balance; }
    
    public Date getCreated_at() { return created_at; }
    public void setCreated_at(Date created_at) { this.created_at = created_at; }
    
    public Date getUpdated_at() { return updated_at; }
    public void setUpdated_at(Date updated_at) { this.updated_at = updated_at; }
}
