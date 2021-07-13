# GIPHY

The Giphy Integration uses the [Giphy Android SDK](https://github.com/Giphy/giphy-android-sdk/) 
which requires an API key to work. You can get yourself an API key from creating an account at 
[developers.giphy.com](https://developers.giphy.com) and set the `GIPHY_API_KEY` in your `local.properties` 
file (which shouldn't be pushed to the repo).  

The `GIPHY_API_KEY` is then accessible through `BuildConfig.GIPHY_API_KEY` in the code using the 
`com.google.android.secrets-gradle-plugin` gradle plugin. We also have the local.defaults.properties 
(which is pushed to the repo with `"PLACE_HOLDER"` values so we can get a `BuildConfig.GIPHY_API_KEY`.  
