package au.edu.jcu.wordkeep.di

import javax.inject.Qualifier

/** Dispatcher for database and network work. Replaced with a test dispatcher in tests. */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class IoDispatcher

/**
 * A scope that lives as long as the app. Used for writes that must finish even if the screen
 * that started them closes (saving an answer, erasing all data).
 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class ApplicationScope
