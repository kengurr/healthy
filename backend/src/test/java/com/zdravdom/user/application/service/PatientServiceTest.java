package com.zdravdom.user.application.service;

import com.zdravdom.global.exception.GlobalExceptionHandler.ResourceNotFoundException;
import com.zdravdom.user.adapters.out.persistence.PatientRepository;
import com.zdravdom.user.application.dto.AddressRequest;
import com.zdravdom.user.application.dto.PatientResponse;
import com.zdravdom.user.application.dto.UpdatePatientRequest;
import com.zdravdom.user.domain.Patient;
import com.zdravdom.user.domain.Patient.EmergencyContact;
import com.zdravdom.user.domain.Patient.Gender;
import com.zdravdom.user.domain.Patient.InsuranceDetails;
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
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PatientServiceTest {

    @Mock private PatientRepository patientRepository;

    private PatientService patientService;

    @BeforeEach
    void setUp() {
        patientService = new PatientService(patientRepository);
    }

    // ─── getPatientByUserId ───────────────────────────────────────────────────

    @Nested
    @DisplayName("getPatientByUserId()")
    class GetPatientByUserId {

        @Test
        @DisplayName("returns patient response when found")
        void returnsPatient_WhenFound() {
            Patient patient = makePatient(1L, 10L, "patient@test.com");
            when(patientRepository.findByUserId(10L)).thenReturn(Optional.of(patient));

            PatientResponse response = patientService.getPatientByUserId(10L);

            assertThat(response.id()).isEqualTo(1L);
            assertThat(response.email()).isEqualTo("patient@test.com");
            assertThat(response.firstName()).isEqualTo("Janez");
            assertThat(response.lastName()).isEqualTo("Novak");
        }

        @Test
        @DisplayName("throws ResourceNotFoundException when not found")
        void throwsNotFound_WhenNotExists() {
            when(patientRepository.findByUserId(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> patientService.getPatientByUserId(999L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Patient");
        }
    }

    // ─── updatePatient ─────────────────────────────────────────────────────────

    @Nested
    @DisplayName("updatePatient()")
    class UpdatePatient {

        @Test
        @DisplayName("updates all provided fields")
        void updatesAllProvidedFields() {
            Patient patient = makePatient(1L, 10L, "old@test.com");
            when(patientRepository.findByUserId(10L)).thenReturn(Optional.of(patient));
            when(patientRepository.save(any(Patient.class))).thenAnswer(i -> i.getArgument(0));

            UpdatePatientRequest request = new UpdatePatientRequest(
                "NewFirst", "NewLast", "+38699999999",
                new UpdatePatientRequest.InsuranceDetailsRequest("NewInsurance", "POL999", "GRP999"),
                List.of("Peanuts"),
                List.of("Diabetes"),
                new UpdatePatientRequest.EmergencyContactRequest("Jože Novak", "+386111", "Brother")
            );

            PatientResponse response = patientService.updatePatient(10L, request);

            ArgumentCaptor<Patient> captor = ArgumentCaptor.forClass(Patient.class);
            verify(patientRepository).save(captor.capture());

            Patient saved = captor.getValue();
            assertThat(saved.getFirstName()).isEqualTo("NewFirst");
            assertThat(saved.getLastName()).isEqualTo("NewLast");
            assertThat(saved.getPhone()).isEqualTo("+38699999999");
            assertThat(saved.getInsuranceDetails().getInsuranceProvider()).isEqualTo("NewInsurance");
            assertThat(List.of(saved.getAllergies())).containsExactly("Peanuts");
            assertThat(List.of(saved.getChronicConditions())).containsExactly("Diabetes");
            assertThat(saved.getEmergencyContact().getName()).isEqualTo("Jože Novak");
        }

        @Test
        @DisplayName("does not overwrite fields when null in request")
        void leavesFieldsNull_WhenNotProvided() {
            Patient patient = makePatient(1L, 10L, "patient@test.com");
            patient.setFirstName("Original");
            when(patientRepository.findByUserId(10L)).thenReturn(Optional.of(patient));
            when(patientRepository.save(any(Patient.class))).thenAnswer(i -> i.getArgument(0));

            UpdatePatientRequest request = new UpdatePatientRequest(
                null, null, null, null, null, null, null
            );

            PatientResponse response = patientService.updatePatient(10L, request);

            assertThat(response.firstName()).isEqualTo("Original");
        }

        @Test
        @DisplayName("throws ResourceNotFoundException when patient not found")
        void throwsNotFound_WhenPatientNotExists() {
            when(patientRepository.findByUserId(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> patientService.updatePatient(999L,
                new UpdatePatientRequest(null, null, null, null, null, null, null)))
                .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    // ─── getAddresses ─────────────────────────────────────────────────────────

    @Nested
    @DisplayName("getAddresses()")
    class GetAddresses {

        @Test
        @DisplayName("returns address list when patient has address")
        void returnsAddresses_WhenPatientHasAddress() {
            Patient patient = makePatient(1L, 10L, "patient@test.com");
            patient.setAddressStreet("Ljubljanska 10");
            patient.setAddressCity("Ljubljana");
            patient.setAddressPostalCode("1000");
            when(patientRepository.findByUserId(10L)).thenReturn(Optional.of(patient));

            var addresses = patientService.getAddresses(10L);

            assertThat(addresses).hasSize(1);
            assertThat(addresses.get(0).street()).isEqualTo("Ljubljanska 10");
            assertThat(addresses.get(0).city()).isEqualTo("Ljubljana");
        }

        @Test
        @DisplayName("returns empty list when patient has no address")
        void returnsEmptyList_WhenNoAddress() {
            Patient patient = makePatient(1L, 10L, "patient@test.com");
            when(patientRepository.findByUserId(10L)).thenReturn(Optional.of(patient));

            var addresses = patientService.getAddresses(10L);

            assertThat(addresses).isEmpty();
        }

        @Test
        @DisplayName("throws ResourceNotFoundException when patient not found")
        void throwsNotFound_WhenPatientNotExists() {
            when(patientRepository.findByUserId(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> patientService.getAddresses(999L))
                .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    // ─── addAddress ───────────────────────────────────────────────────────────

    @Nested
    @DisplayName("addAddress()")
    class AddAddress {

        @Test
        @DisplayName("saves all address fields and returns response")
        void savesAllAddressFields() {
            Patient patient = makePatient(1L, 10L, "patient@test.com");
            when(patientRepository.findByUserId(10L)).thenReturn(Optional.of(patient));
            when(patientRepository.save(any(Patient.class))).thenAnswer(i -> i.getArgument(0));

            AddressRequest request = new AddressRequest(
                "Home", "Dunajska 5", "15", "3",
                "Maribor", "2000", "Podravska", "SI",
                null, null, "Ring bell", true
            );

            var response = patientService.addAddress(10L, request);

            ArgumentCaptor<Patient> captor = ArgumentCaptor.forClass(Patient.class);
            verify(patientRepository).save(captor.capture());

            Patient saved = captor.getValue();
            assertThat(saved.getAddressStreet()).isEqualTo("Dunajska 5");
            assertThat(saved.getAddressHouseNumber()).isEqualTo("15");
            assertThat(saved.getAddressApartmentNumber()).isEqualTo("3");
            assertThat(saved.getAddressCity()).isEqualTo("Maribor");
            assertThat(saved.getAddressPostalCode()).isEqualTo("2000");
            assertThat(saved.getAddressRegion()).isEqualTo("Podravska");
            assertThat(saved.getAddressCountry()).isEqualTo("SI");
            assertThat(saved.getAddressInstructions()).isEqualTo("Ring bell");
        }

        @Test
        @DisplayName("defaults country to SI when not provided")
        void defaultsCountryToSI() {
            Patient patient = makePatient(1L, 10L, "patient@test.com");
            when(patientRepository.findByUserId(10L)).thenReturn(Optional.of(patient));
            when(patientRepository.save(any(Patient.class))).thenAnswer(i -> i.getArgument(0));

            AddressRequest request = new AddressRequest(
                "Home", "Celjska 1", null, null,
                "Celje", "3000", null, null,
                null, null, null, false
            );

            patientService.addAddress(10L, request);

            ArgumentCaptor<Patient> captor = ArgumentCaptor.forClass(Patient.class);
            verify(patientRepository).save(captor.capture());
            assertThat(captor.getValue().getAddressCountry()).isEqualTo("SI");
        }
    }

    // ─── exportGdprData ───────────────────────────────────────────────────────

    @Nested
    @DisplayName("exportGdprData()")
    class ExportGdpr {

        @Test
        @DisplayName("returns job with PROCESSING status")
        void returnsJobWithProcessingStatus() {
            Patient patient = makePatient(1L, 10L, "patient@test.com");
            when(patientRepository.findByUserId(10L)).thenReturn(Optional.of(patient));

            var response = patientService.exportGdprData(10L);

            assertThat(response.status()).isEqualTo("PROCESSING");
            assertThat(response.jobId()).isNotNull();
        }

        @Test
        @DisplayName("throws ResourceNotFoundException when patient not found")
        void throwsNotFound_WhenPatientNotExists() {
            when(patientRepository.findByUserId(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> patientService.exportGdprData(999L))
                .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    // ─── Helper ───────────────────────────────────────────────────────────────

    private Patient makePatient(Long id, Long userId, String email) {
        Patient p = new Patient();
        p.setId(id);
        p.setEmail(email);
        p.setPhone("+38612345678");
        p.setFirstName("Janez");
        p.setLastName("Novak");
        p.setDateOfBirth(LocalDate.of(1985, 3, 15));
        p.setGender(Gender.MALE);
        p.setActive(true);
        p.setVerified(true);
        p.setCreatedAt(LocalDateTime.now().minusMonths(6));
        p.setUpdatedAt(LocalDateTime.now());
        return p;
    }
}
