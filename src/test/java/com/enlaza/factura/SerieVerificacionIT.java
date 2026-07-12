package com.enlaza.factura;

import com.enlaza.AbstractIntegrationTest;
import com.enlaza.cliente.dto.ClienteRequest;
import com.enlaza.cliente.dto.ClienteResponse;
import com.enlaza.empresa.dto.EmpresaRequest;
import com.enlaza.empresa.dto.EmpresaResponse;
import com.enlaza.factura.dto.FacturaRequest;
import com.enlaza.factura.dto.FacturaResponse;
import com.enlaza.factura.dto.LineaFacturaRequest;
import com.enlaza.factura.dto.SerieVerificacionResponse;
import com.enlaza.security.dto.LoginRequest;
import com.enlaza.security.dto.LoginResponse;
import com.enlaza.serie.dto.SerieFacturacionRequest;
import com.enlaza.serie.dto.SerieFacturacionResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class SerieVerificacionIT extends AbstractIntegrationTest {

    private static final String ADMIN_EMAIL = "admin@test.enlaza.dev";
    private static final String ADMIN_PASSWORD = "AdminTest123!";

    @Autowired
    private TestRestTemplate restTemplate;
    @Autowired
    private FacturaRepository facturaRepository;
    @Autowired
    private LineaFacturaRepository lineaFacturaRepository;

    private String adminToken;
    private UUID empresaId;
    private UUID serieId;
    private UUID clienteId;

    @BeforeEach
    void setUp() {
        adminToken = login(ADMIN_EMAIL, ADMIN_PASSWORD);

        empresaId = crear("/api/empresas",
                new EmpresaRequest("Empresa Verificacion " + UUID.randomUUID(), "T" + UUID.randomUUID().toString().substring(0, 8), null, null, null),
                EmpresaResponse.class).id();

        serieId = crear("/api/empresas/" + empresaId + "/series",
                new SerieFacturacionRequest("A"), SerieFacturacionResponse.class).id();

        clienteId = crear("/api/empresas/" + empresaId + "/clientes",
                new ClienteRequest("Cliente Verificacion", "C" + UUID.randomUUID().toString().substring(0, 8), null, null),
                ClienteResponse.class).id();
    }

    @Test
    void serieSinFacturas_esIntegra() {
        SerieVerificacionResponse verificacion = verificar();

        assertThat(verificacion.integra()).isTrue();
        assertThat(verificacion.totalFacturas()).isZero();
    }

    @Test
    void serieConCadenaIntacta_esIntegra() {
        crearFactura("A");
        crearFactura("B");
        crearFactura("C");

        SerieVerificacionResponse verificacion = verificar();

        assertThat(verificacion.integra()).isTrue();
        assertThat(verificacion.totalFacturas()).isEqualTo(3);
        assertThat(verificacion.facturaId()).isNull();
    }

    @Test
    void manipularElTotalDeUnaFacturaDirectamenteEnBd_rompeLaVerificacion() {
        FacturaResponse primera = crearFactura("A");
        crearFactura("B");

        Factura factura = facturaRepository.findById(primera.id()).orElseThrow();
        factura.setTotal(factura.getTotal().add(BigDecimal.ONE));
        facturaRepository.save(factura);

        SerieVerificacionResponse verificacion = verificar();

        assertThat(verificacion.integra()).isFalse();
        assertThat(verificacion.facturaId()).isEqualTo(primera.id());
        assertThat(verificacion.numero()).isEqualTo(1);
    }

    /**
     * Manipular una linea sin tocar el total guardado de la factura: si el
     * hash solo cubriera el total (el bug que la revision de la Fase 5 ya
     * corrigio), esta manipulacion pasaria desapercibida.
     */
    @Test
    void manipularUnaLineaSinTocarElTotal_rompeIgualLaVerificacion() {
        FacturaResponse factura = crearFactura("Original");

        LineaFactura linea = lineaFacturaRepository.findByFacturaIdOrderByOrdenAsc(factura.id()).get(0);
        linea.setDescripcion("Manipulada");
        lineaFacturaRepository.save(linea);

        SerieVerificacionResponse verificacion = verificar();

        assertThat(verificacion.integra()).isFalse();
        assertThat(verificacion.facturaId()).isEqualTo(factura.id());
    }

    /**
     * SerieVerificacionService recorre la cadena de UNA serie; este test
     * confirma que lo hace de verdad scoped a esa serie y no mezcla el
     * recorrido con otra serie de la misma empresa que tambien tenga
     * facturas manipuladas.
     */
    @Test
    void manipularUnaSerie_noAfectaLaVerificacionDeOtraSerieDeLaMismaEmpresa() {
        UUID otraSerieId = crear("/api/empresas/" + empresaId + "/series",
                new SerieFacturacionRequest("B"), SerieFacturacionResponse.class).id();

        crearFactura("A");
        FacturaResponse facturaOtraSerie = crearFacturaEnSerie(otraSerieId, "Otra serie");

        Factura factura = facturaRepository.findById(facturaOtraSerie.id()).orElseThrow();
        factura.setTotal(factura.getTotal().add(BigDecimal.ONE));
        facturaRepository.save(factura);

        SerieVerificacionResponse verificacionSerieOriginal = verificar();
        assertThat(verificacionSerieOriginal.integra()).isTrue();

        ResponseEntity<SerieVerificacionResponse> verificacionOtraSerie = restTemplate.exchange(
                "/api/empresas/" + empresaId + "/series/" + otraSerieId + "/facturas/verificacion",
                HttpMethod.GET, authenticated(adminToken), SerieVerificacionResponse.class);
        assertThat(verificacionOtraSerie.getBody().integra()).isFalse();
        assertThat(verificacionOtraSerie.getBody().facturaId()).isEqualTo(facturaOtraSerie.id());
    }

    private SerieVerificacionResponse verificar() {
        ResponseEntity<SerieVerificacionResponse> response = restTemplate.exchange(
                "/api/empresas/" + empresaId + "/series/" + serieId + "/facturas/verificacion",
                HttpMethod.GET, authenticated(adminToken), SerieVerificacionResponse.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        return response.getBody();
    }

    private FacturaResponse crearFactura(String descripcion) {
        return crearFacturaEnSerie(serieId, descripcion);
    }

    private FacturaResponse crearFacturaEnSerie(UUID destinoSerieId, String descripcion) {
        FacturaRequest request = new FacturaRequest(clienteId, List.of(
                new LineaFacturaRequest(descripcion, BigDecimal.ONE, new BigDecimal("10.00"), BigDecimal.ZERO, BigDecimal.ZERO)));
        return crear("/api/empresas/" + empresaId + "/series/" + destinoSerieId + "/facturas", request, FacturaResponse.class);
    }

    private <T> T crear(String url, Object body, Class<T> responseType) {
        ResponseEntity<T> response = restTemplate.exchange(url, HttpMethod.POST, authenticated(adminToken, body), responseType);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        return response.getBody();
    }

    private String login(String email, String password) {
        ResponseEntity<LoginResponse> response = restTemplate.postForEntity(
                "/api/auth/login", new LoginRequest(email, password), LoginResponse.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        return response.getBody().accessToken();
    }

    private HttpEntity<Void> authenticated(String accessToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        return new HttpEntity<>(headers);
    }

    private <T> HttpEntity<T> authenticated(String accessToken, T body) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        return new HttpEntity<>(body, headers);
    }
}
