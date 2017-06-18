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
