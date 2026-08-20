# Quick Contacts Widget

Android home-screen widget for many favorite contacts.

## Features
- Up to 10 contacts
- Two-row, five-column home-screen widget
- Contact photo when available
- One-tap direct calling when CALL_PHONE permission is granted
- Falls back to the dialer if call permission is unavailable
- Easy "Add widget to home screen" button
- Selected contacts can be changed at any time
- Widget refreshes automatically after changes

## Build
Open the folder in Android Studio and build/install the `app` module.

Minimum Android version: Android 8.0 (API 26).

## v1.2.0
- Contact picker stays open after adding a contact; the added person disappears from the available list.
- Transparent responsive widget with 3/4/5-column layouts chosen by widget width.
- Fixed-size avatar bubbles so resizing does not stretch contact photos.
- Neon-style avatar ring and call badge while keeping the widget background fully transparent.
- Animated calling transition before the actual call/dial action.
- New application launcher icon.


## v1.4.0
- Airier transparent widget layout
- Even spacing using flexible contact slots
- Less aggressive 5-column mode
- More vertical breathing room between rows
