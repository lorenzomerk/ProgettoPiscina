-- Gestione Piscina - installazione completa del database.
-- Crea da zero tabelle, indici, viste e trigger coerenti con i capitoli 1-3.
-- Lo script è idempotente e non elimina dati esistenti. Per una reinstallazione
-- completamente pulita eliminare prima piscina_progetto da MySQL Workbench.

CREATE DATABASE IF NOT EXISTS piscina_progetto
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE piscina_progetto;

CREATE TABLE IF NOT EXISTS UTENTE (
    ID_Utente BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    Codice_Fiscale CHAR(16) NOT NULL,
    Nome VARCHAR(80) NOT NULL,
    Cognome VARCHAR(80) NOT NULL,
    Data_Nascita DATE NOT NULL,
    Data_Registrazione DATE NOT NULL DEFAULT (CURRENT_DATE),
    Scadenza_Certificato_Medico DATE,
    Email VARCHAR(255),
    Telefono VARCHAR(30),
    Creato_Il TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    Aggiornato_Il TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
        ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT PK_UTENTE PRIMARY KEY (ID_Utente),
    CONSTRAINT UQ_UTENTE_CODICE_FISCALE UNIQUE (Codice_Fiscale),
    CONSTRAINT UQ_UTENTE_EMAIL UNIQUE (Email),
    CONSTRAINT CK_UTENTE_CODICE_FISCALE CHECK (
        Codice_Fiscale REGEXP '^[A-Z0-9]{16}$'
    ),
    CONSTRAINT CK_UTENTE_NOME CHECK (CHAR_LENGTH(TRIM(Nome)) > 0),
    CONSTRAINT CK_UTENTE_COGNOME CHECK (CHAR_LENGTH(TRIM(Cognome)) > 0),
    INDEX IDX_UTENTE_COGNOME_NOME (Cognome, Nome),
    INDEX IDX_UTENTE_SCADENZA_CERTIFICATO (
        Scadenza_Certificato_Medico
    )
);

-- Allinea anche le installazioni precedenti, nelle quali UTENTE aveva uno
-- stato applicativo ora demandato esclusivamente ad ACCOUNT.Attivo.
SET @drop_utente_attivo = IF(
    EXISTS (
        SELECT 1
        FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 'UTENTE'
          AND COLUMN_NAME = 'Attivo'
    ),
    'ALTER TABLE UTENTE DROP COLUMN Attivo',
    'SELECT 1'
);
PREPARE drop_utente_attivo FROM @drop_utente_attivo;
EXECUTE drop_utente_attivo;
DEALLOCATE PREPARE drop_utente_attivo;

-- Specializzazione parziale e sovrapposta di UTENTE.
CREATE TABLE IF NOT EXISTS ATLETA (
    ID_Utente BIGINT UNSIGNED NOT NULL,
    CONSTRAINT PK_ATLETA PRIMARY KEY (ID_Utente),
    CONSTRAINT FK_ATLETA_UTENTE FOREIGN KEY (ID_Utente)
        REFERENCES UTENTE (ID_Utente)
        ON UPDATE CASCADE
        ON DELETE RESTRICT
);

CREATE TABLE IF NOT EXISTS ISTRUTTORE (
    ID_Utente BIGINT UNSIGNED NOT NULL,
    Qualifica VARCHAR(120) NOT NULL,
    CONSTRAINT PK_ISTRUTTORE PRIMARY KEY (ID_Utente),
    CONSTRAINT FK_ISTRUTTORE_UTENTE FOREIGN KEY (ID_Utente)
        REFERENCES UTENTE (ID_Utente)
        ON UPDATE CASCADE
        ON DELETE RESTRICT,
    CONSTRAINT CK_ISTRUTTORE_QUALIFICA CHECK (
        CHAR_LENGTH(TRIM(Qualifica)) > 0
    )
);

CREATE TABLE IF NOT EXISTS TIPO_ABBONAMENTO (
    ID_Tipo_Abbonamento BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    Nome VARCHAR(120) NOT NULL,
    Costo DECIMAL(10, 2) NOT NULL,
    Attivo BOOLEAN NOT NULL DEFAULT FALSE,
    Modalita_Validita ENUM('TEMPO', 'INGRESSI') NOT NULL,
    Condizioni_Utilizzo TEXT NOT NULL,
    Durata_Giorni INT UNSIGNED,
    Numero_Ingressi INT UNSIGNED,
    CONSTRAINT PK_TIPO_ABBONAMENTO PRIMARY KEY (ID_Tipo_Abbonamento),
    CONSTRAINT UQ_TIPO_ABBONAMENTO_NOME UNIQUE (Nome),
    CONSTRAINT CK_TIPO_ABBONAMENTO_COSTO CHECK (Costo >= 0),
    CONSTRAINT CK_TIPO_ABBONAMENTO_MODALITA CHECK (
        (Modalita_Validita = 'TEMPO'
            AND Durata_Giorni IS NOT NULL
            AND Durata_Giorni > 0
            AND Numero_Ingressi IS NULL)
        OR
        (Modalita_Validita = 'INGRESSI'
            AND Numero_Ingressi IS NOT NULL
            AND Numero_Ingressi > 0
            AND Durata_Giorni IS NULL)
    )
);

CREATE TABLE IF NOT EXISTS TIPO_ATTIVITA (
    ID_Tipo_Attivita BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    Nome VARCHAR(120) NOT NULL,
    Modalita_Partecipazione ENUM(
        'ISCRIZIONE', 'ACCESSO_LIBERO', 'SQUADRA'
    ) NOT NULL,
    Richiede_Istruttore BOOLEAN NOT NULL DEFAULT FALSE,
    Descrizione TEXT,
    CONSTRAINT PK_TIPO_ATTIVITA PRIMARY KEY (ID_Tipo_Attivita),
    CONSTRAINT UQ_TIPO_ATTIVITA_NOME UNIQUE (Nome)
);

CREATE TABLE IF NOT EXISTS COMPATIBILITA (
    ID_Tipo_Abbonamento BIGINT UNSIGNED NOT NULL,
    ID_Tipo_Attivita BIGINT UNSIGNED NOT NULL,
    CONSTRAINT PK_COMPATIBILITA PRIMARY KEY (
        ID_Tipo_Abbonamento, ID_Tipo_Attivita
    ),
    CONSTRAINT FK_COMPATIBILITA_ABBONAMENTO FOREIGN KEY (
        ID_Tipo_Abbonamento
    ) REFERENCES TIPO_ABBONAMENTO (ID_Tipo_Abbonamento)
        ON UPDATE CASCADE
        ON DELETE RESTRICT,
    CONSTRAINT FK_COMPATIBILITA_ATTIVITA FOREIGN KEY (
        ID_Tipo_Attivita
    ) REFERENCES TIPO_ATTIVITA (ID_Tipo_Attivita)
        ON UPDATE CASCADE
        ON DELETE RESTRICT
);

CREATE TABLE IF NOT EXISTS ABBONAMENTO (
    ID_Abbonamento BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    ID_Utente BIGINT UNSIGNED NOT NULL,
    ID_Tipo_Abbonamento BIGINT UNSIGNED NOT NULL,
    Data_Acquisto DATE NOT NULL DEFAULT (CURRENT_DATE),
    Data_Inizio DATE NOT NULL,
    Data_Fine DATE,
    Stato ENUM('ATTIVO', 'SCADUTO', 'ESAURITO') NOT NULL DEFAULT 'ATTIVO',
    Ingressi_Rimanenti INT UNSIGNED,
    CONSTRAINT PK_ABBONAMENTO PRIMARY KEY (ID_Abbonamento),
    CONSTRAINT FK_ABBONAMENTO_UTENTE FOREIGN KEY (ID_Utente)
        REFERENCES UTENTE (ID_Utente)
        ON UPDATE CASCADE
        ON DELETE RESTRICT,
    CONSTRAINT FK_ABBONAMENTO_TIPO FOREIGN KEY (ID_Tipo_Abbonamento)
        REFERENCES TIPO_ABBONAMENTO (ID_Tipo_Abbonamento)
        ON UPDATE CASCADE
        ON DELETE RESTRICT,
    CONSTRAINT CK_ABBONAMENTO_DATE CHECK (
        Data_Acquisto <= Data_Inizio
        AND (Data_Fine IS NULL OR Data_Inizio <= Data_Fine)
    ),
    INDEX IDX_ABBONAMENTO_UTENTE (ID_Utente),
    INDEX IDX_ABBONAMENTO_VALIDITA (Stato, Data_Inizio, Data_Fine)
);

