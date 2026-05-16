#!/bin/sh
# Avvia il client MESOS con interfaccia testuale (TUI).
# Richiede client.jar nella stessa cartella.
cd "$(dirname "$0")" || exit 1

if command -v java >/dev/null 2>&1; then
    JAVA_CMD=java
elif [ -n "$JAVA_HOME" ] && [ -x "$JAVA_HOME/bin/java" ]; then
    JAVA_CMD="$JAVA_HOME/bin/java"
else
    echo "Java non trovato. Installare Java 25+ e aggiungerlo al PATH, oppure impostare JAVA_HOME."
    exit 1
fi

"$JAVA_CMD" -Dfile.encoding=UTF-8 -jar client.jar --tui
