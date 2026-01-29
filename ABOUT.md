# About Gold Tea Sales Management

Gold Tea is a high-performance Android application designed for efficient sales tracking and note management. It leverages a modern cloud-native stack to provide a seamless, high-speed experience even with large datasets.

## ✨ Core Features

### 🎨 Premium UI/UX
- **Dynamic Dashboard**: Real-time statistics, monthly performance overviews, and recent activity at a glance.
- **Modern Aesthetics**: A curated color palette (Gold & Tea Green), glassmorphism effects, and smooth micro-animations.
- **Theme Awareness**: Fully consistent UI across Light and Dark modes, including dynamic chart labels and high-contrast accessibility.

### 📝 Smart Notepad
- **Full-Screen Editing**: Immersive note-taking experience with automatic timestamping.
- **Search & Filtering**: Instant search across all notes with a dedicated count tracker.
- **LRU Memory**: Remembers the 5 most recently opened notes for instant re-opening.

### 📊 Advanced Reporting
- **Date-Range Filtering**: View performance by Today, This Week, This Month, or This Year.
- **Visual Analytics**: Interactive charts for Tea Types, Payment Status, and Sales Trends.
- **Top Customers**: Automatically identifies and lists your most valuable clients.

---

## 🚀 Performance & Optimizations

### 🧠 Hybrid Caching System
- **4-Minute In-Memory Layer**: The app keeps frequently accessed data (Sales batch, Notes batch, and Reports) in RAM for 240 seconds.
- **LRU Note Cache**: Stores the 5 most recently opened notes to avoid redundant database reads during browsing.
- **Query Normalization**: Range-based queries (like Reports) are normalized to the "end of the day" to ensure stable cache hits as the clock moves.

### 📉 Firestore Scaling
- **Infinite Scrolling**: Uses cursor-based pagination to load data in small, efficient batches (50 sales or 30 notes at a time).
- **Date-Filtered Fetching**: Only fetches the specific data needed for the current view, dramatically reducing bandwidth and battery usage.

### 📡 Offline Capability
- **Disk Persistence**: Firestore is configured to store data locally. The app remains fully functional without internet.
- **Auto-Sync**: Any changes made offline are automatically queued and synchronized with the cloud once a connection is restored.

---

## 🛣️ Roadmap: Single-User vs. Multi-User

> [!NOTE]
> Currently, this application is designed for **Single-User/Small Business** use, where a single device interacts with a central database.

### Path to Multi-User Support
To scale this application for multiple independent users or organizations, the following architecture would be required:

1.  **Authentication System**: Integration of Firebase Auth (Email/Google) to identify individual users.
2.  **Schema Scoping**: Modification of the database schema to include a `userId` or `orgId` on every record (Sales, Customers, Notes).
3.  **Security Rules**: Implementation of Firestore Security Rules to ensure users can only ever see or modify data matching their `userId`.
4.  **Private Data Storage**: Transitioning from a global collection structure to user-private subcollections or partitioned data.
5.  **Separate Backend (Optional)**: For complex business logic, multi-tenant billing, or advanced data validation, a Node.js/Python backend (Cloud Functions) would act as a bridge between the app and the private database.
