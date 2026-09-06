-- Gestione Piscina - query operative e dimostrative OP1-OP11.
-- Eseguire dopo 01_schema_completo.sql; 02_popolamento_demo.sql è facoltativo.
-- Le risorse temporanee sono indipendenti dal giorno e dal popolamento iniziale.
-- Le scritture dimostrative OP1-OP6 sono racchiuse in una transazione
-- annullata prima delle consultazioni, quindi non sporcano il database.
USE piscina_progetto;

SET @dal = MAKEDATE(YEAR(CURRENT_DATE), 1);
SET @al = CURRENT_DATE;
START TRANSACTION;

-- ===========================================================================
-- OP1 - GESTIONE DEGLI UTENTI
-- ===========================================================================

INSERT INTO UTENTE
    (Codice_Fiscale, Nome, Cognome, Data_Nascita,
     Email, Telefono, Scadenza_Certificato_Medico)
VALUES
    ('DMOQRY00A01H501K', 'Utente', 'Dimostrativo', '2000-01-01',
     'query.demo@piscina.local', '3330000000',
     DATE_ADD(CURRENT_DATE, INTERVAL 1 YEAR));

SET @utente_query = LAST_INSERT_ID();

INSERT INTO ATLETA (ID_Utente) VALUES (@utente_query);
INSERT INTO ISTRUTTORE (ID_Utente, Qualifica)
VALUES (@utente_query, 'Istruttore dimostrativo');

UPDATE UTENTE
SET Telefono = '3330000001'
WHERE ID_Utente = @utente_query;

SELECT
    u.ID_Utente, u.Codice_Fiscale, u.Nome, u.Cognome,
    EXISTS (
        SELECT 1 FROM ATLETA a
        WHERE a.ID_Utente = u.ID_Utente
    ) AS Atleta,
    EXISTS (
        SELECT 1 FROM ISTRUTTORE i
        WHERE i.ID_Utente = u.ID_Utente
    ) AS Istruttore
FROM UTENTE u
WHERE u.ID_Utente = @utente_query;

-- ===========================================================================
-- OP2 - GESTIONE DEGLI ABBONAMENTI
-- ===========================================================================

INSERT INTO TIPO_ABBONAMENTO
    (Nome, Costo, Attivo, Modalita_Validita,
     Condizioni_Utilizzo, Durata_Giorni, Numero_Ingressi)
VALUES
    ('Tipo temporaneo query', 10.00, FALSE, 'TEMPO',
     'Creato esclusivamente dentro la transazione dimostrativa',
     30, NULL);

SET @tipo_abbonamento_query = LAST_INSERT_ID();
INSERT INTO TIPO_ATTIVITA
    (Nome, Modalita_Partecipazione, Richiede_Istruttore)
VALUES ('Corso temporaneo query', 'ISCRIZIONE', TRUE);
SET @tipo_corso = LAST_INSERT_ID();

INSERT INTO COMPATIBILITA
    (ID_Tipo_Abbonamento, ID_Tipo_Attivita)
VALUES (@tipo_abbonamento_query, @tipo_corso);

UPDATE TIPO_ABBONAMENTO
SET Attivo = TRUE
WHERE ID_Tipo_Abbonamento = @tipo_abbonamento_query;

INSERT INTO ABBONAMENTO
    (ID_Utente, ID_Tipo_Abbonamento, Data_Acquisto, Data_Inizio)
VALUES
    (@utente_query, @tipo_abbonamento_query,
     CURRENT_DATE, CURRENT_DATE);

SET @abbonamento_query = LAST_INSERT_ID();

-- Verifica esplicita di validità e compatibilità.
SELECT
    a.ID_Abbonamento,
    a.Stato,
    a.Data_Inizio,
    a.Data_Fine,
    c.ID_Tipo_Attivita
FROM ABBONAMENTO a
JOIN COMPATIBILITA c
  ON c.ID_Tipo_Abbonamento = a.ID_Tipo_Abbonamento
WHERE a.ID_Abbonamento = @abbonamento_query
  AND a.Stato = 'ATTIVO'
  AND CURRENT_DATE BETWEEN a.Data_Inizio AND a.Data_Fine
  AND c.ID_Tipo_Attivita = @tipo_corso;

