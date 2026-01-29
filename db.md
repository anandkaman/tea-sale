# Database & Authentication Setup Guide 🔥

The GOLD Tea Sales app relies on **Google Firebase** for real-time data sync and secure authentication. Follow these steps to set up your own environment.

## 📡 Firebase Project Setup

1. **Create Project**: Go to [Firebase Console](https://console.firebase.google.com/) and create a new project named "GOLD Tea Sales".
2. **Add Android App**: Register your app with the package name `com.goldtea.sales`.
3. **Download Config**: Download the `google-services.json` file and place it in the `app/` folder of the Android project.

## 🗄️ Firestore Database Configuration

1. **Enable Firestore**: In the console, navigate to **Build > Firestore Database** and click "Create database".
2. **Collections**: The app will automatically create these collections, but you should know their structure:
   - `sales`: All transaction records.
   - `villages`: List of operational areas.
   - `pricing`: Rate mapping for tea types and packages.
   - `customers`: Saved customer names for autocomplete.

### 🛡️ Security Rules

Paste these rules into the **Rules** tab of your Firestore Database to allow the app to function securely with anonymous authentication:

```javascript
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    match /{document=**} {
      // Allow read/write for authenticated users (including anonymous)
      allow read, write: if request.auth != null;
    }
  }
}
```

## 🔐 Authentication Setup

1. **Enable Sign-in Methods**: Navigate to **Build > Authentication > Sign-in method**.
2. **Anonymous Auth**: Enable the **Anonymous** provider. The app uses this to ensure that even without a username/password, the data is linked to a valid Firebase session for security.

## 🛠️ Data Migration Tips

If you are migrating data from an old system:
- Ensure every record has a strictly unique ID.
- The `sale_id` field in the app is used for internal tracking but the **Firestore Document ID** is the "Source of Truth" for updates and deletions.

---
© 2026 GOLD Tea Powder. Reliable Cloud Architecture.
