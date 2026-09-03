-- Gestione Piscina - popolamento dimostrativo consolidato.
-- Eseguire dopo 01_schema_completo.sql.
-- Inserisce utenti, qualifiche, configurazioni, attività, rapporti sportivi
-- e account tecnici per provare tutte le viste applicative.
USE piscina_progetto;

SET SQL_SAFE_UPDATES = 0;

-- Rimuove esclusivamente il receptionist demo delle versioni precedenti,
-- non previsto tra le viste Utente, Club e Amministratore della relazione.
DELETE FROM ACCOUNT
WHERE Email = 'reception@piscina.local'
  AND Password_Hash =
      'HT+VnzvB4Sh5Tq0YM8Ai80maVKR3V9kMsfhfFTGGReA='
  AND Password_Salt = 'apOYKLo+ZMMaLSae1vl6dw==';

-- ===========================================================================
-- UTENTI E QUALIFICHE DI DOMINIO
-- ===========================================================================

INSERT IGNORE INTO UTENTE
    (Codice_Fiscale, Nome, Cognome, Data_Nascita,
     Email, Telefono, Scadenza_Certificato_Medico)
VALUES
    ('VSTRVI80A01H501X', 'Ivo', 'Istruttore', '1980-01-01',
     'istruttore@piscina.local', '3331111111', '2027-03-31'),
    ('CLNCHR95B42H501Y', 'Chiara', 'Cliente', '1995-02-02',
     'cliente@piscina.local', '3332222222', NULL),
    ('TLLLCA02C43H501Z', 'Alice', 'Atleta', '2002-03-03',
     'atleta@piscina.local', '3333333333', '2027-06-30'),
    ('BNCDRA90D01H501W', 'Andrea', 'Completo', '1990-04-01',
     'atleta.istruttore@piscina.local', '3334444444', '2027-06-30');

INSERT IGNORE INTO ATLETA (ID_Utente)
SELECT ID_Utente
FROM UTENTE
WHERE Email IN (
    'atleta@piscina.local',
    'atleta.istruttore@piscina.local'
);

INSERT INTO ISTRUTTORE (ID_Utente, Qualifica)
SELECT ID_Utente, 'Istruttore di nuoto'
FROM UTENTE
WHERE Email = 'istruttore@piscina.local'
ON DUPLICATE KEY UPDATE Qualifica = VALUES(Qualifica);

INSERT INTO ISTRUTTORE (ID_Utente, Qualifica)
SELECT ID_Utente, 'Allenatore e istruttore'
FROM UTENTE
WHERE Email = 'atleta.istruttore@piscina.local'
ON DUPLICATE KEY UPDATE Qualifica = VALUES(Qualifica);

-- ===========================================================================
-- CONFIGURAZIONE COMMERCIALE E OPERATIVA
-- ===========================================================================

INSERT INTO TIPO_ATTIVITA
    (Nome, Modalita_Partecipazione, Richiede_Istruttore, Descrizione)
VALUES
    ('Corso di nuoto', 'ISCRIZIONE', TRUE,
     'Corso a iscrizione individuale'),
    ('Nuoto libero', 'ACCESSO_LIBERO', FALSE,
     'Fascia per accessi effettivi senza iscrizione'),
    ('Allenamento sportivo', 'SQUADRA', TRUE,
     'Allenamento riservato alle squadre')
ON DUPLICATE KEY UPDATE
    Modalita_Partecipazione = VALUES(Modalita_Partecipazione),
    Richiede_Istruttore = VALUES(Richiede_Istruttore),
    Descrizione = VALUES(Descrizione);

INSERT INTO TIPO_ABBONAMENTO
    (Nome, Costo, Attivo, Modalita_Validita,
     Condizioni_Utilizzo, Durata_Giorni, Numero_Ingressi)
VALUES
    ('Annuale corsi', 420.00, FALSE, 'TEMPO',
     'Valido per corsi e nuoto libero per 365 giorni', 365, NULL),
    ('Dieci ingressi', 75.00, FALSE, 'INGRESSI',
     'Dieci accessi alle fasce di nuoto libero', NULL, 10)
ON DUPLICATE KEY UPDATE
    Costo = VALUES(Costo),
    Attivo = FALSE,
    Modalita_Validita = VALUES(Modalita_Validita),
    Condizioni_Utilizzo = VALUES(Condizioni_Utilizzo),
    Durata_Giorni = VALUES(Durata_Giorni),
    Numero_Ingressi = VALUES(Numero_Ingressi);

INSERT IGNORE INTO COMPATIBILITA
    (ID_Tipo_Abbonamento, ID_Tipo_Attivita)