-- ===========================================================================
-- OP3 - GESTIONE DELLE ATTIVITA E DEGLI SPAZI
-- ===========================================================================

SET @giorno_query = ELT(WEEKDAY(CURRENT_DATE) + 1,
    'LUNEDI', 'MARTEDI', 'MERCOLEDI', 'GIOVEDI', 'VENERDI', 'SABATO', 'DOMENICA');
INSERT INTO VASCA (Nome, Lunghezza, Larghezza, Profondita, Temperatura, Tipologia)
VALUES ('Vasca temporanea query', 25, 12, 1.5, 28, 'Coperta');
SET @vasca_principale = LAST_INSERT_ID();
INSERT INTO CORSIA (ID_Vasca, Numero) VALUES (@vasca_principale, 1), (@vasca_principale, 2);

INSERT INTO ATTIVITA_PROGRAMMATA
    (ID_Tipo_Attivita, Titolo, Giorno_Settimanale,
     Ora_Inizio, Ora_Fine, Capienza_Massima,
     Periodo_Inizio, Periodo_Fine, Stato)
VALUES (@tipo_corso, 'Corso temporaneo query', @giorno_query,
        '08:00:00', '09:00:00', 10, CURRENT_DATE,
        CURRENT_DATE + INTERVAL 29 DAY, 'PROGRAMMATA');
SET @attivita_query = LAST_INSERT_ID();
SET @corso_demo = @attivita_query;

INSERT INTO UTILIZZA
    (ID_Attivita_Programmata, ID_Vasca, Numero_Corsia)
VALUES (@attivita_query, @vasca_principale, 1);

INSERT INTO ASSEGNATO_A
    (ID_Utente_Istruttore, ID_Attivita_Programmata)
VALUES (@utente_query, @attivita_query);

SELECT
    ap.ID_Attivita_Programmata,
    ap.Titolo,
    ap.Giorno_Settimanale,
    ap.Ora_Inizio,
    ap.Ora_Fine,
    u.ID_Vasca,
    u.Numero_Corsia
FROM ATTIVITA_PROGRAMMATA ap
JOIN UTILIZZA u
  ON u.ID_Attivita_Programmata =
     ap.ID_Attivita_Programmata
WHERE ap.ID_Attivita_Programmata = @attivita_query;

-- ===========================================================================
-- OP4 - ISCRIZIONE A UN'ATTIVITA
-- ===========================================================================

INSERT INTO ISCRIZIONE_ATTIVITA
    (ID_Abbonamento, ID_Attivita_Programmata, Data_Iscrizione)
VALUES (@abbonamento_query, @corso_demo, CURRENT_DATE);

SELECT
    i.ID_Iscrizione_Attivita,
    i.Data_Iscrizione,
    ap.Titolo,
    CONCAT(u.Cognome, ' ', u.Nome) AS Utente
FROM ISCRIZIONE_ATTIVITA i
JOIN ABBONAMENTO a
  ON a.ID_Abbonamento = i.ID_Abbonamento
JOIN UTENTE u ON u.ID_Utente = a.ID_Utente
JOIN ATTIVITA_PROGRAMMATA ap
  ON ap.ID_Attivita_Programmata =
     i.ID_Attivita_Programmata
WHERE a.ID_Utente = @utente_query;

-- ===========================================================================
-- OP5 - REGISTRAZIONE DI UN ACCESSO AL NUOTO LIBERO
-- ===========================================================================

INSERT INTO TIPO_ATTIVITA (Nome, Modalita_Partecipazione, Richiede_Istruttore)
VALUES ('Nuoto libero temporaneo query', 'ACCESSO_LIBERO', FALSE);
SET @tipo_libero_query = LAST_INSERT_ID();
INSERT INTO TIPO_ABBONAMENTO
    (Nome, Costo, Attivo, Modalita_Validita, Numero_Ingressi, Condizioni_Utilizzo)
VALUES ('Dieci ingressi temporanei query', 10, FALSE, 'INGRESSI', 10,
        'Accessi alla fascia dimostrativa');
