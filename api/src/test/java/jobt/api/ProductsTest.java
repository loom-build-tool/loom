package jobt.api;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import org.junit.Test;

// FIXME must run via gradle
public class ProductsTest {

    private final ExecutionContext executionContext = new ExecutionContext() {
        private final Map<String, ProductPromise> products = new HashMap<>();
        @Override
        public Map<String, ProductPromise> getProducts() {
            return products;
        }
    };

    @Test
    public void successProvideAndUse() {

        final ProvidedProducts providedProducts = provides("a", "b");

        final UsedProducts usedProducts = uses("a");

        providedProducts.complete("a", "foo");

        assertEquals("foo", usedProducts.readProduct("a", String.class));

    }

    @Test
    public void failDoubleComplete() {

        final ProvidedProducts providedProducts = provides("a");

        providedProducts.complete("a", "result");

        try {
            providedProducts.complete("a", "result-double");
            fail();
        } catch(final IllegalStateException e) {
            assertEquals("Product promise <a> already completed", e.getMessage());
        }

    }

    @Test
    public void failCompleteWithNull() {

        final ProvidedProducts providedProducts = provides("a");

        try {
            providedProducts.complete("a", null);
        } catch(final IllegalArgumentException e) {
            assertEquals("Must not complete product <a> with null value", e.getMessage());
        }
    }

    @Test(expected = IllegalStateException.class)
    public void invalidFormat() {

        provides("ZZZ");

    }

    @Test(expected = IllegalStateException.class)
    public void failOnMultipleProvides() {

        provides("x");
        provides("x");

    }

    @Test
    public void failOnUnknownProduct() {

        try {
            final UsedProducts uses = uses("x");
            uses.readProduct("x", String.class);
            fail();
        } catch(final NullPointerException npe) {
            assertEquals("No product found by id <x>", npe.getMessage());
        }

    }

    private final UsedProducts uses(final String... productIdLists) {
        return new UsedProducts(
            new HashSet<>(Arrays.asList(productIdLists)), executionContext);
    }

    private final ProvidedProducts provides(final String... productIdLists) {
        return new ProvidedProducts(
            new HashSet<>(Arrays.asList(productIdLists)), executionContext);
    }
}