SELECT ta.ID_Tipo_Abbonamento, tt.ID_Tipo_Attivita
FROM TIPO_ABBONAMENTO ta
JOIN TIPO_ATTIVITA tt
WHERE
    (ta.Nome = 'Annuale corsi'
        AND tt.Nome IN ('Corso di nuoto', 'Nuoto libero'))
    OR
    (ta.Nome = 'Dieci ingressi' AND tt.Nome = 'Nuoto libero');

UPDATE TIPO_ABBONAMENTO
SET Attivo = TRUE
WHERE Nome IN ('Annuale corsi', 'Dieci ingressi');

INSERT INTO VASCA
    (Nome, Lunghezza, Larghezza, Profondita, Temperatura, Tipologia)
VALUES
    ('Vasca principale', 25.00, 12.50, 1.80, 27.0, 'Sportiva'),
    ('Vasca riabilitativa', 12.00, 6.00, 1.20, 31.5, 'Riabilitativa')
ON DUPLICATE KEY UPDATE
    Lunghezza = VALUES(Lunghezza),
    Larghezza = VALUES(Larghezza),
    Profondita = VALUES(Profondita),
    Temperatura = VALUES(Temperatura),
    Tipologia = VALUES(Tipologia);

INSERT IGNORE INTO CORSIA (ID_Vasca, Numero)
SELECT ID_Vasca, n.Numero
FROM VASCA
JOIN (
    SELECT 1 AS Numero
    UNION ALL SELECT 2
    UNION ALL SELECT 3
) n
WHERE Nome = 'Vasca principale';

-- La vasca non suddivisa usa una corsia convenzionale.
INSERT IGNORE INTO CORSIA (ID_Vasca, Numero)
SELECT ID_Vasca, 1
FROM VASCA
WHERE Nome = 'Vasca riabilitativa';

INSERT INTO CLUB_SPORTIVO (Nome, Email, Telefono, Indirizzo)
VALUES (
    'Nuoto Emilia',
    'segreteria@nuotoemilia.example',
    '0510000000',
    'Via delle Piscine 1, Bologna'
)
ON DUPLICATE KEY UPDATE
    Email = VALUES(Email),
    Telefono = VALUES(Telefono),
    Indirizzo = VALUES(Indirizzo);

INSERT INTO SQUADRA (ID_Club, Nome, Categoria)
SELECT ID_Club, 'Agonistica Senior', 'Senior'
FROM CLUB_SPORTIVO
WHERE Nome = 'Nuoto Emilia'
ON DUPLICATE KEY UPDATE Categoria = VALUES(Categoria);

INSERT INTO APPARTENENZA_SQUADRA
    (ID_Utente_Atleta, ID_Squadra, Data_Inizio, Data_Fine)
SELECT u.ID_Utente, s.ID_Squadra,
       DATE_SUB(CURRENT_DATE, INTERVAL 180 DAY), NULL
FROM UTENTE u
JOIN SQUADRA s ON s.Nome = 'Agonistica Senior'
WHERE u.Email = 'atleta@piscina.local'
  AND NOT EXISTS (
      SELECT 1
      FROM APPARTENENZA_SQUADRA a
      WHERE a.ID_Utente_Atleta = u.ID_Utente
        AND a.Data_Fine IS NULL
  );

INSERT INTO INCARICO_SQUADRA
    (ID_Utente_Istruttore, ID_Squadra, Data_Inizio, Data_Fine)
SELECT u.ID_Utente, s.ID_Squadra,
       DATE_SUB(CURRENT_DATE, INTERVAL 180 DAY), NULL
FROM UTENTE u
JOIN SQUADRA s ON s.Nome = 'Agonistica Senior'
WHERE u.Email = 'atleta.istruttore@piscina.local'
  AND NOT EXISTS (
      SELECT 1
      FROM INCARICO_SQUADRA i
      WHERE i.ID_Utente_Istruttore = u.ID_Utente
        AND i.ID_Squadra = s.ID_Squadra
        AND i.Data_Fine IS NULL
  );

SET @giorno_corrente = ELT(
    WEEKDAY(CURRENT_DATE) + 1,
    'LUNEDI', 'MARTEDI', 'MERCOLEDI', 'GIOVEDI',
    'VENERDI', 'SABATO', 'DOMENICA'
);

INSERT INTO ATTIVITA_PROGRAMMATA
    (ID_Tipo_Attivita, Titolo, Giorno_Settimanale,
     Ora_Inizio, Ora_Fine, Capienza_Massima,
     Periodo_Inizio, Periodo_Fine, Stato)
SELECT ID_Tipo_Attivita, 'Corso adulti demo', @giorno_corrente,
       '09:00:00', '10:00:00', 20,
       DATE_SUB(CURRENT_DATE, INTERVAL 30 DAY),
       DATE_ADD(CURRENT_DATE, INTERVAL 60 DAY), 'PROGRAMMATA'
