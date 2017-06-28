package jobt;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import jobt.api.ProductGraphNode;

@SuppressWarnings("checkstyle:regexpmultiline")
public final class GraphvizOutput {

    private GraphvizOutput() {
    }

    public static void generateDot(final Map<String, TaskGraphNodeImpl> tasks) {
        try {
            final Path reportDir = Files.createDirectories(Paths.get("jobtbuild", "reports"));
            final Path tasksFile = reportDir.resolve(Paths.get("tasks.dot"));

            try (PrintWriter pw = new PrintWriter(tasksFile.toFile(), "UTF-8")) {
                writeTasks(tasks, pw);
            }

            System.out.println("Task overview written to " + tasksFile);
            System.out.println("Use Graphviz to visualize: `dot -Tpng " + tasksFile
                + " > tasks.png`");
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static void writeTasks(final Map<String, TaskGraphNodeImpl> tasks,
                                   final PrintWriter pw) {
        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        final PrintWriter graphBuffer = new PrintWriter(out);
        pw.println("digraph dependencies {");
        pw.println("    rankdir=\"RL\";");
        pw.println("    graph [splines=spline, nodesep=1];");

        final Set<String> allProducts = new HashSet<>();

        for (final Map.Entry<String, TaskGraphNodeImpl> entry : tasks.entrySet()) {
            final String taskName = entry.getKey();

            final List<String> providedProducts =
                entry.getValue().getProvidedProductNodes().stream()
                .map(n -> n.getProductId())
                .map(p -> "produced_" + p) // FIXME
                .collect(Collectors.toList());

            final List<String> usedProducts = entry.getValue().getUsedProductNodes().stream()
                .map(ProductGraphNode::getProductId)
                .map(p -> "product_"+taskName+"__" + p) // FIXME
                .collect(Collectors.toList());


            allProducts.addAll(providedProducts);
            allProducts.addAll(usedProducts);

            // task -> provided products
            writeKeyValue(graphBuffer, taskName, providedProducts);

            // task -> used products
            writeKeyValue(graphBuffer, taskName, usedProducts);
        }

        pw.println("    node [shape=rectangle]; " + String.join(";", tasks.keySet()));
        pw.println("    node [shape=oval]; " + String.join(";", allProducts));

        graphBuffer.flush();
        pw.print(out.toString());

        pw.println("}");
    }

    private static void writeKeyValue(final PrintWriter pw, final String key,
                                      final List<String> values) {
        pw.print("    ");
        pw.print(key);
        if (!values.isEmpty()) {
            pw.print(" -> ");
            pw.print(constructValue(values));
        }
        pw.println(";");
    }

    private static String constructValue(final List<String> dependentNodes) {
        if (dependentNodes == null || dependentNodes.isEmpty()) {
            throw new IllegalArgumentException("dependentNodes must be > 0");
        }

        if (dependentNodes.size() == 1) {
            return dependentNodes.get(0);
        }

        return "{" + dependentNodes.stream()
            .collect(Collectors.joining(", "))
            + "}";
    }

}