CREATE TABLE IF NOT EXISTS ATTIVITA_PROGRAMMATA (
    ID_Attivita_Programmata BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    ID_Tipo_Attivita BIGINT UNSIGNED NOT NULL,
    Titolo VARCHAR(160) NOT NULL,
    Giorno_Settimanale ENUM(
        'LUNEDI', 'MARTEDI', 'MERCOLEDI', 'GIOVEDI',
        'VENERDI', 'SABATO', 'DOMENICA'
    ) NOT NULL,
    Ora_Inizio TIME NOT NULL,
    Ora_Fine TIME NOT NULL,
    Capienza_Massima INT UNSIGNED NOT NULL,
    Periodo_Inizio DATE NOT NULL,
    Periodo_Fine DATE NOT NULL,
    Stato ENUM(
        'PROGRAMMATA', 'ATTIVA', 'CONCLUSA', 'ANNULLATA'
    ) NOT NULL DEFAULT 'PROGRAMMATA',
    CONSTRAINT PK_ATTIVITA_PROGRAMMATA PRIMARY KEY (
        ID_Attivita_Programmata
    ),
    CONSTRAINT FK_ATTIVITA_PROGRAMMATA_TIPO FOREIGN KEY (
        ID_Tipo_Attivita
    ) REFERENCES TIPO_ATTIVITA (ID_Tipo_Attivita)
        ON UPDATE CASCADE
        ON DELETE RESTRICT,
    CONSTRAINT CK_ATTIVITA_PROGRAMMATA_INTERVALLI CHECK (
        Periodo_Inizio <= Periodo_Fine
        AND Ora_Inizio < Ora_Fine
        AND Capienza_Massima > 0
    ),
    INDEX IDX_ATTIVITA_CALENDARIO (
        Giorno_Settimanale, Periodo_Inizio, Periodo_Fine,
        Ora_Inizio, Ora_Fine
    )
);

CREATE TABLE IF NOT EXISTS ISCRIZIONE_ATTIVITA (
    ID_Iscrizione_Attivita BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    ID_Abbonamento BIGINT UNSIGNED NOT NULL,
    ID_Attivita_Programmata BIGINT UNSIGNED NOT NULL,
    Data_Iscrizione DATE NOT NULL DEFAULT (CURRENT_DATE),
    CONSTRAINT PK_ISCRIZIONE_ATTIVITA PRIMARY KEY (
        ID_Iscrizione_Attivita
    ),
    CONSTRAINT FK_ISCRIZIONE_ABBONAMENTO FOREIGN KEY (ID_Abbonamento)
        REFERENCES ABBONAMENTO (ID_Abbonamento)
        ON UPDATE CASCADE
        ON DELETE RESTRICT,
    CONSTRAINT FK_ISCRIZIONE_ATTIVITA FOREIGN KEY (
        ID_Attivita_Programmata
    ) REFERENCES ATTIVITA_PROGRAMMATA (ID_Attivita_Programmata)
        ON UPDATE CASCADE
        ON DELETE RESTRICT,
    INDEX IDX_ISCRIZIONE_ATTIVITA (ID_Attivita_Programmata)
);

CREATE TABLE IF NOT EXISTS ACCESSO_NUOTO_LIBERO (
    ID_Accesso_Nuoto_Libero BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    ID_Abbonamento BIGINT UNSIGNED NOT NULL,
    ID_Attivita_Programmata BIGINT UNSIGNED NOT NULL,
    Data_Ora_Accesso DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT PK_ACCESSO_NUOTO_LIBERO PRIMARY KEY (
        ID_Accesso_Nuoto_Libero
    ),
    CONSTRAINT FK_ACCESSO_ABBONAMENTO FOREIGN KEY (ID_Abbonamento)
        REFERENCES ABBONAMENTO (ID_Abbonamento)
        ON UPDATE CASCADE
        ON DELETE RESTRICT,
    CONSTRAINT FK_ACCESSO_ATTIVITA FOREIGN KEY (
        ID_Attivita_Programmata
    ) REFERENCES ATTIVITA_PROGRAMMATA (ID_Attivita_Programmata)
        ON UPDATE CASCADE
        ON DELETE RESTRICT,
    INDEX IDX_ACCESSO_DATA (Data_Ora_Accesso),
    INDEX IDX_ACCESSO_ATTIVITA (ID_Attivita_Programmata)
);

CREATE TABLE IF NOT EXISTS VASCA (
    ID_Vasca BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    Nome VARCHAR(120) NOT NULL,
    Lunghezza DECIMAL(6, 2) NOT NULL,
    Larghezza DECIMAL(6, 2) NOT NULL,
    Profondita DECIMAL(5, 2) NOT NULL,
    Temperatura DECIMAL(4, 1) NOT NULL,
    Tipologia VARCHAR(100) NOT NULL,
    CONSTRAINT PK_VASCA PRIMARY KEY (ID_Vasca),
    CONSTRAINT UQ_VASCA_NOME UNIQUE (Nome),
    CONSTRAINT CK_VASCA_DIMENSIONI CHECK (
        Lunghezza > 0 AND Larghezza > 0
        AND Profondita > 0 AND Temperatura > 0
    )
);

-- CORSIA è identificata dalla vasca e dal numero.
CREATE TABLE IF NOT EXISTS CORSIA (
    ID_Vasca BIGINT UNSIGNED NOT NULL,
    Numero INT UNSIGNED NOT NULL,
    CONSTRAINT PK_CORSIA PRIMARY KEY (ID_Vasca, Numero),
    CONSTRAINT FK_CORSIA_VASCA FOREIGN KEY (ID_Vasca)
        REFERENCES VASCA (ID_Vasca)
        ON UPDATE CASCADE
        ON DELETE RESTRICT,
    CONSTRAINT CK_CORSIA_NUMERO CHECK (Numero > 0)
);

CREATE TABLE IF NOT EXISTS UTILIZZA (
    ID_Attivita_Programmata BIGINT UNSIGNED NOT NULL,
    ID_Vasca BIGINT UNSIGNED NOT NULL,
    Numero_Corsia INT UNSIGNED NOT NULL,
    CONSTRAINT PK_UTILIZZA PRIMARY KEY (
        ID_Attivita_Programmata, ID_Vasca, Numero_Corsia
    ),
    CONSTRAINT FK_UTILIZZA_ATTIVITA FOREIGN KEY (
        ID_Attivita_Programmata
    ) REFERENCES ATTIVITA_PROGRAMMATA (ID_Attivita_Programmata)
        ON UPDATE CASCADE
        ON DELETE RESTRICT,
    CONSTRAINT FK_UTILIZZA_CORSIA FOREIGN KEY (
        ID_Vasca, Numero_Corsia
    ) REFERENCES CORSIA (ID_Vasca, Numero)
        ON UPDATE CASCADE
        ON DELETE RESTRICT
);

