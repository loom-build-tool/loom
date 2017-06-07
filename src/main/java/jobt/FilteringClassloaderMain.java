package jobt;

import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class FilteringClassloaderMain {



    public static void main(final String[] args) throws Exception {


        final URLClassLoader urlClassLoader = new URLClassLoader(new URL[] {
            Paths.get("/Users/sostermayr/.jobt/binary/1.0.0/plugin-java//jobt-plugin-java-1.0.0.jar").toUri().toURL()
        }, new FilteringClassLoader(Thread.currentThread().getContextClassLoader()));

        urlClassLoader.loadClass("jobt.plugin.java.JavaPlugin");



    }

    public static class FilteringClassLoader extends ClassLoader {

        private static final ClassLoader EXT_CLASS_LOADER;
        private static final Set<String> SYSTEM_PACKAGES = new HashSet<>();


        static {

            EXT_CLASS_LOADER = getPlatformClassLoader();
            Package[] systemPackages;
            try {
                final Method method = ClassLoader.class.getMethod("getDefinedPackages");
                systemPackages = (Package[]) method.invoke(EXT_CLASS_LOADER);
            } catch (final NoSuchMethodException e) {
                System.out.println("FIX Java 9 Support");
                // We must not be on Java 9 where the getDefinedPackages() method exists. Fall back to getPackages()
//                JavaMethod<ClassLoader, Package[]> method = JavaReflectionUtil.method(ClassLoader.class, Package[].class, "getPackages");
//                systemPackages = method.invoke(EXT_CLASS_LOADER);
                throw new RuntimeException("JAVA 9 SUPPORT - TODO", e); // FIXME
            } catch (final Exception e) {
                throw new RuntimeException(e);
            }
            for (final Package p : systemPackages) {
                SYSTEM_PACKAGES.add(p.getName());
            }
        }


        public FilteringClassLoader(final ClassLoader parent) {
            super(parent);
        }

        @Override
        protected Class<?> loadClass(final String name, final boolean resolve) throws ClassNotFoundException {
            try {
                return EXT_CLASS_LOADER.loadClass(name);
            } catch (final ClassNotFoundException ignore) {
                // ignore
            }

            if (!classAllowed(name)) {
                throw new ClassNotFoundException(name + " not found.");
            }

            final Class<?> cl = super.loadClass(name, false);
            if (resolve) {
                resolveClass(cl);
            }

            return cl;
        }

        @Override
        protected Package getPackage(final String name) {
            final Package p = super.getPackage(name);
            if (p == null || !allowed(p)) {
                return null;
            }
            return p;
        }

        @Override
        protected Package[] getPackages() {
            final List<Package> packages = new ArrayList<>();
            for (final Package p : super.getPackages()) {
                if (allowed(p)) {
                    packages.add(p);
                }
            }
            return packages.toArray(new Package[0]);
        }

//        @Override
//        public URL getResource(final String name) {
//            if (allowed(name)) {
//                return super.getResource(name);
//            }
//            return EXT_CLASS_LOADER.getResource(name);
//        }

//        @Override
//        public Enumeration<URL> getResources(final String name) throws IOException {
//            if (allowed(name)) {
//                return super.getResources(name);
//            }
//            return EXT_CLASS_LOADER.getResources(name);
//        }

//        private boolean allowed(final String resourceName) {
//            if (resourceNames.contains(resourceName)) {
//                return true;
//            }
//            for (final String resourcePrefix : resourcePrefixes) {
//                if (resourceName.startsWith(resourcePrefix)) {
//                    return true;
//                }
//            }
//            return false;
//        }

        private boolean allowed(final Package pkg) {
            System.out.println("allowed="+pkg);
            return true;
//            for (final String packagePrefix : disallowedPackagePrefixes) {
//                if (pkg.getName().startsWith(packagePrefix)) {
//                    return false;
//                }
//            }
//
//            if (SYSTEM_PACKAGES.contains(pkg.getName())) {
//                return true;
//            }
//            if (packageNames.contains(pkg.getName())) {
//                return true;
//            }
//            for (final String packagePrefix : packagePrefixes) {
//                if (pkg.getName().startsWith(packagePrefix)) {
//                    return true;
//                }
//            }
//            return false;
        }

        private boolean classAllowed(final String className) {

            System.out.println("classAllowed="+className);
            return true;
//            if (disallowedClassNames.contains(className)) {
//                return false;
//            }
//            for (final String packagePrefix : disallowedPackagePrefixes) {
//                if (className.startsWith(packagePrefix)) {
//                    return false;
//                }
//            }
//
//            if (classNames.contains(className)) {
//                return true;
//            }
//            for (final String packagePrefix : packagePrefixes) {
//                if (className.startsWith(packagePrefix)) {
//                    return true;
//                }
//
//                if (packagePrefix.startsWith(DEFAULT_PACKAGE) && isInDefaultPackage(className)) {
//                    return true;
//                }
//            }
//            return false;
        }

        private boolean isInDefaultPackage(final String className) {
            return !className.contains(".");
        }

    }

    public static ClassLoader getPlatformClassLoader() {
        return ClassLoader.getSystemClassLoader().getParent();
    }

}
