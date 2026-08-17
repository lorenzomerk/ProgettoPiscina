package it.unibo.piscina.dao;

import it.unibo.piscina.model.AccountAutenticazione;
import java.util.Optional;

/** Contratto di persistenza degli account applicativi. */
public interface AccountDAO {

    Optional<AccountAutenticazione> findByEmail(String email);

    long insert(AccountAutenticazione account);

    void recordSuccessfulAccess(long accountId);
}
