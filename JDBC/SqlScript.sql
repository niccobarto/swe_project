CREATE TABLE "user"
(
    id             SERIAL PRIMARY KEY,
    name           VARCHAR(100)        NOT NULL,
    surname        VARCHAR(100)        NOT NULL,
    email          VARCHAR(255) UNIQUE NOT NULL,
    password       VARCHAR(255)        NOT NULL,
    is_moderator   BOOLEAN DEFAULT FALSE,
    is_admin       BOOLEAN DEFAULT FALSE,
    next_file_name integer DEFAULT 1 NOT NULL
);

CREATE TABLE collection
(
    id              SERIAL PRIMARY KEY,
    collection_name VARCHAR(255) NOT NULL,
    description     TEXT,
    user_id         INTEGER      NOT NULL,

    CONSTRAINT fk_collection_user
        FOREIGN KEY (user_id)
            REFERENCES "user" (id)
            ON DELETE CASCADE
);

CREATE TABLE document
(
    id            SERIAL PRIMARY KEY,
    title         VARCHAR(255) NOT NULL,
    description   TEXT,
    status        VARCHAR(50)  NOT NULL,
    file_format   VARCHAR(20)  NOT NULL,
    file_path     VARCHAR(500) NOT NULL,
    file_name     VARCHAR(255) NOT NULL,
    author_id     INTEGER      NOT NULL,
    creation_date DATE         NOT NULL,
    period        varchar(10)  NOT NULL,

    CONSTRAINT fk_document_author
        FOREIGN KEY (author_id)
            REFERENCES "user" (id)
            ON DELETE CASCADE
);

CREATE TABLE favourite_collection
(
    user_id       INTEGER NOT NULL,
    collection_id INTEGER NOT NULL,

    PRIMARY KEY (user_id, collection_id),

    CONSTRAINT fk_favcoll_user
        FOREIGN KEY (user_id)
            REFERENCES "user" (id)
            ON DELETE CASCADE,

    CONSTRAINT fk_favcoll_collection
        FOREIGN KEY (collection_id)
            REFERENCES collection (id)
            ON DELETE CASCADE
);

CREATE TABLE favourite_document
(
    user_id     INTEGER NOT NULL,
    document_id INTEGER NOT NULL,

    PRIMARY KEY (user_id, document_id),

    CONSTRAINT fk_favdoc_user
        FOREIGN KEY (user_id)
            REFERENCES "user" (id)
            ON DELETE CASCADE,

    CONSTRAINT fk_favdoc_document
        FOREIGN KEY (document_id)
            REFERENCES document (id)
            ON DELETE CASCADE
);
CREATE TABLE document_collection
(
    collection_id INTEGER NOT NULL,
    document_id   INTEGER NOT NULL,

    PRIMARY KEY (collection_id, document_id),

    CONSTRAINT fk_doccoll_collection
        FOREIGN KEY (collection_id)
            REFERENCES collection (id)
            ON DELETE CASCADE,

    CONSTRAINT fk_doccoll_document
        FOREIGN KEY (document_id)
            REFERENCES document (id)
            ON DELETE CASCADE
);
CREATE TABLE tag
(
    tag_label   VARCHAR(50) PRIMARY KEY,
    description TEXT
);

CREATE TABLE document_tags
(
    document_id INTEGER     NOT NULL,
    tag_label   VARCHAR(50) NOT NULL,

    PRIMARY KEY (document_id, tag_label),

    CONSTRAINT fk_doctag_document
        FOREIGN KEY (document_id)
            REFERENCES document (id)
            ON DELETE CASCADE,

    CONSTRAINT fk_doctag_tag
        FOREIGN KEY (tag_label)
            REFERENCES tag (tag_label)
            ON DELETE CASCADE
);
CREATE TABLE document_relation
(
    source_id      INTEGER     NOT NULL,
    destination_id INTEGER     NOT NULL,
    relation_type  VARCHAR(50) NOT NULL,
    confirmed     BOOLEAN     DEFAULT FALSE NOT NULL,

    PRIMARY KEY (source_id, destination_id),

    CONSTRAINT fk_docrel_source
        FOREIGN KEY (source_id)
            REFERENCES document (id)
            ON DELETE CASCADE,

    CONSTRAINT fk_docrel_destination
        FOREIGN KEY (destination_id)
            REFERENCES document (id)
            ON DELETE CASCADE
);
CREATE TABLE comment
(
    id          SERIAL PRIMARY KEY,
    user_id     INTEGER NOT NULL,
    document_id INTEGER NOT NULL,
    text        TEXT    NOT NULL,
    date        DATE    NOT NULL,

    CONSTRAINT fk_comment_user
        FOREIGN KEY (user_id)
            REFERENCES "user" (id)
            ON DELETE CASCADE,

    CONSTRAINT fk_comment_document
        FOREIGN KEY (document_id)
            REFERENCES document (id)
            ON DELETE CASCADE
);
CREATE TABLE publish_request
(
    id                SERIAL PRIMARY KEY,
    request_status    VARCHAR(50) NOT NULL,
    denial_motivation TEXT,
    date_request      DATE        NOT NULL,
    date_result       DATE,
    document_id       INTEGER     NOT NULL,
    moderator_id      INTEGER, -- può essere NULL

    -- FK verso documento richiesto per pubblicazione
    CONSTRAINT fk_publish_request_document
        FOREIGN KEY (document_id)
            REFERENCES document (id)
            ON DELETE CASCADE,

    -- FK verso moderatore (può essere NULL)
    CONSTRAINT fk_publish_request_moderator
        FOREIGN KEY (moderator_id)
            REFERENCES "user" (id)
);