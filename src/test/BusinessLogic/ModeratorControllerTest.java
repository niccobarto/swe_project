package BusinessLogic;

import DomainModel.*;
import ORM.*;
import org.junit.jupiter.api.*;
import DomainModel.Tag;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.*;

class ModeratorControllerTest {

    private Connection conn;
    private UserDAO userDAO;
    private DocumentDAO documentDAO;
    private PublishRequestDAO publishRequestDAO;
    private TagDAO tagDAO;
    private TagChangeRequestDAO tagChangeRequestDAO;
    private User adminUser;
    private User normalUser;
    private ModeratorController moderatorController;
    private UserController userController;

    @BeforeEach
    void setUp() {
        try {
            // ATTIVA la modalità TEST e forza la ricreazione dell'istanza singleton
            DBConnection.setEnableTesting(true);
            DBConnection.resetInstance();

            conn = DBConnection.getInstance().getConnection();
            conn.setAutoCommit(false);

            userDAO = new UserDAO();
            documentDAO = new DocumentDAO();
            publishRequestDAO = new PublishRequestDAO();

            // Crea utenti (direttamente via DAO)
            String adminEmail = "mod+" + System.currentTimeMillis() + "@example.com";
            userDAO.addUser("Mod", "User", adminEmail, "pwd", true, true); // moderator AND admin (come richiede il controller attuale)
            adminUser = userDAO.getUserByEmail(adminEmail);
            assertNotNull(adminUser);

            String userEmail = "user+" + System.currentTimeMillis() + "@example.com";
            userDAO.addUser("Normal", "User", userEmail, "pwd", false, false);
            normalUser = userDAO.getUserByEmail(userEmail);
            assertNotNull(normalUser);

            moderatorController = new ModeratorController(adminUser);
        } catch (Exception e) {
            fail("setUp failed: " + e.getMessage());
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
            fail("tearDown failed: " + e.getMessage());
        }
    }

    @Test
    void updateDocumentStatus_positive_and_negative_and_auth() {
        // POSITIVO: crea doc + richiesta pendente, approva tramite moderatorController
        documentDAO.addDocument(normalUser, "P", "d", "2000", DocumentFormat.PDF, "fp", "fn", List.of("t"));
        int docId = documentDAO.getDocumentsByAuthor(normalUser.getId()).get(0).getId();
        publishRequestDAO.addRequest(documentDAO.getDocumentById(docId));

        moderatorController.updateDocumentStatus(docId, RequestStatus.APPROVED);

        // il documento deve essere pubblicato
        Document published = documentDAO.getDocumentById(docId);
        assertNotNull(published);
        assertEquals(DocumentStatus.PUBLISHED, published.getStatus());

        // la richiesta deve risultare APPROVED
        List<PublishRequest> approvedReqs = publishRequestDAO.getRequestsByStatus(RequestStatus.APPROVED);
        assertTrue(approvedReqs.stream().anyMatch(r -> r.getDocument() != null && r.getDocument().getId() == docId));

        // NEGATIVO: documento senza richiesta pendente -> non cambia lo status
        documentDAO.addDocument(normalUser, "NoPending", "d", "2001", DocumentFormat.TXT, "fp", "fn2", List.of("t"));
        int docNoPending = documentDAO.getDocumentsByAuthor(normalUser.getId()).get(0).getId();
        // non aggiungiamo publishRequest
        moderatorController.updateDocumentStatus(docNoPending, RequestStatus.APPROVED);
        Document notChanged = documentDAO.getDocumentById(docNoPending);
        assertNotNull(notChanged);
        assertEquals(DocumentStatus.DRAFT, notChanged.getStatus());

        // AUTH: utente non moderatore prova ad approvare -> non deve cambiare lo stato
        ModeratorController nonMod = new ModeratorController(normalUser);
        // crea doc+request per testare l'accesso negato
        documentDAO.addDocument(normalUser, "AuthTest", "d", "2002", DocumentFormat.PDF, "fp", "fn3", List.of("t"));
        int docAuth = documentDAO.getDocumentsByAuthor(normalUser.getId()).get(0).getId();
        publishRequestDAO.addRequest(documentDAO.getDocumentById(docAuth));
        nonMod.updateDocumentStatus(docAuth, RequestStatus.REJECTED);

        // essendo non moderatore lo status della richiesta rimane PENDING e il documento resta DRAFT
        List<PublishRequest> pending = publishRequestDAO.getRequestsByStatus(RequestStatus.PENDING);
        assertTrue(pending.stream().anyMatch(r -> r.getDocument() != null && r.getDocument().getId() == docAuth));
        assertEquals(DocumentStatus.PENDING, documentDAO.getDocumentById(docAuth).getStatus());
    }