SET @tipo_dieci_ingressi = LAST_INSERT_ID();
INSERT INTO COMPATIBILITA (ID_Tipo_Abbonamento, ID_Tipo_Attivita)
VALUES (@tipo_dieci_ingressi, @tipo_libero_query);
UPDATE TIPO_ABBONAMENTO SET Attivo = TRUE
WHERE ID_Tipo_Abbonamento = @tipo_dieci_ingressi;
INSERT INTO ATTIVITA_PROGRAMMATA
    (ID_Tipo_Attivita, Titolo, Giorno_Settimanale, Ora_Inizio, Ora_Fine,
     Capienza_Massima, Periodo_Inizio, Periodo_Fine, Stato)
VALUES (@tipo_libero_query, 'Fascia temporanea query', @giorno_query,
        '00:00:00', '23:59:59', 10, CURRENT_DATE, CURRENT_DATE, 'PROGRAMMATA');
SET @nuoto_libero_demo = LAST_INSERT_ID();
INSERT INTO UTILIZZA (ID_Attivita_Programmata, ID_Vasca, Numero_Corsia)
VALUES (@nuoto_libero_demo, @vasca_principale, 2);
UPDATE ATTIVITA_PROGRAMMATA SET Stato = 'ATTIVA'
WHERE ID_Attivita_Programmata = @nuoto_libero_demo;

INSERT INTO ABBONAMENTO
    (ID_Utente, ID_Tipo_Abbonamento, Data_Acquisto, Data_Inizio)
VALUES
    (@utente_query, @tipo_dieci_ingressi,
     CURRENT_DATE, CURRENT_DATE);

SET @abbonamento_ingressi_query = LAST_INSERT_ID();

INSERT INTO ACCESSO_NUOTO_LIBERO
    (ID_Abbonamento, ID_Attivita_Programmata, Data_Ora_Accesso)
VALUES
    (@abbonamento_ingressi_query,
     @nuoto_libero_demo, CURRENT_TIMESTAMP);

SELECT ID_Abbonamento, Stato, Ingressi_Rimanenti
FROM ABBONAMENTO
WHERE ID_Abbonamento = @abbonamento_ingressi_query;

-- ===========================================================================
-- OP6 - GESTIONE DELLA COMPONENTE SPORTIVA
-- ===========================================================================

INSERT INTO CLUB_SPORTIVO (Nome, Email)
VALUES ('Club temporaneo query', 'club.query@piscina.local');
SET @club_query = LAST_INSERT_ID();

INSERT INTO SQUADRA (ID_Club, Nome, Categoria)
VALUES (@club_query, 'Squadra temporanea query', 'Demo');
SET @squadra_query = LAST_INSERT_ID();

INSERT INTO APPARTENENZA_SQUADRA
    (ID_Utente_Atleta, ID_Squadra, Data_Inizio)
VALUES (@utente_query, @squadra_query, CURRENT_DATE);

INSERT INTO INCARICO_SQUADRA
    (ID_Utente_Istruttore, ID_Squadra, Data_Inizio)
VALUES (@utente_query, @squadra_query, CURRENT_DATE);

INSERT INTO TIPO_ATTIVITA (Nome, Modalita_Partecipazione, Richiede_Istruttore)
VALUES ('Allenamento temporaneo query', 'SQUADRA', TRUE);
SET @tipo_squadra_query = LAST_INSERT_ID();
INSERT INTO ATTIVITA_PROGRAMMATA
    (ID_Tipo_Attivita, Titolo, Giorno_Settimanale, Ora_Inizio, Ora_Fine,
     Capienza_Massima, Periodo_Inizio, Periodo_Fine, Stato)
VALUES (@tipo_squadra_query, 'Allenamento temporaneo query', @giorno_query,
        '09:00:00', '10:00:00', 10, CURRENT_DATE, CURRENT_DATE, 'PROGRAMMATA');
SET @allenamento_demo = LAST_INSERT_ID();
INSERT INTO UTILIZZA (ID_Attivita_Programmata, ID_Vasca, Numero_Corsia)
VALUES (@allenamento_demo, @vasca_principale, 1);
INSERT INTO ASSEGNATO_A (ID_Utente_Istruttore, ID_Attivita_Programmata)
VALUES (@utente_query, @allenamento_demo);

INSERT INTO SVOLGE
    (ID_Squadra, ID_Attivita_Programmata)
VALUES (@squadra_query, @allenamento_demo);

