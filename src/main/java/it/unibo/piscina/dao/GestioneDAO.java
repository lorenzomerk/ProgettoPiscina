package it.unibo.piscina.dao;

import it.unibo.piscina.model.DatiTabella;
import it.unibo.piscina.model.OpzioneRiferimento;
import java.util.List;

/** Operazioni JDBC comuni ai moduli configurabili del dominio. */
public interface GestioneDAO {

    DatiTabella query(String sql);

    List<OpzioneRiferimento> caricaOpzioni(String sql);

    int execute(String sql, List<Object> parameters);

    void sincronizzaStati();
}
