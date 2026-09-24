package com.livemedica.helix.data.di

import com.livemedica.helix.core.network.NetworkMonitor
import com.livemedica.helix.core.network.SettingsAwareNetworkMonitor
import com.livemedica.helix.core.network.InMemoryTokenProvider
import com.livemedica.helix.core.network.TokenProvider
import com.livemedica.helix.data.repository.MockAppointmentRepository
import com.livemedica.helix.data.repository.MockDoctorRepository
import com.livemedica.helix.data.repository.MockIntegrationRepository
import com.livemedica.helix.data.repository.MockNotificationRepository
import com.livemedica.helix.data.repository.MockOrderRepository
import com.livemedica.helix.data.repository.MockPatientRepository
import com.livemedica.helix.data.repository.MockResultRepository
import com.livemedica.helix.data.repository.MockSearchRepository
import com.livemedica.helix.data.repository.MockTodayRepository
import com.livemedica.helix.data.repository.SettingsRepositoryImpl
import com.livemedica.helix.domain.repository.AppointmentRepository
import com.livemedica.helix.domain.repository.DoctorRepository
import com.livemedica.helix.domain.repository.IntegrationRepository
import com.livemedica.helix.domain.repository.NotificationRepository
import com.livemedica.helix.domain.repository.OrderRepository
import com.livemedica.helix.domain.repository.PatientRepository
import com.livemedica.helix.domain.repository.ResultRepository
import com.livemedica.helix.domain.repository.SearchRepository
import com.livemedica.helix.domain.repository.SettingsRepository
import com.livemedica.helix.domain.repository.TodayRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * The single seam between Phase 1 and Phase 2.
 *
 * Every binding below points at a `Mock*` implementation. When the AWS backend is ready, each line
 * changes to the corresponding `Api*` implementation and nothing else in the app moves — no
 * ViewModel, no composable, no navigation.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds @Singleton
    abstract fun bindDoctorRepository(impl: MockDoctorRepository): DoctorRepository

    @Binds @Singleton
    abstract fun bindAppointmentRepository(impl: MockAppointmentRepository): AppointmentRepository

    @Binds @Singleton
    abstract fun bindPatientRepository(impl: MockPatientRepository): PatientRepository

    @Binds @Singleton
    abstract fun bindOrderRepository(impl: MockOrderRepository): OrderRepository

    @Binds @Singleton
    abstract fun bindResultRepository(impl: MockResultRepository): ResultRepository

    @Binds @Singleton
    abstract fun bindTodayRepository(impl: MockTodayRepository): TodayRepository

    @Binds @Singleton
    abstract fun bindNotificationRepository(impl: MockNotificationRepository): NotificationRepository

    @Binds @Singleton
    abstract fun bindSearchRepository(impl: MockSearchRepository): SearchRepository

    @Binds @Singleton
    abstract fun bindIntegrationRepository(impl: MockIntegrationRepository): IntegrationRepository

    @Binds @Singleton
    abstract fun bindSettingsRepository(impl: SettingsRepositoryImpl): SettingsRepository

    @Binds @Singleton
    abstract fun bindNetworkMonitor(impl: SettingsAwareNetworkMonitor): NetworkMonitor
}

@Module
@InstallIn(SingletonComponent::class)
object NetworkProvisionModule {

    /**
     * Phase 1 holds no token at all. This exists so the auth plumbing has a real shape to grow
     * into; it is never populated from source, from a build config, or from the network yet.
     */
    @Provides
    @Singleton
    fun provideTokenProvider(): TokenProvider = InMemoryTokenProvider()
}
