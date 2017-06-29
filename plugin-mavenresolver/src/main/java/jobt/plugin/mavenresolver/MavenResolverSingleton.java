package jobt.plugin.mavenresolver;

public final class MavenResolverSingleton {

    private static volatile MavenResolver instance;

    private MavenResolverSingleton() {
    }

    public static MavenResolver getInstance() {

        if (instance == null) {
            synchronized (MavenResolverSingleton.class) {
                if (instance == null) {

                    final ProgressIndicator progressIndicator =
                        new ProgressIndicator("mavenResolver");

                    instance = new MavenResolver(progressIndicator);
                }
            }
        }

        return instance;
    }

}