    @Test
    void viewPendingDocumentRequests_positive_and_auth() {
        // setup: crea 2 documenti con richieste pendenti
        documentDAO.addDocument(normalUser, "VP1", "d", "1999", DocumentFormat.PDF, "fp", "f1", List.of("t"));
        int d1 = documentDAO.getDocumentsByAuthor(normalUser.getId()).get(0).getId();
        publishRequestDAO.addRequest(documentDAO.getDocumentById(d1));

        documentDAO.addDocument(normalUser, "VP2", "d", "2000", DocumentFormat.TXT, "fp", "f2", List.of("t"));
        int d2 = documentDAO.getDocumentsByAuthor(normalUser.getId()).get(0).getId();
        publishRequestDAO.addRequest(documentDAO.getDocumentById(d2));

        List<PublishRequest> pendings = moderatorController.viewPendingDocumentRequests();
        assertNotNull(pendings);
        assertTrue(pendings.size() >= 2);
        assertTrue(pendings.stream().anyMatch(r -> r.getDocument() != null && (r.getDocument().getId() == d1 || r.getDocument().getId() == d2)));

        // accesso negato: utente non moderatore ottiene lista vuota
        ModeratorController nonMod = new ModeratorController(normalUser);
        List<PublishRequest> none = nonMod.viewPendingDocumentRequests();
        assertNotNull(none);
        assertTrue(none.isEmpty());
    }