SELECT
    c.Nome AS Club,
    s.Nome AS Squadra,
    a.Data_Inizio AS Inizio_Appartenenza,
    i.Data_Inizio AS Inizio_Incarico
FROM CLUB_SPORTIVO c
JOIN SQUADRA s ON s.ID_Club = c.ID_Club
JOIN APPARTENENZA_SQUADRA a
  ON a.ID_Squadra = s.ID_Squadra
JOIN INCARICO_SQUADRA i
  ON i.ID_Squadra = s.ID_Squadra
WHERE s.ID_Squadra = @squadra_query;

-- Tutte le scritture OP1-OP6 erano dimostrative.
ROLLBACK;

-- ===========================================================================
-- OP7-OP11 - CONSULTAZIONI OPERATIVE E AGGREGATE
-- ===========================================================================

-- OP7: attività che hanno raggiunto la capienza.
SELECT ID_Attivita_Programmata AS ID, Titolo, Numero_Iscrizioni AS Iscritti, Capienza_Massima AS Capienza 
FROM VW_ATTIVITA_COMPLETE 
ORDER BY Titolo; 

-- OP8: classifiche di club e squadre; tutti i pari merito sono visibili.
SELECT ID_Club AS ID, Nome, Numero_Atleti AS Atleti_Attivi 
FROM VW_CLUB_ATLETI_ATTIVI 
ORDER BY Numero_Atleti DESC, Nome; 

SELECT ID_Squadra AS ID, Nome, Club, Numero_Atleti AS Atleti_Attivi 
FROM VW_SQUADRA_ATLETI_ATTIVI 
ORDER BY Numero_Atleti DESC, Nome; 

-- OP9: classifica degli istruttori per attività sovrapposte al periodo richiesto.

SELECT
    i.ID_Utente AS ID,
    CONCAT(u.Cognome, ' ', u.Nome) AS Istruttore,
    COUNT(ap.ID_Attivita_Programmata) AS Attivita
FROM ISTRUTTORE i
JOIN UTENTE u ON u.ID_Utente = i.ID_Utente
LEFT JOIN ASSEGNATO_A aa
  ON aa.ID_Utente_Istruttore = i.ID_Utente
LEFT JOIN ATTIVITA_PROGRAMMATA ap
  ON ap.ID_Attivita_Programmata = aa.ID_Attivita_Programmata
 AND ap.Periodo_Inizio <= @al
 AND ap.Periodo_Fine >= @dal
GROUP BY i.ID_Utente, u.Cognome, u.Nome
ORDER BY Attivita DESC, u.Cognome, u.Nome;

-- OP10: tipo di attività a iscrizione e tipo di abbonamento più frequenti.
-- Come nella GUI: un unico risultato, inclusi i tipi con frequenza zero.
SELECT 'ATTIVITA' AS Ambito, ta.Nome,
       COUNT(i.ID_Iscrizione_Attivita) AS Frequenza
FROM TIPO_ATTIVITA ta
LEFT JOIN ATTIVITA_PROGRAMMATA ap
  ON ap.ID_Tipo_Attivita = ta.ID_Tipo_Attivita
LEFT JOIN ISCRIZIONE_ATTIVITA i
  ON i.ID_Attivita_Programmata = ap.ID_Attivita_Programmata
 AND i.Data_Iscrizione BETWEEN @dal AND @al
WHERE ta.Modalita_Partecipazione = 'ISCRIZIONE'
GROUP BY ta.ID_Tipo_Attivita, ta.Nome
UNION ALL
SELECT 'ABBONAMENTO' AS Ambito, t.Nome,
       COUNT(a.ID_Abbonamento) AS Frequenza
FROM TIPO_ABBONAMENTO t
LEFT JOIN ABBONAMENTO a
  ON a.ID_Tipo_Abbonamento = t.ID_Tipo_Abbonamento
 AND a.Data_Acquisto BETWEEN @dal AND @al
GROUP BY t.ID_Tipo_Abbonamento, t.Nome
ORDER BY Ambito, Frequenza DESC, Nome
;

-- OP11: numero di abbonamenti attualmente utilizzabili.
SELECT COUNT(*) AS Abbonamenti_Attualmente_Utilizzabili
FROM VW_ABBONAMENTI_UTILIZZABILI;
