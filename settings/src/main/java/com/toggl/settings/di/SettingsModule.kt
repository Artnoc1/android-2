package com.toggl.settings.di

import android.content.Context
import com.toggl.settings.domain.SettingsSectionBlueprint
import com.toggl.settings.domain.SettingsSelector
import com.toggl.settings.domain.SettingsStructureBlueprint
import com.toggl.settings.domain.SingleChoiceSettingSelector
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ApplicationComponent
import dagger.hilt.android.components.FragmentComponent
import dagger.hilt.android.qualifiers.ActivityContext
import dagger.hilt.android.scopes.FragmentScoped
import javax.inject.Singleton

@Module
@InstallIn(ApplicationComponent::class)
object SettingsModule {
    @Provides
    @Singleton
    internal fun sectionsBlueprint(): List<SettingsSectionBlueprint> = SettingsStructureBlueprint.sections
}

@Module
@InstallIn(FragmentComponent::class)
object FragmentSettingsModule {
    @Provides
    @FragmentScoped
    internal fun settingsSelector(@ActivityContext context: Context, sectionsBlueprint: List<SettingsSectionBlueprint>) = SettingsSelector(context, sectionsBlueprint)

    @Provides
    @FragmentScoped
    internal fun singleChoiceSettingsSelector(@ActivityContext context: Context) = SingleChoiceSettingSelector(context)
}