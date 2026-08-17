# Script SQL consolidati Gestione Piscina

La cartella `doc/sql` contiene i quattro script SQL ufficiali del progetto:

1. `01_schema_completo.sql` crea database, tabelle, indici, viste e trigger
   coerenti con i capitoli 1-3 della relazione.
2. `02_popolamento_demo.sql` inserisce in un unico passaggio utenti,
   qualifiche, abbonamenti, attività, spazi, club, rapporti sportivi e account
   dimostrativi per accedere all'applicazione.
3. `03_migrazione_database_esistente.sql` aggiorna il nucleo della prima
   versione senza cancellare utenti o account.
4. `04_query_operazioni.sql` raccoglie esempi eseguibili per OP1-OP11. Le
   scritture dimostrative vengono annullate con `ROLLBACK`.

## Nuova installazione

Con un account amministrativo MySQL eseguire nell'ordine:

1. `01_schema_completo.sql`;
2. `02_popolamento_demo.sql`.

`01_schema_completo.sql` è idempotente e non elimina automaticamente un
database esistente. Per una reinstallazione completamente pulita eliminare
prima `piscina_progetto` da MySQL Workbench, dopo avere eseguito un backup.

## Aggiornamento della prima versione

Per conservare utenti e account già presenti:

1. eseguire `03_migrazione_database_esistente.sql`;
2. eseguire `01_schema_completo.sql`, che aggiunge le nuove tabelle, viste e
   trigger senza eliminare i dati;
3. eseguire facoltativamente `02_popolamento_demo.sql` se servono i dati
   dimostrativi.

`04_query_operazioni.sql` è documentale e non è necessario per avviare
l'applicazione.

## Accessi creati dal popolamento

Dopo avere eseguito `02_popolamento_demo.sql`, nella schermata iniziale
dell'applicazione selezionare **Accedi** e usare una delle credenziali
seguenti:

| Prospettiva della relazione | Email | Password |
|---|---|---|
| Amministratore | `admin@piscina.local` | `Admin123!` |
| Club | `club@piscina.local` | `Club123!` |
| Utente | `cliente@piscina.local` | `Cliente123!` |
| Utente con qualifica di atleta | `atleta@piscina.local` | `Atleta123!` |
| Utente con qualifica di istruttore | `istruttore@piscina.local` | `Istruttore123!` |
| Utente atleta e istruttore | `atleta.istruttore@piscina.local` | `Completo123!` |

I profili applicativi Amministratore, Club e Utente corrispondono alle tre
prospettive operative della relazione. Atleta e istruttore sono qualifiche
della persona collegata e non profili di autenticazione autonomi.

Queste credenziali applicative sono distinte da quelle dell'utente JDBC usato
dal programma per collegarsi a MySQL.
