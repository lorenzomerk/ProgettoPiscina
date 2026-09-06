Avvio rapido Gestione Piscina
Connessione MySQL preconfigurata: 127.0.0.1:3306,
database piscina_progetto, utente root, password vuota.

1) Avvia MySQL sulla porta 3306.

2) In MySQL Workbench, per un database nuovo esegui integralmente, in ordine:
   doc/sql/01_schema_completo.sql
   doc/sql/02_popolamento_demo.sql
   Per aggiornare un database di una precedente versione esegui invece:
   doc/sql/03_migrazione_database_esistente.sql
   doc/sql/01_schema_completo.sql
   Esegui anche 02 se servono i dati e gli account demo.

3) Apri ProgettoPiscina in Visual Studio Code, con le estensioni
   Language Support for Java e Debugger for Java installate.
   In Esegui e debug seleziona "Avvia Gestione Piscina" e premi F5.

4) Nella schermata iniziale seleziona "Accedi" e usa un login demo.

Login demo amministratore: admin@piscina.local / Admin123!
Login demo club: club@piscina.local / Club123!
Login demo utente: cliente@piscina.local / Cliente123!
Login demo atleta: atleta@piscina.local / Atleta123!
Login demo istruttore: istruttore@piscina.local / Istruttore123!
Login demo atleta e istruttore: atleta.istruttore@piscina.local / Completo123!
