package jobt.plugin.mavenresolver;


public class SysoutMain {


    public static void main(final String[] args) throws InterruptedException {

        System.out.print("start...");

        for(int i=0;i<40;i++) {
            Thread.sleep(20);
            System.out.print("\r ... working i"+i);
        }

        // https://stackoverflow.com/questions/7522022/how-to-delete-stuff-printed-to-console-by-system-out-println
//        clearLine();
        System.out.print("                                     ");
        System.out.println("\rcompleted");


    }

    private static void clearLine() {
        System.out.print("\033[2K");
    }
}
