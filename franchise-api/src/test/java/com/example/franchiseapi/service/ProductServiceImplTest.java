package com.example.franchiseapi.service;

import com.example.franchiseapi.dto.request.CreateProductRequest;
import com.example.franchiseapi.dto.request.UpdateStockRequest;
import com.example.franchiseapi.dto.response.ProductResponse;
import com.example.franchiseapi.entity.Branch;
import com.example.franchiseapi.entity.Franchise;
import com.example.franchiseapi.entity.Product;
import com.example.franchiseapi.exception.BusinessException;
import com.example.franchiseapi.exception.ResourceNotFoundException;
import com.example.franchiseapi.repository.BranchRepository;
import com.example.franchiseapi.repository.ProductRepository;
import com.example.franchiseapi.service.impl.ProductServiceImpl;
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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("ProductService")
class ProductServiceImplTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private BranchRepository branchRepository;

    @InjectMocks
    private ProductServiceImpl productService;

    private Branch branch;

    @BeforeEach
    void setUp() {
        Franchise franchise = Franchise.builder().name("Franquicia A").build();
        franchise.setId(1L);

        branch = Branch.builder().name("Sucursal Norte").franchise(franchise).build();
        branch.setId(1L);
        branch.setCreatedAt(LocalDateTime.now());
        branch.setUpdatedAt(LocalDateTime.now());
    }

    @Nested
    @DisplayName("create")
    class Create {

        @Test
        @DisplayName("persists the product linked to its branch")
        void createsProduct() {
            when(branchRepository.findById(1L)).thenReturn(Optional.of(branch));
            when(productRepository.existsByBranchIdAndNameIgnoreCase(1L, "Laptop Lenovo"))
                    .thenReturn(false);
            when(productRepository.save(any(Product.class)))
                    .thenAnswer(invocation -> withIdAndTimestamps(invocation.getArgument(0), 1L));

            ProductResponse response = productService.create(
                    1L, new CreateProductRequest("Laptop Lenovo", 25));

            assertThat(response.id()).isEqualTo(1L);
            assertThat(response.name()).isEqualTo("Laptop Lenovo");
            assertThat(response.stock()).isEqualTo(25);
            assertThat(response.branchId()).isEqualTo(1L);
        }

        @Test
        @DisplayName("accepts an initial stock of 0")
        void acceptsZeroStock() {
            when(branchRepository.findById(1L)).thenReturn(Optional.of(branch));
            when(productRepository.existsByBranchIdAndNameIgnoreCase(1L, "Mouse")).thenReturn(false);
            when(productRepository.save(any(Product.class)))
                    .thenAnswer(invocation -> withIdAndTimestamps(invocation.getArgument(0), 2L));

            assertThat(productService.create(1L, new CreateProductRequest("Mouse", 0)).stock())
                    .isZero();
        }

        @Test
        @DisplayName("raises 404 when the branch does not exist and never persists")
        void raisesNotFoundForUnknownBranch() {
            when(branchRepository.findById(10L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> productService.create(
                    10L, new CreateProductRequest("Laptop Lenovo", 25)))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("Branch with id 10 not found");

            verify(productRepository, never()).save(any());
        }

        @Test
        @DisplayName("rejects a name already used inside the same branch with 409 CONFLICT")
        void rejectsDuplicatedNameInSameBranch() {
            when(branchRepository.findById(1L)).thenReturn(Optional.of(branch));
            when(productRepository.existsByBranchIdAndNameIgnoreCase(1L, "Laptop Lenovo"))
                    .thenReturn(true);

            assertThatThrownBy(() -> productService.create(
                    1L, new CreateProductRequest("Laptop Lenovo", 25)))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("already exists in branch 1")
                    .extracting(ex -> ((BusinessException) ex).getStatus())
                    .isEqualTo(HttpStatus.CONFLICT);

            verify(productRepository, never()).save(any());
        }

        @Test
        @DisplayName("rejects a negative initial stock with 400 BAD REQUEST")
        void rejectsNegativeStock() {
            when(branchRepository.findById(1L)).thenReturn(Optional.of(branch));

            assertThatThrownBy(() -> productService.create(
                    1L, new CreateProductRequest("Laptop Lenovo", -1)))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage("Stock must be greater than or equal to 0")
                    .extracting(ex -> ((BusinessException) ex).getStatus())
                    .isEqualTo(HttpStatus.BAD_REQUEST);

            verify(productRepository, never()).save(any());
        }

        @Test
        @DisplayName("trims surrounding whitespace before persisting")
        void trimsName() {
            when(branchRepository.findById(1L)).thenReturn(Optional.of(branch));
            when(productRepository.existsByBranchIdAndNameIgnoreCase(1L, "Teclado")).thenReturn(false);
            when(productRepository.save(any(Product.class)))
                    .thenAnswer(invocation -> withIdAndTimestamps(invocation.getArgument(0), 3L));

            productService.create(1L, new CreateProductRequest("  Teclado  ", 10));

            ArgumentCaptor<Product> captor = ArgumentCaptor.forClass(Product.class);
            verify(productRepository).save(captor.capture());
            assertThat(captor.getValue().getName()).isEqualTo("Teclado");
        }
    }

    @Nested
    @DisplayName("delete")
    class Delete {

        @Test
        @DisplayName("removes a product that belongs to the given branch")
        void deletesProduct() {
            Product product = product(1L, "Laptop Lenovo", 25);
            when(productRepository.findByIdAndBranchId(1L, 1L)).thenReturn(Optional.of(product));

            productService.delete(1L, 1L);

            verify(productRepository).delete(product);
        }

        @Test
        @DisplayName("raises 404 for an unknown product")
        void raisesNotFoundForUnknownProduct() {
            when(productRepository.findByIdAndBranchId(99L, 1L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> productService.delete(1L, 99L))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("Product with id 99 not found in branch 1");

            verify(productRepository, never()).delete(any());
        }

        @Test
        @DisplayName("raises 404 when the product exists but belongs to another branch")
        void raisesNotFoundWhenProductBelongsToAnotherBranch() {
            // The repository lookup is scoped by branch, so a mismatch yields empty.
            when(productRepository.findByIdAndBranchId(1L, 2L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> productService.delete(2L, 1L))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("Product with id 1 not found in branch 2");

            verify(productRepository, never()).delete(any());
        }
    }

    @Nested
    @DisplayName("updateStock")
    class UpdateStock {

        @Test
        @DisplayName("stores the new absolute stock and returns the updated product")
        void updatesStock() {
            Product product = product(1L, "Laptop Lenovo", 25);
            when(productRepository.findByIdAndBranchId(1L, 1L)).thenReturn(Optional.of(product));
            when(productRepository.saveAndFlush(product)).thenReturn(product);

            ProductResponse response = productService.updateStock(1L, 1L, new UpdateStockRequest(50));

            assertThat(response.stock()).isEqualTo(50);
            assertThat(product.getStock()).isEqualTo(50);
        }

        @Test
        @DisplayName("allows dropping the stock to 0")
        void allowsZeroStock() {
            Product product = product(1L, "Laptop Lenovo", 25);
            when(productRepository.findByIdAndBranchId(1L, 1L)).thenReturn(Optional.of(product));
            when(productRepository.saveAndFlush(product)).thenReturn(product);

            assertThat(productService.updateStock(1L, 1L, new UpdateStockRequest(0)).stock())
                    .isZero();
        }

        @Test
        @DisplayName("rejects a negative stock with 400 BAD REQUEST and does not touch the product")
        void rejectsNegativeStock() {
            assertThatThrownBy(() -> productService.updateStock(1L, 1L, new UpdateStockRequest(-5)))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage("Stock must be greater than or equal to 0")
                    .extracting(ex -> ((BusinessException) ex).getStatus())
                    .isEqualTo(HttpStatus.BAD_REQUEST);

            // Validation happens before the lookup, so nothing is read or written.
            verify(productRepository, never()).findByIdAndBranchId(any(), any());
            verify(productRepository, never()).saveAndFlush(any());
        }

        @Test
        @DisplayName("raises 404 for an unknown product")
        void raisesNotFoundForUnknownProduct() {
            when(productRepository.findByIdAndBranchId(99L, 1L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> productService.updateStock(1L, 99L, new UpdateStockRequest(50)))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("Product with id 99 not found in branch 1");

            verify(productRepository, never()).saveAndFlush(any());
        }

        @Test
        @DisplayName("raises 404 when the product belongs to another branch")
        void raisesNotFoundWhenProductBelongsToAnotherBranch() {
            when(productRepository.findByIdAndBranchId(1L, 2L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> productService.updateStock(2L, 1L, new UpdateStockRequest(50)))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("Product with id 1 not found in branch 2");

            verify(productRepository, never()).saveAndFlush(any());
        }
    }

    @Nested
    @DisplayName("reads")
    class Reads {

        @Test
        @DisplayName("lists the products of a branch ordered by id")
        void listsProductsOfBranch() {
            when(branchRepository.existsById(1L)).thenReturn(true);
            when(productRepository.findByBranchIdOrderByIdAsc(1L)).thenReturn(List.of(
                    product(1L, "Laptop", 20),
                    product(2L, "Mouse", 50)));

            assertThat(productService.findByBranch(1L))
                    .extracting(ProductResponse::name)
                    .containsExactly("Laptop", "Mouse");
        }

        @Test
        @DisplayName("raises 404 when listing the products of an unknown branch")
        void raisesNotFoundForUnknownBranch() {
            when(branchRepository.existsById(10L)).thenReturn(false);

            assertThatThrownBy(() -> productService.findByBranch(10L))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("Branch with id 10 not found");
        }

        @Test
        @DisplayName("returns a product by id")
        void returnsProductById() {
            when(productRepository.findById(1L)).thenReturn(Optional.of(product(1L, "Laptop", 20)));

            assertThat(productService.findById(1L).name()).isEqualTo("Laptop");
        }

        @Test
        @DisplayName("raises 404 for an unknown product id")
        void raisesNotFoundForUnknownProductId() {
            when(productRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> productService.findById(99L))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("Product with id 99 not found");
        }
    }

    private Product product(Long id, String name, Integer stock) {
        return withIdAndTimestamps(
                Product.builder().name(name).stock(stock).branch(branch).build(), id);
    }

    private static Product withIdAndTimestamps(Product product, Long id) {
        product.setId(id);
        product.setCreatedAt(LocalDateTime.now());
        product.setUpdatedAt(LocalDateTime.now());
        return product;
    }
}