CREATE TABLE IF NOT EXISTS ASSEGNATO_A (
    ID_Utente_Istruttore BIGINT UNSIGNED NOT NULL,
    ID_Attivita_Programmata BIGINT UNSIGNED NOT NULL,
    CONSTRAINT PK_ASSEGNATO_A PRIMARY KEY (
        ID_Utente_Istruttore, ID_Attivita_Programmata
    ),
    CONSTRAINT FK_ASSEGNATO_A_ISTRUTTORE FOREIGN KEY (
        ID_Utente_Istruttore
    ) REFERENCES ISTRUTTORE (ID_Utente)
        ON UPDATE CASCADE
        ON DELETE RESTRICT,
    CONSTRAINT FK_ASSEGNATO_A_ATTIVITA FOREIGN KEY (
        ID_Attivita_Programmata
    ) REFERENCES ATTIVITA_PROGRAMMATA (ID_Attivita_Programmata)
        ON UPDATE CASCADE
        ON DELETE RESTRICT
);

CREATE TABLE IF NOT EXISTS CLUB_SPORTIVO (
    ID_Club BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    Nome VARCHAR(160) NOT NULL,
    Email VARCHAR(255),
    Telefono VARCHAR(30),
    Indirizzo VARCHAR(255),
    CONSTRAINT PK_CLUB_SPORTIVO PRIMARY KEY (ID_Club),
    CONSTRAINT UQ_CLUB_SPORTIVO_NOME UNIQUE (Nome)
);

CREATE TABLE IF NOT EXISTS SQUADRA (
    ID_Squadra BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    ID_Club BIGINT UNSIGNED NOT NULL,
    Nome VARCHAR(160) NOT NULL,
    Categoria VARCHAR(100) NOT NULL,
    CONSTRAINT PK_SQUADRA PRIMARY KEY (ID_Squadra),
    CONSTRAINT UQ_SQUADRA_CLUB_NOME UNIQUE (ID_Club, Nome),
    CONSTRAINT FK_SQUADRA_CLUB FOREIGN KEY (ID_Club)
        REFERENCES CLUB_SPORTIVO (ID_Club)
        ON UPDATE CASCADE
        ON DELETE RESTRICT
);

CREATE TABLE IF NOT EXISTS APPARTENENZA_SQUADRA (
    ID_Appartenenza BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    ID_Utente_Atleta BIGINT UNSIGNED NOT NULL,
    ID_Squadra BIGINT UNSIGNED NOT NULL,
    Data_Inizio DATE NOT NULL,
    Data_Fine DATE,
    CONSTRAINT PK_APPARTENENZA_SQUADRA PRIMARY KEY (ID_Appartenenza),
    CONSTRAINT FK_APPARTENENZA_ATLETA FOREIGN KEY (ID_Utente_Atleta)
        REFERENCES ATLETA (ID_Utente)
        ON UPDATE CASCADE
        ON DELETE RESTRICT,
    CONSTRAINT FK_APPARTENENZA_SQUADRA FOREIGN KEY (ID_Squadra)
        REFERENCES SQUADRA (ID_Squadra)
        ON UPDATE CASCADE
        ON DELETE RESTRICT,
    CONSTRAINT CK_APPARTENENZA_DATE CHECK (
        Data_Fine IS NULL OR Data_Inizio <= Data_Fine
    ),
    INDEX IDX_APPARTENENZA_ATLETA_DATE (
        ID_Utente_Atleta, Data_Inizio, Data_Fine
    )
);

CREATE TABLE IF NOT EXISTS INCARICO_SQUADRA (
    ID_Incarico_Squadra BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    ID_Utente_Istruttore BIGINT UNSIGNED NOT NULL,
    ID_Squadra BIGINT UNSIGNED NOT NULL,
    Data_Inizio DATE NOT NULL,
    Data_Fine DATE,
    CONSTRAINT PK_INCARICO_SQUADRA PRIMARY KEY (ID_Incarico_Squadra),
    CONSTRAINT FK_INCARICO_ISTRUTTORE FOREIGN KEY (
        ID_Utente_Istruttore
    ) REFERENCES ISTRUTTORE (ID_Utente)
        ON UPDATE CASCADE
        ON DELETE RESTRICT,
    CONSTRAINT FK_INCARICO_SQUADRA FOREIGN KEY (ID_Squadra)
        REFERENCES SQUADRA (ID_Squadra)
        ON UPDATE CASCADE
        ON DELETE RESTRICT,
    CONSTRAINT CK_INCARICO_DATE CHECK (
        Data_Fine IS NULL OR Data_Inizio <= Data_Fine
    ),
    INDEX IDX_INCARICO_ISTRUTTORE_SQUADRA (
        ID_Utente_Istruttore, ID_Squadra, Data_Inizio, Data_Fine
    )
);

CREATE TABLE IF NOT EXISTS SVOLGE (
    ID_Squadra BIGINT UNSIGNED NOT NULL,
    ID_Attivita_Programmata BIGINT UNSIGNED NOT NULL,
    CONSTRAINT PK_SVOLGE PRIMARY KEY (
        ID_Squadra, ID_Attivita_Programmata
    ),
    CONSTRAINT FK_SVOLGE_SQUADRA FOREIGN KEY (ID_Squadra)
        REFERENCES SQUADRA (ID_Squadra)
        ON UPDATE CASCADE
        ON DELETE RESTRICT,
    CONSTRAINT FK_SVOLGE_ATTIVITA FOREIGN KEY (
        ID_Attivita_Programmata
    ) REFERENCES ATTIVITA_PROGRAMMATA (ID_Attivita_Programmata)
        ON UPDATE CASCADE
        ON DELETE RESTRICT
);

-- ACCOUNT è un supporto tecnico opzionale dell'applicazione e non una
-- entità del dominio E/R. Atleta e istruttore restano qualifiche di UTENTE.
CREATE TABLE IF NOT EXISTS ACCOUNT (
    ID_Account BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    ID_Utente BIGINT UNSIGNED,
    ID_Club BIGINT UNSIGNED,
    Nome VARCHAR(80) NOT NULL,
    Cognome VARCHAR(80) NOT NULL,
    Email VARCHAR(255) NOT NULL,
    Password_Hash VARCHAR(128) NOT NULL,
    Password_Salt VARCHAR(64) NOT NULL,
    Password_Iterazioni INT UNSIGNED NOT NULL,
    Ruolo VARCHAR(30) NOT NULL DEFAULT 'UTENTE',
    Attivo BOOLEAN NOT NULL DEFAULT TRUE,
    Creato_Il TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    Ultimo_Accesso TIMESTAMP NULL,
    CONSTRAINT PK_ACCOUNT PRIMARY KEY (ID_Account),
    CONSTRAINT UQ_ACCOUNT_EMAIL UNIQUE (Email),
    CONSTRAINT UQ_ACCOUNT_UTENTE UNIQUE (ID_Utente),
    CONSTRAINT UQ_ACCOUNT_CLUB UNIQUE (ID_Club),
    CONSTRAINT FK_ACCOUNT_UTENTE FOREIGN KEY (ID_Utente)
        REFERENCES UTENTE (ID_Utente)
        ON UPDATE CASCADE
        ON DELETE RESTRICT,
    CONSTRAINT FK_ACCOUNT_CLUB FOREIGN KEY (ID_Club)
        REFERENCES CLUB_SPORTIVO (ID_Club)
        ON UPDATE CASCADE
        ON DELETE RESTRICT,
    CONSTRAINT CK_ACCOUNT_RUOLO CHECK (
        Ruolo IN (
            'AMMINISTRATORE', 'CLUB', 'UTENTE'
        )
    ),
    CONSTRAINT CK_ACCOUNT_ITERAZIONI CHECK (
        Password_Iterazioni >= 100000
    ),
    CONSTRAINT CK_ACCOUNT_COLLEGAMENTO CHECK (
        (Ruolo = 'AMMINISTRATORE'
            AND ID_Utente IS NULL AND ID_Club IS NULL)
        OR (Ruolo = 'CLUB'
            AND ID_Utente IS NULL AND ID_Club IS NOT NULL)
        OR (Ruolo = 'UTENTE'
            AND ID_Utente IS NOT NULL AND ID_Club IS NULL)
    )
);