    @Test
    void decideTagRequest_positive_negative_and_auth() {
        TagDAO tagDAO = new TagDAO();
        TagChangeRequestDAO reqDAO = new TagChangeRequestDAO();
        UserController authorController = new UserController(normalUser);

        // ==============================
        // SETUP: documento dell'autore
        // ==============================
        authorController.createDocument("DocTag", "desc", "1900",
                DocumentFormat.PDF, List.of());
        Document doc = documentDAO.getDocumentsByAuthor(normalUser.getId()).get(0);
        int docId = doc.getId();

        // ----------------------------------------------------
        // 1) APPROVE: ADD di TAG ESISTENTE
        // ----------------------------------------------------
        String existingLabel = "ESISTENTE-" + System.currentTimeMillis();
        tagDAO.addTag(new Tag(existingLabel, "desc esistente"));

        // autore chiede l'aggiunta di un tag già esistente
        authorController.requestAddExistingTag(docId, existingLabel);

        // recupero la richiesta appena creata
        TagChangeRequest addExistingReq = reqDAO.getByAuthor(normalUser.getId())
                .stream()
                .filter(r -> r.getDocument() != null
                        && r.getDocument().getId() == docId
                        && r.getOperation() == TagChangeOperation.ADD
                        && existingLabel.equals(r.getExistingTagLabel())
                        && r.getStatus() == RequestStatus.PENDING)
                .findFirst()
                .orElseThrow(() -> new AssertionError("Pending ADD existing-tag request not found"));

        int addExistingReqId = addExistingReq.getId();

        // il moderatore approva
        moderatorController.decideTagRequest(addExistingReqId, RequestStatus.APPROVED);

        // la richiesta dev'essere APPROVED e avere il moderatore settato
        TagChangeRequest addExistingReqAfter = reqDAO.getById(addExistingReqId);
        assertNotNull(addExistingReqAfter);
        assertEquals(RequestStatus.APPROVED, addExistingReqAfter.getStatus());
        assertNotNull(addExistingReqAfter.getModerator());
        assertEquals(adminUser.getId(), addExistingReqAfter.getModerator().getId());

        // il documento deve avere il tag esistente associato
        Document docAfterAddExisting = documentDAO.getDocumentById(docId);
        assertTrue(
                docAfterAddExisting.getTags().stream()
                        .anyMatch(t -> existingLabel.equals(t.getLabel())),
                "Document should contain existing tag after APPROVED request"
        );

        // ----------------------------------------------------
        // 2) APPROVE: ADD di NUOVO TAG (creazione + link)
        // ----------------------------------------------------
        String newLabel = "NUOVO-" + System.currentTimeMillis();

        // autore propone un nuovo tag
        authorController.requestAddNewTag(docId, newLabel);

        TagChangeRequest addNewReq = reqDAO.getByAuthor(normalUser.getId())
                .stream()
                .filter(r -> r.getDocument() != null
                        && r.getDocument().getId() == docId
                        && r.getOperation() == TagChangeOperation.ADD
                        && newLabel.equals(r.getProposedLabel())
                        && r.getStatus() == RequestStatus.PENDING)
                .findFirst()
                .orElseThrow(() -> new AssertionError("Pending ADD new-tag request not found"));

        int addNewReqId = addNewReq.getId();

        // moderatore approva
        moderatorController.decideTagRequest(addNewReqId, RequestStatus.APPROVED);

        // la richiesta deve essere APPROVED
        TagChangeRequest addNewReqAfter = reqDAO.getById(addNewReqId);
        assertNotNull(addNewReqAfter);
        assertEquals(RequestStatus.APPROVED, addNewReqAfter.getStatus());

        // il tag dev'essere stato creato in tabella Tag
        Tag createdTag = tagDAO.findByLabelNormalized(newLabel);
        assertNotNull(createdTag, "New tag should be created in Tag table");
        assertEquals(newLabel, createdTag.getLabel());

        // e linkato al documento
        Document docAfterAddNew = documentDAO.getDocumentById(docId);
        assertTrue(
                docAfterAddNew.getTags().stream()
                        .anyMatch(t -> newLabel.equals(t.getLabel())),
                "Document should contain new tag after APPROVED request"
        );

        // 3) APPROVE: REMOVE di un TAG ESISTENTE
        //    (rimuovo il tag existingLabel, lascio il newLabel)
        authorController.requestRemoveTag(docId, existingLabel);

        TagChangeRequest removeReq = reqDAO.getByAuthor(normalUser.getId())
                .stream()
                .filter(r -> r.getDocument() != null
                        && r.getDocument().getId() == docId
                        && r.getOperation() == TagChangeOperation.REMOVE
                        && existingLabel.equals(r.getExistingTagLabel())
                        && r.getStatus() == RequestStatus.PENDING)
                .findFirst()
                .orElseThrow(() -> new AssertionError("Pending REMOVE request not found"));

        int removeReqId = removeReq.getId();

        // moderatore approva la rimozione
        moderatorController.decideTagRequest(removeReqId, RequestStatus.APPROVED);

        TagChangeRequest removeReqAfter = reqDAO.getById(removeReqId);
        assertNotNull(removeReqAfter);
        assertEquals(RequestStatus.APPROVED, removeReqAfter.getStatus());

        Document docAfterRemove = documentDAO.getDocumentById(docId);
        assertFalse(
                docAfterRemove.getTags().stream()
                        .anyMatch(t -> existingLabel.equals(t.getLabel())),
                "Existing tag should be removed from document after APPROVED REMOVE"
        );
        // il nuovo tag deve ancora esserci
        assertTrue(
                docAfterRemove.getTags().stream()
                        .anyMatch(t -> newLabel.equals(t.getLabel())),
                "New tag should still be present on document"
        );

        // 4) REJECT: richiesta di nuovo tag → niente tag creato
        String rejectedLabel = "REJECT-" + System.currentTimeMillis();
        authorController.requestAddNewTag(docId, rejectedLabel);

        TagChangeRequest rejectReq = reqDAO.getByAuthor(normalUser.getId())
                .stream()
                .filter(r -> r.getDocument() != null
                        && r.getDocument().getId() == docId
                        && r.getOperation() == TagChangeOperation.ADD
                        && rejectedLabel.equals(r.getProposedLabel())
                        && r.getStatus() == RequestStatus.PENDING)
                .findFirst()
                .orElseThrow(() -> new AssertionError("Pending ADD new-tag (to REJECT) not found"));

        int rejectReqId = rejectReq.getId();

        // moderatore rifiuta
        moderatorController.decideTagRequest(rejectReqId, RequestStatus.REJECTED);

        TagChangeRequest rejectReqAfter = reqDAO.getById(rejectReqId);
        assertNotNull(rejectReqAfter);
        assertEquals(RequestStatus.REJECTED, rejectReqAfter.getStatus());

        // il tag NON dev'essere stato creato
        assertNull(
                tagDAO.findByLabelNormalized(rejectedLabel),
                "Rejected new tag should NOT be created in Tag table"
        );

        // e il documento non deve avere quel tag
        Document docAfterReject = documentDAO.getDocumentById(docId);
        assertFalse(
                docAfterReject.getTags().stream()
                        .anyMatch(t -> rejectedLabel.equals(t.getLabel())),
                "Document should NOT contain rejected new tag"
        );

        // 5) AUTH: utente NON moderatore non deve poter decidere
        // Creo un'altra richiesta PENDING
        String labelForAuth = "AUTH-" + System.currentTimeMillis();
        authorController.requestAddNewTag(docId, labelForAuth);
        TagChangeRequest authReq = reqDAO.getByAuthor(normalUser.getId())
                .stream()
                .filter(r -> r.getDocument() != null
                        && r.getDocument().getId() == docId
                        && r.getOperation() == TagChangeOperation.ADD
                        && labelForAuth.equals(r.getProposedLabel())
                        && r.getStatus() == RequestStatus.PENDING)
                .findFirst()
                .orElseThrow(() -> new AssertionError("Pending request for auth test not found"));

        int authReqId = authReq.getId();

        // controller creato con utente NON moderatore
        ModeratorController nonModController = new ModeratorController(normalUser);
        nonModController.decideTagRequest(authReqId, RequestStatus.APPROVED);

        // essendo non moderatore, la richiesta deve rimanere PENDING
        TagChangeRequest authReqAfter = reqDAO.getById(authReqId);
        assertNotNull(authReqAfter);
        assertEquals(
                RequestStatus.PENDING,
                authReqAfter.getStatus(),
                "Non-moderator should not be able to change TagChangeRequest status"
        );
    }


