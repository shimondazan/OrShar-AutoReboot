# OrShar AutoReboot

אפליקציית אנדרואיד לאתחול יומי אוטומטי ללא Root.

## בניית ה-APK

### אפשרות א׳ — GitHub Actions (מומלץ)
1. העלה את הפרויקט ל-GitHub Repository
2. לך ל-Actions → "Build APK" → Run workflow
3. הורד את ה-APK מ-Artifacts

### אפשרות ב׳ — Android Studio מקומי
1. פתח את התיקייה ב-Android Studio
2. Build → Build Bundle(s) / APK(s) → Build APK(s)

## הגדרת Device Owner (פעם אחת בלבד)

לאחר התקנת ה-APK, חובה להגדיר Device Owner:
```
adb shell dpm set-device-owner com.orshar.autoreboot/.AdminReceiver
```
**חשוב:** המכשיר חייב להיות ללא חשבון Google מוגדר!