-- Viste operative e aggregate corrispondenti a OP7-OP11.
CREATE OR REPLACE VIEW VW_ATTIVITA_COMPLETE AS
SELECT
    ap.ID_Attivita_Programmata,
    ap.Titolo,
    ap.Capienza_Massima,
    COUNT(ia.ID_Iscrizione_Attivita) AS Numero_Iscrizioni
FROM ATTIVITA_PROGRAMMATA ap
JOIN TIPO_ATTIVITA ta
  ON ta.ID_Tipo_Attivita = ap.ID_Tipo_Attivita
LEFT JOIN ISCRIZIONE_ATTIVITA ia
  ON ia.ID_Attivita_Programmata = ap.ID_Attivita_Programmata
WHERE ta.Modalita_Partecipazione = 'ISCRIZIONE'
GROUP BY
    ap.ID_Attivita_Programmata, ap.Titolo, ap.Capienza_Massima
HAVING COUNT(ia.ID_Iscrizione_Attivita) >= ap.Capienza_Massima;

CREATE OR REPLACE VIEW VW_CLUB_ATLETI_ATTIVI AS
SELECT
    c.ID_Club,
    c.Nome,
    COUNT(DISTINCT a.ID_Utente_Atleta) AS Numero_Atleti
FROM CLUB_SPORTIVO c
LEFT JOIN SQUADRA s ON s.ID_Club = c.ID_Club
LEFT JOIN APPARTENENZA_SQUADRA a
  ON a.ID_Squadra = s.ID_Squadra
 AND CURRENT_DATE BETWEEN a.Data_Inizio
     AND COALESCE(a.Data_Fine, '9999-12-31')
GROUP BY c.ID_Club, c.Nome;

CREATE OR REPLACE VIEW VW_SQUADRA_ATLETI_ATTIVI AS
SELECT
    s.ID_Squadra,
    s.Nome,
    c.Nome AS Club,
    COUNT(a.ID_Appartenenza) AS Numero_Atleti
FROM SQUADRA s
JOIN CLUB_SPORTIVO c ON c.ID_Club = s.ID_Club
LEFT JOIN APPARTENENZA_SQUADRA a
  ON a.ID_Squadra = s.ID_Squadra
 AND CURRENT_DATE BETWEEN a.Data_Inizio
     AND COALESCE(a.Data_Fine, '9999-12-31')
GROUP BY s.ID_Squadra, s.Nome, c.Nome;

CREATE OR REPLACE VIEW VW_ABBONAMENTI_UTILIZZABILI AS
SELECT a.*
FROM ABBONAMENTO a
JOIN TIPO_ABBONAMENTO t
  ON t.ID_Tipo_Abbonamento = a.ID_Tipo_Abbonamento
WHERE a.Stato = 'ATTIVO'
  AND a.Data_Inizio <= CURRENT_DATE
  AND (
      (t.Modalita_Validita = 'TEMPO'
          AND CURRENT_DATE <= a.Data_Fine)
      OR
      (t.Modalita_Validita = 'INGRESSI'
          AND a.Ingressi_Rimanenti > 0)
  );

DELIMITER $$

DROP TRIGGER IF EXISTS TR_COMPATIBILITA_BI$$
CREATE TRIGGER TR_COMPATIBILITA_BI
BEFORE INSERT ON COMPATIBILITA
FOR EACH ROW
BEGIN
    DECLARE v_modalita_validita VARCHAR(20);
    DECLARE v_modalita_partecipazione VARCHAR(30);

    SELECT Modalita_Validita
      INTO v_modalita_validita
      FROM TIPO_ABBONAMENTO
     WHERE ID_Tipo_Abbonamento = NEW.ID_Tipo_Abbonamento;
    SELECT Modalita_Partecipazione
      INTO v_modalita_partecipazione
      FROM TIPO_ATTIVITA
     WHERE ID_Tipo_Attivita = NEW.ID_Tipo_Attivita;

    IF v_modalita_validita = 'INGRESSI'
       AND v_modalita_partecipazione <> 'ACCESSO_LIBERO' THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT =
                'Un abbonamento a ingressi consente solo accesso libero';
    END IF;
END$$

DROP TRIGGER IF EXISTS TR_TIPO_ABBONAMENTO_BU$$
CREATE TRIGGER TR_TIPO_ABBONAMENTO_BU
BEFORE UPDATE ON TIPO_ABBONAMENTO
FOR EACH ROW
BEGIN
    IF (
        NEW.Modalita_Validita <> OLD.Modalita_Validita
        OR NOT (NEW.Durata_Giorni <=> OLD.Durata_Giorni)
        OR NOT (NEW.Numero_Ingressi <=> OLD.Numero_Ingressi)
    ) AND EXISTS (
        SELECT 1 FROM ABBONAMENTO a
         WHERE a.ID_Tipo_Abbonamento = OLD.ID_Tipo_Abbonamento
    ) THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT =
                'Non modificare validita o quantita di un tipo gia acquistato';
    END IF;
    IF NEW.Modalita_Validita = 'INGRESSI' AND EXISTS (
        SELECT 1
          FROM COMPATIBILITA c
          JOIN TIPO_ATTIVITA ta
            ON ta.ID_Tipo_Attivita = c.ID_Tipo_Attivita
         WHERE c.ID_Tipo_Abbonamento =
               OLD.ID_Tipo_Abbonamento
           AND ta.Modalita_Partecipazione <> 'ACCESSO_LIBERO'
    ) THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT =
                'Gli abbonamenti a ingressi consentono solo accesso libero';
    END IF;
    IF NEW.Attivo = TRUE AND NOT EXISTS (
        SELECT 1
          FROM COMPATIBILITA c
         WHERE c.ID_Tipo_Abbonamento = OLD.ID_Tipo_Abbonamento
    ) THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT =
                'Un tipo attivo deve avere almeno una compatibilita';
    END IF;
END$$

DROP TRIGGER IF EXISTS TR_TIPO_ATTIVITA_BU$$
CREATE TRIGGER TR_TIPO_ATTIVITA_BU
BEFORE UPDATE ON TIPO_ATTIVITA
FOR EACH ROW
BEGIN
    IF NEW.Modalita_Partecipazione <>
       OLD.Modalita_Partecipazione AND EXISTS (
        SELECT 1 FROM ATTIVITA_PROGRAMMATA ap
         WHERE ap.ID_Tipo_Attivita = OLD.ID_Tipo_Attivita
    ) THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT =
                'Non cambiare modalita a un tipo gia programmato';
    END IF;
    IF NEW.Modalita_Partecipazione <> 'ACCESSO_LIBERO' AND EXISTS (
        SELECT 1
          FROM COMPATIBILITA c
          JOIN TIPO_ABBONAMENTO t
            ON t.ID_Tipo_Abbonamento =
               c.ID_Tipo_Abbonamento
         WHERE c.ID_Tipo_Attivita = OLD.ID_Tipo_Attivita
           AND t.Modalita_Validita = 'INGRESSI'
    ) THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT =
                'Esiste una compatibilita con abbonamenti a ingressi';
    END IF;
    IF NEW.Richiede_Istruttore = TRUE
       AND OLD.Richiede_Istruttore = FALSE AND EXISTS (
        SELECT 1
          FROM ATTIVITA_PROGRAMMATA ap
         WHERE ap.ID_Tipo_Attivita = OLD.ID_Tipo_Attivita
           AND ap.Stato = 'ATTIVA'
           AND NOT EXISTS (
               SELECT 1 FROM ASSEGNATO_A aa
                WHERE aa.ID_Attivita_Programmata =
                      ap.ID_Attivita_Programmata
           )
    ) THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT =
                'Assegnare gli istruttori prima di rendere il requisito obbligatorio';
    END IF;
