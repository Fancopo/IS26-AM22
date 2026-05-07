package it.polimi.ingsw.am22.network.server.databases;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public final class DatabaseConfig {

    private static final Properties PROPS = load();

    private DatabaseConfig() {}

    private static Properties load() {
        Properties p = new Properties();
        try (InputStream in = DatabaseConfig.class
                .getResourceAsStream("/db.properties")) {
            if (in == null) {
                throw new IllegalStateException(
                        "db.properties non trovato nel classpath " +
                        "(deve stare in src/main/resources)");
            }
            p.load(in);
        } catch (IOException e) {
            throw new IllegalStateException("Errore lettura db.properties", e);
        }
        return p;
    }

    public static String url()      { return PROPS.getProperty("db.url"); }
    public static String user()     { return PROPS.getProperty("db.user"); }
    public static String password() { return PROPS.getProperty("db.password"); }
}
