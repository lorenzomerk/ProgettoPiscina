package it.unibo.piscina.dao;

import it.unibo.piscina.model.Utente;
import java.util.List;

/** Contratto di persistenza per le anagrafiche degli utenti. */
public interface UtenteDAO {

    List<Utente> findAll();

    long insert(Utente utente);

    boolean update(Utente utente);

    boolean deactivate(long id);
}
