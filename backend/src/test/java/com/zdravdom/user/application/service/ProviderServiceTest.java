package com.zdravdom.user.application.service;

import com.zdravdom.auth.domain.Role;
import com.zdravdom.global.exception.GlobalExceptionHandler.ResourceNotFoundException;
import com.zdravdom.user.adapters.out.persistence.ProviderRepository;
import com.zdravdom.user.adapters.out.persistence.ProviderScheduleRepository;
import com.zdravdom.user.adapters.out.persistence.ProviderDocumentRepository;
import com.zdravdom.user.application.dto.ProviderAvailabilityResponse;
import com.zdravdom.user.application.dto.ProviderResponse;
import com.zdravdom.user.application.dto.UpdateAvailabilityRequest;

import com.zdravdom.user.domain.Provider;
import com.zdravdom.user.domain.Provider.Language;
import com.zdravdom.user.domain.Provider.Profession;
import com.zdravdom.user.domain.Provider.ProviderStatus;
import com.zdravdom.user.domain.Provider.Specialty;
import com.zdravdom.user.domain.ProviderSchedule;
import com.zdravdom.user.domain.ProviderSchedule.DayOfWeek;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProviderServiceTest {

    @Mock private ProviderRepository providerRepository;
    @Mock private ProviderScheduleRepository scheduleRepository;
    @Mock private ProviderDocumentRepository documentRepository;
    @Mock private DocumentStorageService documentStorageService;

    private ProviderService providerService;

    @BeforeEach
    void setUp() {
        providerService = new ProviderService(
            providerRepository, scheduleRepository, documentRepository, documentStorageService);
    }

    // ─── getProviderByUserId ───────────────────────────────────────────────────

    @Nested
    @DisplayName("getProviderByUserId()")
    class GetProviderByUserId {

        @Test
        @DisplayName("returns provider response when found")
        void returnsProvider_WhenFound() {
            Provider provider = makeProvider(1L, 10L, "dr@test.com");
            when(providerRepository.findByUserId(10L)).thenReturn(Optional.of(provider));

            ProviderResponse response = providerService.getProviderByUserId(10L);

            assertThat(response.id()).isEqualTo(1L);
            assertThat(response.firstName()).isEqualTo("Marko");
            assertThat(response.lastName()).isEqualTo("Horvat");
            assertThat(response.email()).isEqualTo("dr@test.com");
            assertThat(response.profession()).isEqualTo(Profession.NURSE);
            assertThat(response.status()).isEqualTo(ProviderStatus.ACTIVE);
            assertThat(response.languages()).contains(Language.SLOVENIAN);
        }

        @Test
        @DisplayName("throws ResourceNotFoundException when not found")
        void throwsNotFound_WhenNotExists() {
            when(providerRepository.findByUserId(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> providerService.getProviderByUserId(999L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Provider");
        }
    }

    // ─── getProviderById ───────────────────────────────────────────────────────

    @Nested
    @DisplayName("getProviderById()")
    class GetProviderById {

        @Test
        @DisplayName("returns provider when found by provider id")
        void returnsProvider_WhenFound() {
            Provider provider = makeProvider(5L, 20L, "nurse@test.com");
            when(providerRepository.findById(5L)).thenReturn(Optional.of(provider));

            ProviderResponse response = providerService.getProviderById(5L);

            assertThat(response.id()).isEqualTo(5L);
            assertThat(response.email()).isEqualTo("nurse@test.com");
        }

        @Test
        @DisplayName("throws ResourceNotFoundException when not found")
        void throwsNotFound_WhenNotExists() {
            when(providerRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> providerService.getProviderById(999L))
                .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    // ─── getAvailability ───────────────────────────────────────────────────────

    @Nested
    @DisplayName("getAvailability()")
    class GetAvailability {

        @Test
        @DisplayName("returns schedule from repository when provider found")
        void returnsSchedule_WhenProviderExists() {
            Provider provider = makeProvider(1L, 10L, "dr@test.com");
            when(providerRepository.findByUserId(10L)).thenReturn(Optional.of(provider));
            when(scheduleRepository.findByProviderId(1L)).thenReturn(List.of(
                ProviderSchedule.availableSlot(1L, DayOfWeek.MONDAY,
                    LocalTime.of(8, 0), LocalTime.of(17, 0)),
                ProviderSchedule.blocked(1L, LocalDate.of(2026, 5, 1))
            ));

            ProviderAvailabilityResponse response = providerService.getAvailability(10L);

            assertThat(response.weeklySchedule()).hasSize(1);
            assertThat(response.weeklySchedule().get(0).day()).isEqualTo(UpdateAvailabilityRequest.DayOfWeek.MONDAY);
            assertThat(response.weeklySchedule().get(0).startTime()).isEqualTo("08:00");
            assertThat(response.blockedDates()).containsExactly(LocalDate.of(2026, 5, 1));
        }

        @Test
        @DisplayName("returns empty schedule when no schedule entries")
        void returnsEmptySchedule_WhenProviderExists() {
            Provider provider = makeProvider(1L, 10L, "dr@test.com");
            when(providerRepository.findByUserId(10L)).thenReturn(Optional.of(provider));
            when(scheduleRepository.findByProviderId(1L)).thenReturn(List.of());

            ProviderAvailabilityResponse response = providerService.getAvailability(10L);

            assertThat(response.weeklySchedule()).isEmpty();
            assertThat(response.blockedDates()).isEmpty();
        }

        @Test
        @DisplayName("throws ResourceNotFoundException when provider not found")
        void throwsNotFound_WhenProviderNotExists() {
            when(providerRepository.findByUserId(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> providerService.getAvailability(999L))
                .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    // ─── updateAvailability ───────────────────────────────────────────────────

    @Nested
    @DisplayName("updateAvailability()")
    class UpdateAvailability {

        @Test
        @DisplayName("validates provider exists before updating")
        void validatesProviderExists() {
            when(providerRepository.findByUserId(999L)).thenReturn(Optional.empty());

            UpdateAvailabilityRequest request = new UpdateAvailabilityRequest(
                List.of(new UpdateAvailabilityRequest.WeeklyScheduleItem(
                    UpdateAvailabilityRequest.DayOfWeek.MONDAY, "08:00", "17:00")),
                List.of(LocalDate.now().plusDays(5))
            );

            assertThatThrownBy(() -> providerService.updateAvailability(999L, request))
                .isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        @DisplayName("returns updated availability response with saved schedule")
        void returnsUpdatedAvailability() {
            Provider provider = makeProvider(1L, 10L, "dr@test.com");
            when(providerRepository.findByUserId(10L)).thenReturn(Optional.of(provider));

            UpdateAvailabilityRequest request = new UpdateAvailabilityRequest(
                List.of(
                    new UpdateAvailabilityRequest.WeeklyScheduleItem(
                        UpdateAvailabilityRequest.DayOfWeek.TUESDAY, "09:00", "16:00"),
                    new UpdateAvailabilityRequest.WeeklyScheduleItem(
                        UpdateAvailabilityRequest.DayOfWeek.WEDNESDAY, "09:00", "16:00")
                ),
                List.of(LocalDate.of(2026, 5, 1))
            );

            when(scheduleRepository.findByProviderId(1L)).thenReturn(List.of(
                ProviderSchedule.availableSlot(1L, DayOfWeek.TUESDAY,
                    LocalTime.of(9, 0), LocalTime.of(16, 0)),
                ProviderSchedule.availableSlot(1L, DayOfWeek.WEDNESDAY,
                    LocalTime.of(9, 0), LocalTime.of(16, 0)),
                ProviderSchedule.blocked(1L, LocalDate.of(2026, 5, 1))
            ));

            ProviderAvailabilityResponse response = providerService.updateAvailability(10L, request);

            assertThat(response.weeklySchedule()).hasSize(2);
            assertThat(response.blockedDates()).containsExactly(LocalDate.of(2026, 5, 1));

            verify(scheduleRepository).deleteByProviderIdAndIsBlockedFalse(1L);
            verify(scheduleRepository, times(3)).save(any(ProviderSchedule.class));
        }

        @Test
        @DisplayName("handles null schedule gracefully")
        void handlesNullSchedule() {
            Provider provider = makeProvider(1L, 10L, "dr@test.com");
            when(providerRepository.findByUserId(10L)).thenReturn(Optional.of(provider));
            when(scheduleRepository.findByProviderId(1L)).thenReturn(List.of());

            UpdateAvailabilityRequest request = new UpdateAvailabilityRequest(null, null);
            ProviderAvailabilityResponse response = providerService.updateAvailability(10L, request);

            assertThat(response.weeklySchedule()).isEmpty();
            assertThat(response.blockedDates()).isEmpty();

            verify(scheduleRepository).deleteByProviderIdAndIsBlockedFalse(1L);
        }
    }

    // ─── uploadDocument ───────────────────────────────────────────────────────

    @Nested
    @DisplayName("uploadDocument()")
    class UploadDocument {

        @Test
        @DisplayName("validates provider exists before uploading")
        void validatesProviderExists() {
            when(providerRepository.findByUserId(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> providerService.uploadDocument(999L, "LICENSE", new byte[0], "doc.pdf"))
                .isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        @DisplayName("returns document upload response with pending status")
        void returnsPendingStatus() throws Exception {
            Provider provider = makeProvider(1L, 10L, "dr@test.com");
            when(providerRepository.findByUserId(10L)).thenReturn(Optional.of(provider));
            when(documentStorageService.uploadDocument(any(), eq(1L), eq("LICENSE")))
                .thenReturn("providers/1/LICENSE/abc123.pdf");

            var response = providerService.uploadDocument(10L, "LICENSE", new byte[]{1, 2}, "license.pdf");

            assertThat(response.id()).isNotNull();
            assertThat(response.url()).contains("zdravdom-documents");
            assertThat(response.type()).isEqualTo("LICENSE");
            assertThat(response.status()).isEqualTo("PENDING_REVIEW");
        }
    }

    // ─── Helper ───────────────────────────────────────────────────────────────

    private Provider makeProvider(Long id, Long userId, String email) {
        Provider p = new Provider();
        p.setId(id);
        p.setEmail(email);
        p.setPhone("+38698765432");
        p.setFirstName("Marko");
        p.setLastName("Horvat");
        p.setRole(Role.PROVIDER);
        p.setProfession(Profession.NURSE);
        p.setSpecialty(Specialty.GENERAL_CARE);
        p.setRating(4.8);
        p.setReviewsCount(127);
        p.setLanguages(new Language[]{Language.SLOVENIAN, Language.ENGLISH});
        p.setYearsOfExperience(12);
        p.setBio("Experienced home healthcare nurse.");
        p.setStatus(ProviderStatus.ACTIVE);
        p.setVerified(true);
        p.setCreatedAt(LocalDateTime.now().minusYears(2));
        p.setUpdatedAt(LocalDateTime.now());
        return p;
    }
}
