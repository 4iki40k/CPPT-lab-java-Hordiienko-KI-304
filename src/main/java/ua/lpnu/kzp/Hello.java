package ua.lpnu.kzp;

/** Навчальна точка входу для перевірки JDK, пакування та версії JAR. */
public class Hello {
    /**
     * Виводить привітання або версію з метаданих JAR.
     *
     * @param args аргументи запуску; перший може бути {@code --version}
     */
    public static void main(String[] args) {
        if (args.length > 0 && "--version".equals(args[0])) {
            String version = Hello.class.getPackage().getImplementationVersion();
            System.out.printf("lab-java %s%n", version == null ? "development" : version);
            return;
        }
        System.out.println("Привіт, Java!");
    }
}