FROM TIPO_ATTIVITA
WHERE Nome = 'Corso di nuoto'
  AND NOT EXISTS (
      SELECT 1 FROM ATTIVITA_PROGRAMMATA
      WHERE Titolo = 'Corso adulti demo'
  );

INSERT INTO ATTIVITA_PROGRAMMATA
    (ID_Tipo_Attivita, Titolo, Giorno_Settimanale,
     Ora_Inizio, Ora_Fine, Capienza_Massima,
     Periodo_Inizio, Periodo_Fine, Stato)
SELECT ID_Tipo_Attivita, 'Nuoto libero demo', @giorno_corrente,
       '00:00:00', '23:59:59', 80,
       DATE_SUB(CURRENT_DATE, INTERVAL 30 DAY),
       DATE_ADD(CURRENT_DATE, INTERVAL 60 DAY), 'PROGRAMMATA'
FROM TIPO_ATTIVITA
WHERE Nome = 'Nuoto libero'
  AND NOT EXISTS (
      SELECT 1 FROM ATTIVITA_PROGRAMMATA
      WHERE Titolo = 'Nuoto libero demo'
  );

INSERT INTO ATTIVITA_PROGRAMMATA
    (ID_Tipo_Attivita, Titolo, Giorno_Settimanale,
     Ora_Inizio, Ora_Fine, Capienza_Massima,
     Periodo_Inizio, Periodo_Fine, Stato)
SELECT ID_Tipo_Attivita, 'Allenamento squadra demo', @giorno_corrente,
       '10:00:00', '12:00:00', 30,
       DATE_SUB(CURRENT_DATE, INTERVAL 30 DAY),
       DATE_ADD(CURRENT_DATE, INTERVAL 60 DAY), 'PROGRAMMATA'
FROM TIPO_ATTIVITA
WHERE Nome = 'Allenamento sportivo'
  AND NOT EXISTS (
      SELECT 1 FROM ATTIVITA_PROGRAMMATA
      WHERE Titolo = 'Allenamento squadra demo'
  );

INSERT IGNORE INTO UTILIZZA
    (ID_Attivita_Programmata, ID_Vasca, Numero_Corsia)
SELECT ap.ID_Attivita_Programmata, v.ID_Vasca,
       CASE ap.Titolo
           WHEN 'Corso adulti demo' THEN 1
           WHEN 'Allenamento squadra demo' THEN 2
           ELSE 3
       END
FROM ATTIVITA_PROGRAMMATA ap
JOIN VASCA v ON v.Nome = 'Vasca principale'
WHERE ap.Titolo IN (
    'Corso adulti demo',
    'Nuoto libero demo',
    'Allenamento squadra demo'
);

INSERT IGNORE INTO ASSEGNATO_A
    (ID_Utente_Istruttore, ID_Attivita_Programmata)
SELECT u.ID_Utente, ap.ID_Attivita_Programmata
FROM UTENTE u
JOIN ATTIVITA_PROGRAMMATA ap
  ON ap.Titolo IN (
      'Corso adulti demo',
      'Allenamento squadra demo'
  )
WHERE u.Email = 'atleta.istruttore@piscina.local';

INSERT IGNORE INTO SVOLGE
    (ID_Squadra, ID_Attivita_Programmata)
SELECT s.ID_Squadra, ap.ID_Attivita_Programmata
FROM SQUADRA s
JOIN ATTIVITA_PROGRAMMATA ap
  ON ap.Titolo = 'Allenamento squadra demo'
WHERE s.Nome = 'Agonistica Senior';

UPDATE ATTIVITA_PROGRAMMATA
SET Stato = 'ATTIVA'
WHERE Titolo IN (
    'Corso adulti demo',
    'Nuoto libero demo',
    'Allenamento squadra demo'
);

INSERT INTO ABBONAMENTO
    (ID_Utente, ID_Tipo_Abbonamento,
     Data_Acquisto, Data_Inizio, Data_Fine,
     Stato, Ingressi_Rimanenti)
SELECT u.ID_Utente, t.ID_Tipo_Abbonamento,
       DATE_SUB(CURRENT_DATE, INTERVAL 30 DAY),
       DATE_SUB(CURRENT_DATE, INTERVAL 30 DAY),
       NULL, 'ATTIVO', NULL
FROM UTENTE u
JOIN TIPO_ABBONAMENTO t ON t.Nome = 'Annuale corsi'
WHERE u.Email = 'cliente@piscina.local'
  AND NOT EXISTS (
      SELECT 1 FROM ABBONAMENTO a
      WHERE a.ID_Utente = u.ID_Utente
        AND a.ID_Tipo_Abbonamento = t.ID_Tipo_Abbonamento
  );

