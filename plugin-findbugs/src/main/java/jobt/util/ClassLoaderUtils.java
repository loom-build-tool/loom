package jobt.util;

import java.net.URL;
import java.net.URLClassLoader;
import java.security.AccessController;
import java.security.PrivilegedAction;

import jobt.plugin.findbugs.FindBugsPlugin;

public final class ClassLoaderUtils {

    private ClassLoaderUtils() {
    }


    public static ClassLoader getInitialClassLoader() {
        final ClassLoader extClassLoader = FindBugsPlugin.class.getClassLoader().getParent();
        // must be sun.misc.Launcher$ExtClassLoader
        Preconditions.checkState(extClassLoader.getClass().getSimpleName().equals("ExtClassLoader"));
        return extClassLoader;
    }


    public static ClassLoader getInitialClassLoaderZZ() {

        ClassLoader.getSystemClassLoader();

        ClassLoader cl = ClassLoaderUtils.class.getClassLoader();
        debug(cl);

        for(int i=0;i<5 && cl != null;i++) {
            if (cl.getClass().getSimpleName().equals("AppClassLoader")) {
                return cl.getParent();
            }
            cl = cl.getParent();
        }

        throw new IllegalStateException("AppClassLoader not found in hierarchy!");
    }

    public static void debug(ClassLoader cl) {

        for(int i=0;i<5 && cl != null;i++) {
            System.out.println(cl.getClass());
            cl = cl.getParent();
        }
        // TODO Auto-generated method stub

    }


    public static URLClassLoader createUrlClassLoader(final URL[] fbUrls, final ClassLoader parent) {
        System.out.println("build urlClassloader with parent="+parent);

        final URLClassLoader urlClassLoader = AccessController.doPrivileged(
            new PrivilegedAction<URLClassLoader>() {
              @Override
            public URLClassLoader run() {
                  return new URLClassLoader(
                      fbUrls, parent);
              }
            }
          );

        return urlClassLoader;
    }

}
