-- Gestione Piscina - migrazione cumulativa del database esistente.
-- Conserva i dati della prima versione, separa profili e qualifiche e prepara
-- il nucleo preesistente per lo schema completo.
-- Dopo questo file eseguire 01_schema_completo.sql: essendo idempotente,
-- aggiungerà le nuove tabelle, viste e trigger senza cancellare i dati.
USE piscina_progetto;

-- MySQL Workbench può avere SQL_SAFE_UPDATES attivo. La migrazione conserva
-- l'impostazione della sessione, la disattiva solo durante l'aggiornamento e
-- la ripristina al termine.
SET @OLD_SQL_SAFE_UPDATES = @@SQL_SAFE_UPDATES;
SET SQL_SAFE_UPDATES = 0;

-- ===========================================================================
-- SPECIALIZZAZIONE DI UTENTE E NORMALIZZAZIONE DEI PROFILI TECNICI
-- ===========================================================================

CREATE TABLE IF NOT EXISTS ATLETA (
    ID_Utente BIGINT UNSIGNED NOT NULL,
    CONSTRAINT PK_ATLETA PRIMARY KEY (ID_Utente),
    CONSTRAINT FK_ATLETA_UTENTE FOREIGN KEY (ID_Utente)
        REFERENCES UTENTE (ID_Utente)
        ON UPDATE CASCADE
        ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS ISTRUTTORE (
    ID_Utente BIGINT UNSIGNED NOT NULL,
    Qualifica VARCHAR(120) NOT NULL,
    CONSTRAINT PK_ISTRUTTORE PRIMARY KEY (ID_Utente),
    CONSTRAINT FK_ISTRUTTORE_UTENTE FOREIGN KEY (ID_Utente)
        REFERENCES UTENTE (ID_Utente)
        ON UPDATE CASCADE
        ON DELETE CASCADE,
    CONSTRAINT CK_ISTRUTTORE_QUALIFICA CHECK (
        CHAR_LENGTH(TRIM(Qualifica)) > 0
    )
);

SET @drop_account_role_check = IF(
    EXISTS (
        SELECT 1
        FROM information_schema.TABLE_CONSTRAINTS
        WHERE CONSTRAINT_SCHEMA = DATABASE()
          AND TABLE_NAME = 'ACCOUNT'
          AND CONSTRAINT_NAME = 'CK_ACCOUNT_RUOLO'
    ),
    'ALTER TABLE ACCOUNT DROP CHECK CK_ACCOUNT_RUOLO',
    'SELECT 1'
);
PREPARE drop_account_role_check FROM @drop_account_role_check;
EXECUTE drop_account_role_check;
DEALLOCATE PREPARE drop_account_role_check;

-- Elimina soltanto il receptionist creato dal vecchio popolamento demo,
-- riconosciuto tramite le credenziali tecniche originarie.
DELETE FROM ACCOUNT
WHERE Email = 'reception@piscina.local'
  AND Password_Hash =
      'HT+VnzvB4Sh5Tq0YM8Ai80maVKR3V9kMsfhfFTGGReA='
  AND Password_Salt = 'apOYKLo+ZMMaLSae1vl6dw==';

UPDATE ACCOUNT
SET Ruolo = 'UTENTE'
WHERE Ruolo IN ('CLIENTE', 'ISTRUTTORE');

UPDATE ACCOUNT
SET Ruolo = 'CLUB'
WHERE Ruolo = 'REFERENTE_CLUB';

-- Il profilo receptionist non appartiene alle viste della relazione. Gli
-- eventuali account preesistenti vengono conservati ma disattivati, evitando
-- di assegnare automaticamente privilegi appartenenti a un'altra vista.
UPDATE ACCOUNT
SET Ruolo = 'UTENTE', Attivo = FALSE
WHERE Ruolo = 'RECEPTIONIST';

ALTER TABLE ACCOUNT
    MODIFY Ruolo VARCHAR(30) NOT NULL DEFAULT 'UTENTE',
    ADD CONSTRAINT CK_ACCOUNT_RUOLO CHECK (
        Ruolo IN (
            'AMMINISTRATORE', 'CLUB', 'UTENTE'
        )
    );

-- Collega ogni account alla sola identità coerente con il proprio ruolo.
SET @add_account_club = IF(
    EXISTS (
        SELECT 1
        FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 'ACCOUNT'
          AND COLUMN_NAME = 'ID_Club'
    ),
    'SELECT 1',
    'ALTER TABLE ACCOUNT ADD COLUMN ID_Club BIGINT UNSIGNED NULL AFTER ID_Utente'
);
PREPARE add_account_club FROM @add_account_club;
EXECUTE add_account_club;
DEALLOCATE PREPARE add_account_club;

UPDATE ACCOUNT a
JOIN UTENTE u ON u.Email = a.Email
SET a.ID_Utente = u.ID_Utente
WHERE a.Ruolo = 'UTENTE'
  AND a.ID_Utente IS NULL;

UPDATE ACCOUNT a
JOIN CLUB_SPORTIVO c
  ON c.Email = a.Email
  OR c.Nome = TRIM(CONCAT(a.Nome, ' ', a.Cognome))
SET a.ID_Club = c.ID_Club
WHERE a.Ruolo = 'CLUB'
  AND a.ID_Club IS NULL;

UPDATE ACCOUNT
SET ID_Utente = CASE WHEN Ruolo = 'UTENTE' THEN ID_Utente END,
    ID_Club = CASE WHEN Ruolo = 'CLUB' THEN ID_Club END;

SET @add_uq_account_club = IF(
    EXISTS (
        SELECT 1
        FROM information_schema.STATISTICS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 'ACCOUNT'
          AND INDEX_NAME = 'UQ_ACCOUNT_CLUB'
    ),
    'SELECT 1',
    'ALTER TABLE ACCOUNT ADD CONSTRAINT UQ_ACCOUNT_CLUB UNIQUE (ID_Club)'
);
PREPARE add_uq_account_club FROM @add_uq_account_club;
EXECUTE add_uq_account_club;
DEALLOCATE PREPARE add_uq_account_club;

SET @drop_fk_account_utente = IF(
    EXISTS (
        SELECT 1
        FROM information_schema.REFERENTIAL_CONSTRAINTS
        WHERE CONSTRAINT_SCHEMA = DATABASE()
          AND TABLE_NAME = 'ACCOUNT'
          AND CONSTRAINT_NAME = 'FK_ACCOUNT_UTENTE'
    ),
    'ALTER TABLE ACCOUNT DROP FOREIGN KEY FK_ACCOUNT_UTENTE',
    'SELECT 1'
);
PREPARE drop_fk_account_utente FROM @drop_fk_account_utente;
EXECUTE drop_fk_account_utente;
DEALLOCATE PREPARE drop_fk_account_utente;

ALTER TABLE ACCOUNT
    ADD CONSTRAINT FK_ACCOUNT_UTENTE FOREIGN KEY (ID_Utente)
        REFERENCES UTENTE (ID_Utente)
        ON UPDATE CASCADE
        ON DELETE RESTRICT;

SET @add_fk_account_club = IF(
    EXISTS (
        SELECT 1
        FROM information_schema.REFERENTIAL_CONSTRAINTS
        WHERE CONSTRAINT_SCHEMA = DATABASE()
          AND TABLE_NAME = 'ACCOUNT'
          AND CONSTRAINT_NAME = 'FK_ACCOUNT_CLUB'
    ),
    'SELECT 1',
    'ALTER TABLE ACCOUNT ADD CONSTRAINT FK_ACCOUNT_CLUB FOREIGN KEY (ID_Club) REFERENCES CLUB_SPORTIVO (ID_Club) ON UPDATE CASCADE ON DELETE RESTRICT'
);
PREPARE add_fk_account_club FROM @add_fk_account_club;
EXECUTE add_fk_account_club;
DEALLOCATE PREPARE add_fk_account_club;

-- Se esistono vecchi account UTENTE/CLUB privi di anagrafica, l'aggiunta del
-- CHECK si arresta intenzionalmente: vanno collegati prima della migrazione.
SET @add_account_link_check = IF(
    EXISTS (
        SELECT 1
        FROM information_schema.TABLE_CONSTRAINTS
        WHERE CONSTRAINT_SCHEMA = DATABASE()
          AND TABLE_NAME = 'ACCOUNT'
          AND CONSTRAINT_NAME = 'CK_ACCOUNT_COLLEGAMENTO'
    ),
    'SELECT 1',
    'ALTER TABLE ACCOUNT ADD CONSTRAINT CK_ACCOUNT_COLLEGAMENTO CHECK ((Ruolo = ''AMMINISTRATORE'' AND ID_Utente IS NULL AND ID_Club IS NULL) OR (Ruolo = ''CLUB'' AND ID_Utente IS NULL AND ID_Club IS NOT NULL) OR (Ruolo = ''UTENTE'' AND ID_Utente IS NOT NULL AND ID_Club IS NULL))'
);
PREPARE add_account_link_check FROM @add_account_link_check;
EXECUTE add_account_link_check;
DEALLOCATE PREPARE add_account_link_check;

-- ===========================================================================
-- ATTRIBUTI E REGOLE REFERENZIALI DEI CAPITOLI 1-3
-- ===========================================================================

SET @add_data_registrazione = IF(
    EXISTS (
        SELECT 1
        FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 'UTENTE'
          AND COLUMN_NAME = 'Data_Registrazione'
    ),
    'SELECT 1',
    'ALTER TABLE UTENTE ADD COLUMN Data_Registrazione DATE NOT NULL DEFAULT (CURRENT_DATE) AFTER Data_Nascita'
);
PREPARE add_data_registrazione FROM @add_data_registrazione;
EXECUTE add_data_registrazione;
DEALLOCATE PREPARE add_data_registrazione;

-- Le qualifiche non vengono cancellate se esistono rapporti sportivi storici.
SET @drop_fk_atleta = IF(
    EXISTS (
        SELECT 1
        FROM information_schema.REFERENTIAL_CONSTRAINTS
        WHERE CONSTRAINT_SCHEMA = DATABASE()
          AND TABLE_NAME = 'ATLETA'
          AND CONSTRAINT_NAME = 'FK_ATLETA_UTENTE'
    ),
    'ALTER TABLE ATLETA DROP FOREIGN KEY FK_ATLETA_UTENTE',
    'SELECT 1'
);
PREPARE drop_fk_atleta FROM @drop_fk_atleta;
EXECUTE drop_fk_atleta;
DEALLOCATE PREPARE drop_fk_atleta;

ALTER TABLE ATLETA
    ADD CONSTRAINT FK_ATLETA_UTENTE FOREIGN KEY (ID_Utente)
        REFERENCES UTENTE (ID_Utente)
        ON UPDATE CASCADE
        ON DELETE RESTRICT;

SET @drop_fk_istruttore = IF(
    EXISTS (
        SELECT 1
        FROM information_schema.REFERENTIAL_CONSTRAINTS
        WHERE CONSTRAINT_SCHEMA = DATABASE()
          AND TABLE_NAME = 'ISTRUTTORE'
          AND CONSTRAINT_NAME = 'FK_ISTRUTTORE_UTENTE'
    ),
    'ALTER TABLE ISTRUTTORE DROP FOREIGN KEY FK_ISTRUTTORE_UTENTE',
    'SELECT 1'
);
PREPARE drop_fk_istruttore FROM @drop_fk_istruttore;
EXECUTE drop_fk_istruttore;
DEALLOCATE PREPARE drop_fk_istruttore;

ALTER TABLE ISTRUTTORE
    ADD CONSTRAINT FK_ISTRUTTORE_UTENTE FOREIGN KEY (ID_Utente)
        REFERENCES UTENTE (ID_Utente)
        ON UPDATE CASCADE
        ON DELETE RESTRICT;

SET SQL_SAFE_UPDATES = @OLD_SQL_SAFE_UPDATES;
