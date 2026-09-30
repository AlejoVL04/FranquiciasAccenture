package com.example.franchiseapi.service;

import com.example.franchiseapi.dto.request.CreateBranchRequest;
import com.example.franchiseapi.dto.response.BranchResponse;
import com.example.franchiseapi.entity.Branch;
import com.example.franchiseapi.entity.Franchise;
import com.example.franchiseapi.exception.BusinessException;
import com.example.franchiseapi.exception.ResourceNotFoundException;
import com.example.franchiseapi.repository.BranchRepository;
import com.example.franchiseapi.repository.FranchiseRepository;
import com.example.franchiseapi.service.impl.BranchServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("BranchService")
class BranchServiceImplTest {

    @Mock
    private BranchRepository branchRepository;

    @Mock
    private FranchiseRepository franchiseRepository;

    @InjectMocks
    private BranchServiceImpl branchService;

    private Franchise franchise;

    @BeforeEach
    void setUp() {
        franchise = Franchise.builder().name("Franquicia Medellin").build();
        franchise.setId(1L);
        franchise.setCreatedAt(LocalDateTime.now());
        franchise.setUpdatedAt(LocalDateTime.now());
    }

    @Nested
    @DisplayName("create")
    class Create {

        @Test
        @DisplayName("persists the branch linked to its franchise")
        void createsBranch() {
            when(franchiseRepository.findById(1L)).thenReturn(Optional.of(franchise));
            when(branchRepository.existsByFranchiseIdAndNameIgnoreCase(1L, "Sucursal El Poblado"))
                    .thenReturn(false);
            when(branchRepository.save(any(Branch.class)))
                    .thenAnswer(invocation -> withIdAndTimestamps(invocation.getArgument(0), 5L));

            BranchResponse response = branchService.create(
                    1L, new CreateBranchRequest("Sucursal El Poblado"));

            assertThat(response.id()).isEqualTo(5L);
            assertThat(response.name()).isEqualTo("Sucursal El Poblado");
            assertThat(response.franchiseId()).isEqualTo(1L);

            ArgumentCaptor<Branch> captor = ArgumentCaptor.forClass(Branch.class);
            verify(branchRepository).save(captor.capture());
            assertThat(captor.getValue().getFranchise()).isSameAs(franchise);
        }

        @Test
        @DisplayName("trims surrounding whitespace before persisting")
        void trimsName() {
            when(franchiseRepository.findById(1L)).thenReturn(Optional.of(franchise));
            when(branchRepository.existsByFranchiseIdAndNameIgnoreCase(1L, "Sucursal Norte"))
                    .thenReturn(false);
            when(branchRepository.save(any(Branch.class)))
                    .thenAnswer(invocation -> withIdAndTimestamps(invocation.getArgument(0), 5L));

            branchService.create(1L, new CreateBranchRequest("  Sucursal Norte  "));

            ArgumentCaptor<Branch> captor = ArgumentCaptor.forClass(Branch.class);
            verify(branchRepository).save(captor.capture());
            assertThat(captor.getValue().getName()).isEqualTo("Sucursal Norte");
        }

        @Test
        @DisplayName("raises 404 when the franchise does not exist and never persists")
        void raisesNotFoundForUnknownFranchise() {
            when(franchiseRepository.findById(10L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> branchService.create(
                    10L, new CreateBranchRequest("Sucursal El Poblado")))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("Franchise with id 10 not found");

            verify(branchRepository, never()).save(any());
        }

        @Test
        @DisplayName("rejects a name already used inside the same franchise with 409 CONFLICT")
        void rejectsDuplicatedNameInSameFranchise() {
            when(franchiseRepository.findById(1L)).thenReturn(Optional.of(franchise));
            when(branchRepository.existsByFranchiseIdAndNameIgnoreCase(1L, "Sucursal El Poblado"))
                    .thenReturn(true);

            assertThatThrownBy(() -> branchService.create(
                    1L, new CreateBranchRequest("Sucursal El Poblado")))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("already exists in franchise 1")
                    .extracting(ex -> ((BusinessException) ex).getStatus())
                    .isEqualTo(HttpStatus.CONFLICT);

            verify(branchRepository, never()).save(any());
        }

        @Test
        @DisplayName("scopes the duplicate check to the franchise, so the same name is free elsewhere")
        void allowsSameNameInAnotherFranchise() {
            Franchise other = Franchise.builder().name("Franquicia Bogota").build();
            other.setId(2L);
            when(franchiseRepository.findById(2L)).thenReturn(Optional.of(other));
            when(branchRepository.existsByFranchiseIdAndNameIgnoreCase(2L, "Sucursal El Poblado"))
                    .thenReturn(false);
            when(branchRepository.save(any(Branch.class)))
                    .thenAnswer(invocation -> withIdAndTimestamps(invocation.getArgument(0), 9L));

            BranchResponse response = branchService.create(
                    2L, new CreateBranchRequest("Sucursal El Poblado"));

            assertThat(response.franchiseId()).isEqualTo(2L);
        }

        @Test
        @DisplayName("treats names differing only in case as duplicates")
        void duplicateCheckIsCaseInsensitive() {
            when(franchiseRepository.findById(1L)).thenReturn(Optional.of(franchise));
            when(branchRepository.existsByFranchiseIdAndNameIgnoreCase(anyLong(), anyString()))
                    .thenReturn(true);

            assertThatThrownBy(() -> branchService.create(
                    1L, new CreateBranchRequest("SUCURSAL EL POBLADO")))
                    .isInstanceOf(BusinessException.class);
        }
    }

    @Nested
    @DisplayName("findByFranchise")
    class FindByFranchise {

        @Test
        @DisplayName("returns the branches of the franchise ordered by id")
        void returnsBranches() {
            Branch north = withIdAndTimestamps(
                    Branch.builder().name("Sucursal Norte").franchise(franchise).build(), 1L);
            Branch south = withIdAndTimestamps(
                    Branch.builder().name("Sucursal Sur").franchise(franchise).build(), 2L);

            when(franchiseRepository.existsById(1L)).thenReturn(true);
            when(branchRepository.findByFranchiseIdOrderByIdAsc(1L)).thenReturn(List.of(north, south));

            assertThat(branchService.findByFranchise(1L))
                    .extracting(BranchResponse::name)
                    .containsExactly("Sucursal Norte", "Sucursal Sur");
        }

        @Test
        @DisplayName("returns an empty list for a franchise with no branches")
        void returnsEmptyList() {
            when(franchiseRepository.existsById(1L)).thenReturn(true);
            when(branchRepository.findByFranchiseIdOrderByIdAsc(1L)).thenReturn(List.of());

            assertThat(branchService.findByFranchise(1L)).isEmpty();
        }

        @Test
        @DisplayName("raises 404 for an unknown franchise")
        void raisesNotFoundForUnknownFranchise() {
            when(franchiseRepository.existsById(10L)).thenReturn(false);

            assertThatThrownBy(() -> branchService.findByFranchise(10L))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("Franchise with id 10 not found");
        }
    }

    private static Branch withIdAndTimestamps(Branch branch, Long id) {
        branch.setId(id);
        branch.setCreatedAt(LocalDateTime.now());
        branch.setUpdatedAt(LocalDateTime.now());
        return branch;
    }
}
