-- ============================================================================
-- Setup MySQL per il progetto MESOS_database.
-- Da eseguire UNA SOLA VOLTA su ogni PC del gruppo che possa fare da server.
--
-- Come eseguirlo:
--   1) aprire MySQL Workbench, connessione "Local instance MySQL" (utente root)
--   2) File -> Open SQL Script -> selezionare questo file
--   3) cliccare il fulmine (Execute All)
-- In alternativa da terminale:
--   mysql -u root -p < db_setup.sql
-- ============================================================================

-- 1) Crea il database (se non esiste).
CREATE DATABASE IF NOT EXISTS MESOS_database
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE MESOS_database;

-- 2) Crea la tabella delle partite finite (idempotente: non riscrive se esiste).
CREATE TABLE IF NOT EXISTS match_results (
    id            INT AUTO_INCREMENT PRIMARY KEY,
    nickname      VARCHAR(50)  NOT NULL,
    final_score   INT          NOT NULL,
    end_date      DATETIME     NOT NULL,
    num_players   INT          NOT NULL,
    INDEX idx_num_players (num_players)
);

-- 3) Crea l'utente applicativo CONDIVISO con credenziali fisse:
--    user: MESOS   password: 123456789
--    cosi' tutti i PC del gruppo usano lo stesso login -> il file
--    db.properties commit-tato funziona ovunque senza modifiche.
--
-- '%' significa "da qualunque host", utile se il game-server gira su un PC
-- e il DB su un altro PC della stessa rete. Se preferisci limitare al solo
-- localhost, sostituisci '%' con 'localhost' nelle due righe sotto.
CREATE USER IF NOT EXISTS 'MESOS'@'%' IDENTIFIED BY '123456789';
GRANT ALL PRIVILEGES ON MESOS_database.* TO 'MESOS'@'%';
FLUSH PRIVILEGES;

-- 4) Verifica rapida (output atteso: tabella vuota).
SELECT * FROM match_results;
