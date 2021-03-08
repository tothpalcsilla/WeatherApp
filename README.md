# WeatherApp

![image](https://github.com/tothpalcsilla/WeatherApp/blob/master/images/land.png)

### A forráskód  
A forráskódot a következő github repository **master** branch tartalmazza: https://github.com/tothpalcsilla/WeatherApp

### Fordítási útmutató  
A projekt forráskódját clone-ozva, vagy tömörített mappaként letöltve, majd megfelelő fejlesztőkörnyezetben (például AndroidStudio-ban) megnyitva a szokásos módon fordítható.   
- Nem aláírt APK: Fordítás után az elkészült apk a WeatherApp\app\build\outputs\apk\debug mappában található app-debug.apk néven.
- Aláírt APK: Android Studio-ba betöltve a forráskódot a Build/Generate Signed Bundle/APK menüpontot kiválasztva megjelenik egy felugró ablak. Első lépésben az APK-t kell választani, majd a Next gombbal továbblépve egy már létező key store-t kiválasztva, vagy újat generálva hozható létre aláírt APK. Az aláírt APK készítésének pontos lépéseiről itt olvashat: [Sign your app](https://developer.android.com/studio/publish/app-signing#sign_release)


### Telepítési útmutató  
A WeatherApp telepítéséhez másolja az [apk](https://github.com/tothpalcsilla/WeatherApp/blob/master/apk/app-release.apk)-t az Android eszközre majd telepítse azt.
A minimum elvárt SDK verzió 19-es (Android 4.4)

### Javaslatok továbbfejlesztésre  
- időjárás előrejelzés
- korábbi időjárásadatok tárolása, statusztikák készítése belőlük

### Mi az ami ebben a feladatban újdonság volt, utánajárást igényelt
Korábban csak Java nyelven fejlesztettem Android alkalmazást, így a legnagyobb újdonság a Kotlin nyelv használata, és nyelvi elemeinek a megismerése volt.

![image](https://github.com/tothpalcsilla/WeatherApp/blob/master/images/menu.png)
