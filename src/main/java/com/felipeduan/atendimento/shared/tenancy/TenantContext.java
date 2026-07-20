package com.felipeduan.atendimento.shared.tenancy;

import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

public final class TenantContext {
    
    private static final ThreadLocal<UUID> TENANT_ID = new ThreadLocal<>();

    private TenantContext() {
        throw new IllegalStateException("Classe utilitária não deve ser instanciada");
    }

    public static Optional<UUID> getTenantId() {
        return Optional.ofNullable(TENANT_ID.get());
    }

    public static void setTenantId(UUID tenantId) {
        TENANT_ID.set(tenantId);
    }

    public static void clear() {
        TENANT_ID.remove();
    }

    public static <T> T withTenantId(UUID tenantId, Supplier<T> acao) {
        UUID anterior = TenantContext.getTenantId().orElse(null);
        try {
            setTenantId(tenantId);
            return acao.get();
        } finally {
            if (anterior == null) {
                clear();
            } else {
                setTenantId(anterior);
            }
        }
    }
    
    public static void withTenantId(UUID tenantId, Runnable acao) {
        withTenantId(tenantId, () -> {
            acao.run();
            return null;
        });
    }
}
