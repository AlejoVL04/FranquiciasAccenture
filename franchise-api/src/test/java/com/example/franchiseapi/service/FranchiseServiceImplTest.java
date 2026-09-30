package com.example.franchiseapi.service;

import com.example.franchiseapi.dto.request.CreateFranchiseRequest;
import com.example.franchiseapi.dto.response.FranchiseResponse;
import com.example.franchiseapi.dto.response.TopStockProductResponse;
import com.example.franchiseapi.entity.Franchise;
import com.example.franchiseapi.exception.BusinessException;
import com.example.franchiseapi.exception.ResourceNotFoundException;
import com.example.franchiseapi.repository.FranchiseRepository;
import com.example.franchiseapi.repository.ProductRepository;
import com.example.franchiseapi.repository.projection.TopStockProductProjection;
import com.example.franchiseapi.service.impl.FranchiseServiceImpl;
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
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("FranchiseService")
class FranchiseServiceImplTest {

    @Mock
    private FranchiseRepository franchiseRepository;

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private FranchiseServiceImpl franchiseService;

    @Nested
    @DisplayName("create")
    class Create {

        @Test
        @DisplayName("persists the franchise and returns it with its generated id")
        void createsFranchise() {
            when(franchiseRepository.existsByNameIgnoreCase("Franquicia Medellin")).thenReturn(false);
            when(franchiseRepository.save(any(Franchise.class)))
                    .thenAnswer(invocation -> withIdAndTimestamps(invocation.getArgument(0), 1L));

            FranchiseResponse response = franchiseService.create(
                    new CreateFranchiseRequest("Franquicia Medellin"));

            assertThat(response.id()).isEqualTo(1L);
            assertThat(response.name()).isEqualTo("Franquicia Medellin");
            assertThat(response.createdAt()).isNotNull();
            assertThat(response.updatedAt()).isNotNull();
        }

        @Test
        @DisplayName("trims surrounding whitespace before persisting")
        void trimsName() {
            when(franchiseRepository.existsByNameIgnoreCase("Franquicia A")).thenReturn(false);
            when(franchiseRepository.save(any(Franchise.class)))
                    .thenAnswer(invocation -> withIdAndTimestamps(invocation.getArgument(0), 7L));

            franchiseService.create(new CreateFranchiseRequest("   Franquicia A   "));

            ArgumentCaptor<Franchise> captor = ArgumentCaptor.forClass(Franchise.class);
            verify(franchiseRepository).save(captor.capture());
            assertThat(captor.getValue().getName()).isEqualTo("Franquicia A");
        }

        @Test
        @DisplayName("rejects a duplicated name with 409 CONFLICT and does not persist")
        void rejectsDuplicatedName() {
            when(franchiseRepository.existsByNameIgnoreCase("Franquicia Medellin")).thenReturn(true);

            assertThatThrownBy(() -> franchiseService.create(
                    new CreateFranchiseRequest("Franquicia Medellin")))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("already exists")
                    .extracting(ex -> ((BusinessException) ex).getStatus())
                    .isEqualTo(HttpStatus.CONFLICT);

            verify(franchiseRepository, never()).save(any());
        }

        @Test
        @DisplayName("treats names differing only in case as duplicates")
        void duplicateCheckIsCaseInsensitive() {
            when(franchiseRepository.existsByNameIgnoreCase(anyString())).thenReturn(true);

            assertThatThrownBy(() -> franchiseService.create(new CreateFranchiseRequest("FRANQUICIA A")))
                    .isInstanceOf(BusinessException.class);
        }
    }

    @Nested
    @DisplayName("findById")
    class FindById {

        @Test
        @DisplayName("returns the franchise when it exists")
        void returnsFranchise() {
            Franchise franchise = withIdAndTimestamps(
                    Franchise.builder().name("Franquicia A").build(), 3L);
            when(franchiseRepository.findById(3L)).thenReturn(Optional.of(franchise));

            assertThat(franchiseService.findById(3L).name()).isEqualTo("Franquicia A");
        }

