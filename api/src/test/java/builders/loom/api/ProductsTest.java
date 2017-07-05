package builders.loom.api;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;

import org.junit.Test;

import builders.loom.api.product.Product;

public class ProductsTest {

    private final ProductRepository productRepository = new ProductRepositoryDummy();

    @Test
    public void successProvideAndUse() throws InterruptedException {

        final ProvidedProducts providedProducts = provides("a", "b");

        final UsedProducts usedProducts = uses("a");

        providedProducts.complete("a", new StringProduct("foo"));

        assertEquals("foo", usedProducts.readProduct("a", StringProduct.class).toString());

    }

    @Test
    public void failDoubleComplete() {

        final ProvidedProducts providedProducts = provides("a");

        providedProducts.complete("a", new StringProduct("result"));

        try {
            providedProducts.complete("a", new StringProduct("result-double"));
            fail();
        } catch (final IllegalStateException e) {
            assertEquals("Task <sampleTask> has tried to complete"
                + " the already completed product <a>", e.getMessage());
        }

    }

    @Test
    public void failCompleteWithNull() {

        final ProvidedProducts providedProducts = provides("a");

        try {
            providedProducts.complete("a", null);
        } catch (final NullPointerException e) {
            assertEquals("Must not complete product <a> in task <sampleTask> with null value",
                e.getMessage());
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void invalidFormat() {

        provides("ZZZ");

    }

    @Test(expected = IllegalStateException.class)
    public void failOnMultipleProvides() {

        provides("x");
        provides("x");

    }

    @Test
    public void failOnUnknownProduct() throws InterruptedException {

        try {
            final UsedProducts uses = uses("x");
            uses.readProduct("x", StringProduct.class);
            fail();
        } catch (final NullPointerException npe) {
            assertEquals("No product found by id <x>", npe.getMessage());
        }

    }

    private UsedProducts uses(final String... productIdLists) {
        return new UsedProducts(
            new HashSet<>(Arrays.asList(productIdLists)), productRepository);
    }

    private ProvidedProducts provides(final String... productIdLists) {
        Stream.of(productIdLists)
            .forEach(productRepository::createProduct);
        return new ProvidedProducts(
            new HashSet<>(Arrays.asList(productIdLists)), productRepository, "sampleTask");
    }

    private static class ProductRepositoryDummy implements ProductRepository {

        private final Map<String, ProductPromise> products = new HashMap<>();

        @Override
        public ProductPromise lookup(final String productId) {
            return Objects.requireNonNull(
                products.get(productId),
                "No product found by id <" + productId + ">");
        }

        @Override
        public void createProduct(final String productId) {
            final ProductPromise old = products.putIfAbsent(
                productId, new ProductPromise(productId));
            if (old != null) {
                throw new IllegalStateException();
            }
        }
    }

    static class StringProduct implements Product {

        private final String str;

        StringProduct(final String str) {
            this.str = str;
        }

        @Override
        public String toString() {
            return str;
        }
    }
}