    @Test
    void viewPendingTagRequests_positive_and_auth() {
        TagDAO tagDAO = new TagDAO();
        TagChangeRequestDAO reqDAO = new TagChangeRequestDAO();

        // 1) Creo un documento + tag esistente + richiesta PENDING (ADD esistente)
        documentDAO.addDocument(normalUser, "DocTag1", "d", "2000",
                DocumentFormat.PDF, "fp", "f1", List.of());
        int d1 = documentDAO.getDocumentsByAuthor(normalUser.getId()).get(0).getId();
        Document doc1 = documentDAO.getDocumentById(d1);

        String existingLabel = "EXISTING-" + System.currentTimeMillis();
        DomainModel.Tag existingTag = new DomainModel.Tag(existingLabel, "desc");
        tagDAO.addTag(existingTag);

        TagChangeRequest req1 = TagChangeRequest.forExistingTag(
                doc1,
                existingTag.getLabel(),
                TagChangeOperation.ADD
        );
        reqDAO.addRequestForExistingTag(req1);
        assertTrue(req1.getId() > 0);

        // 2) Secondo documento + richiesta PENDING per NUOVO tag
        documentDAO.addDocument(normalUser, "DocTag2", "d", "2001",
                DocumentFormat.TXT, "fp", "f2", List.of());
        int d2 = documentDAO.getDocumentsByAuthor(normalUser.getId()).get(0).getId();
        Document doc2 = documentDAO.getDocumentById(d2);

        String newLabel = "NEW-" + System.currentTimeMillis();
        TagChangeRequest req2 = TagChangeRequest.forNewLabel(doc2, newLabel);
        reqDAO.addRequestForNewTag(req2);
        assertTrue(req2.getId() > 0);

        // 3) Chiamo il controller del moderatore → deve vedere entrambe le PENDING
        var pending = moderatorController.viewPendingTagRequests();
        assertNotNull(pending);
        assertTrue(pending.size() >= 2);

        assertTrue(
                pending.stream().anyMatch(r -> r.getId() == req1.getId()),
                "Pending list should contain req1"
        );
        assertTrue(
                pending.stream().anyMatch(r -> r.getId() == req2.getId()),
                "Pending list should contain req2"
        );

        // 4) Scenario auth: utente NON moderatore → lista vuota
        ModeratorController nonMod = new ModeratorController(normalUser);
        var none = nonMod.viewPendingTagRequests();
        assertNotNull(none);
        assertTrue(none.isEmpty(), "Non-moderator should not see pending tag requests");
    }

