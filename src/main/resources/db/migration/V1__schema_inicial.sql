CREATE TABLE empresa (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    nome            VARCHAR(255) NOT NULL,
    cnpj            VARCHAR(14)  NOT NULL,
    email           VARCHAR(255) NOT NULL,
    status          VARCHAR(20)  NOT NULL DEFAULT 'ATIVA',
    phone_number_id VARCHAR(64),
    data_criacao    TIMESTAMPTZ  NOT NULL DEFAULT now(),

    CONSTRAINT uk_empresa_cnpj UNIQUE (cnpj),
    CONSTRAINT ck_empresa_status CHECK (status IN ('ATIVA', 'INATIVA'))
);

CREATE UNIQUE INDEX uk_empresa_phone_number_id
    ON empresa (phone_number_id)
    WHERE phone_number_id IS NOT NULL;

CREATE TABLE usuario (
    id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    nome              VARCHAR(255) NOT NULL,
    email             VARCHAR(255) NOT NULL,
    senha_hash        VARCHAR(255) NOT NULL,
    last_empresa_id   UUID,
    deve_trocar_senha BOOLEAN      NOT NULL DEFAULT false,
    data_criacao      TIMESTAMPTZ  NOT NULL DEFAULT now(),

    CONSTRAINT uk_usuario_email UNIQUE (email),
    CONSTRAINT fk_usuario_last_empresa
        FOREIGN KEY (last_empresa_id) REFERENCES empresa (id)
);

CREATE INDEX idx_usuario_last_empresa_id ON usuario (last_empresa_id);

CREATE TABLE administrador_plataforma (
    id         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    nome       VARCHAR(255) NOT NULL,
    email      VARCHAR(255) NOT NULL,
    senha_hash VARCHAR(255) NOT NULL,

    CONSTRAINT uk_admin_plataforma_email UNIQUE (email)
);

CREATE TABLE usuario_empresa (
    usuario_id   UUID        NOT NULL,
    empresa_id   UUID        NOT NULL,
    perfil       VARCHAR(20) NOT NULL,
    status       VARCHAR(20) NOT NULL DEFAULT 'ATIVO',
    data_vinculo TIMESTAMPTZ NOT NULL DEFAULT now(),

    PRIMARY KEY (usuario_id, empresa_id),

    CONSTRAINT fk_usuario_empresa_usuario
        FOREIGN KEY (usuario_id) REFERENCES usuario (id),
    CONSTRAINT fk_usuario_empresa_empresa
        FOREIGN KEY (empresa_id) REFERENCES empresa (id),
    CONSTRAINT ck_usuario_empresa_perfil
        CHECK (perfil IN ('ADMINISTRADOR', 'ATENDENTE')),
    CONSTRAINT ck_usuario_empresa_status
        CHECK (status IN ('ATIVO', 'INATIVO'))
);

CREATE INDEX idx_usuario_empresa_empresa_id ON usuario_empresa (empresa_id);

CREATE TABLE contato (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    empresa_id      UUID         NOT NULL,
    nome            VARCHAR(255) NOT NULL,
    numero_whatsapp VARCHAR(20)  NOT NULL,
    email           VARCHAR(255),
    observacoes     TEXT,
    status          VARCHAR(20)  NOT NULL DEFAULT 'ATIVO',
    data_criacao    TIMESTAMPTZ  NOT NULL DEFAULT now(),

    CONSTRAINT fk_contato_empresa
        FOREIGN KEY (empresa_id) REFERENCES empresa (id),
    CONSTRAINT uk_contato_empresa_numero
        UNIQUE (empresa_id, numero_whatsapp),
    CONSTRAINT ck_contato_status CHECK (status IN ('ATIVO', 'INATIVO'))
);