INSERT INTO ABBONAMENTO
    (ID_Utente, ID_Tipo_Abbonamento,
     Data_Acquisto, Data_Inizio, Data_Fine,
     Stato, Ingressi_Rimanenti)
SELECT u.ID_Utente, t.ID_Tipo_Abbonamento,
       DATE_SUB(CURRENT_DATE, INTERVAL 7 DAY),
       DATE_SUB(CURRENT_DATE, INTERVAL 7 DAY),
       NULL, 'ATTIVO', NULL
FROM UTENTE u
JOIN TIPO_ABBONAMENTO t ON t.Nome = 'Dieci ingressi'
WHERE u.Email = 'cliente@piscina.local'
  AND NOT EXISTS (
      SELECT 1 FROM ABBONAMENTO a
      WHERE a.ID_Utente = u.ID_Utente
        AND a.ID_Tipo_Abbonamento = t.ID_Tipo_Abbonamento
  );

INSERT INTO ISCRIZIONE_ATTIVITA
    (ID_Abbonamento, ID_Attivita_Programmata, Data_Iscrizione)
SELECT a.ID_Abbonamento, ap.ID_Attivita_Programmata, CURRENT_DATE
FROM ABBONAMENTO a
JOIN TIPO_ABBONAMENTO t
  ON t.ID_Tipo_Abbonamento = a.ID_Tipo_Abbonamento
JOIN UTENTE u ON u.ID_Utente = a.ID_Utente
JOIN ATTIVITA_PROGRAMMATA ap
  ON ap.Titolo = 'Corso adulti demo'
WHERE u.Email = 'cliente@piscina.local'
  AND t.Nome = 'Annuale corsi'
  AND NOT EXISTS (
      SELECT 1 FROM ISCRIZIONE_ATTIVITA i
      WHERE i.ID_Abbonamento = a.ID_Abbonamento
        AND i.ID_Attivita_Programmata =
            ap.ID_Attivita_Programmata
  );

/*
INSERT INTO ACCESSO_NUOTO_LIBERO
    (ID_Abbonamento, ID_Attivita_Programmata, Data_Ora_Accesso)
SELECT a.ID_Abbonamento, ap.ID_Attivita_Programmata, CURRENT_TIMESTAMP
FROM ABBONAMENTO a
JOIN TIPO_ABBONAMENTO t
  ON t.ID_Tipo_Abbonamento = a.ID_Tipo_Abbonamento
JOIN UTENTE u ON u.ID_Utente = a.ID_Utente
JOIN ATTIVITA_PROGRAMMATA ap
  ON ap.Titolo = 'Nuoto libero demo'
WHERE u.Email = 'cliente@piscina.local'
  AND t.Nome = 'Dieci ingressi'
  AND NOT EXISTS (
      SELECT 1 FROM ACCESSO_NUOTO_LIBERO x
      WHERE x.ID_Abbonamento = a.ID_Abbonamento
        AND x.ID_Attivita_Programmata =
            ap.ID_Attivita_Programmata
        AND DATE(x.Data_Ora_Accesso) = CURRENT_DATE
  );
*/

-- 1. Resettiamo le variabili per sicurezza
SET @id_abb = NULL;
SET @id_att = NULL;

-- 2. Leggiamo i dati dalla tabella ABBONAMENTO e li salviamo in memoria
SELECT a.ID_Abbonamento, ap.ID_Attivita_Programmata 
INTO @id_abb, @id_att
FROM ABBONAMENTO a
JOIN TIPO_ABBONAMENTO t
  ON t.ID_Tipo_Abbonamento = a.ID_Tipo_Abbonamento
JOIN UTENTE u ON u.ID_Utente = a.ID_Utente
JOIN ATTIVITA_PROGRAMMATA ap
  ON ap.Titolo = 'Nuoto libero demo'
WHERE u.Email = 'cliente@piscina.local'
  AND t.Nome = 'Dieci ingressi'
  AND NOT EXISTS (
      SELECT 1 FROM ACCESSO_NUOTO_LIBERO x
      WHERE x.ID_Abbonamento = a.ID_Abbonamento
        AND x.ID_Attivita_Programmata = ap.ID_Attivita_Programmata
        AND DATE(x.Data_Ora_Accesso) = CURRENT_DATE
  )
LIMIT 1;

-- 3. Eseguiamo l'inserimento usando solo le variabili (nessun conflitto)
INSERT INTO ACCESSO_NUOTO_LIBERO
    (ID_Abbonamento, ID_Attivita_Programmata, Data_Ora_Accesso)
SELECT @id_abb, @id_att, CURRENT_TIMESTAMP
WHERE @id_abb IS NOT NULL;


