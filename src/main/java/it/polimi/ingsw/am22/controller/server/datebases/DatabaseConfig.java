package it.polimi.ingsw.am22.controller.server.datebases;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Lettore delle credenziali del database (URL, utente, password).
 *
 * <p>Carica una sola volta {@code db.properties} dal classpath al primo
 * accesso ed espone i singoli campi tramite metodi statici. Va tenuto
 * fuori dal codice sorgente per evitare di committare credenziali.
 */
public final class DatabaseConfig {

    private static final Properties PROPS = load();

    private DatabaseConfig() {}

    /**
     * Carica il file {@code db.properties} dal classpath. Solleva
     * {@link IllegalStateException} se il file e' mancante o non leggibile:
     * la classe non puo' funzionare senza configurazione.
     */
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

    /** URL JDBC del database (es. {@code jdbc:mysql://host:3306/dbname}). */
    public static String url()      { return PROPS.getProperty("db.url"); }
    /** Username per la connessione al database. */
    public static String user()     { return PROPS.getProperty("db.user"); }
    /** Password per la connessione al database. */
    public static String password() { return PROPS.getProperty("db.password"); }
}
