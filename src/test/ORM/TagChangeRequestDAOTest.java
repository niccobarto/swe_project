package ORM;

import DomainModel.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class TagChangeRequestDAOTest {

    private Connection conn;

    private UserDAO userDAO;
    private DocumentDAO documentDAO;
    private TagDAO tagDAO;
    private TagChangeRequestDAO tagChangeRequestDAO;

    private User authorUser;
    private User moderatorUser;
    private Document testDocument;

    @BeforeEach
    void setUp() {
        try {
            DBConnection.setEnableTesting(true);
            DBConnection.resetInstance();
            conn = DBConnection.getInstance().getConnection();
            conn.setAutoCommit(false);

            // inizializzo i DAO
            userDAO = new UserDAO();
            documentDAO = new DocumentDAO();
            tagDAO = new TagDAO();
            tagChangeRequestDAO = new TagChangeRequestDAO();

            String base = String.valueOf(System.currentTimeMillis());

            String authorEmail = "tagreq-author+" + base + "@example.com";
            userDAO.addUser("Author", "TagReq", authorEmail, "pwd", false, false);
            authorUser = userDAO.getUserByEmail(authorEmail);
            assertNotNull(authorUser, "Impossibile creare l'author di test");

            String modEmail = "tagreq-moderator+" + base + "@example.com";
            userDAO.addUser("Moderator", "TagReq", modEmail, "pwd", true, false);
            moderatorUser = userDAO.getUserByEmail(modEmail);
            assertNotNull(moderatorUser, "Impossibile creare il moderatore di test");

            documentDAO.addDocument(
                    authorUser,
                    "TagReqDoc",
                    "descrizione",
                    "1900",
                    DocumentFormat.TXT,
                    "document/" + authorUser.getId() + "/",
                    "file1",
                    List.of()
            );

            List<Document> docs = documentDAO.getDocumentsByAuthor(authorUser.getId());
            assertFalse(docs.isEmpty(), "Non è stato creato il documento di test");
            testDocument = docs.get(0);

        } catch (SQLException e) {
            fail("setUp fallito in TagChangeRequestDAOTest: " + e.getMessage());
        }
    }

    @AfterEach
    void tearDown() {
        try {
            if (conn != null) {
                conn.rollback();       // annulla tutte le modifiche fatte nel test
                conn.setAutoCommit(true);
            }
        } catch (SQLException e) {
            fail("tearDown fallito in TagChangeRequestDAOTest: " + e.getMessage());
        }
    }

    @Test
    void addRequestForExistingTag_persistsRequestAndSetsId() {
        // 1) Creo il tag "rock" in tabella tag, perché esiste una FK su existing_tag_label
        tagDAO.addTag(new Tag("rock", "descrizione rock"));

        // 2) Creo una richiesta PENDING per il tag esistente "rock" sul documento di test
        TagChangeRequest req = TagChangeRequest.forExistingTag(
                testDocument,
                "rock",
                TagChangeOperation.ADD
        );

        // 3) Chiamata al metodo da testare
        tagChangeRequestDAO.addRequestForExistingTag(req);

        // 4) Verifico che l'id sia stato valorizzato (se fosse null qui avresti NPE)
        int generatedId = req.getId();
        assertTrue(generatedId > 0, "Dopo l'inserimento l'id della request deve essere > 0");

        // 5) Recupero la stessa request dal DB e verifico i campi
        TagChangeRequest fromDb = tagChangeRequestDAO.getById(generatedId);
        assertNotNull(fromDb, "La request appena inserita deve essere recuperabile con getById");

        assertEquals(RequestStatus.PENDING, fromDb.getStatus(), "La request deve essere PENDING");
        assertEquals(TagChangeOperation.ADD, fromDb.getOperation(), "Operation deve essere ADD");
        assertEquals("rock", fromDb.getExistingTagLabel(), "existingTagLabel deve essere 'rock'");
        assertNull(fromDb.getProposedLabel(), "proposedLabel deve essere null per una richiesta su tag esistente");
        assertNotNull(fromDb.getDocument(), "Il documento associato non deve essere null");
        assertEquals(testDocument.getId(), fromDb.getDocument().getId(),
                "Il document_id deve essere quello del testDocument");
    }

    @Test
    void addRequestForExistingTag_withNewLabelRequest_doesNotInsert() {
        // nessuna richiesta iniziale
        int before = countAllTagChangeRequests();

        // creo una richiesta per NUOVO label (non per existingTag)
        TagChangeRequest wrongReq = TagChangeRequest.forNewLabel(
                testDocument,
                "nuovo-tag"
        );

        // chiamata al metodo SBAGLIATO: dovrebbe fallire la validazione interna
        tagChangeRequestDAO.addRequestForExistingTag(wrongReq);

        // dopo la chiamata, il numero di richieste deve essere invariato
        int after = countAllTagChangeRequests();
        assertEquals(before, after,
                "Una request per newLabel passata a addRequestForExistingTag non deve inserire righe");
    }

    // helper per contare tutte le richieste in tabella
    private int countAllTagChangeRequests() {
        try {
            PreparedStatement ps = conn.prepareStatement("SELECT COUNT(*) AS cnt FROM tag_change_request");
            ResultSet rs = ps.executeQuery();
            int cnt = rs.next() ? rs.getInt("cnt") : 0;
            rs.close();
            ps.close();
            return cnt;
        } catch (Exception e) {
            fail("DB query fallita in countAllTagChangeRequests: " + e.getMessage());
            return -1;
        }
    }

    @Test
    void addRequestForNewTag_persistsRequestAndSetsId() {
        // 1) Creo una richiesta per NUOVO label (ADD implicito nel factory)
        TagChangeRequest req = TagChangeRequest.forNewLabel(
                testDocument,
                "fantasy"
        );

        // 2) Chiamo il metodo sotto test
        tagChangeRequestDAO.addRequestForNewTag(req);

        // 3) L'id deve essere stato valorizzato dal RETURNING id
        int generatedId = req.getId();
        assertTrue(generatedId > 0, "Dopo l'inserimento l'id della request deve essere > 0");

        // 4) Recupero dal DB e controllo i campi
        TagChangeRequest fromDb = tagChangeRequestDAO.getById(generatedId);
        assertNotNull(fromDb, "La request appena inserita deve essere recuperabile con getById");

        assertEquals(RequestStatus.PENDING, fromDb.getStatus(), "La request deve essere PENDING");
        assertEquals(TagChangeOperation.ADD, fromDb.getOperation(), "Operation deve essere ADD per un nuovo tag");
        assertEquals("fantasy", fromDb.getProposedLabel(), "proposedLabel deve contenere il nuovo label");
        assertNull(fromDb.getExistingTagLabel(), "existingTagLabel deve essere null per una richiesta di nuovo tag");
        assertNotNull(fromDb.getDocument(), "Il documento associato non deve essere null");
        assertEquals(testDocument.getId(), fromDb.getDocument().getId(),
                "Il document_id deve essere quello del testDocument");
    }

    @Test
    void updateStatus_updatesStatusDateAndModerator() {
        // creo prima il tag per rispettare la FK su existing_tag_label
        tagDAO.addTag(new Tag("rock", "descrizione"));

        // creo una richiesta PENDING per existing tag "rock"
        TagChangeRequest req = TagChangeRequest.forExistingTag(
                testDocument,
                "rock",
                TagChangeOperation.ADD
        );
        tagChangeRequestDAO.addRequestForExistingTag(req);
        int id = req.getId();
        assertTrue(id > 0, "Id request non valorizzato correttamente");

        // ora aggiorno lo status ad APPROVED, impostando il moderatore
        tagChangeRequestDAO.updateStatus(id, moderatorUser.getId(), RequestStatus.APPROVED);

        // ricarico dal DB
        TagChangeRequest fromDb = tagChangeRequestDAO.getById(id);
        assertNotNull(fromDb, "La request deve esistere dopo updateStatus");

        // 1) status aggiornato
        assertEquals(RequestStatus.APPROVED, fromDb.getStatus(), "Lo status deve essere aggiornato ad APPROVED");

        // 2) date_result impostata
        assertNotNull(fromDb.getDateResult(), "date_result deve essere impostata da updateStatus");

        // 3) moderatore impostato e collegato
        assertNotNull(fromDb.getModerator(), "Il moderatore deve essere impostato");
        assertEquals(moderatorUser.getId(), fromDb.getModerator().getId(),
                "Il moderatore associato deve essere quello passato a updateStatus");
    }

    @Test
    void updateStatus_withNonExistingId_doesNotChangeAnything() {
        int before = countAllTagChangeRequests();

        // uso un id sicuramente inesistente (molto grande)
        int nonExistingId = 99999999;

        // la chiamata non deve far esplodere il test
        tagChangeRequestDAO.updateStatus(nonExistingId, moderatorUser.getId(), RequestStatus.APPROVED);

        int after = countAllTagChangeRequests();
        assertEquals(before, after,
                "updateStatus con id inesistente non deve inserire o modificare altre richieste");
    }

    @Test
    void getById_existingId_returnsMappedRequest() {
        // creo il tag per soddisfare la FK su existing_tag_label
        tagDAO.addTag(new Tag("rock", "descrizione"));

        // creo una richiesta PENDING per il tag esistente "rock"
        TagChangeRequest req = TagChangeRequest.forExistingTag(
                testDocument,
                "rock",
                TagChangeOperation.ADD
        );
        tagChangeRequestDAO.addRequestForExistingTag(req);
        int id = req.getId();
        assertTrue(id > 0, "Id della request non valorizzato correttamente");

        // chiamata al metodo sotto test
        TagChangeRequest fromDb = tagChangeRequestDAO.getById(id);
        assertNotNull(fromDb, "getById deve restituire la request appena inserita");

        // controlli sui campi principali
        assertEquals(id, fromDb.getId(), "L'id letto deve coincidere con quello inserito");
        assertEquals(RequestStatus.PENDING, fromDb.getStatus(), "La request deve essere PENDING");
        assertEquals(TagChangeOperation.ADD, fromDb.getOperation(), "Operation deve essere ADD");
        assertEquals("rock", fromDb.getExistingTagLabel(), "existingTagLabel deve essere 'rock'");
        assertNull(fromDb.getProposedLabel(), "proposedLabel deve essere null per una existing-tag request");
        assertNotNull(fromDb.getDocument(), "Il documento associato non deve essere null");
        assertEquals(testDocument.getId(), fromDb.getDocument().getId(),
                "document_id deve essere quello del testDocument");
        // opzionale: prima di updateStatus, moderator e dateResult dovrebbero essere null
        assertNull(fromDb.getModerator(), "Prima dell'updateStatus il moderatore deve essere null");
        assertNull(fromDb.getDateResult(), "Prima dell'updateStatus date_result deve essere null");
    }

    @Test
    void getPending_returnsOnlyPendingRequests() {
        // nessuna request all'inizio
        assertEquals(0, countAllTagChangeRequests(), "All'inizio la tabella deve essere vuota");

        // 1) creo tre richieste per NUOVI label
        TagChangeRequest r1 = TagChangeRequest.forNewLabel(testDocument, "tag1");
        TagChangeRequest r2 = TagChangeRequest.forNewLabel(testDocument, "tag2");
        TagChangeRequest r3 = TagChangeRequest.forNewLabel(testDocument, "tag3");

        tagChangeRequestDAO.addRequestForNewTag(r1);
        tagChangeRequestDAO.addRequestForNewTag(r2);
        tagChangeRequestDAO.addRequestForNewTag(r3);

        // 2) aggiorno lo stato di r3 ad APPROVED, lasciando r1 e r2 PENDING
        tagChangeRequestDAO.updateStatus(r3.getId(), moderatorUser.getId(), RequestStatus.APPROVED);

        // 3) chiamo getPending
        List<TagChangeRequest> pending = tagChangeRequestDAO.getPending();
        assertNotNull(pending, "getPending non deve restituire null");

        // devono esserci solo r1 e r2
        assertEquals(2, pending.size(), "Devono esserci solo due richieste PENDING");
        assertTrue(pending.stream().allMatch(r -> r.getStatus() == RequestStatus.PENDING),
                "Tutte le richieste restituite devono essere PENDING");

        // contiene r1 e r2...
        assertTrue(pending.stream().anyMatch(r -> r.getId() == r1.getId()),
                "getPending deve contenere r1");
        assertTrue(pending.stream().anyMatch(r -> r.getId() == r2.getId()),
                "getPending deve contenere r2");

        // ...e NON contiene r3
        assertFalse(pending.stream().anyMatch(r -> r.getId() == r3.getId()),
                "getPending non deve contenere r3 che è APPROVED");
    }

    @Test
    void getRequestByModerator_returnsOnlyRequestsOfGivenModerator() {
        // creo un secondo moderatore
        String mod2Email = "tagreq-moderator2+" + System.currentTimeMillis() + "@example.com";
        userDAO.addUser("Moderator2", "TagReq", mod2Email, "pwd", true, false);
        User otherModerator = userDAO.getUserByEmail(mod2Email);
        assertNotNull(otherModerator, "Impossibile creare il secondo moderatore di test");

        // creo 3 richieste per new label (così evitiamo la FK sui tag)
        TagChangeRequest r1 = TagChangeRequest.forNewLabel(testDocument, "tag1");
        TagChangeRequest r2 = TagChangeRequest.forNewLabel(testDocument, "tag2");
        TagChangeRequest r3 = TagChangeRequest.forNewLabel(testDocument, "tag3");

        tagChangeRequestDAO.addRequestForNewTag(r1);
        tagChangeRequestDAO.addRequestForNewTag(r2);
        tagChangeRequestDAO.addRequestForNewTag(r3);

        // assegno r1 e r2 al moderatore principale
        tagChangeRequestDAO.updateStatus(r1.getId(), moderatorUser.getId(), RequestStatus.APPROVED);
        tagChangeRequestDAO.updateStatus(r2.getId(), moderatorUser.getId(), RequestStatus.REJECTED);

        // assegno r3 all'altro moderatore
        tagChangeRequestDAO.updateStatus(r3.getId(), otherModerator.getId(), RequestStatus.APPROVED);

        // --- test: richieste del moderatore principale ---
        List<TagChangeRequest> listForMain = tagChangeRequestDAO.getRequestByModerator(moderatorUser.getId());
        assertNotNull(listForMain);
        // devono esserci almeno r1 e r2
        assertTrue(listForMain.stream().anyMatch(r -> r.getId() == r1.getId()),
                "getRequestByModerator deve contenere r1 per il moderatore principale");
        assertTrue(listForMain.stream().anyMatch(r -> r.getId() == r2.getId()),
                "getRequestByModerator deve contenere r2 per il moderatore principale");
        // non deve contenere r3
        assertFalse(listForMain.stream().anyMatch(r -> r.getId() == r3.getId()),
                "getRequestByModerator NON deve contenere r3, che è di un altro moderatore");

        // --- test: richieste del secondo moderatore ---
        List<TagChangeRequest> listForOther = tagChangeRequestDAO.getRequestByModerator(otherModerator.getId());
        assertNotNull(listForOther);
        assertTrue(listForOther.stream().anyMatch(r -> r.getId() == r3.getId()),
                "getRequestByModerator per il secondo moderatore deve contenere r3");
        assertFalse(listForOther.stream().anyMatch(r -> r.getId() == r1.getId() || r.getId() == r2.getId()),
                "getRequestByModerator per il secondo moderatore NON deve contenere r1 o r2");
    }

    @Test
    void getByAuthor_returnsOnlyRequestsOfDocumentsByThatAuthor() {
        // creo un secondo autore con un suo documento
        String author2Email = "tagreq-author2+" + System.currentTimeMillis() + "@example.com";
        userDAO.addUser("Author2", "TagReq", author2Email, "pwd", false, false);
        User otherAuthor = userDAO.getUserByEmail(author2Email);
        assertNotNull(otherAuthor, "Impossibile creare il secondo autore di test");

        // documento per il secondo autore
        documentDAO.addDocument(
                otherAuthor,
                "DocAuthor2",
                "descrizione",
                "1950",
                DocumentFormat.TXT,
                "document/" + otherAuthor.getId() + "/",
                "fileA2",
                List.of()
        );
        List<Document> docsAuthor2 = documentDAO.getDocumentsByAuthor(otherAuthor.getId());
        assertFalse(docsAuthor2.isEmpty(), "Non è stato creato il documento del secondo autore");
        Document otherDoc = docsAuthor2.get(0);

        // --- richieste per autore principale (authorUser) ---
        TagChangeRequest r1 = TagChangeRequest.forNewLabel(testDocument, "tagA1");
        TagChangeRequest r2 = TagChangeRequest.forNewLabel(testDocument, "tagA2");
        tagChangeRequestDAO.addRequestForNewTag(r1);
        tagChangeRequestDAO.addRequestForNewTag(r2);

        // --- richiesta per secondo autore ---
        TagChangeRequest r3 = TagChangeRequest.forNewLabel(otherDoc, "tagB1");
        tagChangeRequestDAO.addRequestForNewTag(r3);

        // --- test: richieste di authorUser ---
        List<TagChangeRequest> forAuthor1 = tagChangeRequestDAO.getByAuthor(authorUser.getId());
        assertNotNull(forAuthor1);
        assertTrue(forAuthor1.stream().allMatch(
                        r -> r.getDocument() != null
                                && r.getDocument().getAuthor() != null
                                && r.getDocument().getAuthor().getId() == authorUser.getId()),
                "Tutte le richieste restituite devono essere legate a documenti dell'authorUser");
        assertTrue(forAuthor1.stream().anyMatch(r -> r.getId() == r1.getId()),
                "getByAuthor deve contenere r1 per authorUser");
        assertTrue(forAuthor1.stream().anyMatch(r -> r.getId() == r2.getId()),
                "getByAuthor deve contenere r2 per authorUser");
        assertFalse(forAuthor1.stream().anyMatch(r -> r.getId() == r3.getId()),
                "getByAuthor NON deve contenere r3 (documento di un altro autore)");

        // --- test: richieste del secondo autore ---
        List<TagChangeRequest> forAuthor2 = tagChangeRequestDAO.getByAuthor(otherAuthor.getId());
        assertNotNull(forAuthor2);
        assertTrue(forAuthor2.stream().allMatch(
                        r -> r.getDocument() != null
                                && r.getDocument().getAuthor() != null
                                && r.getDocument().getAuthor().getId() == otherAuthor.getId()),
                "Tutte le richieste restituite devono essere legate a documenti del secondo autore");
        assertTrue(forAuthor2.stream().anyMatch(r -> r.getId() == r3.getId()),
                "getByAuthor per il secondo autore deve contenere r3");
        assertFalse(forAuthor2.stream().anyMatch(r -> r.getId() == r1.getId() || r.getId() == r2.getId()),
                "getByAuthor per il secondo autore NON deve contenere r1 o r2");
    }

    @Test
    void getRequestsByDocument_returnsOnlyRequestsForThatDocument() {
        // creo un secondo documento per lo stesso autore
        documentDAO.addDocument(
                authorUser,
                "SecondDoc",
                "descrizione",
                "1950",
                DocumentFormat.TXT,
                "document/" + authorUser.getId() + "/",
                "file-second",
                List.of()
        );
        List<Document> docs = documentDAO.getDocumentsByAuthor(authorUser.getId());
        assertTrue(docs.size() >= 2, "Servono almeno due documenti per questo test");

        // assumiamo che testDocument sia uno, prendiamo l'altro come doc2
        Document doc1 = testDocument;
        Document doc2 = docs.stream()
                .filter(d -> d.getId() != doc1.getId())
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Non trovato un secondo documento"));

        // creo richieste su doc1
        TagChangeRequest r1 = TagChangeRequest.forNewLabel(doc1, "tag-doc1-1");
        TagChangeRequest r2 = TagChangeRequest.forNewLabel(doc1, "tag-doc1-2");
        tagChangeRequestDAO.addRequestForNewTag(r1);
        tagChangeRequestDAO.addRequestForNewTag(r2);

        // e una richiesta su doc2
        TagChangeRequest r3 = TagChangeRequest.forNewLabel(doc2, "tag-doc2-1");
        tagChangeRequestDAO.addRequestForNewTag(r3);

        // --- test: richieste di doc1 ---
        List<TagChangeRequest> forDoc1 = tagChangeRequestDAO.getRequestsByDocument(doc1.getId());
        assertNotNull(forDoc1);
        assertTrue(forDoc1.stream().allMatch(r -> r.getDocument() != null && r.getDocument().getId() == doc1.getId()),
                "Tutte le richieste restituite devono essere legate a doc1");
        assertTrue(forDoc1.stream().anyMatch(r -> r.getId() == r1.getId()),
                "Deve contenere r1");
        assertTrue(forDoc1.stream().anyMatch(r -> r.getId() == r2.getId()),
                "Deve contenere r2");
        assertFalse(forDoc1.stream().anyMatch(r -> r.getId() == r3.getId()),
                "NON deve contenere r3 (doc2)");

        List<TagChangeRequest> forDoc2 = tagChangeRequestDAO.getRequestsByDocument(doc2.getId());
        assertNotNull(forDoc2);
        assertTrue(forDoc2.stream().allMatch(r -> r.getDocument() != null && r.getDocument().getId() == doc2.getId()),
                "Tutte le richieste restituite devono essere legate a doc2");
        assertTrue(forDoc2.stream().anyMatch(r -> r.getId() == r3.getId()),
                "Per doc2 deve contenere r3");
        assertFalse(forDoc2.stream().anyMatch(r -> r.getId() == r1.getId() || r.getId() == r2.getId()),
                "Per doc2 NON deve contenere r1 o r2");
    }

    @Test
    void existsPendingDuplicate_returnsTrueForPendingExistingTagRequest() {
        // creo il tag per la FK
        tagDAO.addTag(new Tag("rock", "descrizione"));

        // creo una richiesta PENDING per existing tag "rock"
        TagChangeRequest req = TagChangeRequest.forExistingTag(
                testDocument,
                "rock",
                TagChangeOperation.ADD
        );
        tagChangeRequestDAO.addRequestForExistingTag(req);

        boolean dup = tagChangeRequestDAO.existsPendingDuplicate(
                testDocument.getId(),
                TagChangeOperation.ADD,
                "rock",
                null
        );

        assertTrue(dup, "Deve restituire true se esiste una richiesta PENDING uguale per existing tag");
    }

    @Test
    void existsPendingDuplicate_returnsFalseWhenStatusIsNotPending() {
        tagDAO.addTag(new Tag("rock", "descrizione"));

        TagChangeRequest req = TagChangeRequest.forExistingTag(
                testDocument,
                "rock",
                TagChangeOperation.ADD
        );
        tagChangeRequestDAO.addRequestForExistingTag(req);

        // la porto ad APPROVED
        tagChangeRequestDAO.updateStatus(req.getId(), moderatorUser.getId(), RequestStatus.APPROVED);

        boolean dup = tagChangeRequestDAO.existsPendingDuplicate(
                testDocument.getId(),
                TagChangeOperation.ADD,
                "rock",
                null
        );

        assertFalse(dup, "Se la richiesta non è più PENDING, non deve essere considerata duplicato");
    }
}