-- 1. Creazione di un'attività completa (con 1 solo posto disponibile)
INSERT INTO ATTIVITA_PROGRAMMATA
    (ID_Tipo_Attivita, Titolo, Giorno_Settimanale,
     Ora_Inizio, Ora_Fine, Capienza_Massima,
     Periodo_Inizio, Periodo_Fine, Stato)
SELECT ID_Tipo_Attivita, 'Corso esclusivo al completo', @giorno_corrente,
       '14:00:00', '15:00:00', 1,
       DATE_SUB(CURRENT_DATE, INTERVAL 30 DAY),
       DATE_ADD(CURRENT_DATE, INTERVAL 60 DAY), 'PROGRAMMATA'
FROM TIPO_ATTIVITA
WHERE Nome = 'Corso di nuoto'
  AND NOT EXISTS (
      SELECT 1 FROM ATTIVITA_PROGRAMMATA
      WHERE Titolo = 'Corso esclusivo al completo'
  );

-- 2. Creazione della Corsia 4 nella Vasca principale
INSERT IGNORE INTO CORSIA (ID_Vasca, Numero)
SELECT ID_Vasca, 4
FROM VASCA
WHERE Nome = 'Vasca principale';

-- 3. Assegnazione della nuova corsia all'attività
INSERT IGNORE INTO UTILIZZA
    (ID_Attivita_Programmata, ID_Vasca, Numero_Corsia)
SELECT ap.ID_Attivita_Programmata, v.ID_Vasca, 4
FROM ATTIVITA_PROGRAMMATA ap
JOIN VASCA v ON v.Nome = 'Vasca principale'
WHERE ap.Titolo = 'Corso esclusivo al completo';

-- 4. Assegnazione istruttore
INSERT IGNORE INTO ASSEGNATO_A
    (ID_Utente_Istruttore, ID_Attivita_Programmata)
SELECT u.ID_Utente, ap.ID_Attivita_Programmata
FROM UTENTE u
JOIN ATTIVITA_PROGRAMMATA ap
  ON ap.Titolo = 'Corso esclusivo al completo'
WHERE u.Email = 'istruttore@piscina.local';

-- 5. Attivazione dell'attività
UPDATE ATTIVITA_PROGRAMMATA
SET Stato = 'ATTIVA'
WHERE Titolo = 'Corso esclusivo al completo';

-- 6. Iscrizione dell'utente cliente
INSERT INTO ISCRIZIONE_ATTIVITA
    (ID_Abbonamento, ID_Attivita_Programmata, Data_Iscrizione)
SELECT a.ID_Abbonamento, ap.ID_Attivita_Programmata, CURRENT_DATE
FROM ABBONAMENTO a
JOIN TIPO_ABBONAMENTO t
  ON t.ID_Tipo_Abbonamento = a.ID_Tipo_Abbonamento
JOIN UTENTE u ON u.ID_Utente = a.ID_Utente
JOIN ATTIVITA_PROGRAMMATA ap
  ON ap.Titolo = 'Corso esclusivo al completo'
WHERE u.Email = 'cliente@piscina.local'
  AND t.Nome = 'Annuale corsi'
  AND NOT EXISTS (
      SELECT 1 FROM ISCRIZIONE_ATTIVITA i
      WHERE i.ID_Abbonamento = a.ID_Abbonamento
        AND i.ID_Attivita_Programmata = ap.ID_Attivita_Programmata
  );

-- ESPANSIONE DATI DEMO: CASI LIMITE, STORICO E NUOVI CLUB

-- 1. Utente con abbonamento ad ingressi esaurito.

-- A. Creazione Utente
INSERT IGNORE INTO UTENTE
    (Codice_Fiscale, Nome, Cognome, Data_Nascita, Email, Telefono, Scadenza_Certificato_Medico)
VALUES
    ('VRDLGI70A01H501K', 'Luigi', 'Verdi', '1970-05-05',
     'scaduto@piscina.local', '3335555555', DATE_SUB(CURRENT_DATE, INTERVAL 1 YEAR));

-- B. Creazione Offerta (Inganno per il trigger: partiamo da 2 ingressi)
INSERT INTO TIPO_ABBONAMENTO
    (Nome, Costo, Attivo, Modalita_Validita, Condizioni_Utilizzo, Durata_Giorni, Numero_Ingressi)
VALUES
    ('Ingresso Singolo', 8.50, FALSE, 'INGRESSI', 'Singolo accesso al nuoto libero', NULL, 2)
ON DUPLICATE KEY UPDATE Costo = VALUES(Costo), Numero_Ingressi = VALUES(Numero_Ingressi);