END$$

DROP TRIGGER IF EXISTS TR_TIPO_ABBONAMENTO_BI$$
CREATE TRIGGER TR_TIPO_ABBONAMENTO_BI
BEFORE INSERT ON TIPO_ABBONAMENTO
FOR EACH ROW
BEGIN
    IF NEW.Attivo = TRUE THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT =
                'Creare il tipo non attivo, configurare le compatibilita e attivarlo';
    END IF;
END$$

DROP TRIGGER IF EXISTS TR_COMPATIBILITA_BD$$
CREATE TRIGGER TR_COMPATIBILITA_BD
BEFORE DELETE ON COMPATIBILITA
FOR EACH ROW
BEGIN
    IF EXISTS (
        SELECT 1
          FROM TIPO_ABBONAMENTO t
         WHERE t.ID_Tipo_Abbonamento = OLD.ID_Tipo_Abbonamento
           AND t.Attivo = TRUE
    ) AND (
        SELECT COUNT(*)
          FROM COMPATIBILITA c
         WHERE c.ID_Tipo_Abbonamento = OLD.ID_Tipo_Abbonamento
    ) = 1 THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT =
                'Disattivare il tipo prima di rimuovere l ultima compatibilita';
    END IF;
END$$

DROP TRIGGER IF EXISTS TR_ABBONAMENTO_BI$$
CREATE TRIGGER TR_ABBONAMENTO_BI
BEFORE INSERT ON ABBONAMENTO
FOR EACH ROW
BEGIN
    DECLARE v_modalita VARCHAR(20);
    DECLARE v_durata INT;
    DECLARE v_ingressi INT;
    DECLARE v_tipo_attivo BOOLEAN;

    SELECT Modalita_Validita, Durata_Giorni, Numero_Ingressi, Attivo
      INTO v_modalita, v_durata, v_ingressi, v_tipo_attivo
      FROM TIPO_ABBONAMENTO
     WHERE ID_Tipo_Abbonamento = NEW.ID_Tipo_Abbonamento;

    IF v_tipo_attivo = FALSE THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT =
                'Il tipo di abbonamento non e disponibile per nuovi acquisti';
    END IF;

    IF NEW.Data_Acquisto > NEW.Data_Inizio THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT =
                'La data di acquisto non puo superare la data di inizio';
    END IF;

    IF v_modalita = 'TEMPO' THEN
        IF NEW.Data_Fine IS NULL THEN
            SET NEW.Data_Fine =
                DATE_ADD(
                    NEW.Data_Inizio,
                    INTERVAL (v_durata - 1) DAY
                );
        END IF;
        IF NEW.Data_Fine <>
           DATE_ADD(
               NEW.Data_Inizio,
               INTERVAL (v_durata - 1) DAY
           ) THEN
            SIGNAL SQLSTATE '45000'
                SET MESSAGE_TEXT =
                    'La data di fine non rispetta la durata del tipo';
        END IF;
        SET NEW.Ingressi_Rimanenti = NULL;
    ELSE
        SET NEW.Data_Fine = NULL;
        IF NEW.Ingressi_Rimanenti IS NULL THEN
            SET NEW.Ingressi_Rimanenti = v_ingressi;
        END IF;
        IF NEW.Ingressi_Rimanenti > v_ingressi THEN
            SIGNAL SQLSTATE '45000'
                SET MESSAGE_TEXT =
                    'Gli ingressi residui superano quelli del tipo';
        END IF;
    END IF;
    IF v_modalita = 'TEMPO'
       AND CURRENT_DATE > NEW.Data_Fine THEN
        SET NEW.Stato = 'SCADUTO';
    ELSEIF v_modalita = 'INGRESSI'
       AND NEW.Ingressi_Rimanenti = 0 THEN
        SET NEW.Stato = 'ESAURITO';
    ELSE
        SET NEW.Stato = 'ATTIVO';
    END IF;
END$$

DROP TRIGGER IF EXISTS TR_ABBONAMENTO_BU$$
CREATE TRIGGER TR_ABBONAMENTO_BU
BEFORE UPDATE ON ABBONAMENTO
FOR EACH ROW
BEGIN
    DECLARE v_modalita VARCHAR(20);
    DECLARE v_durata INT;
    DECLARE v_ingressi INT;

    SELECT Modalita_Validita, Durata_Giorni, Numero_Ingressi
      INTO v_modalita, v_durata, v_ingressi
      FROM TIPO_ABBONAMENTO
     WHERE ID_Tipo_Abbonamento = NEW.ID_Tipo_Abbonamento;

    IF (
        NEW.ID_Utente <> OLD.ID_Utente
        OR NEW.ID_Tipo_Abbonamento <> OLD.ID_Tipo_Abbonamento
    ) AND (
        EXISTS (
            SELECT 1 FROM ISCRIZIONE_ATTIVITA i
             WHERE i.ID_Abbonamento = OLD.ID_Abbonamento
        )
        OR EXISTS (
            SELECT 1 FROM ACCESSO_NUOTO_LIBERO x
             WHERE x.ID_Abbonamento = OLD.ID_Abbonamento
        )
    ) THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT =
                'Un abbonamento gia utilizzato non puo cambiare titolare o tipo';
    END IF;

    IF NEW.Data_Acquisto > NEW.Data_Inizio THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Intervallo dell abbonamento non valido';
    END IF;
    IF v_modalita = 'TEMPO' THEN
        IF NEW.Data_Fine IS NULL OR NEW.Data_Fine <>
           DATE_ADD(
               NEW.Data_Inizio,
               INTERVAL (v_durata - 1) DAY
           )
           OR NEW.Ingressi_Rimanenti IS NOT NULL THEN
            SIGNAL SQLSTATE '45000'
                SET MESSAGE_TEXT =
                    'Dati non coerenti con un abbonamento a tempo';
        END IF;
    ELSEIF NEW.Data_Fine IS NOT NULL
       OR NEW.Ingressi_Rimanenti IS NULL
       OR NEW.Ingressi_Rimanenti > v_ingressi THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT =
                'Dati non coerenti con un abbonamento a ingressi';
    END IF;

    IF v_modalita = 'TEMPO'
       AND CURRENT_DATE > NEW.Data_Fine THEN
        SET NEW.Stato = 'SCADUTO';
    ELSEIF v_modalita = 'INGRESSI'
       AND NEW.Ingressi_Rimanenti = 0 THEN
        SET NEW.Stato = 'ESAURITO';
    ELSE
        SET NEW.Stato = 'ATTIVO';
    END IF;
END$$

DROP TRIGGER IF EXISTS TR_ATTIVITA_BI$$
CREATE TRIGGER TR_ATTIVITA_BI
BEFORE INSERT ON ATTIVITA_PROGRAMMATA
FOR EACH ROW
BEGIN
    IF NEW.Periodo_Inizio > NEW.Periodo_Fine
       OR NEW.Ora_Inizio >= NEW.Ora_Fine THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Intervallo dell attivita non valido';
    END IF;
    IF NEW.Stato = 'ATTIVA' THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT =
                'Creare prima l attivita come PROGRAMMATA e assegnare le risorse';
    END IF;
END$$

