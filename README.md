# SplitBill - Mobile App

SplitBill is an Android expense-sharing application that allows users to:
- Create groups for splitting expenses (friends, roommates, trips, etc.)
- Add members to groups
- Record and track expenses within groups
- Take photos of receipts using the device camera and automatically extract expense information
- View expense summaries and balances in a dashboard
- See who owes what to whom
- Manage user profiles

The app uses modern Android development technologies:
- Kotlin as the primary programming language
- Jetpack Compose for the UI framework
- MVVM architecture with ViewModels
- Navigation Component for screen navigation
- Firebase for backend services:
    - Authentication
    - Realtime Firebase for database storage
    - Firebase ML Kit for text recognition from receipt photos

The application demonstrates several Android best practices:
- Permission handling for camera access
- File handling for storing receipt photos
- Image processing with Firebase ML Kit
- Real-time data with Realtime Firebase listeners
- Clean architecture with separation of concerns

The receipt scanning feature allows users to quickly add expenses by taking a photo of a receipt, automatically extracting relevant information like amount and date, and then adding it to the group's expenses.
