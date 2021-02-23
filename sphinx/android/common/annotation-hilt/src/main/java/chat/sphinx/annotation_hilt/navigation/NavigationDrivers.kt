package chat.sphinx.annotation_hilt.navigation

import javax.inject.Qualifier

/**
 * To ensure injection of the correct BaseNavigationDriver is had for the respective Navigator.
 * */

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class MainDriver