package au.edu.jcu.wordkeep.di

import javax.inject.Qualifier

/** Dispatcher for database and network work. Replaced with a test dispatcher in tests. */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class IoDispatcher