INSERT IGNORE INTO COMPATIBILITA (ID_Tipo_Abbonamento, ID_Tipo_Attivita)
SELECT ta.ID_Tipo_Abbonamento, tt.ID_Tipo_Attivita
FROM TIPO_ABBONAMENTO ta JOIN TIPO_ATTIVITA tt
WHERE ta.Nome = 'Ingresso Singolo' AND tt.Nome = 'Nuoto libero';

UPDATE TIPO_ABBONAMENTO SET Attivo = TRUE WHERE Nome = 'Ingresso Singolo';

-- C. Acquisto pulito
INSERT INTO ABBONAMENTO
    (ID_Utente, ID_Tipo_Abbonamento, Data_Acquisto, Data_Inizio, Data_Fine, Stato, Ingressi_Rimanenti)
SELECT u.ID_Utente, t.ID_Tipo_Abbonamento, 
       DATE_SUB(CURRENT_DATE, INTERVAL 10 DAY), 
       DATE_SUB(CURRENT_DATE, INTERVAL 10 DAY), 
       NULL, 'ATTIVO', 2
FROM UTENTE u JOIN TIPO_ABBONAMENTO t ON t.Nome = 'Ingresso Singolo'
WHERE u.Email = 'scaduto@piscina.local';

-- D. Consumo effettivo (Il decremento lo porterà matematicamente a 0 senza errori)
SET @storico_abb = NULL;
SET @storico_att = NULL;

SELECT a.ID_Abbonamento, ap.ID_Attivita_Programmata 
INTO @storico_abb, @storico_att
FROM ABBONAMENTO a
JOIN TIPO_ABBONAMENTO t ON t.ID_Tipo_Abbonamento = a.ID_Tipo_Abbonamento
JOIN UTENTE u ON u.ID_Utente = a.ID_Utente
JOIN ATTIVITA_PROGRAMMATA ap ON ap.Titolo = 'Nuoto libero demo'
WHERE u.Email = 'scaduto@piscina.local' AND t.Nome = 'Ingresso Singolo'
LIMIT 1;

INSERT INTO ACCESSO_NUOTO_LIBERO
    (ID_Abbonamento, ID_Attivita_Programmata, Data_Ora_Accesso)
SELECT @storico_abb, @storico_att, DATE_SUB(CURRENT_TIMESTAMP, INTERVAL 7 DAY)
WHERE @storico_abb IS NOT NULL;

-- 2. Nuovo Club e Nuova Squadra
INSERT INTO CLUB_SPORTIVO (Nome, Email, Telefono, Indirizzo)
VALUES ('Delfini Blu', 'info@delfiniblu.example', '0519999999', 'Via del Mare 2, Bologna')
ON DUPLICATE KEY UPDATE Email = VALUES(Email);

INSERT INTO SQUADRA (ID_Club, Nome, Categoria)
SELECT ID_Club, 'Esordienti A', 'Junior'
FROM CLUB_SPORTIVO WHERE Nome = 'Delfini Blu'
ON DUPLICATE KEY UPDATE Categoria = VALUES(Categoria);

-- 3. Attività Storica (Conclusa)
INSERT INTO ATTIVITA_PROGRAMMATA
    (ID_Tipo_Attivita, Titolo, Giorno_Settimanale, Ora_Inizio, Ora_Fine, Capienza_Massima, Periodo_Inizio, Periodo_Fine, Stato)
SELECT ID_Tipo_Attivita, 'Corso estivo passato', 'LUNEDI', '16:00:00', '17:00:00', 15,
       DATE_SUB(CURRENT_DATE, INTERVAL 1 YEAR), DATE_SUB(CURRENT_DATE, INTERVAL 11 MONTH), 'CONCLUSA'
FROM TIPO_ATTIVITA WHERE Nome = 'Corso di nuoto'
  AND NOT EXISTS (SELECT 1 FROM ATTIVITA_PROGRAMMATA WHERE Titolo = 'Corso estivo passato');


  -- ===========================================================================
-- POPOLAMENTO AGGIUNTIVO: ATLETI E ISTRUTTORI PER LE SQUADRE
-- ===========================================================================

-- 1. Creazione delle anagrafiche per i nuovi sportivi
INSERT IGNORE INTO UTENTE
    (Codice_Fiscale, Nome, Cognome, Data_Nascita, Email, Telefono, Scadenza_Certificato_Medico)
VALUES
    ('MRCJNR10A01H501A', 'Marco', 'Junior', '2010-01-01', 'junior1@piscina.local', '3336666661', '2027-12-31'),
    ('SFAGVN11B02H501B', 'Sofia', 'Giovane', '2011-02-02', 'junior2@piscina.local', '3336666662', '2027-12-31'),
    ('LCAMST95C03H501C', 'Luca', 'Master', '1995-03-03', 'senior2@piscina.local', '3336666663', '2027-12-31'),
    ('GLACCH85D04H501D', 'Giulia', 'Coach', '1985-04-04', 'coach@piscina.local', '3336666664', '2027-12-31');

