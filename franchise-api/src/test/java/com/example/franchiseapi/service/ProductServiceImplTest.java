package com.example.franchiseapi.service;

import com.example.franchiseapi.dto.request.CreateProductRequest;
import com.example.franchiseapi.dto.request.UpdateNameRequest;
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
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * The business rules are enforced by the stored procedures and covered against
 * MySQL in the integration tests. These tests check that the service calls the
 * right procedure and turns its errors into the right exceptions.
 */
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
        @DisplayName("returns the product stored by the procedure, linked to its branch")
        void createsProduct() {
            when(productRepository.create(1L, "Laptop Lenovo", 25))
                    .thenReturn(product(1L, "Laptop Lenovo", 25));

            ProductResponse response = productService.create(
                    1L, new CreateProductRequest("Laptop Lenovo", 25));

            assertThat(response.id()).isEqualTo(1L);
            assertThat(response.name()).isEqualTo("Laptop Lenovo");
            assertThat(response.stock()).isEqualTo(25);
            assertThat(response.branchId()).isEqualTo(1L);
        }

        @Test
        @DisplayName("raises 404 when the procedure reports an unknown branch")
        void raisesNotFoundForUnknownBranch() {
            when(productRepository.create(10L, "Laptop Lenovo", 25))
                    .thenThrow(ProcedureFailures.notFound("Branch with id 10 not found"));

            assertThatThrownBy(() -> productService.create(
                    10L, new CreateProductRequest("Laptop Lenovo", 25)))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("Branch with id 10 not found");
        }

        @Test
        @DisplayName("raises 409 when the procedure reports a name already used in the branch")
        void rejectsDuplicatedName() {
            when(productRepository.create(1L, "Laptop Lenovo", 25))
                    .thenThrow(ProcedureFailures.conflict(
                            "A product named 'Laptop Lenovo' already exists in branch 1"));

            assertThatThrownBy(() -> productService.create(
                    1L, new CreateProductRequest("Laptop Lenovo", 25)))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("already exists in branch 1")
                    .extracting(ex -> ((BusinessException) ex).getStatus())
                    .isEqualTo(HttpStatus.CONFLICT);
        }

        @Test
        @DisplayName("raises 400 when the procedure rejects a negative stock")
        void rejectsNegativeStock() {
            when(productRepository.create(1L, "Laptop Lenovo", -1))
                    .thenThrow(ProcedureFailures.badRequest("Stock must be greater than or equal to 0"));

            assertThatThrownBy(() -> productService.create(
                    1L, new CreateProductRequest("Laptop Lenovo", -1)))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage("Stock must be greater than or equal to 0")
                    .extracting(ex -> ((BusinessException) ex).getStatus())
                    .isEqualTo(HttpStatus.BAD_REQUEST);
        }
    }

    @Nested
    @DisplayName("delete")
    class Delete {

        @Test
        @DisplayName("calls the delete procedure scoped by branch")
        void deletesProduct() {
            productService.delete(1L, 3L);

            verify(productRepository).deleteFromBranch(1L, 3L);
        }

        @Test
        @DisplayName("raises 404 when the product does not exist in that branch")
        void raisesNotFoundThroughWrongBranch() {
            doThrow(ProcedureFailures.notFound("Product with id 3 not found in branch 2"))
                    .when(productRepository).deleteFromBranch(2L, 3L);

            assertThatThrownBy(() -> productService.delete(2L, 3L))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("Product with id 3 not found in branch 2");
        }
    }

    @Nested
    @DisplayName("updateStock")
    class UpdateStock {

        @Test
        @DisplayName("returns the product with the stock stored by the procedure")
        void updatesStock() {
            when(productRepository.updateStock(1L, 3L, 50)).thenReturn(product(3L, "Laptop Lenovo", 50));

            assertThat(productService.updateStock(1L, 3L, new UpdateStockRequest(50)).stock())
                    .isEqualTo(50);
        }

        @Test
        @DisplayName("raises 400 when the procedure rejects a negative stock")
        void rejectsNegativeStock() {
            when(productRepository.updateStock(1L, 3L, -5))
                    .thenThrow(ProcedureFailures.badRequest("Stock must be greater than or equal to 0"));

            assertThatThrownBy(() -> productService.updateStock(1L, 3L, new UpdateStockRequest(-5)))
                    .isInstanceOf(BusinessException.class)
                    .extracting(ex -> ((BusinessException) ex).getStatus())
                    .isEqualTo(HttpStatus.BAD_REQUEST);
        }

        @Test
        @DisplayName("raises 404 when the product does not exist in that branch")
        void raisesNotFoundThroughWrongBranch() {
            when(productRepository.updateStock(2L, 3L, 50))
                    .thenThrow(ProcedureFailures.notFound("Product with id 3 not found in branch 2"));

            assertThatThrownBy(() -> productService.updateStock(2L, 3L, new UpdateStockRequest(50)))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("Product with id 3 not found in branch 2");
        }
    }

    @Nested
    @DisplayName("updateName")
    class UpdateName {

        @Test
        @DisplayName("returns the product renamed by the procedure, stock untouched")
        void renamesProduct() {
            when(productRepository.updateName(1L, 3L, "Laptop Lenovo X1"))
                    .thenReturn(product(3L, "Laptop Lenovo X1", 25));

            ProductResponse response = productService.updateName(
                    1L, 3L, new UpdateNameRequest("Laptop Lenovo X1"));

            assertThat(response.name()).isEqualTo("Laptop Lenovo X1");
            assertThat(response.stock()).isEqualTo(25);
        }

        @Test
        @DisplayName("raises 404 when the product does not exist in that branch")
        void raisesNotFoundThroughWrongBranch() {
            when(productRepository.updateName(2L, 3L, "Otro"))
                    .thenThrow(ProcedureFailures.notFound("Product with id 3 not found in branch 2"));

            assertThatThrownBy(() -> productService.updateName(2L, 3L, new UpdateNameRequest("Otro")))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("Product with id 3 not found in branch 2");
        }

        @Test
        @DisplayName("raises 409 when the procedure reports a name used by a sibling product")
        void rejectsNameOfSiblingProduct() {
            when(productRepository.updateName(1L, 3L, "Mouse Logitech"))
                    .thenThrow(ProcedureFailures.conflict(
                            "A product named 'Mouse Logitech' already exists in branch 1"));

            assertThatThrownBy(() -> productService.updateName(
                    1L, 3L, new UpdateNameRequest("Mouse Logitech")))
                    .isInstanceOf(BusinessException.class)
                    .extracting(ex -> ((BusinessException) ex).getStatus())
                    .isEqualTo(HttpStatus.CONFLICT);
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
        Product product = Product.builder().name(name).stock(stock).branch(branch).build();
        product.setId(id);
        product.setCreatedAt(LocalDateTime.now());
        product.setUpdatedAt(LocalDateTime.now());
        return product;
    }
}
