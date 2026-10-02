package com.example.franchiseapi.service;

import com.example.franchiseapi.dto.request.CreateBranchRequest;
import com.example.franchiseapi.dto.request.UpdateNameRequest;
import com.example.franchiseapi.dto.response.BranchResponse;
import com.example.franchiseapi.entity.Branch;
import com.example.franchiseapi.entity.Franchise;
import com.example.franchiseapi.exception.BusinessException;
import com.example.franchiseapi.exception.ResourceNotFoundException;
import com.example.franchiseapi.repository.BranchRepository;
import com.example.franchiseapi.repository.FranchiseRepository;
import com.example.franchiseapi.service.impl.BranchServiceImpl;
import com.example.franchiseapi.support.ProcedureFailures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

/**
 * The business rules are enforced by the stored procedures and covered against
 * MySQL in the integration tests. These tests check that the service calls the
 * right procedure and turns its errors into the right exceptions.
 */
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
        @DisplayName("returns the branch stored by the procedure, linked to its franchise")
        void createsBranch() {
            when(branchRepository.create(1L, "Sucursal El Poblado"))
                    .thenReturn(branch(5L, "Sucursal El Poblado"));

            BranchResponse response = branchService.create(
                    1L, new CreateBranchRequest("Sucursal El Poblado"));

            assertThat(response.id()).isEqualTo(5L);
            assertThat(response.name()).isEqualTo("Sucursal El Poblado");
            assertThat(response.franchiseId()).isEqualTo(1L);
        }

        @Test
        @DisplayName("raises 404 when the procedure reports an unknown franchise")
        void raisesNotFoundForUnknownFranchise() {
            when(branchRepository.create(10L, "Sucursal El Poblado"))
                    .thenThrow(ProcedureFailures.notFound("Franchise with id 10 not found"));

            assertThatThrownBy(() -> branchService.create(
                    10L, new CreateBranchRequest("Sucursal El Poblado")))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("Franchise with id 10 not found");
        }

        @Test
        @DisplayName("raises 409 when the procedure reports a name already used in the franchise")
        void rejectsDuplicatedName() {
            when(branchRepository.create(1L, "Sucursal El Poblado"))
                    .thenThrow(ProcedureFailures.conflict(
                            "A branch named 'Sucursal El Poblado' already exists in franchise 1"));

            assertThatThrownBy(() -> branchService.create(
                    1L, new CreateBranchRequest("Sucursal El Poblado")))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("already exists in franchise 1")
                    .extracting(ex -> ((BusinessException) ex).getStatus())
                    .isEqualTo(HttpStatus.CONFLICT);
        }
    }

    @Nested
    @DisplayName("updateName")
    class UpdateName {

        @Test
        @DisplayName("returns the branch renamed by the procedure")
        void renamesBranch() {
            when(branchRepository.updateName(5L, "Sucursal Centro"))
                    .thenReturn(branch(5L, "Sucursal Centro"));

            BranchResponse response = branchService.updateName(5L, new UpdateNameRequest("Sucursal Centro"));

            assertThat(response.id()).isEqualTo(5L);
            assertThat(response.name()).isEqualTo("Sucursal Centro");
            assertThat(response.franchiseId()).isEqualTo(1L);
        }

        @Test
        @DisplayName("raises 404 when the procedure reports an unknown branch")
        void raisesNotFoundForUnknownBranch() {
            when(branchRepository.updateName(50L, "Otra"))
                    .thenThrow(ProcedureFailures.notFound("Branch with id 50 not found"));

            assertThatThrownBy(() -> branchService.updateName(50L, new UpdateNameRequest("Otra")))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("Branch with id 50 not found");
        }

        @Test
        @DisplayName("raises 409 when the procedure reports a name used by a sibling branch")
        void rejectsNameOfSiblingBranch() {
            when(branchRepository.updateName(5L, "Sucursal Sur"))
                    .thenThrow(ProcedureFailures.conflict(
                            "A branch named 'Sucursal Sur' already exists in franchise 1"));

            assertThatThrownBy(() -> branchService.updateName(5L, new UpdateNameRequest("Sucursal Sur")))
                    .isInstanceOf(BusinessException.class)
                    .extracting(ex -> ((BusinessException) ex).getStatus())
                    .isEqualTo(HttpStatus.CONFLICT);
        }
    }

    @Nested
    @DisplayName("findByFranchise")
    class FindByFranchise {

        @Test
        @DisplayName("returns the branches of the franchise ordered by id")
        void returnsBranches() {
            when(franchiseRepository.existsById(1L)).thenReturn(true);
            when(branchRepository.findByFranchiseIdOrderByIdAsc(1L))
                    .thenReturn(List.of(branch(1L, "Sucursal Norte"), branch(2L, "Sucursal Sur")));

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

    private Branch branch(Long id, String name) {
        Branch branch = Branch.builder().name(name).franchise(franchise).build();
        branch.setId(id);
        branch.setCreatedAt(LocalDateTime.now());
        branch.setUpdatedAt(LocalDateTime.now());
        return branch;
    }
}