DROP TRIGGER IF EXISTS TR_ATTIVITA_BU$$
CREATE TRIGGER TR_ATTIVITA_BU
BEFORE UPDATE ON ATTIVITA_PROGRAMMATA
FOR EACH ROW
BEGIN
    DECLARE v_modalita VARCHAR(30);
    DECLARE v_richiede_istruttore BOOLEAN;

    IF NEW.Periodo_Inizio > NEW.Periodo_Fine
       OR NEW.Ora_Inizio >= NEW.Ora_Fine THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Intervallo dell attivita non valido';
    END IF;

    SELECT Modalita_Partecipazione, Richiede_Istruttore
      INTO v_modalita, v_richiede_istruttore
      FROM TIPO_ATTIVITA
     WHERE ID_Tipo_Attivita = NEW.ID_Tipo_Attivita;

    IF v_modalita <> 'ISCRIZIONE' AND EXISTS (
        SELECT 1 FROM ISCRIZIONE_ATTIVITA i
         WHERE i.ID_Attivita_Programmata =
               OLD.ID_Attivita_Programmata
    ) THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT =
                'L attivita possiede iscrizioni individuali';
    END IF;
    IF v_modalita <> 'ACCESSO_LIBERO' AND EXISTS (
        SELECT 1 FROM ACCESSO_NUOTO_LIBERO x
         WHERE x.ID_Attivita_Programmata =
               OLD.ID_Attivita_Programmata
    ) THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT =
                'L attivita possiede accessi individuali';
    END IF;
    IF v_modalita <> 'SQUADRA' AND EXISTS (
        SELECT 1 FROM SVOLGE s
         WHERE s.ID_Attivita_Programmata =
               OLD.ID_Attivita_Programmata
    ) THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT =
                'L attivita e collegata a una squadra';
    END IF;

    IF EXISTS (
        SELECT 1
          FROM UTILIZZA propria
          JOIN UTILIZZA altra
            ON altra.ID_Vasca = propria.ID_Vasca
           AND altra.Numero_Corsia = propria.Numero_Corsia
           AND altra.ID_Attivita_Programmata <>
               propria.ID_Attivita_Programmata
          JOIN ATTIVITA_PROGRAMMATA esistente
            ON esistente.ID_Attivita_Programmata =
               altra.ID_Attivita_Programmata
         WHERE propria.ID_Attivita_Programmata =
               OLD.ID_Attivita_Programmata
           AND esistente.Stato <> 'ANNULLATA'
           AND NEW.Stato <> 'ANNULLATA'
           AND esistente.Giorno_Settimanale =
               NEW.Giorno_Settimanale
           AND esistente.Periodo_Inizio <= NEW.Periodo_Fine
           AND NEW.Periodo_Inizio <= esistente.Periodo_Fine
           AND esistente.Ora_Inizio < NEW.Ora_Fine
           AND NEW.Ora_Inizio < esistente.Ora_Fine
    ) THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT =
                'Il nuovo orario sovrappone l utilizzo di una corsia';
    END IF;

    IF EXISTS (
        SELECT 1
          FROM ASSEGNATO_A propria
          JOIN ASSEGNATO_A altra
            ON altra.ID_Utente_Istruttore =
               propria.ID_Utente_Istruttore
           AND altra.ID_Attivita_Programmata <>
               propria.ID_Attivita_Programmata
          JOIN ATTIVITA_PROGRAMMATA esistente
            ON esistente.ID_Attivita_Programmata =
               altra.ID_Attivita_Programmata
         WHERE propria.ID_Attivita_Programmata =
               OLD.ID_Attivita_Programmata
           AND esistente.Stato <> 'ANNULLATA'
           AND NEW.Stato <> 'ANNULLATA'
           AND esistente.Giorno_Settimanale =
               NEW.Giorno_Settimanale
           AND esistente.Periodo_Inizio <= NEW.Periodo_Fine
           AND NEW.Periodo_Inizio <= esistente.Periodo_Fine
           AND esistente.Ora_Inizio < NEW.Ora_Fine
           AND NEW.Ora_Inizio < esistente.Ora_Fine
    ) THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT =
                'Il nuovo orario sovrappone un incarico dell istruttore';
    END IF;

    IF NEW.Stato = 'ATTIVA' THEN
        IF NOT EXISTS (
            SELECT 1 FROM UTILIZZA u
             WHERE u.ID_Attivita_Programmata =
                   OLD.ID_Attivita_Programmata
        ) THEN
            SIGNAL SQLSTATE '45000'
                SET MESSAGE_TEXT =
                    'Una attivita attiva deve usare almeno una corsia';
        END IF;
        IF v_richiede_istruttore = TRUE AND NOT EXISTS (
            SELECT 1 FROM ASSEGNATO_A a
             WHERE a.ID_Attivita_Programmata =
                   OLD.ID_Attivita_Programmata
        ) THEN
            SIGNAL SQLSTATE '45000'
                SET MESSAGE_TEXT =
                    'Questa attivita richiede almeno un istruttore';
        END IF;
        IF v_modalita = 'SQUADRA' AND NOT EXISTS (
            SELECT 1 FROM SVOLGE s
             WHERE s.ID_Attivita_Programmata =
                   OLD.ID_Attivita_Programmata
        ) THEN
            SIGNAL SQLSTATE '45000'
                SET MESSAGE_TEXT =
                    'Una attivita di squadra richiede almeno una squadra';
        END IF;
    END IF;
END$$

DROP TRIGGER IF EXISTS TR_ISCRIZIONE_BI$$
CREATE TRIGGER TR_ISCRIZIONE_BI
BEFORE INSERT ON ISCRIZIONE_ATTIVITA
FOR EACH ROW
BEGIN
    DECLARE v_utente BIGINT UNSIGNED;
    DECLARE v_tipo_abbonamento BIGINT UNSIGNED;
    DECLARE v_tipo_attivita BIGINT UNSIGNED;
    DECLARE v_stato_abbonamento VARCHAR(20);
    DECLARE v_stato_attivita VARCHAR(20);
    DECLARE v_modalita VARCHAR(30);
    DECLARE v_data_inizio DATE;
    DECLARE v_data_fine DATE;
    DECLARE v_ingressi INT;
    DECLARE v_capienza INT;
    DECLARE v_iscritti INT;

    SELECT ID_Utente, ID_Tipo_Abbonamento, Stato,
           Data_Inizio, Data_Fine, Ingressi_Rimanenti
      INTO v_utente, v_tipo_abbonamento, v_stato_abbonamento,
           v_data_inizio, v_data_fine, v_ingressi
      FROM ABBONAMENTO
     WHERE ID_Abbonamento = NEW.ID_Abbonamento;

    SELECT ap.ID_Tipo_Attivita, ap.Stato, ta.Modalita_Partecipazione,
           ap.Capienza_Massima
      INTO v_tipo_attivita, v_stato_attivita, v_modalita, v_capienza
      FROM ATTIVITA_PROGRAMMATA ap
      JOIN TIPO_ATTIVITA ta
        ON ta.ID_Tipo_Attivita = ap.ID_Tipo_Attivita
     WHERE ap.ID_Attivita_Programmata =
           NEW.ID_Attivita_Programmata;

    IF v_modalita <> 'ISCRIZIONE'
       OR v_stato_attivita IN ('CONCLUSA', 'ANNULLATA') THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Attivita non disponibile per iscrizione';
    END IF;
    IF v_stato_abbonamento <> 'ATTIVO'
       OR NEW.Data_Iscrizione < v_data_inizio
       OR (v_data_fine IS NOT NULL
           AND NEW.Data_Iscrizione > v_data_fine)
       OR (v_data_fine IS NULL
           AND COALESCE(v_ingressi, 0) <= 0) THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Abbonamento non utilizzabile';
    END IF;
    IF NOT EXISTS (
        SELECT 1 FROM COMPATIBILITA c
         WHERE c.ID_Tipo_Abbonamento = v_tipo_abbonamento
           AND c.ID_Tipo_Attivita = v_tipo_attivita
    ) THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Abbonamento non compatibile';
    END IF;
    IF EXISTS (
        SELECT 1
          FROM ISCRIZIONE_ATTIVITA i
          JOIN ABBONAMENTO a
            ON a.ID_Abbonamento = i.ID_Abbonamento
         WHERE i.ID_Attivita_Programmata =
               NEW.ID_Attivita_Programmata
           AND a.ID_Utente = v_utente
    ) THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Utente gia iscritto a questa attivita';
    END IF;
    SELECT COUNT(*) INTO v_iscritti
      FROM ISCRIZIONE_ATTIVITA
     WHERE ID_Attivita_Programmata =
           NEW.ID_Attivita_Programmata;
    IF v_iscritti >= v_capienza THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Capienza massima raggiunta';
    END IF;
