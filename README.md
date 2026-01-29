# GOLD Tea Sales - Native Android App 🍵

A professional, native Android application designed for tea powder distributors to manage sales, track collections, and analyze business performance with secure biometric access and cloud sync.

## ✨ Key Features

- 🔐 **Biometric Security**: Industry-standard authentication using Fingerprint/Face ID with PIN fallback.
- 🔥 **Real-time Sync**: Full integration with **Firebase Firestore** for instant cloud synchronization across devices.
- 📶 **Offline Support**: Local persistence allows recording sales even without an internet connection.
- 💳 **Mark as Paid**: One-tap payment tracking to clear balances and update collection status.
- 📊 **Advanced Analytics**:
  - Interactive Pie Charts for Tea Types and Payment Status.
  - Bar Charts for Sales Trends.
  - Detailed performance filters (Today, This Week, This Month, This Year).
- 📍 **Village Management**: Dynamic village configuration to organize sales by area.
- 🏷️ **Pricing System**: Customizable pricing logic based on tea type (Mix/Barik) and package size.
- 🔍 **Global Search**: Search sales by customer name or village with status filtering.

## 🛠️ Technology Stack

- **Lanuage**: Java (Native Android)
- **Database**: Google Firebase Firestore
- **Authentication**: Android Biometric API
- **Charts**: MPAndroidChart
- **UI Architecture**: Single Activity + Navigation Components
- **Design**: Material Design 3 (Tea Green & Gold aesthetic)

## 📁 Project Structure

- `com.goldtea.sales.ui`: High-level UI fragments (Dashboard, New Sale, Reports, Settings).
- `com.goldtea.sales.data.firestore`: Centralized logic for all database transactions.
- `com.goldtea.sales.data.model`: Standardized POJOs for Sales, Customers, Villages, and Pricing.
- `com.goldtea.sales.auth`: Biometric authentication logic.

## 🚀 Setup & Installation

1. **Clone the Repo**:
   ```bash
   git clone https://github.com/anandkaman/tea-sale.git
   ```
2. **Firebase Configuration**:
   - Place your `google-services.json` in the `app/` directory.
   - Ensure Firestore and Anonymous Auth are enabled in your Firebase Console.
3. **Build & Run**:
   - Open in Android Studio (Arctic Fox+).
   - Sync Gradle and Run on an Android device (API 26+).

## 📄 Documentation

For deep technical details, refer to:
- [reference.md](./reference.md): Detailed breakdown of every file and its core logic.
- [db.md](./db.md): Comprehensive guide on setting up the Firestore database and Auth rules.

---
© 2026 GOLD Tea Powder. Built with ❤️ for the Tea Industry.
