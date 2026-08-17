-- Gestione Piscina - query operative e dimostrative OP1-OP11.
-- Eseguire dopo 01_schema_completo.sql e 02_popolamento_demo.sql.
-- Le scritture dimostrative OP1-OP6 sono racchiuse in una transazione
-- annullata prima delle consultazioni, quindi non sporcano il database.
USE piscina_progetto;

SET @dal = CURRENT_DATE - INTERVAL 1 YEAR;
SET @al = CURRENT_DATE;
SET @corso_demo = (
    SELECT ID_Attivita_Programmata
    FROM ATTIVITA_PROGRAMMATA
    WHERE Titolo = 'Corso adulti demo'
    LIMIT 1
);
SET @nuoto_libero_demo = (
    SELECT ID_Attivita_Programmata
    FROM ATTIVITA_PROGRAMMATA
    WHERE Titolo = 'Nuoto libero demo'
    LIMIT 1
);
SET @allenamento_demo = (
    SELECT ID_Attivita_Programmata
    FROM ATTIVITA_PROGRAMMATA
    WHERE Titolo = 'Allenamento squadra demo'
    LIMIT 1
);

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
SET @tipo_corso = (
    SELECT ID_Tipo_Attivita
    FROM TIPO_ATTIVITA
    WHERE Nome = 'Corso di nuoto'
    LIMIT 1
);

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

INSERT INTO TIPO_ATTIVITA
    (Nome, Modalita_Partecipazione,
     Richiede_Istruttore, Descrizione)
VALUES
    ('Attivita temporanea query', 'ISCRIZIONE', TRUE,
     'Creata esclusivamente per mostrare OP3');

SET @tipo_attivita_query = LAST_INSERT_ID();

INSERT INTO ATTIVITA_PROGRAMMATA
    (ID_Tipo_Attivita, Titolo, Giorno_Settimanale,
     Ora_Inizio, Ora_Fine, Capienza_Massima,
     Periodo_Inizio, Periodo_Fine, Stato)
VALUES
    (@tipo_attivita_query, 'Attivita temporanea query',
     'DOMENICA', '02:00:00', '03:00:00', 10,
     '2035-01-01', '2035-12-31', 'PROGRAMMATA');

SET @attivita_query = LAST_INSERT_ID();
SET @vasca_principale = (
    SELECT ID_Vasca FROM VASCA
    WHERE Nome = 'Vasca principale'
    LIMIT 1
);

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

SET @tipo_dieci_ingressi = (
    SELECT ID_Tipo_Abbonamento
    FROM TIPO_ABBONAMENTO
    WHERE Nome = 'Dieci ingressi'
    LIMIT 1
);

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
SELECT *
FROM VW_ATTIVITA_COMPLETE
ORDER BY Titolo;

-- OP8: club e squadra con più atleti attualmente appartenenti.
SELECT *
FROM VW_CLUB_ATLETI_ATTIVI
ORDER BY Numero_Atleti DESC, Nome
LIMIT 1;

SELECT *
FROM VW_SQUADRA_ATLETI_ATTIVI
ORDER BY Numero_Atleti DESC, Nome
LIMIT 1;

-- OP9: istruttore con più attività sovrapposte al periodo richiesto.

SELECT
    i.ID_Utente,
    CONCAT(u.Cognome, ' ', u.Nome) AS Istruttore,
    COUNT(ap.ID_Attivita_Programmata) AS Numero_Attivita
FROM ISTRUTTORE i
JOIN UTENTE u ON u.ID_Utente = i.ID_Utente
LEFT JOIN ASSEGNATO_A aa
  ON aa.ID_Utente_Istruttore = i.ID_Utente
LEFT JOIN ATTIVITA_PROGRAMMATA ap
  ON ap.ID_Attivita_Programmata = aa.ID_Attivita_Programmata
 AND ap.Periodo_Inizio <= @al
 AND ap.Periodo_Fine >= @dal
GROUP BY i.ID_Utente, u.Cognome, u.Nome
ORDER BY Numero_Attivita DESC
LIMIT 1;

-- OP10: tipo di attività a iscrizione e tipo di abbonamento più frequenti.
SELECT ta.ID_Tipo_Attivita, ta.Nome, COUNT(*) AS Numero_Iscrizioni
FROM ISCRIZIONE_ATTIVITA i
JOIN ATTIVITA_PROGRAMMATA ap
  ON ap.ID_Attivita_Programmata = i.ID_Attivita_Programmata
JOIN TIPO_ATTIVITA ta
  ON ta.ID_Tipo_Attivita = ap.ID_Tipo_Attivita
WHERE i.Data_Iscrizione BETWEEN @dal AND @al
  AND ta.Modalita_Partecipazione = 'ISCRIZIONE'
GROUP BY ta.ID_Tipo_Attivita, ta.Nome
ORDER BY Numero_Iscrizioni DESC
LIMIT 1;

SELECT t.ID_Tipo_Abbonamento, t.Nome, COUNT(*) AS Numero_Acquisti
FROM ABBONAMENTO a
JOIN TIPO_ABBONAMENTO t
  ON t.ID_Tipo_Abbonamento = a.ID_Tipo_Abbonamento
WHERE a.Data_Acquisto BETWEEN @dal AND @al
GROUP BY t.ID_Tipo_Abbonamento, t.Nome
ORDER BY Numero_Acquisti DESC
LIMIT 1;

-- OP11: numero di abbonamenti attualmente utilizzabili.
SELECT COUNT(*) AS Abbonamenti_Utilizzabili
FROM VW_ABBONAMENTI_UTILIZZABILI;
