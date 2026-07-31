CREATE TABLE blocked_extension (
                                   id          BIGSERIAL PRIMARY KEY,
                                   name        VARCHAR(20)  NOT NULL,
                                   type        VARCHAR(10)  NOT NULL,
                                   is_blocked  BOOLEAN      NOT NULL DEFAULT FALSE,
                                   created_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
                                   updated_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
                                   CONSTRAINT uq_blocked_extension_name UNIQUE (name),
                                   CONSTRAINT ck_blocked_extension_type CHECK (type IN ('FIXED', 'CUSTOM')),
                                   CONSTRAINT ck_blocked_extension_name CHECK (name ~ '^[a-z0-9]{1,20}$')
    );

COMMENT ON COLUMN blocked_extension.name IS '소문자 정규화, 점(.) 제외';
COMMENT ON CONSTRAINT uq_blocked_extension_name ON blocked_extension
    IS '고정/커스텀 교차 중복까지 구조적으로 차단';

INSERT INTO blocked_extension (name, type, is_blocked) VALUES
                                                           ('bat','FIXED',false), ('cmd','FIXED',false), ('com','FIXED',false),
                                                           ('cpl','FIXED',false), ('exe','FIXED',false), ('scr','FIXED',false),
                                                           ('js','FIXED',false);