package au.edu.jcu.wordkeep.di

import android.content.Context
import androidx.room.Room
import au.edu.jcu.wordkeep.data.local.WordkeepDatabase
import au.edu.jcu.wordkeep.data.local.dao.CardDao
import au.edu.jcu.wordkeep.data.local.dao.LookAlikeDao
import au.edu.jcu.wordkeep.data.local.dao.ReviewLogDao
import au.edu.jcu.wordkeep.data.local.dao.WordDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): WordkeepDatabase =
        Room.databaseBuilder(context, WordkeepDatabase::class.java, WordkeepDatabase.NAME).build()

    @Provides
    fun provideWordDao(database: WordkeepDatabase): WordDao = database.wordDao()

    @Provides
    fun provideCardDao(database: WordkeepDatabase): CardDao = database.cardDao()

    @Provides
    fun provideReviewLogDao(database: WordkeepDatabase): ReviewLogDao = database.reviewLogDao()

    @Provides
    fun provideLookAlikeDao(database: WordkeepDatabase): LookAlikeDao = database.lookAlikeDao()
}
