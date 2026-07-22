package com.felipeduan.atendimento.modules.empresas;

import com.felipeduan.atendimento.modules.empresas.dto.AdminInicialResponse;
import com.felipeduan.atendimento.modules.empresas.dto.EmpresaResponse;
import com.felipeduan.atendimento.modules.empresas.dto.EmpresaResumoResponse;
import com.felipeduan.atendimento.modules.usuarios.Usuario;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface EmpresaMapper {

  EmpresaResumoResponse toResumoResponse(Empresa empresa);

  EmpresaResumoResponse toResumoResponse(EmpresaRegistro registro);

  @Mapping(target = "id", source = "empresa.id")
  @Mapping(target = "nome", source = "empresa.nome")
  @Mapping(target = "cnpj", source = "empresa.cnpj")
  @Mapping(target = "email", source = "empresa.email")
  @Mapping(target = "status", source = "empresa.status")
  @Mapping(target = "phoneNumberId", source = "empresa.phoneNumberId")
  @Mapping(target = "dataCriacao", source = "empresa.dataCriacao")
  @Mapping(target = "administradorInicial", source = "usuario")
  EmpresaResponse toResponse(Empresa empresa, Usuario usuario);

  @Mapping(target = "administradorInicial", ignore = true)
  EmpresaResponse toResponse(Empresa empresa);

  @Mapping(target = "usuarioId", source = "id")
  AdminInicialResponse toAdminInicialResponse(Usuario usuario);
}
