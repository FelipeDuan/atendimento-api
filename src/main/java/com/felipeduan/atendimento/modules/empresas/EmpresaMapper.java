package com.felipeduan.atendimento.modules.empresas;

import com.felipeduan.atendimento.modules.empresas.dto.AdminInicialResponse;
import com.felipeduan.atendimento.modules.empresas.dto.EmpresaResponse;
import com.felipeduan.atendimento.modules.usuarios.Usuario;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface EmpresaMapper {

  @Mapping(target = "id", source = "empresa.id")
  @Mapping(target = "nome", source = "empresa.nome")
  @Mapping(target = "cnpj", source = "empresa.cnpj")
  @Mapping(target = "email", source = "empresa.email")
  @Mapping(target = "status", source = "empresa.status")
  @Mapping(target = "dataCriacao", source = "empresa.dataCriacao")
  @Mapping(target = "adminInicial", source = "usuario")
  EmpresaResponse toResponse(Empresa empresa, Usuario usuario);

  @Mapping(target = "usuarioId", source = "id")
  AdminInicialResponse toAdminInicialResponse(Usuario usuario);
}
