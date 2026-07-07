package com.enlaza.cliente;

import com.enlaza.cliente.dto.ClienteRequest;
import com.enlaza.cliente.dto.ClienteResponse;
import com.enlaza.common.exception.ResourceNotFoundException;
import com.enlaza.empresa.EmpresaAccessService;
import com.enlaza.empresa.EmpresaService;
import com.enlaza.security.EnlazaUserPrincipal;
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
    private final EmpresaAccessService empresaAccessService;

    @Transactional
    public ClienteResponse crear(EnlazaUserPrincipal principal, UUID empresaId, ClienteRequest request) {
        empresaAccessService.verificarAcceso(principal, empresaId);
        empresaService.buscarPorId(empresaId);

        Cliente cliente = Cliente.builder()
                .empresaId(empresaId)
                .nombre(request.nombre())
                .nif(request.nif())
                .direccion(request.direccion())
                .email(request.email())
                .build();

        return ClienteResponse.of(clienteRepository.save(cliente));
    }

    public List<ClienteResponse> listar(EnlazaUserPrincipal principal, UUID empresaId) {
        empresaAccessService.verificarAcceso(principal, empresaId);
        return clienteRepository.findByEmpresaId(empresaId).stream()
                .map(ClienteResponse::of)
                .toList();
    }

    public ClienteResponse obtener(EnlazaUserPrincipal principal, UUID empresaId, UUID clienteId) {
        empresaAccessService.verificarAcceso(principal, empresaId);
        return ClienteResponse.of(buscarPorIdYEmpresa(clienteId, empresaId));
    }

    @Transactional
    public ClienteResponse actualizar(EnlazaUserPrincipal principal, UUID empresaId, UUID clienteId, ClienteRequest request) {
        empresaAccessService.verificarAcceso(principal, empresaId);
        Cliente cliente = buscarPorIdYEmpresa(clienteId, empresaId);

        cliente.setNombre(request.nombre());
        cliente.setNif(request.nif());
        cliente.setDireccion(request.direccion());
        cliente.setEmail(request.email());

        return ClienteResponse.of(cliente);
    }

    @Transactional
    public void eliminar(EnlazaUserPrincipal principal, UUID empresaId, UUID clienteId) {
        empresaAccessService.verificarAcceso(principal, empresaId);
        clienteRepository.delete(buscarPorIdYEmpresa(clienteId, empresaId));
    }

    private Cliente buscarPorIdYEmpresa(UUID clienteId, UUID empresaId) {
        return clienteRepository.findByIdAndEmpresaId(clienteId, empresaId)
                .orElseThrow(() -> new ResourceNotFoundException("Cliente no encontrado: " + clienteId));
    }
}
