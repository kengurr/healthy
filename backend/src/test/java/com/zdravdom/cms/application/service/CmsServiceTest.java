package com.zdravdom.cms.application.service;

import com.zdravdom.cms.adapters.out.persistence.ServicePackageRepository;
import com.zdravdom.cms.adapters.out.persistence.ServiceRepository;
import com.zdravdom.cms.application.dto.PackageResponse;
import com.zdravdom.cms.application.dto.ServiceListResponse;
import com.zdravdom.cms.application.dto.ServiceResponse;
import com.zdravdom.cms.domain.Service;
import com.zdravdom.cms.domain.ServicePackage;
import com.zdravdom.global.exception.GlobalExceptionHandler.ResourceNotFoundException;
import com.zdravdom.global.testing.TestReflectionUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CmsServiceTest {

    @Mock private ServiceRepository serviceRepository;
    @Mock private ServicePackageRepository packageRepository;

    private CmsService cmsService;

    @BeforeEach
    void setUp() {
        cmsService = new CmsService(serviceRepository, packageRepository);
    }

    // ─── getServices ──────────────────────────────────────────────────────────

    @Nested
    @DisplayName("getServices()")
    class GetServices {

        @Test
        @DisplayName("returns all active services when no filters")
        void returnsAllActiveServices() {
            Service s1 = createService(1L, "Nursing Visit", Service.ServiceCategory.NURSING_CARE);
            Service s2 = createService(2L, "Physiotherapy", Service.ServiceCategory.PHYSIOTHERAPY);
            when(serviceRepository.findByActiveTrue()).thenReturn(List.of(s1, s2));

            ServiceListResponse response = cmsService.getServices(null, null, 0, 10);

            assertThat(response.content()).hasSize(2);
            assertThat(response.content().get(0).name()).isEqualTo("Nursing Visit");
        }

        @Test
        @DisplayName("filters by category")
        void filtersByCategory() {
            Service s1 = createService(1L, "Home Nursing", Service.ServiceCategory.NURSING_CARE);
            when(serviceRepository.findByCategory(Service.ServiceCategory.NURSING_CARE))
                .thenReturn(List.of(s1));

            ServiceListResponse response = cmsService.getServices(Service.ServiceCategory.NURSING_CARE, null, 0, 10);

            assertThat(response.content()).hasSize(1);
            assertThat(response.content().get(0).category()).isEqualTo(Service.ServiceCategory.NURSING_CARE);
        }

        @Test
        @DisplayName("filters by search term (name contains)")
        void filtersBySearchTerm() {
            Service s1 = createService(1L, "Blood Test", Service.ServiceCategory.LABORATORY_SERVICES);
            when(serviceRepository.findByNameContainingIgnoreCase("blood"))
                .thenReturn(List.of(s1));

            ServiceListResponse response = cmsService.getServices(null, "blood", 0, 10);

            assertThat(response.content()).hasSize(1);
            assertThat(response.content().get(0).name()).isEqualTo("Blood Test");
        }

        @Test
        @DisplayName("filters by both category and search")
        void filtersByCategoryAndSearch() {
            Service s1 = createService(1L, "Home Blood Test", Service.ServiceCategory.LABORATORY_SERVICES);
            when(serviceRepository.findByCategory(Service.ServiceCategory.LABORATORY_SERVICES))
                .thenReturn(List.of(s1));

            ServiceListResponse response = cmsService.getServices(Service.ServiceCategory.LABORATORY_SERVICES, "home", 0, 10);

            assertThat(response.content()).hasSize(1);
            assertThat(response.content().get(0).name()).isEqualTo("Home Blood Test");
        }
    }

    // ─── getServiceById ───────────────────────────────────────────────────────

    @Nested
    @DisplayName("getServiceById()")
    class GetServiceById {

        @Test
        @DisplayName("returns service by UUID")
        void returnsServiceByUuid() {
            UUID uuid = UUID.randomUUID();
            Service service = createService(1L, "Nursing", Service.ServiceCategory.NURSING_CARE);
            when(serviceRepository.findByUuid(uuid)).thenReturn(Optional.of(service));

            ServiceResponse response = cmsService.getServiceById(uuid);

            assertThat(response.name()).isEqualTo("Nursing");
        }

        @Test
        @DisplayName("throws when service not found")
        void throwsWhenNotFound() {
            when(serviceRepository.findByUuid(any(UUID.class))).thenReturn(Optional.empty());

            assertThatThrownBy(() -> cmsService.getServiceById(UUID.randomUUID()))
                .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    // ─── getServicePackages ────────────────────────────────────────────────────

    @Nested
    @DisplayName("getServicePackages()")
    class GetServicePackages {

        @Test
        @DisplayName("returns packages for a service")
        void returnsPackagesForService() {
            UUID serviceUuid = UUID.randomUUID();
            Service service = createService(1L, "Nursing", Service.ServiceCategory.NURSING_CARE);
            when(serviceRepository.findByUuid(serviceUuid)).thenReturn(Optional.of(service));

            ServicePackage pkg = TestReflectionUtil.newInstance(ServicePackage.class);
            setId(pkg, 1L);
            pkg.setName("Monthly Package");
            pkg.setDescription("Monthly nursing visits");
            pkg.setPrice(BigDecimal.valueOf(299));
            pkg.setDiscountPercent(BigDecimal.valueOf(10));
            pkg.setValidityDays(30);
            pkg.setSize(ServicePackage.PackageSize.M);

            when(packageRepository.findByServiceId(1L)).thenReturn(List.of(pkg));

            List<PackageResponse> packages = cmsService.getServicePackages(serviceUuid);

            assertThat(packages).hasSize(1);
            assertThat(packages.get(0).name()).isEqualTo("Monthly Package");
        }

        @Test
        @DisplayName("returns all active packages when serviceId is null")
        void returnsAllActiveWhenNoServiceId() {
            ServicePackage pkg = TestReflectionUtil.newInstance(ServicePackage.class);
            setId(pkg, 1L);
            pkg.setName("Standard");
            pkg.setActive(true);
            pkg.setSize(ServicePackage.PackageSize.S);
            when(packageRepository.findByActiveTrue()).thenReturn(List.of(pkg));

            List<PackageResponse> packages = cmsService.getServicePackages(null);

            assertThat(packages).hasSize(1);
        }
    }

    // ─── Helpers ───────────────────────────────────────────────────────────────

    private Service createService(Long id, String name, Service.ServiceCategory category) {
        Service s = Service.create();
        s.setId(id);
        s.setName(name);
        s.setCategory(category);
        s.setDescription("Test service");
        s.setDurationMinutes(60);
        s.setPrice(BigDecimal.valueOf(50));
        s.setActive(true);
        return s;
    }

    private void setId(ServicePackage pkg, Long id) {
        TestReflectionUtil.setId(pkg, id);
    }
}