END$$

DROP TRIGGER IF EXISTS TR_ACCESSO_BI$$
CREATE TRIGGER TR_ACCESSO_BI
BEFORE INSERT ON ACCESSO_NUOTO_LIBERO
FOR EACH ROW
BEGIN
    DECLARE v_tipo_abbonamento BIGINT UNSIGNED;
    DECLARE v_tipo_attivita BIGINT UNSIGNED;
    DECLARE v_modalita_validita VARCHAR(20);
    DECLARE v_modalita_partecipazione VARCHAR(30);
    DECLARE v_stato_abbonamento VARCHAR(20);
    DECLARE v_stato_attivita VARCHAR(20);
    DECLARE v_data_inizio DATE;
    DECLARE v_data_fine DATE;
    DECLARE v_ingressi INT;
    DECLARE v_periodo_inizio DATE;
    DECLARE v_periodo_fine DATE;
    DECLARE v_ora_inizio TIME;
    DECLARE v_ora_fine TIME;
    DECLARE v_giorno VARCHAR(20);
    DECLARE v_giorno_accesso VARCHAR(20);

    SELECT a.ID_Tipo_Abbonamento, a.Stato, a.Data_Inizio,
           a.Data_Fine, a.Ingressi_Rimanenti, t.Modalita_Validita
      INTO v_tipo_abbonamento, v_stato_abbonamento, v_data_inizio,
           v_data_fine, v_ingressi, v_modalita_validita
      FROM ABBONAMENTO a
      JOIN TIPO_ABBONAMENTO t
        ON t.ID_Tipo_Abbonamento = a.ID_Tipo_Abbonamento
     WHERE a.ID_Abbonamento = NEW.ID_Abbonamento
     FOR UPDATE;

    SELECT ap.ID_Tipo_Attivita, ap.Stato,
           ap.Periodo_Inizio, ap.Periodo_Fine,
           ap.Ora_Inizio, ap.Ora_Fine, ap.Giorno_Settimanale,
           ta.Modalita_Partecipazione
      INTO v_tipo_attivita, v_stato_attivita,
           v_periodo_inizio, v_periodo_fine,
           v_ora_inizio, v_ora_fine, v_giorno,
           v_modalita_partecipazione
      FROM ATTIVITA_PROGRAMMATA ap
      JOIN TIPO_ATTIVITA ta
        ON ta.ID_Tipo_Attivita = ap.ID_Tipo_Attivita
     WHERE ap.ID_Attivita_Programmata =
           NEW.ID_Attivita_Programmata;

    SET v_giorno_accesso = CASE DAYOFWEEK(NEW.Data_Ora_Accesso)
        WHEN 1 THEN 'DOMENICA'
        WHEN 2 THEN 'LUNEDI'
        WHEN 3 THEN 'MARTEDI'
        WHEN 4 THEN 'MERCOLEDI'
        WHEN 5 THEN 'GIOVEDI'
        WHEN 6 THEN 'VENERDI'
        WHEN 7 THEN 'SABATO'
    END;

    IF v_modalita_partecipazione <> 'ACCESSO_LIBERO'
       OR v_stato_attivita <> 'ATTIVA' THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Fascia di accesso libero non attiva';
    END IF;
    IF DATE(NEW.Data_Ora_Accesso) NOT BETWEEN
       v_periodo_inizio AND v_periodo_fine
       OR TIME(NEW.Data_Ora_Accesso) NOT BETWEEN
          v_ora_inizio AND v_ora_fine
       OR v_giorno_accesso <> v_giorno THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT =
                'Accesso fuori dal periodo o dalla fascia prevista';
    END IF;
    IF v_stato_abbonamento <> 'ATTIVO'
       OR DATE(NEW.Data_Ora_Accesso) < v_data_inizio
       OR (v_data_fine IS NOT NULL
           AND DATE(NEW.Data_Ora_Accesso) > v_data_fine)
       OR (v_modalita_validita = 'INGRESSI' AND v_ingressi <= 0) THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Abbonamento non utilizzabile';
    END IF;
    IF NOT EXISTS (
        SELECT 1 FROM COMPATIBILITA c
         WHERE c.ID_Tipo_Abbonamento = v_tipo_abbonamento
           AND c.ID_Tipo_Attivita = v_tipo_attivita
    ) THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Abbonamento non compatibile';
    END IF;

    IF v_modalita_validita = 'INGRESSI' THEN
        UPDATE ABBONAMENTO
           SET Ingressi_Rimanenti = v_ingressi - 1,
               Stato = IF(v_ingressi - 1 = 0,
                          'ESAURITO', 'ATTIVO')
         WHERE ID_Abbonamento = NEW.ID_Abbonamento;
    END IF;
END$$

DROP TRIGGER IF EXISTS TR_UTILIZZA_BI$$
CREATE TRIGGER TR_UTILIZZA_BI
BEFORE INSERT ON UTILIZZA
FOR EACH ROW
BEGIN
    IF EXISTS (
        SELECT 1
          FROM UTILIZZA u
          JOIN ATTIVITA_PROGRAMMATA esistente
            ON esistente.ID_Attivita_Programmata =
               u.ID_Attivita_Programmata
          JOIN ATTIVITA_PROGRAMMATA nuova
            ON nuova.ID_Attivita_Programmata =
               NEW.ID_Attivita_Programmata
         WHERE u.ID_Vasca = NEW.ID_Vasca
           AND u.Numero_Corsia = NEW.Numero_Corsia
           AND esistente.Stato <> 'ANNULLATA'
           AND nuova.Stato <> 'ANNULLATA'
           AND esistente.Giorno_Settimanale =
               nuova.Giorno_Settimanale
           AND esistente.Periodo_Inizio <= nuova.Periodo_Fine
           AND nuova.Periodo_Inizio <= esistente.Periodo_Fine
           AND esistente.Ora_Inizio < nuova.Ora_Fine
           AND nuova.Ora_Inizio < esistente.Ora_Fine
    ) THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Corsia gia occupata nella stessa fascia';
    END IF;
END$$

DROP TRIGGER IF EXISTS TR_UTILIZZA_BD$$
CREATE TRIGGER TR_UTILIZZA_BD
BEFORE DELETE ON UTILIZZA
FOR EACH ROW
BEGIN
    IF (
        SELECT Stato FROM ATTIVITA_PROGRAMMATA
         WHERE ID_Attivita_Programmata =
               OLD.ID_Attivita_Programmata
    ) = 'ATTIVA' AND (
        SELECT COUNT(*) FROM UTILIZZA
         WHERE ID_Attivita_Programmata =
               OLD.ID_Attivita_Programmata
    ) = 1 THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT =
                'Una attivita attiva deve mantenere almeno una corsia';
    END IF;
END$$

