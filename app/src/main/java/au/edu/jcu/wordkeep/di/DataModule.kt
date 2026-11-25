package au.edu.jcu.wordkeep.di

import au.edu.jcu.wordkeep.data.audio.AndroidFeedbackPlayer
import au.edu.jcu.wordkeep.data.audio.AndroidPronouncer
import au.edu.jcu.wordkeep.data.audio.FeedbackPlayer
import au.edu.jcu.wordkeep.data.audio.Pronouncer
import au.edu.jcu.wordkeep.data.repository.OfflineFirstWordRepository
import au.edu.jcu.wordkeep.data.repository.PrivacyRepository
import au.edu.jcu.wordkeep.data.repository.RoomPrivacyRepository
import au.edu.jcu.wordkeep.data.repository.RoomStatsRepository
import au.edu.jcu.wordkeep.data.repository.RoomStudyRepository
import au.edu.jcu.wordkeep.data.repository.StatsRepository
import au.edu.jcu.wordkeep.data.repository.StudyRepository
import au.edu.jcu.wordkeep.data.repository.WordRepository
import au.edu.jcu.wordkeep.data.settings.DataStoreSettingsRepository
import au.edu.jcu.wordkeep.data.settings.SettingsRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/** Binds each interface to its implementation. ViewModels only ever see the interfaces. */
@Module
@InstallIn(SingletonComponent::class)
abstract class DataModule {

    @Binds
    abstract fun bindWordRepository(impl: OfflineFirstWordRepository): WordRepository

    @Binds
    abstract fun bindStudyRepository(impl: RoomStudyRepository): StudyRepository

    @Binds
    abstract fun bindStatsRepository(impl: RoomStatsRepository): StatsRepository

    @Binds
    abstract fun bindPrivacyRepository(impl: RoomPrivacyRepository): PrivacyRepository

    @Binds
    abstract fun bindSettingsRepository(impl: DataStoreSettingsRepository): SettingsRepository

    @Binds
    abstract fun bindPronouncer(impl: AndroidPronouncer): Pronouncer

    @Binds
    abstract fun bindFeedbackPlayer(impl: AndroidFeedbackPlayer): FeedbackPlayer
}
