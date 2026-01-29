# Technical Reference Guide 📖

This document provides a comprehensive breakdown of the GOLD Tea Sales application's source code, explaining the logic and purpose of each file to facilitate maintenance and future updates.

## 🏗️ Core Architecture Components

### 🔄 Database Management
- **[FirestoreManager.java](file:///e:/tea%20powder/android_app/app/src/main/java/com/goldtea/sales/data/firestore/FirestoreManager.java)**: The heartbeat of the app's data layer. It handles all CRUD operations (Create, Read, Update, Delete) for Sales, Villages, Pricing, and Customers.
  - *Key Logic*: Standardizes record identifiers using Firestore Document IDs to prevent sync conflicts.

### 🛡️ Security & Auth
- **[BiometricAuthManager.java](file:///e:/tea%20powder/android_app/app/src/main/java/com/goldtea/sales/auth/BiometricAuthManager.java)**: Manages the integration with Android's `BiometricPrompt`. Handles encryption, verification, and UI callbacks for secure login.
- **[AppPreferences.java](file:///e:/tea%20powder/android_app/app/src/main/java/com/goldtea/sales/data/AppPreferences.java)**: Lightweight storage for app state, such as whether a user is currently logged in.

### 🧩 UI Fragments (Activities & Pages)
- **[MainActivity.java](file:///e:/tea%20powder/android_app/app/src/main/java/com/goldtea/sales/MainActivity.java)**: The main shell of the app. Initializes bottom navigation and manages fragment transitions.
- **[SplashActivity.java](file:///e:/tea%20powder/android_app/app/src/main/java/com/goldtea/sales/ui/splash/SplashActivity.java)**: Entry point that triggers Biometric Auth before allowing access to the main dashboard.
- **[DashboardFragment.java](file:///e:/tea%20powder/android_app/app/src/main/java/com/goldtea/sales/ui/dashboard/DashboardFragment.java)**: Provides a quick "at-a-glance" overview of today's stats.
- **[NewSaleFragment.java](file:///e:/tea%20powder/android_app/app/src/main/java/com/goldtea/sales/ui/newsale/NewSaleFragment.java)**: Complex form processing. includes auto-calculators for totals and balances, and autocomplete for customers and villages.
- **[ViewSalesFragment.java](file:///e:/tea%20powder/android_app/app/src/main/java/com/goldtea/sales/ui/viewsales/ViewSalesFragment.java)**: The main database interface. Features a filtered list view where users can delete sales or "Mark as Paid".
- **[ReportsFragment.java](file:///e:/tea%20powder/android_app/app/src/main/java/com/goldtea/sales/ui/reports/ReportsFragment.java)**: Data visualization logic. processes raw Firestore data into chart-ready entries with specific period filtering (Day/Week/Month/Year).

### 📦 Data Models (POJOs)
- **[Sale.java](file:///e:/tea%20powder/android_app/app/src/main/java/com/goldtea/sales/data/model/Sale.java)**: Contains all sale-related fields. Automatically generates a UUID for every new sale.
- **[Pricing.java](file:///e:/tea%20powder/android_app/app/src/main/java/com/goldtea/sales/data/model/Pricing.java)**: Maps pricing to Tea Type (Mix/Barik) and Package size.
- **[Village.java](file:///e:/tea%20powder/android_app/app/src/main/java/com/goldtea/sales/data/model/Village.java)** & **[Customer.java](file:///e:/tea%20powder/android_app/app/src/main/java/com/goldtea/sales/data/model/Customer.java)**: Simple entities for organizational data.

### 🎨 Adapters
- **[SalesAdapter.java](file:///e:/tea%20powder/android_app/app/src/main/java/com/goldtea/sales/ui/viewsales/SalesAdapter.java)**: Bridges Sale objects to the UI card layout. Includes logic for color-coded payment badges.
- **[TopCustomerAdapter.java](file:///e:/tea%20powder/android_app/app/src/main/java/com/goldtea/sales/ui/reports/TopCustomerAdapter.java)**: Specialized adapter for the leaderboard ranking on the Reports page.

---
© 2026 GOLD Tea Powder. Dedicated Documentation for reference.