    @Test
    void viewRequestHistory() {
        // 1) Setup: documento + richiesta di pubblicazione tramite UserController
        documentDAO.addDocument(
                normalUser,
                "HistoryDoc",
                "desc",
                "2020",
                DocumentFormat.PDF,
                "fp",
                "fnHistory",
                List.of("tagHistory")
        );

        // Prendo l'id del documento appena creato
        int docId = documentDAO
                .getDocumentsByAuthor(normalUser.getId())
                .stream()
                .filter(d -> "HistoryDoc".equals(d.getTitle()))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Document not found in setup"))
                .getId();

        // L'utente normale chiede la pubblicazione
        UserController userController = new UserController(normalUser);
        userController.askForPublication(docId);

        // Verifico che esista una richiesta PENDING per quel documento
        List<PublishRequest> pendingBefore = publishRequestDAO.getRequestsByStatus(RequestStatus.PENDING);
        assertTrue(
                pendingBefore.stream()
                        .anyMatch(r -> r.getDocument() != null && r.getDocument().getId() == docId),
                "There should be a pending request for the document before moderator decision"
        );

        // 2) Il moderatore approva il documento (usa anche PublishRequestDAO.updateRequestStatus)
        moderatorController.updateDocumentStatus(docId, RequestStatus.APPROVED);

        // 3) Chiamo il metodo da testare: history del moderatore corrente
        List<PublishRequest> history = moderatorController.viewRequestHistory();

        assertNotNull(history);
        assertFalse(history.isEmpty(), "History should not be empty");

        // Ci deve essere una richiesta per quel documento, APPROVED,
        // con il moderatore corretto
        assertTrue(
                history.stream().anyMatch(r ->
                        r.getDocument() != null &&
                                r.getDocument().getId() == docId &&
                                r.getStatus() == RequestStatus.APPROVED &&
                                r.getModerator() != null &&
                                r.getModerator().getId() == adminUser.getId()
                ),
                "History must contain the approved request for this moderator and document"
        );

        // 4) Scenario AUTH: un utente non moderatore non deve vedere nulla
        ModeratorController nonMod = new ModeratorController(normalUser);
        List<PublishRequest> none = nonMod.viewRequestHistory();
        assertNotNull(none);
        assertTrue(none.isEmpty(), "Non-moderator should see an empty history");
    }

    @Test
    void viewTagRequestHistory() {
        TagDAO tagDAO = new TagDAO();
        TagChangeRequestDAO tagChangeRequestDAO = new TagChangeRequestDAO();

        // Controller locali, così non dipendiamo da campi eventualmente null
        UserController localUserController = new UserController(normalUser);
        ModeratorController localModeratorController = new ModeratorController(adminUser);

        // 1) Creo un tag di base nel DB
        Tag tag = new Tag("HistoryTag", "Tag for history test");
        tagDAO.addTag(tag);

        // 2) Creo un documento dell'utente normale
        documentDAO.addDocument(
                normalUser,
                "DocTagHistory",
                "doc for tag history",
                "2024",
                DocumentFormat.PDF,
                "path",
                "fileNameHistory",
                List.of("HistoryTag")
        );

        int docId = documentDAO.getDocumentsByAuthor(normalUser.getId())
                .stream()
                .filter(d -> "DocTagHistory".equals(d.getTitle()))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Document not found in setup"))
                .getId();

        // 3) L'autore crea una richiesta di modifica tag (es: REMOVE di HistoryTag)
        localUserController.requestRemoveTag(docId, "HistoryTag");

        // Verifico che esista una richiesta PENDING per quel documento
        List<TagChangeRequest> pending = tagChangeRequestDAO.getPending();
        assertTrue(
                pending.stream().anyMatch(r ->
                        r.getDocument() != null &&
                                r.getDocument().getId() == docId &&
                                r.getStatus() == RequestStatus.PENDING
                ),
                "Expected at least one pending tag change request for the document"
        );

        TagChangeRequest created = pending.stream()
                .filter(r -> r.getDocument() != null && r.getDocument().getId() == docId)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Pending tag request not found for doc"));
        int requestId = created.getId();

        // 4) Il moderatore decide la richiesta (APPROVED)
        localModeratorController.decideTagRequest(requestId, RequestStatus.APPROVED);

        // 5) Ora la history del moderatore deve contenere questa richiesta
        ArrayList<TagChangeRequest> history = localModeratorController.viewTagRequestHistory();
        assertNotNull(history);
        assertFalse(history.isEmpty(), "Tag request history should not be empty for moderator");

        assertTrue(
                history.stream().anyMatch(r ->
                        r.getId() == requestId &&
                                r.getStatus() == RequestStatus.APPROVED &&
                                r.getModerator() != null &&
                                r.getModerator().getId() == adminUser.getId() &&
                                r.getDocument() != null &&
                                r.getDocument().getId() == docId
                ),
                "History must contain the approved tag request with correct moderator and document"
        );

        // 6) Utente NON moderatore deve vedere history vuota
        ModeratorController nonModeratorController = new ModeratorController(normalUser);
        ArrayList<TagChangeRequest> nonModHistory = nonModeratorController.viewTagRequestHistory();
        assertNotNull(nonModHistory);
        assertTrue(nonModHistory.isEmpty(), "Non-moderator should see an empty tag request history");
    }
}