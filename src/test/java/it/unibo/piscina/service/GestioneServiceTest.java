package it.unibo.piscina.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import it.unibo.piscina.dao.GestioneDAO;
import it.unibo.piscina.model.CampoEntita;
import it.unibo.piscina.model.DatiTabella;
import it.unibo.piscina.model.DefinizioneEntita;
import it.unibo.piscina.model.OpzioneRiferimento;
import it.unibo.piscina.model.TipoCampo;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class GestioneServiceTest {

    private RecordingDAO dao;
    private GestioneService service;
    private DefinizioneEntita definition;

    @BeforeEach
    void setUp() {
        dao = new RecordingDAO();
        service = new GestioneService(dao);
        definition = new DefinizioneEntita(
            "Esempio",
            "Test",
            "ESEMPIO",
            "SELECT ID, Nome, Attivo FROM ESEMPIO",
            List.of(
                CampoEntita.id("ID", "ID"),
                CampoEntita.campo(
                    "Nome", "Nome", TipoCampo.TESTO, true
                ),
                CampoEntita.campo(
                    "Attivo", "Attivo", TipoCampo.BOOLEANO, true
                )
            ),
            true,
            true,
            true
        );
    }

    @Test
    void inserimentoEscludeLaChiaveGenerata() {
        service.inserisci(definition, List.of("Prova", true));

        assertEquals(
            "INSERT INTO ESEMPIO (Nome, Attivo) VALUES (?, ?)",
            dao.lastSql
        );
        assertEquals(List.of("Prova", true), dao.lastParameters);
    }

    @Test
    void modificaUsaLaChiaveOriginale() {
        service.modifica(
            definition,
            List.of(7L, "Prima", true),
            List.of("Dopo", false)
        );

        assertEquals(
            "UPDATE ESEMPIO SET Nome = ?, Attivo = ? WHERE ID = ?",
            dao.lastSql
        );
        assertEquals(List.of("Dopo", false, 7L), dao.lastParameters);
    }

    @Test
    void rispettaIPermessiDellaDefinizione() {
        final DefinizioneEntita readOnly = new DefinizioneEntita(
            definition.titolo(),
            definition.descrizione(),
            definition.tabella(),
            definition.querySelezione(),
            definition.campi(),
            false,
            false,
            false
        );

        assertThrows(
            IllegalArgumentException.class,
            () -> service.inserisci(readOnly, List.of("Prova", true))
        );
    }

    private static final class RecordingDAO implements GestioneDAO {

        private String lastSql;
        private List<Object> lastParameters;

        @Override
        public DatiTabella query(final String sql) {
            return new DatiTabella(List.of(), List.of());
        }

        @Override
        public List<OpzioneRiferimento> caricaOpzioni(final String sql) {
            return List.of();
        }

        @Override
        public int execute(
                final String sql,
                final List<Object> parameters) {

            lastSql = sql;
            lastParameters = List.copyOf(parameters);
            return 1;
        }

        @Override
        public void sincronizzaStati() {
        }
    }
}