CREATE TABLE conversa (
    id                       UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    empresa_id               UUID        NOT NULL,
    contato_id               UUID        NOT NULL,
    status                   VARCHAR(20) NOT NULL DEFAULT 'ABERTA',
    responsavel_id           UUID,
    previous_conversation_id UUID,
    ultima_interacao         TIMESTAMPTZ NOT NULL DEFAULT now(),
    data_criacao             TIMESTAMPTZ NOT NULL DEFAULT now(),
    data_encerramento        TIMESTAMPTZ,

    CONSTRAINT fk_conversa_empresa
        FOREIGN KEY (empresa_id) REFERENCES empresa (id),
    CONSTRAINT fk_conversa_contato
        FOREIGN KEY (contato_id) REFERENCES contato (id),
    CONSTRAINT fk_conversa_responsavel
        FOREIGN KEY (responsavel_id) REFERENCES usuario (id),
    CONSTRAINT fk_conversa_anterior
        FOREIGN KEY (previous_conversation_id) REFERENCES conversa (id),
    CONSTRAINT ck_conversa_status CHECK (status IN ('ABERTA', 'ENCERRADA'))
);

CREATE INDEX idx_conversa_empresa_contato_status
    ON conversa (empresa_id, contato_id, status);

CREATE TABLE mensagem (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    empresa_id          UUID        NOT NULL,
    conversa_id         UUID        NOT NULL,
    tipo                VARCHAR(20) NOT NULL,
    conteudo            TEXT        NOT NULL,
    sentido             VARCHAR(10) NOT NULL,
    whatsapp_message_id VARCHAR(128),
    erro_envio          TEXT,
    data_hora           TIMESTAMPTZ NOT NULL DEFAULT now(),

    CONSTRAINT fk_mensagem_empresa
        FOREIGN KEY (empresa_id) REFERENCES empresa (id),
    CONSTRAINT fk_mensagem_conversa
        FOREIGN KEY (conversa_id) REFERENCES conversa (id),
    CONSTRAINT ck_mensagem_tipo
        CHECK (tipo IN ('TEXTO', 'IMAGEM', 'DOCUMENTO')),
    CONSTRAINT ck_mensagem_sentido
        CHECK (sentido IN ('ENTRADA', 'SAIDA'))
);

CREATE INDEX idx_mensagem_empresa_conversa_data
    ON mensagem (empresa_id, conversa_id, data_hora);

CREATE UNIQUE INDEX uk_mensagem_empresa_whatsapp_id
    ON mensagem (empresa_id, whatsapp_message_id)
    WHERE whatsapp_message_id IS NOT NULL;

ALTER TABLE usuario_empresa ENABLE ROW LEVEL SECURITY;
ALTER TABLE usuario_empresa FORCE ROW LEVEL SECURITY;

CREATE POLICY tenant_isolation ON usuario_empresa
    USING (empresa_id = current_setting('app.tenant_id', true)::uuid)
    WITH CHECK (empresa_id = current_setting('app.tenant_id', true)::uuid);


ALTER TABLE contato ENABLE ROW LEVEL SECURITY;
ALTER TABLE contato FORCE ROW LEVEL SECURITY;

CREATE POLICY tenant_isolation ON contato
    USING (empresa_id = current_setting('app.tenant_id', true)::uuid)
    WITH CHECK (empresa_id = current_setting('app.tenant_id', true)::uuid);


ALTER TABLE conversa ENABLE ROW LEVEL SECURITY;
ALTER TABLE conversa FORCE ROW LEVEL SECURITY;

CREATE POLICY tenant_isolation ON conversa
    USING (empresa_id = current_setting('app.tenant_id', true)::uuid)
    WITH CHECK (empresa_id = current_setting('app.tenant_id', true)::uuid);


ALTER TABLE mensagem ENABLE ROW LEVEL SECURITY;
ALTER TABLE mensagem FORCE ROW LEVEL SECURITY;

CREATE POLICY tenant_isolation ON mensagem
    USING (empresa_id = current_setting('app.tenant_id', true)::uuid)
    WITH CHECK (empresa_id = current_setting('app.tenant_id', true)::uuid);