DROP POLICY tenant_isolation ON usuario_empresa;
CREATE POLICY tenant_isolation ON usuario_empresa
    USING (empresa_id = NULLIF(current_setting('app.tenant_id', true), '')::uuid)
    WITH CHECK (empresa_id = NULLIF(current_setting('app.tenant_id', true), '')::uuid);

DROP POLICY tenant_isolation ON contato;
CREATE POLICY tenant_isolation ON contato
    USING (empresa_id = NULLIF(current_setting('app.tenant_id', true), '')::uuid)
    WITH CHECK (empresa_id = NULLIF(current_setting('app.tenant_id', true), '')::uuid);

DROP POLICY tenant_isolation ON conversa;
CREATE POLICY tenant_isolation ON conversa
    USING (empresa_id = NULLIF(current_setting('app.tenant_id', true), '')::uuid)
    WITH CHECK (empresa_id = NULLIF(current_setting('app.tenant_id', true), '')::uuid);

DROP POLICY tenant_isolation ON mensagem;
CREATE POLICY tenant_isolation ON mensagem
    USING (empresa_id = NULLIF(current_setting('app.tenant_id', true), '')::uuid)
    WITH CHECK (empresa_id = NULLIF(current_setting('app.tenant_id', true), '')::uuid);
