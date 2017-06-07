package jobt.plugin.findbugs;

import java.io.Serializable;
import java.net.URL;
import java.util.List;
import java.util.stream.Collectors;

import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.FindBugs2;
import jobt.plugin.execution.DefaultWorkResult;
import jobt.plugin.execution.Dispatcher.WorkerProtocol;

public class FindBugsInvoker implements WorkerProtocol<FindBugsArgs>, Serializable {

    private static final long serialVersionUID = 1L;

    public FindBugsInvoker() {
//        Preconditions.checkState(getClass().getClassLoader().getClass() == URLClassLoader.class);
    }

    // NOTE: MUST RUN IN findbugsClassloader
    @Override
    public DefaultWorkResult execute(final FindBugsArgs spec) {
        // TODO - do we need this?
        final ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
        System.out.println("contextClassLoader="+contextClassLoader);
        System.out.println("contextClassLoade.parent="+contextClassLoader.getParent());
        System.out.println("FindBugsInvoker.classloader="+FindBugsInvoker.class.getClassLoader());
//        System.out.println("FindBugs2.classloader="+FindBugs2.class.getClassLoader());
        try {
            Thread.currentThread().setContextClassLoader(FindBugs2.class.getClassLoader());
            System.out.println("loading findBugs using classLoader "+Thread.currentThread().getContextClassLoader());

            System.out.println("FIXME remove"); // FIXME remove
//            runFindBugs();

            System.out.println("execution done.");
            return new DefaultWorkResult(true, null);

        } catch (final Exception e) {
            e.printStackTrace();
            return new DefaultWorkResult(false, e);
        } finally {
            Thread.currentThread().setContextClassLoader(contextClassLoader);
        }
    }

    public static void runFindBugs(final Classpath compileOutput, final List<URL> compileClasspath) {

        final List<BugInstance> bugs = new FindBugsSonarWayMain(compileOutput.getSingleEntry(), compileClasspath).findMyBugs();
        for (final BugInstance bug : bugs) {

            System.out.println("bug #"+bug.getMessage());

        }
    }

    private static String formatClasspath(final List<Classpath> auxclasspaths) {

        return auxclasspaths.stream()
            .flatMap(cps -> cps.getEntries().stream())
            .map(file -> file.toString())
            .collect(Collectors.joining(":"));
    }

}
