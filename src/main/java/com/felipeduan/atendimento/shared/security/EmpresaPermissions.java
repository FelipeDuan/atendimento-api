package com.felipeduan.atendimento.shared.security;

public final class EmpresaPermissions {

  public static final String READ = "read";
  public static final String WRITE = "write";

  private EmpresaPermissions() {
    throw new IllegalStateException("Classe utilitária não deve ser instanciada");
  }
}