        @Test
        @DisplayName("raises 404 for an unknown franchise")
        void raisesNotFound() {
            when(franchiseRepository.findById(10L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> franchiseService.findById(10L))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("Franchise with id 10 not found");
        }
    }

    @Nested
    @DisplayName("findAll")
    class FindAll {

        @Test
        @DisplayName("returns every franchise ordered by id")
        void returnsAll() {
            when(franchiseRepository.findAllByOrderByIdAsc()).thenReturn(List.of(
                    withIdAndTimestamps(Franchise.builder().name("A").build(), 1L),
                    withIdAndTimestamps(Franchise.builder().name("B").build(), 2L)));

            assertThat(franchiseService.findAll())
                    .extracting(FranchiseResponse::name)
                    .containsExactly("A", "B");
        }

        @Test
        @DisplayName("returns an empty list when there are no franchises")
        void returnsEmptyList() {
            when(franchiseRepository.findAllByOrderByIdAsc()).thenReturn(List.of());

            assertThat(franchiseService.findAll()).isEmpty();
        }
    }

    @Nested
    @DisplayName("findTopStockProductPerBranch")
    class TopStock {

        @Test
        @DisplayName("maps one entry per branch for a franchise with several branches")
        void mapsOneEntryPerBranch() {
            when(franchiseRepository.existsById(1L)).thenReturn(true);
            when(productRepository.findTopStockProductPerBranch(1L)).thenReturn(List.of(
                    row(1L, "Franquicia A", 1L, "Sucursal Norte", 10L, "Mouse", 50),
                    row(1L, "Franquicia A", 2L, "Sucursal Centro", 14L, "Laptop", 40),
                    row(1L, "Franquicia A", 3L, "Sucursal Sur", 20L, "Monitor", 30)));

            List<TopStockProductResponse> result =
                    franchiseService.findTopStockProductPerBranch(1L, false);

            assertThat(result).hasSize(3);
            assertThat(result)
                    .extracting(TopStockProductResponse::branchName,
                            TopStockProductResponse::productName,
                            TopStockProductResponse::stock)
                    .containsExactly(
                            org.assertj.core.groups.Tuple.tuple("Sucursal Norte", "Mouse", 50),
                            org.assertj.core.groups.Tuple.tuple("Sucursal Centro", "Laptop", 40),
                            org.assertj.core.groups.Tuple.tuple("Sucursal Sur", "Monitor", 30));
            assertThat(result).allSatisfy(entry -> {
                assertThat(entry.franchiseId()).isEqualTo(1L);
                assertThat(entry.franchiseName()).isEqualTo("Franquicia A");
            });
        }

        @Test
        @DisplayName("omits branches without products by default")
        void omitsEmptyBranchesByDefault() {
            when(franchiseRepository.existsById(1L)).thenReturn(true);
            when(productRepository.findTopStockProductPerBranch(1L)).thenReturn(List.of(
                    row(1L, "Franquicia A", 1L, "Sucursal Norte", 10L, "Mouse", 50),
                    row(1L, "Franquicia A", 2L, "Sucursal Vacia", null, null, null)));

            List<TopStockProductResponse> result =
                    franchiseService.findTopStockProductPerBranch(1L, false);

            assertThat(result)
                    .singleElement()
                    .extracting(TopStockProductResponse::branchName)
                    .isEqualTo("Sucursal Norte");
        }

