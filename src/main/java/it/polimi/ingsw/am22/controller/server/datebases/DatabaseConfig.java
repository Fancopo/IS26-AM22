package it.polimi.ingsw.am22.controller.server.datebases;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Reads the database credentials (URL, user, password).
 *
 * <p>Loads {@code db.properties} from the classpath once, on first access, and
 * exposes the individual fields through static methods. The file is kept out of
 * source control to avoid committing credentials.
 */
public final class DatabaseConfig {

    private static final Properties PROPS = load();

    private DatabaseConfig() {}

    /**
     * Loads {@code db.properties} from the classpath. Throws
     * {@link IllegalStateException} if the file is missing or unreadable: the
     * class cannot work without configuration.
     */
    private static Properties load() {
        Properties p = new Properties();
        try (InputStream in = DatabaseConfig.class
                .getResourceAsStream("/db.properties")) {
            if (in == null) {
                throw new IllegalStateException(
                        "db.properties not found on the classpath " +
                        "(it must live in src/main/resources)");
            }
            p.load(in);
        } catch (IOException e) {
            throw new IllegalStateException("Error reading db.properties", e);
        }
        return p;
    }

    /** @return the JDBC URL of the database (e.g. {@code jdbc:mysql://host:3306/dbname}) */
    public static String url()      { return PROPS.getProperty("db.url"); }

    /** @return the username for the database connection */
    public static String user()     { return PROPS.getProperty("db.user"); }

    /** @return the password for the database connection */
    public static String password() { return PROPS.getProperty("db.password"); }
}