-- 2. Assegnazione delle qualifiche di dominio
INSERT IGNORE INTO ATLETA (ID_Utente)
SELECT ID_Utente FROM UTENTE 
WHERE Email IN ('junior1@piscina.local', 'junior2@piscina.local', 'senior2@piscina.local');

INSERT INTO ISTRUTTORE (ID_Utente, Qualifica)
SELECT ID_Utente, 'Allenatore giovanile' FROM UTENTE WHERE Email = 'coach@piscina.local'
ON DUPLICATE KEY UPDATE Qualifica = VALUES(Qualifica);

-- 3. Appartenenza alle Squadre (Atleti)
-- Inseriamo Marco e Sofia nella nuova squadra "Esordienti A" dei Delfini Blu
INSERT INTO APPARTENENZA_SQUADRA (ID_Utente_Atleta, ID_Squadra, Data_Inizio, Data_Fine)
SELECT u.ID_Utente, s.ID_Squadra, DATE_SUB(CURRENT_DATE, INTERVAL 60 DAY), NULL
FROM UTENTE u CROSS JOIN SQUADRA s
WHERE u.Email IN ('junior1@piscina.local', 'junior2@piscina.local') AND s.Nome = 'Esordienti A'
  AND NOT EXISTS (
      SELECT 1 FROM APPARTENENZA_SQUADRA a 
      WHERE a.ID_Utente_Atleta = u.ID_Utente AND a.Data_Fine IS NULL
  );

-- Inseriamo Luca nella squadra "Agonistica Senior" per fare compagnia ad Alice e Andrea
INSERT INTO APPARTENENZA_SQUADRA (ID_Utente_Atleta, ID_Squadra, Data_Inizio, Data_Fine)
SELECT u.ID_Utente, s.ID_Squadra, DATE_SUB(CURRENT_DATE, INTERVAL 90 DAY), NULL
FROM UTENTE u CROSS JOIN SQUADRA s
WHERE u.Email = 'senior2@piscina.local' AND s.Nome = 'Agonistica Senior'
  AND NOT EXISTS (
      SELECT 1 FROM APPARTENENZA_SQUADRA a 
      WHERE a.ID_Utente_Atleta = u.ID_Utente AND a.Data_Fine IS NULL
  );

-- 4. Incarichi per le Squadre (Istruttori)
-- Assegniamo la nuova istruttrice Giulia alla guida degli Esordienti A
INSERT INTO INCARICO_SQUADRA (ID_Utente_Istruttore, ID_Squadra, Data_Inizio, Data_Fine)
SELECT u.ID_Utente, s.ID_Squadra, DATE_SUB(CURRENT_DATE, INTERVAL 60 DAY), NULL
FROM UTENTE u CROSS JOIN SQUADRA s
WHERE u.Email = 'coach@piscina.local' AND s.Nome = 'Esordienti A'
  AND NOT EXISTS (
      SELECT 1 FROM INCARICO_SQUADRA i 
      WHERE i.ID_Utente_Istruttore = u.ID_Utente AND i.ID_Squadra = s.ID_Squadra AND i.Data_Fine IS NULL
  );

-- Assegniamo l'istruttore esistente (Ivo) come supporto alla Agonistica Senior
INSERT INTO INCARICO_SQUADRA (ID_Utente_Istruttore, ID_Squadra, Data_Inizio, Data_Fine)
SELECT u.ID_Utente, s.ID_Squadra, DATE_SUB(CURRENT_DATE, INTERVAL 120 DAY), NULL
FROM UTENTE u CROSS JOIN SQUADRA s
WHERE u.Email = 'istruttore@piscina.local' AND s.Nome = 'Agonistica Senior'
  AND NOT EXISTS (
      SELECT 1 FROM INCARICO_SQUADRA i 
      WHERE i.ID_Utente_Istruttore = u.ID_Utente AND i.ID_Squadra = s.ID_Squadra AND i.Data_Fine IS NULL
  );

-- 5. Creazione degli account per permetterti di accedere con i nuovi profili
-- Usa "Atleta123!" per gli atleti e "Istruttore123!" per il coach
INSERT INTO ACCOUNT
    (ID_Utente, Nome, Cognome, Email, Password_Hash, Password_Salt, Password_Iterazioni, Ruolo, Attivo)
