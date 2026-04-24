package com.zdravdom.user.application.mapper;

import com.zdravdom.user.application.dto.UpdatePatientRequest;
import com.zdravdom.user.domain.Patient;
import com.zdravdom.user.domain.Patient.EmergencyContact;
import com.zdravdom.user.domain.Patient.InsuranceDetails;

/**
 * Maps UpdatePatientRequest fields onto an existing Patient entity.
 * Only non-null fields from the request are applied — nulls are skipped
 * so existing values are preserved (partial update semantics).
 *
 * <p>This mapper is stateless and has no external dependencies — safe to use
 * directly in transactional service methods.
 */
public final class PatientMapper {

    private PatientMapper() {}

    /**
     * Apply all non-null fields from the request to the patient.
     * Handles embedded InsuranceDetails and EmergencyContact as well.
     */
    public static void updateFromRequest(Patient patient, UpdatePatientRequest r) {
        if (r == null) return;

        if (r.firstName() != null) {
            patient.setFirstName(r.firstName());
        }
        if (r.lastName() != null) {
            patient.setLastName(r.lastName());
        }
        if (r.phone() != null) {
            patient.setPhone(r.phone());
        }
        if (r.insuranceDetails() != null) {
            patient.setInsuranceDetails(new InsuranceDetails(
                r.insuranceDetails().insuranceProvider(),
                r.insuranceDetails().policyNumber(),
                r.insuranceDetails().groupNumber()
            ));
        }
        if (r.allergies() != null) {
            patient.setAllergies(r.allergies().toArray(new String[0]));
        }
        if (r.chronicConditions() != null) {
            patient.setChronicConditions(r.chronicConditions().toArray(new String[0]));
        }
        if (r.emergencyContact() != null) {
            patient.setEmergencyContact(new EmergencyContact(
                r.emergencyContact().name(),
                r.emergencyContact().phone(),
                r.emergencyContact().relationship()
            ));
        }
    }

    /**
     * Apply all non-null fields from an AddressRequest to the patient.
     */
    public static void updateAddressFromRequest(Patient patient,
            com.zdravdom.user.application.dto.AddressRequest r) {
        if (r == null) return;

        if (r.street() != null)    patient.setAddressStreet(r.street());
        if (r.houseNumber() != null)  patient.setAddressHouseNumber(r.houseNumber());
        if (r.apartmentNumber() != null) patient.setAddressApartmentNumber(r.apartmentNumber());
        if (r.city() != null)      patient.setAddressCity(r.city());
        if (r.postalCode() != null) patient.setAddressPostalCode(r.postalCode());
        if (r.region() != null)    patient.setAddressRegion(r.region());
        if (r.country() != null)   patient.setAddressCountry(r.country());
        else patient.setAddressCountry("SI"); // default
        if (r.instructions() != null) patient.setAddressInstructions(r.instructions());
    }
}
