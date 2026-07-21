package com.felipeduan.atendimento.shared.security;

public final class Roles {

  public static final String PLATFORM_ADMIN = "PLATFORM_ADMIN";
  public static final String ADMINISTRADOR = "ADMINISTRADOR";
  public static final String ATENDENTE = "ATENDENTE";

  private Roles() {
    throw new IllegalStateException("Classe utilitária não deve ser instanciada");
  }
}
