package ORM;

import DomainModel.Tag;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.*;

class TagDAOTest {

    private Connection conn;
    private TagDAO tagDAO;

    @BeforeEach
    void setUp() {
        try {
            DBConnection.setEnableTesting(true);
            DBConnection.resetInstance();
            conn = DBConnection.getInstance().getConnection();
            conn.setAutoCommit(false);

            tagDAO = new TagDAO();

        } catch (SQLException e) {
            fail("setUp fallito in TagDAOTest: " + e.getMessage());
        }
    }

    @AfterEach
    void tearDown() {
        try {
            if (conn != null) {
                conn.rollback();
                conn.setAutoCommit(true);
            }
        } catch (SQLException e) {
            fail("tearDown fallito in TagDAOTest: " + e.getMessage());
        }
    }

    @Test
    void findByLabelNormalized_onEmptyTable_returnsNull() {
        Tag result = tagDAO.findByLabelNormalized("qualunque");
        assertNull(result, "Su tabella vuota, findByLabelNormalized deve restituire null");
    }

    @Test
    void addTag_thenFindBySameLabel_returnsTag() {
        Tag t = new Tag("Rock", "Musica rock");
        tagDAO.addTag(t);

        Tag found = tagDAO.findByLabelNormalized("Rock");
        assertNotNull(found, "Il tag appena inserito deve essere trovato");
        assertEquals("Rock", found.getLabel());
        assertEquals("Musica rock", found.getDescription());
    }

    @Test
    void findByLabelNormalized_ignoresCaseAndWhitespace() {
        Tag t = new Tag("Rock Prog", "Progressive Rock");
        tagDAO.addTag(t);

        Tag foundWithSpaces = tagDAO.findByLabelNormalized("   rock prog   ");
        assertNotNull(foundWithSpaces, "La ricerca deve ignorare spazi e maiuscole/minuscole");
        assertEquals("Rock Prog", foundWithSpaces.getLabel());

        Tag foundUppercase = tagDAO.findByLabelNormalized("ROCK PROG");
        assertNotNull(foundUppercase, "La ricerca deve essere case-insensitive");
        assertEquals("Rock Prog", foundUppercase.getLabel());
    }

    @Test
    void findByLabelNormalized_returnsNullForDifferentLabel() {
        Tag t = new Tag("Jazz", "Musica jazz");
        tagDAO.addTag(t);

        Tag found = tagDAO.findByLabelNormalized("Rock");
        assertNull(found, "Cercando una label diversa non devo trovare nulla");
    }

}