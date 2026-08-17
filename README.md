# Gestione Piscina

Applicazione desktop Java 21/Swing per la gestione di una piscina, con
persistenza MySQL tramite JDBC. Il dominio applicativo riproduce l'analisi dei
requisiti e lo schema concettuale descritti nei capitoli 1-3 della relazione.

## Requisiti

- JDK 21;
- MySQL 8.0 o successivo, in ascolto per impostazione predefinita sulla porta
  `3306`;
- un account MySQL amministrativo per creare lo schema;
- PowerShell per eseguire i comandi riportati in questo documento.

## Struttura del progetto

```text
ProgettoPiscina/
├── src/
│   ├── main/
│   │   ├── java/                 sorgenti dell'applicazione
│   │   └── resources/            configurazione JDBC
│   └── test/java/                test JUnit
├── doc/sql/                      quattro script SQL consolidati e numerati
├── lib/                          driver MySQL JDBC
├── tools/                        controllo di coerenza con la relazione
└── README.md
```

Le cartelle `bin/` e `build/`, quando presenti, contengono esclusivamente file
generati dalla compilazione o dai test e non fanno parte dei sorgenti.

## Preparazione del database

Avviare MySQL, quindi aprire MySQL Workbench con un account autorizzato a
creare database, tabelle, viste e trigger.

### Nuova installazione

Eseguire nell'ordine:

1. `doc/sql/01_schema_completo.sql`, obbligatorio;
2. `doc/sql/02_popolamento_demo.sql`, facoltativo ma necessario per disporre
   subito dei dati e degli account dimostrativi elencati più avanti.

Non occorre eseguire gli script `03` e `04` per una nuova installazione.

### Aggiornamento di un database della prima versione

Eseguire nell'ordine:

1. `doc/sql/03_migrazione_database_esistente.sql`;
2. `doc/sql/01_schema_completo.sql`;
3. `doc/sql/02_popolamento_demo.sql` soltanto se servono anche i dati
   dimostrativi.

La migrazione e lo schema non cancellano i dati storici. Prima di aggiornare un
database reale è comunque consigliato eseguire un backup.

### Utente tecnico dell'applicazione

La prima volta, creare l'utente JDBC usato dall'applicazione:

```sql
CREATE USER IF NOT EXISTS
    'piscina_app'@'127.0.0.1'
    IDENTIFIED BY 'Piscina!';

ALTER USER
    'piscina_app'@'127.0.0.1'
    IDENTIFIED BY 'Piscina!';

GRANT SELECT, INSERT, UPDATE, DELETE
ON piscina_progetto.*
TO 'piscina_app'@'127.0.0.1';
```

Questi comandi e gli script di schema devono essere eseguiti da un account
amministrativo MySQL. L'utente `piscina_app` necessita soltanto dei privilegi
DML indicati sopra e non deve essere usato per installare o migrare lo schema.

## Funzionalità implementate

- utenti, contatti, certificato medico e qualifiche sovrapponibili di atleta e
  istruttore;
- tipi di abbonamento, compatibilità e abbonamenti acquistati;
- tipi di attività, calendario, iscrizioni e assegnazioni degli istruttori;
- accessi effettivi al nuoto libero con consumo degli ingressi;
- vasche, corsie convenzionali o fisiche e relativo utilizzo;
- club, squadre, appartenenze degli atleti, incarichi degli istruttori e
  attività svolte dalle squadre;
- viste personali per utente, atleta e istruttore;
- riepiloghi su attività complete, club, squadre, istruttori, tipologie più
  frequenti e abbonamenti utilizzabili.

I vincoli descritti nella relazione sono verificati nel database: modalità e
intervalli coerenti, compatibilità, validità, capienza, iscrizione unica,
sovrapposizioni di corsie e istruttori, ruoli sportivi, rapporti storici e
partecipazione delle squadre.

Lo script facoltativo `doc/sql/04_query_operazioni.sql` documenta con esempi
eseguibili le operazioni OP1-OP11 e annulla le scritture dimostrative con
`ROLLBACK`.