        @Test
        @DisplayName("reports branches without products with null product fields when asked to")
        void reportsEmptyBranchesWhenRequested() {
            when(franchiseRepository.existsById(1L)).thenReturn(true);
            when(productRepository.findTopStockProductPerBranch(1L)).thenReturn(List.of(
                    row(1L, "Franquicia A", 1L, "Sucursal Norte", 10L, "Mouse", 50),
                    row(1L, "Franquicia A", 2L, "Sucursal Vacia", null, null, null)));

            List<TopStockProductResponse> result =
                    franchiseService.findTopStockProductPerBranch(1L, true);

            assertThat(result).hasSize(2);
            TopStockProductResponse empty = result.get(1);
            assertThat(empty.branchName()).isEqualTo("Sucursal Vacia");
            assertThat(empty.productId()).isNull();
            assertThat(empty.productName()).isNull();
            assertThat(empty.stock()).isNull();
        }

        @Test
        @DisplayName("keeps a product whose stock is 0, since zero is a valid maximum")
        void keepsZeroStockProduct() {
            when(franchiseRepository.existsById(1L)).thenReturn(true);
            when(productRepository.findTopStockProductPerBranch(1L)).thenReturn(List.of(
                    row(1L, "Franquicia A", 1L, "Sucursal Agotada", 10L, "Mouse", 0)));

            List<TopStockProductResponse> result =
                    franchiseService.findTopStockProductPerBranch(1L, false);

            assertThat(result).singleElement()
                    .satisfies(entry -> {
                        assertThat(entry.stock()).isZero();
                        assertThat(entry.productId()).isEqualTo(10L);
                    });
        }

        @Test
        @DisplayName("returns the single row the query resolved on a stock tie")
        void returnsSingleRowOnTie() {
            // The tie is broken inside SQL by "id ASC", so the service receives one
            // row per branch already. This asserts the service does not re-expand it.
            when(franchiseRepository.existsById(1L)).thenReturn(true);
            when(productRepository.findTopStockProductPerBranch(1L)).thenReturn(List.of(
                    row(1L, "Franquicia A", 1L, "Sucursal Norte", 5L, "Mouse", 50)));

            List<TopStockProductResponse> result =
                    franchiseService.findTopStockProductPerBranch(1L, false);

            assertThat(result).singleElement()
                    .extracting(TopStockProductResponse::productId)
                    .isEqualTo(5L);
        }

        @Test
        @DisplayName("returns an empty list for a franchise that has no branches")
        void returnsEmptyForFranchiseWithoutBranches() {
            when(franchiseRepository.existsById(1L)).thenReturn(true);
            when(productRepository.findTopStockProductPerBranch(1L)).thenReturn(List.of());

            assertThat(franchiseService.findTopStockProductPerBranch(1L, false)).isEmpty();
        }

        @Test
        @DisplayName("raises 404 for an unknown franchise instead of returning an empty list")
        void raisesNotFoundForUnknownFranchise() {
            when(franchiseRepository.existsById(99L)).thenReturn(false);

            assertThatThrownBy(() -> franchiseService.findTopStockProductPerBranch(99L, false))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("Franchise with id 99 not found");

            verify(productRepository, never()).findTopStockProductPerBranch(any());
        }
    }

    private static Franchise withIdAndTimestamps(Franchise franchise, Long id) {
        franchise.setId(id);
        franchise.setCreatedAt(LocalDateTime.now());
        franchise.setUpdatedAt(LocalDateTime.now());
        return franchise;
    }

    /**
     * Stand-in for the interface-based projection Spring Data materialises from
     * the native query, so the service can be exercised without a database.
     */
    private static TopStockProductProjection row(Long franchiseId, String franchiseName,
                                                 Long branchId, String branchName,
                                                 Long productId, String productName, Integer stock) {
        return new TopStockProductProjection() {
            @Override
            public Long getFranchiseId() {
                return franchiseId;
            }

            @Override
            public String getFranchiseName() {
                return franchiseName;
            }

            @Override
            public Long getBranchId() {
                return branchId;
            }

            @Override
            public String getBranchName() {
                return branchName;
            }

            @Override
            public Long getProductId() {
                return productId;
            }

            @Override
            public String getProductName() {
                return productName;
            }

            @Override
            public Integer getStock() {
                return stock;
            }
        };
    }
}