VALUES
    ((SELECT ID_Utente FROM UTENTE WHERE Email = 'junior1@piscina.local'), 'Marco', 'Junior', 'junior1@piscina.local', 'PmjNbf+5NUapBahRJ40e3Oxl86hc1aWjrqOOl4ZQdek=', 'GAPxreqWrnOdXDdta1KyYQ==', 210000, 'UTENTE', TRUE),
    ((SELECT ID_Utente FROM UTENTE WHERE Email = 'junior2@piscina.local'), 'Sofia', 'Giovane', 'junior2@piscina.local', 'PmjNbf+5NUapBahRJ40e3Oxl86hc1aWjrqOOl4ZQdek=', 'GAPxreqWrnOdXDdta1KyYQ==', 210000, 'UTENTE', TRUE),
    ((SELECT ID_Utente FROM UTENTE WHERE Email = 'senior2@piscina.local'), 'Luca', 'Master', 'senior2@piscina.local', 'PmjNbf+5NUapBahRJ40e3Oxl86hc1aWjrqOOl4ZQdek=', 'GAPxreqWrnOdXDdta1KyYQ==', 210000, 'UTENTE', TRUE),
    ((SELECT ID_Utente FROM UTENTE WHERE Email = 'coach@piscina.local'), 'Giulia', 'Coach', 'coach@piscina.local', 'Ci2KFGEU0mCQn+865yHDg4bRUQlaYs+NAqpCKyOOjfk=', 'ByawZajFziCzwe/ZARpaJg==', 210000, 'UTENTE', TRUE)
ON DUPLICATE KEY UPDATE ID_Utente = VALUES(ID_Utente), Attivo = TRUE;


-- ===========================================================================
-- ACCOUNT TECNICI DELL'INTERFACCIA
-- ===========================================================================
-- Amministratore: admin@piscina.local / Admin123!
-- Club:            club@piscina.local / Club123!
-- Cliente:        cliente@piscina.local / Cliente123!
-- Atleta:         atleta@piscina.local / Atleta123!
-- Istruttore:     istruttore@piscina.local / Istruttore123!
-- Entrambi:       atleta.istruttore@piscina.local / Completo123!

INSERT INTO ACCOUNT
    (ID_Utente, Nome, Cognome, Email, Password_Hash, Password_Salt,
     Password_Iterazioni, Ruolo, Attivo)
VALUES
    (NULL, 'Ada', 'Amministratrice', 'admin@piscina.local',
     'WtIXo7Cuf+dpsbXZk3jy7Mpp7C3M5npIkQ2cDAr7Z4w=',
     'gn61Au/imua65hXgNHGoEA==', 210000, 'AMMINISTRATORE', TRUE),
    (NULL, 'Nuoto', 'Emilia', 'club@piscina.local',
     'YEjRdihexV2bM3i7xEAooSCbJoX0ZtVDMlUE4ZPfM4M=',
     'JwD8tH93fWUSh0L5zTJjDg==', 210000, 'CLUB', TRUE),
    ((SELECT ID_Utente FROM UTENTE
      WHERE Email = 'cliente@piscina.local'),
     'Chiara', 'Cliente', 'cliente@piscina.local',
     'RPE+zAdnSs/ZKKxzehXtt+slgME99Vxkqa8MUlw3eIk=',
     'wGdohnNA44cC06F4yQi59w==', 210000, 'UTENTE', TRUE),
    ((SELECT ID_Utente FROM UTENTE
      WHERE Email = 'atleta@piscina.local'),
     'Alice', 'Atleta', 'atleta@piscina.local',
     'PmjNbf+5NUapBahRJ40e3Oxl86hc1aWjrqOOl4ZQdek=',
     'GAPxreqWrnOdXDdta1KyYQ==', 210000, 'UTENTE', TRUE),
    ((SELECT ID_Utente FROM UTENTE
      WHERE Email = 'istruttore@piscina.local'),
     'Ivo', 'Istruttore', 'istruttore@piscina.local',
     'Ci2KFGEU0mCQn+865yHDg4bRUQlaYs+NAqpCKyOOjfk=',
     'ByawZajFziCzwe/ZARpaJg==', 210000, 'UTENTE', TRUE),
    ((SELECT ID_Utente FROM UTENTE
      WHERE Email = 'atleta.istruttore@piscina.local'),
     'Andrea', 'Completo', 'atleta.istruttore@piscina.local',
     'IkS6gaFAe8o4zjYBm8O28LxxFCXEho8M4L8ofXpShco=',
     'X6VCOgjgHB8sl+kwlVY5kw==', 210000, 'UTENTE', TRUE)
ON DUPLICATE KEY UPDATE
    ID_Utente = VALUES(ID_Utente),
    Nome = VALUES(Nome),
    Cognome = VALUES(Cognome),
    Password_Hash = VALUES(Password_Hash),
    Password_Salt = VALUES(Password_Salt),
    Password_Iterazioni = VALUES(Password_Iterazioni),
    Ruolo = VALUES(Ruolo),
    Attivo = VALUES(Attivo);
