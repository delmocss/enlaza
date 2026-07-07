package com.enlaza.cliente;

import com.enlaza.cliente.dto.ClienteRequest;
import com.enlaza.cliente.dto.ClienteResponse;
import com.enlaza.common.exception.BusinessRuleException;
import com.enlaza.common.exception.ResourceNotFoundException;
import com.enlaza.empresa.Empresa;
import com.enlaza.empresa.EmpresaService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ClienteService {

    private final ClienteRepository clienteRepository;
    private final EmpresaService empresaService;

    @Transactional
    public ClienteResponse crear(UUID empresaId, ClienteRequest request) {
        Empresa empresa = empresaService.buscarPorId(empresaId);
        if (!empresa.isActiva()) {
            throw new BusinessRuleException("No se pueden crear clientes en una empresa desactivada");
        }

        Cliente cliente = Cliente.builder()
                .empresaId(empresaId)
                .nombre(request.nombre())
                .nif(request.nif())
                .direccion(request.direccion())
                .email(request.email())
                .build();

        return ClienteResponse.of(clienteRepository.save(cliente));
    }

    public List<ClienteResponse> listar(UUID empresaId) {
        return clienteRepository.findByEmpresaId(empresaId).stream()
                .map(ClienteResponse::of)
                .toList();
    }

    public ClienteResponse obtener(UUID empresaId, UUID clienteId) {
        return ClienteResponse.of(buscarPorIdYEmpresa(clienteId, empresaId));
    }

    @Transactional
    public ClienteResponse actualizar(UUID empresaId, UUID clienteId, ClienteRequest request) {
        Cliente cliente = buscarPorIdYEmpresa(clienteId, empresaId);

        cliente.setNombre(request.nombre());
        cliente.setNif(request.nif());
        cliente.setDireccion(request.direccion());
        cliente.setEmail(request.email());

        return ClienteResponse.of(cliente);
    }

    @Transactional
    public void eliminar(UUID empresaId, UUID clienteId) {
        clienteRepository.delete(buscarPorIdYEmpresa(clienteId, empresaId));
    }

    private Cliente buscarPorIdYEmpresa(UUID clienteId, UUID empresaId) {
        return clienteRepository.findByIdAndEmpresaId(clienteId, empresaId)
                .orElseThrow(() -> new ResourceNotFoundException("Cliente no encontrado: " + clienteId));
    }
}
