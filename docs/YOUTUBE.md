# YOUTUBE

The YouTube Player Integration uses the [Youtube Android Player API](https://developers.google.com/youtube/android/player?hl=en_US) 
which requires an API key to work. You can get yourself an API key from creating an account at 
[console.cloud.google.com](https://console.cloud.google.com/apis/library/youtube.googleapis.com) and set the `YOUTUBE_API_KEY` in your `local.properties` 
file (which shouldn't be pushed to the repo).  

The `YOUTUBE_API_KEY` is then accessible through `BuildConfig.YOUTUBE_API_KEY` in the code using the 
`com.google.android.secrets-gradle-plugin` gradle plugin. We also have the local.defaults.properties 
(which is pushed to the repo with `"PLACE_HOLDER"` values so we can get a `BuildConfig.YOUTUBE_API_KEY`.  