DROP TRIGGER IF EXISTS TR_ASSEGNATO_A_BI$$
CREATE TRIGGER TR_ASSEGNATO_A_BI
BEFORE INSERT ON ASSEGNATO_A
FOR EACH ROW
BEGIN
    IF EXISTS (
        SELECT 1
          FROM ASSEGNATO_A aa
          JOIN ATTIVITA_PROGRAMMATA esistente
            ON esistente.ID_Attivita_Programmata =
               aa.ID_Attivita_Programmata
          JOIN ATTIVITA_PROGRAMMATA nuova
            ON nuova.ID_Attivita_Programmata =
               NEW.ID_Attivita_Programmata
         WHERE aa.ID_Utente_Istruttore =
               NEW.ID_Utente_Istruttore
           AND esistente.Stato <> 'ANNULLATA'
           AND nuova.Stato <> 'ANNULLATA'
           AND esistente.Giorno_Settimanale =
               nuova.Giorno_Settimanale
           AND esistente.Periodo_Inizio <= nuova.Periodo_Fine
           AND nuova.Periodo_Inizio <= esistente.Periodo_Fine
           AND esistente.Ora_Inizio < nuova.Ora_Fine
           AND nuova.Ora_Inizio < esistente.Ora_Fine
    ) THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT =
                'Istruttore gia assegnato nella stessa fascia';
    END IF;
END$$

DROP TRIGGER IF EXISTS TR_ASSEGNATO_A_BD$$
CREATE TRIGGER TR_ASSEGNATO_A_BD
BEFORE DELETE ON ASSEGNATO_A
FOR EACH ROW
BEGIN
    IF EXISTS (
        SELECT 1
          FROM ATTIVITA_PROGRAMMATA ap
          JOIN TIPO_ATTIVITA ta
            ON ta.ID_Tipo_Attivita = ap.ID_Tipo_Attivita
         WHERE ap.ID_Attivita_Programmata =
               OLD.ID_Attivita_Programmata
           AND ap.Stato = 'ATTIVA'
           AND ta.Richiede_Istruttore = TRUE
    ) AND (
        SELECT COUNT(*) FROM ASSEGNATO_A
         WHERE ID_Attivita_Programmata =
               OLD.ID_Attivita_Programmata
    ) = 1 THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT =
                'L attivita attiva richiede almeno un istruttore';
    END IF;
END$$

DROP TRIGGER IF EXISTS TR_APPARTENENZA_BI$$
CREATE TRIGGER TR_APPARTENENZA_BI
BEFORE INSERT ON APPARTENENZA_SQUADRA
FOR EACH ROW
BEGIN
    IF NEW.Data_Fine IS NOT NULL
       AND NEW.Data_Inizio > NEW.Data_Fine THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Intervallo di appartenenza non valido';
    END IF;
    IF EXISTS (
        SELECT 1 FROM APPARTENENZA_SQUADRA a
         WHERE a.ID_Utente_Atleta = NEW.ID_Utente_Atleta
           AND a.Data_Inizio <= COALESCE(NEW.Data_Fine, '9999-12-31')
           AND NEW.Data_Inizio <= COALESCE(a.Data_Fine, '9999-12-31')
    ) THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT =
                'L atleta appartiene gia a una squadra nel periodo';
    END IF;
END$$

DROP TRIGGER IF EXISTS TR_APPARTENENZA_BU$$
CREATE TRIGGER TR_APPARTENENZA_BU
BEFORE UPDATE ON APPARTENENZA_SQUADRA
FOR EACH ROW
BEGIN
    IF NEW.Data_Fine IS NOT NULL
       AND NEW.Data_Inizio > NEW.Data_Fine THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Intervallo di appartenenza non valido';
    END IF;
    IF EXISTS (
        SELECT 1 FROM APPARTENENZA_SQUADRA a
         WHERE a.ID_Utente_Atleta = NEW.ID_Utente_Atleta
           AND a.ID_Appartenenza <> OLD.ID_Appartenenza
           AND a.Data_Inizio <= COALESCE(NEW.Data_Fine, '9999-12-31')
           AND NEW.Data_Inizio <= COALESCE(a.Data_Fine, '9999-12-31')
    ) THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT =
                'L atleta appartiene gia a una squadra nel periodo';
    END IF;
END$$

DROP TRIGGER IF EXISTS TR_INCARICO_BI$$
CREATE TRIGGER TR_INCARICO_BI
BEFORE INSERT ON INCARICO_SQUADRA
FOR EACH ROW
BEGIN
    IF NEW.Data_Fine IS NOT NULL
       AND NEW.Data_Inizio > NEW.Data_Fine THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Intervallo dell incarico non valido';
    END IF;
    IF EXISTS (
        SELECT 1 FROM INCARICO_SQUADRA i
         WHERE i.ID_Utente_Istruttore =
               NEW.ID_Utente_Istruttore
           AND i.ID_Squadra = NEW.ID_Squadra
           AND i.Data_Inizio <= COALESCE(NEW.Data_Fine, '9999-12-31')
           AND NEW.Data_Inizio <= COALESCE(i.Data_Fine, '9999-12-31')
    ) THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT =
                'Esiste gia un incarico sovrapposto per la squadra';
    END IF;
END$$

DROP TRIGGER IF EXISTS TR_INCARICO_BU$$
CREATE TRIGGER TR_INCARICO_BU
BEFORE UPDATE ON INCARICO_SQUADRA
FOR EACH ROW
BEGIN
    IF NEW.Data_Fine IS NOT NULL
       AND NEW.Data_Inizio > NEW.Data_Fine THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Intervallo dell incarico non valido';
    END IF;
    IF EXISTS (
        SELECT 1 FROM INCARICO_SQUADRA i
         WHERE i.ID_Utente_Istruttore =
               NEW.ID_Utente_Istruttore
           AND i.ID_Squadra = NEW.ID_Squadra
           AND i.ID_Incarico_Squadra <> OLD.ID_Incarico_Squadra
           AND i.Data_Inizio <= COALESCE(NEW.Data_Fine, '9999-12-31')
           AND NEW.Data_Inizio <= COALESCE(i.Data_Fine, '9999-12-31')
    ) THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT =
                'Esiste gia un incarico sovrapposto per la squadra';
    END IF;
END$$

DROP TRIGGER IF EXISTS TR_SVOLGE_BI$$
CREATE TRIGGER TR_SVOLGE_BI
BEFORE INSERT ON SVOLGE
FOR EACH ROW
BEGIN
    DECLARE v_stato VARCHAR(20);

    IF NOT EXISTS (
        SELECT 1
          FROM ATTIVITA_PROGRAMMATA ap
          JOIN TIPO_ATTIVITA ta
            ON ta.ID_Tipo_Attivita = ap.ID_Tipo_Attivita
         WHERE ap.ID_Attivita_Programmata =
               NEW.ID_Attivita_Programmata
           AND ta.Modalita_Partecipazione = 'SQUADRA'
    ) THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT =
                'La squadra puo svolgere solo attivita di squadra';
    END IF;

    -- Nuovo controllo di integrità temporale
    SELECT Stato INTO v_stato
      FROM ATTIVITA_PROGRAMMATA
     WHERE ID_Attivita_Programmata = NEW.ID_Attivita_Programmata;

    IF v_stato IN ('CONCLUSA', 'ANNULLATA') THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT =
                'Impossibile associare la squadra: attivita conclusa o annullata';
    END IF;
END$$

DROP TRIGGER IF EXISTS TR_SVOLGE_BD$$
CREATE TRIGGER TR_SVOLGE_BD
BEFORE DELETE ON SVOLGE
FOR EACH ROW
BEGIN
    IF (
        SELECT Stato FROM ATTIVITA_PROGRAMMATA
         WHERE ID_Attivita_Programmata =
               OLD.ID_Attivita_Programmata
    ) = 'ATTIVA' AND (
        SELECT COUNT(*) FROM SVOLGE
         WHERE ID_Attivita_Programmata =
               OLD.ID_Attivita_Programmata
    ) = 1 THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT =
                'Una attivita di squadra attiva richiede una squadra';
    END IF;
END$$

DELIMITER ;
