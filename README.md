# Codice-Software
# MESOS — Codice-Software

Implementazione del gioco da tavolo **MESOS**, con server multipartita e client
disponibile in due interfacce: testuale (TUI) e grafica (GUI, JavaFX).

## Istruzioni di Avvio

### Prerequisiti

Per avviare il gioco è necessario avere installata sul proprio dispositivo una
versione di **Java JDK uguale o superiore alla 25**. Se sul sistema sono
presenti più versioni di Java, assicurarsi che venga usata quella corretta per
l'esecuzione (verificabile con il comando `java -version`).


> Nota: il `client.jar` contiene le librerie native di JavaFX, che sono
> specifiche per il sistema operativo. Il JAR generato su Windows va eseguito
> su Windows; per Linux/macOS va rigenerato su quella piattaforma.

### Aggiungere Java al PATH

Se digitando `java -version` compare l'errore `'java' is not recognized`
(Windows) o `java: command not found` (Unix), significa che Java non è
presente nel `PATH` di sistema: va aggiunto manualmente, una volta sola.

Per prima cosa individuare la cartella in cui è installato il JDK; al suo
interno deve esistere la sottocartella `bin` (che contiene `java`/`java.exe`).
Esempi tipici di percorso del JDK:

- Windows (JDK installato da IntelliJ): `C:\Users\<utente>\.jdks\openjdk-25`
- Linux: `/usr/lib/jvm/jdk-25`
- macOS: `/Library/Java/JavaVirtualMachines/jdk-25.jdk/Contents/Home`

**Windows**

1. Premere `Win + R`, digitare `sysdm.cpl ,3` e premere Invio: si apre la
   scheda *Avanzate* delle Proprietà di sistema.
2. Cliccare il pulsante *Variabili d'ambiente*.
3. Nel riquadro *Variabili dell'utente* selezionare la voce `Path` e cliccare
   *Modifica*.
4. Cliccare *Nuovo* e incollare il percorso della cartella `bin` del JDK,
   ad esempio:
   ```
   C:\Users\<utente>\.jdks\openjdk-25\bin
   ```
5. Confermare con *OK* tutte le finestre, quindi **chiudere e riaprire** il
   terminale.

In alternativa, da un terminale (sostituendo il percorso con il proprio):

```
setx PATH "%PATH%;C:\Users\<utente>\.jdks\openjdk-25\bin"
```

**Linux / macOS**

Aggiungere al proprio file di profilo (`~/.bashrc`, `~/.zshrc` o `~/.profile`)
la riga seguente, sostituendo il percorso con quello del proprio JDK:

```
export PATH="$PATH:/usr/lib/jvm/jdk-25/bin"
```

Poi ricaricare il profilo (`source ~/.bashrc`) o riaprire il terminale.

Per verificare che tutto funzioni, riaprire il terminale ed eseguire:

```
java -version
```

Deve essere mostrata una versione `25` o superiore.

> In alternativa all'aggiunta al `PATH`, è possibile impostare solo la
> variabile d'ambiente `JAVA_HOME` sulla cartella del JDK (senza `\bin`):
> gli script di avvio `.bat`/`.sh` (vedi sotto) la usano automaticamente come
> ripiego quando `java` non è nel `PATH`. Su Windows:
> ```
> setx JAVA_HOME "C:\Users\<utente>\.jdks\openjdk-25"
> ```

### Avvio tramite JAR

Dopo aver scaricato la cartella del progetto, aprire un terminale nella cartella
che contiene i file `server.jar` e `client.jar` ed eseguire i seguenti comandi.

- **Server** [Windows / Unix]

  ```
  java -jar server.jar
  ```

  All'avvio il server stampa le porte in ascolto:
  - Socket (TCP): porta `12345`
  - RMI: porta `1099`, binding `MESOS_SERVER`

- **Client — Interfaccia Grafica (GUI)** [Windows / Unix]

  ```
  java -jar client.jar --gui
  ```

- **Client — Interfaccia Testuale (TUI)** [Windows / Unix]

  ```
  java -jar client.jar --tui
  ```

Se il client viene avviato **senza specificare l'interfaccia**
(`java -jar client.jar`), all'avvio verrà chiesto interattivamente da terminale
quale interfaccia utilizzare (TUI o GUI).

Una volta avviato il client, verranno richiesti il tipo di connessione
(Socket o RMI), l'indirizzo (`host`) e la porta del server a cui collegarsi.
Per un server in esecuzione sulla stessa macchina usare `host = 127.0.0.1`.

### Avvio semplificato (script)

Per evitare di digitare i comandi a mano sono forniti degli script di avvio.
Vanno collocati **nella stessa cartella** dei file `server.jar` e `client.jar`.

- **Windows** — fare doppio clic su uno dei file `.bat`:
  - `start-server.bat` — avvia il server
  - `start-gui.bat` — avvia il client con interfaccia grafica
  - `start-tui.bat` — avvia il client con interfaccia testuale

- **Unix (Linux / macOS)** — eseguire da terminale uno dei file `.sh`:

  ```
  ./start-server.sh
  ./start-gui.sh
  ./start-tui.sh
  ```

  Se gli script non risultano eseguibili, assegnare i permessi con:

  ```
  chmod +x start-*.sh
  ```

### Generare i JAR dal codice sorgente

Per ricostruire i due JAR a partire dal codice è sufficiente, dalla cartella
`am22`, eseguire:

```
mvnw.cmd clean package        # Windows
./mvnw clean package          # Unix
```

Aggiungere `-DskipTests` per saltare l'esecuzione dei test.

Al termine i JAR si trovano nella cartella `am22/target/`:
- `server.jar`
- `client.jar`

