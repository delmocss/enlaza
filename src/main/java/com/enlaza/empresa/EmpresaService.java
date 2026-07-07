package com.enlaza.empresa;

import com.enlaza.common.exception.BusinessRuleException;
import com.enlaza.common.exception.ResourceNotFoundException;
import com.enlaza.empresa.dto.EmpresaRequest;
import com.enlaza.empresa.dto.EmpresaResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class EmpresaService {

    private final EmpresaRepository empresaRepository;

    @Transactional
    public EmpresaResponse crear(EmpresaRequest request) {
        if (empresaRepository.existsByNif(request.nif())) {
            throw new BusinessRuleException("Ya existe una empresa con NIF " + request.nif());
        }

        Empresa empresa = Empresa.builder()
                .razonSocial(request.razonSocial())
                .nif(request.nif())
                .direccion(request.direccion())
                .email(request.email())
                .telefono(request.telefono())
                .activa(true)
                .build();

        return EmpresaResponse.of(empresaRepository.save(empresa));
    }

    public List<EmpresaResponse> listar() {
        return empresaRepository.findAll().stream()
                .map(EmpresaResponse::of)
                .toList();
    }

    public EmpresaResponse obtener(UUID id) {
        return EmpresaResponse.of(buscarPorId(id));
    }

    @Transactional
    public EmpresaResponse actualizar(UUID id, EmpresaRequest request) {
        Empresa empresa = buscarPorId(id);

        if (!empresa.getNif().equals(request.nif()) && empresaRepository.existsByNif(request.nif())) {
            throw new BusinessRuleException("Ya existe una empresa con NIF " + request.nif());
        }

        empresa.setRazonSocial(request.razonSocial());
        empresa.setNif(request.nif());
        empresa.setDireccion(request.direccion());
        empresa.setEmail(request.email());
        empresa.setTelefono(request.telefono());

        return EmpresaResponse.of(empresa);
    }

    @Transactional
    public EmpresaResponse activar(UUID id) {
        Empresa empresa = buscarPorId(id);
        empresa.setActiva(true);
        return EmpresaResponse.of(empresa);
    }

    @Transactional
    public EmpresaResponse desactivar(UUID id) {
        Empresa empresa = buscarPorId(id);
        empresa.setActiva(false);
        return EmpresaResponse.of(empresa);
    }

    public Empresa buscarPorId(UUID id) {
        return empresaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Empresa no encontrada: " + id));
    }
}