Per configurare una nuova offerta commerciale si crea prima il tipo di
abbonamento non attivo, si inseriscono le compatibilità e infine lo si attiva.
Analogamente, un'attività viene creata come `PROGRAMMATA`, collegata ad almeno
una corsia e agli eventuali istruttori o squadre, quindi portata ad `ATTIVA`.

## Avvio manuale da PowerShell

Il progetto viene compilato e avviato direttamente con `javac` e `java`, senza
Gradle:

```powershell
cd "C:\Users\loren\Desktop\Uni\Basi_di_dati\ProgettoPiscina"

$env:PISCINA_DB_USER = "piscina_app"
$env:PISCINA_DB_PASSWORD = "Piscina!"

javac -d bin `
    -sourcepath src/main/java `
    -cp "lib/*" `
    src/main/java/it/unibo/piscina/App.java

java -cp "bin;lib/*;src/main/resources" it.unibo.piscina.App
```

Il driver `mysql-connector-j-9.7.0.jar` è incluso in `lib`. Le variabili
PowerShell valgono per la sessione corrente e sovrascrivono i valori del file
di configurazione.

## Configurazione JDBC

L'unico file di configurazione è `src/main/resources/database.properties`.
I valori possono essere sostituiti, in ordine di priorità crescente, da:

1. `database.properties`;
2. variabili `PISCINA_DB_URL`, `PISCINA_DB_USER` e `PISCINA_DB_PASSWORD`;
3. proprietà Java `piscina.db.url`, `piscina.db.user` e
   `piscina.db.password`.

Le credenziali JDBC servono al programma per collegarsi a MySQL e sono distinte
dagli account presenti nella tabella `ACCOUNT`.

## Accesso all'applicazione

Per provare l'applicazione con email e password è necessario avere eseguito
`doc/sql/02_popolamento_demo.sql`. Nella schermata iniziale selezionare
**Accedi** e utilizzare una delle credenziali seguenti:

| Prospettiva della relazione | Email | Password | Contenuti disponibili |
|---|---|---|---|
| Amministratore | `admin@piscina.local` | `Admin123!` | Tutte le sezioni, compresa la gestione di club e squadre |
| Utente | `cliente@piscina.local` | `Cliente123!` | Accessi, abbonamenti e attività |
| Utente con qualifica di atleta | `atleta@piscina.local` | `Atleta123!` | Vista personale estesa con i dati da atleta |
| Utente con qualifica di istruttore | `istruttore@piscina.local` | `Istruttore123!` | Vista personale estesa con i dati da istruttore |
| Utente atleta e istruttore | `atleta.istruttore@piscina.local` | `Completo123!` | Vista personale con entrambe le qualifiche |

La prospettiva **Club** si prova accedendo come Amministratore e aprendo la
sezione **Club e squadre**. Gli accessi documentati seguono quindi le classi di
utenza definite nella relazione.

La registrazione dall'interfaccia crea sempre un account utente di base.
Atleta e istruttore non sono account o profili di autenticazione distinti:
sono qualifiche della persona collegata, ricavate rispettivamente dalle tabelle
`ATLETA` e `ISTRUTTORE`. Un utente può possederle entrambe.

La tabella tecnica `ACCOUNT` supporta esclusivamente l'autenticazione
dell'interfaccia e non fa parte del modello concettuale della piscina. Le
prospettive Utente, Club e Amministratore non introducono nuove entità del
dominio. Le password sono protette con PBKDF2-HMAC-SHA256, salt distinto per
account e 210.000 iterazioni.

## Architettura applicativa

Il flusso principale è:

```text
View -> Controller -> Service -> DAO -> MySQL
```

`ApplicationView` gestisce la navigazione principale e
`ApplicationController` coordina sessione e autorizzazioni. Le funzionalità
specifiche sono separate nei rispettivi package `view`, `controller`,
`service`, `dao`, `model` e `data`.
