package com.enlaza.factura;

import com.enlaza.AbstractIntegrationTest;
import com.enlaza.cliente.dto.ClienteRequest;
import com.enlaza.cliente.dto.ClienteResponse;
import com.enlaza.empresa.dto.EmpresaRequest;
import com.enlaza.empresa.dto.EmpresaResponse;
import com.enlaza.factura.dto.FacturaRequest;
import com.enlaza.factura.dto.FacturaResponse;
import com.enlaza.factura.dto.LineaFacturaRequest;
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
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

class FacturaControllerIT extends AbstractIntegrationTest {

    private static final String ADMIN_EMAIL = "admin@test.enlaza.dev";
    private static final String ADMIN_PASSWORD = "AdminTest123!";

    @Autowired
    private TestRestTemplate restTemplate;

    private String adminToken;
    private UUID empresaId;
    private UUID serieId;
    private UUID clienteId;

    @BeforeEach
    void setUp() {
        adminToken = login(ADMIN_EMAIL, ADMIN_PASSWORD);

        empresaId = crear("/api/empresas",
                new EmpresaRequest("Empresa Facturas " + UUID.randomUUID(), "T" + UUID.randomUUID().toString().substring(0, 8), null, null, null),
                EmpresaResponse.class).id();

        serieId = crear("/api/empresas/" + empresaId + "/series",
                new SerieFacturacionRequest("A"), SerieFacturacionResponse.class).id();

        clienteId = crear("/api/empresas/" + empresaId + "/clientes",
                new ClienteRequest("Cliente Facturas", "C" + UUID.randomUUID().toString().substring(0, 8), null, null),
                ClienteResponse.class).id();
    }

    @Test
    void crear_calculaImportesYHashGenesisComoAnterior() {
        ResponseEntity<SerieFacturacionResponse> serie = restTemplate.exchange(
                "/api/empresas/" + empresaId + "/series/" + serieId, HttpMethod.GET, authenticated(adminToken), SerieFacturacionResponse.class);
        String hashGenesis = serie.getBody().ultimoHash();

        FacturaResponse factura = crearFactura(new LineaFacturaRequest("Servicio", new BigDecimal("2"), new BigDecimal("100.00"),
                new BigDecimal("21"), new BigDecimal("15")));

        assertThat(factura.numero()).isEqualTo(1);
        assertThat(factura.hashAnterior()).isEqualTo(hashGenesis);
        assertThat(factura.hashActual()).hasSize(64).matches("[0-9a-f]+").isNotEqualTo(hashGenesis);
        assertThat(factura.lineas()).hasSize(1);
        assertThat(factura.lineas().get(0).importeBase()).isEqualByComparingTo("200.00");
        assertThat(factura.lineas().get(0).importeIva()).isEqualByComparingTo("42.00");
        assertThat(factura.lineas().get(0).importeIrpf()).isEqualByComparingTo("30.00");
        assertThat(factura.lineas().get(0).importeTotal()).isEqualByComparingTo("212.00");
        assertThat(factura.baseImponible()).isEqualByComparingTo("200.00");
        assertThat(factura.totalIva()).isEqualByComparingTo("42.00");
        assertThat(factura.totalIrpf()).isEqualByComparingTo("30.00");
        assertThat(factura.total()).isEqualByComparingTo("212.00");
    }

    @Test
    void segundaFactura_encadenaHashAnteriorConHashActualDeLaPrimera() {
        FacturaResponse primera = crearFactura(new LineaFacturaRequest("A", BigDecimal.ONE, new BigDecimal("10.00"), BigDecimal.ZERO, BigDecimal.ZERO));
        FacturaResponse segunda = crearFactura(new LineaFacturaRequest("B", BigDecimal.ONE, new BigDecimal("20.00"), BigDecimal.ZERO, BigDecimal.ZERO));

        assertThat(primera.numero()).isEqualTo(1);
        assertThat(segunda.numero()).isEqualTo(2);
        assertThat(segunda.hashAnterior()).isEqualTo(primera.hashActual());
    }

    @Test
    void eliminarCliente_conFacturasExistentes_devuelve409NoOpaco500() {
        crearFactura(new LineaFacturaRequest("A", BigDecimal.ONE, new BigDecimal("10.00"), BigDecimal.ZERO, BigDecimal.ZERO));

        ResponseEntity<String> response = restTemplate.exchange(
                "/api/empresas/" + empresaId + "/clientes/" + clienteId, HttpMethod.DELETE,
                authenticated(adminToken), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    void crear_sinLineas_devuelve400() {
        ResponseEntity<String> response = restTemplate.exchange(
                "/api/empresas/" + empresaId + "/series/" + serieId + "/facturas", HttpMethod.POST,
                authenticated(adminToken, new FacturaRequest(clienteId, List.of())), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    /**
     * Regresion del bug de doble redondeo corregido en la Fase 6: sin el
     * @Digits(fraction=2) en LineaFacturaRequest, un precioUnitario con 3+
     * decimales producia un importe distinto segun se redondeara antes o
     * despues de multiplicar. Este test confirma que la validacion de
     * entrada ya lo bloquea con 400 antes de que llegue a calcularLinea().
     */
    @Test
    void crear_conPrecioUnitarioDeMasDeDosDecimales_devuelve400() {
        FacturaRequest request = new FacturaRequest(clienteId, List.of(
                new LineaFacturaRequest("Servicio", BigDecimal.ONE, new BigDecimal("10.005"), BigDecimal.ZERO, BigDecimal.ZERO)));

        ResponseEntity<String> response = restTemplate.exchange(
                "/api/empresas/" + empresaId + "/series/" + serieId + "/facturas", HttpMethod.POST,
                authenticated(adminToken, request), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void crear_conCantidadDeMasDeDosDecimales_devuelve400() {
        FacturaRequest request = new FacturaRequest(clienteId, List.of(
                new LineaFacturaRequest("Servicio", new BigDecimal("1.125"), new BigDecimal("10.00"), BigDecimal.ZERO, BigDecimal.ZERO)));

        ResponseEntity<String> response = restTemplate.exchange(
                "/api/empresas/" + empresaId + "/series/" + serieId + "/facturas", HttpMethod.POST,
                authenticated(adminToken, request), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void crear_enSerieDesactivada_devuelve409() {
        restTemplate.exchange("/api/empresas/" + empresaId + "/series/" + serieId + "/desactivar",
                HttpMethod.PATCH, authenticated(adminToken), SerieFacturacionResponse.class);

        ResponseEntity<String> response = restTemplate.exchange(
                "/api/empresas/" + empresaId + "/series/" + serieId + "/facturas", HttpMethod.POST,
                authenticated(adminToken, unaLineaRequest()), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    void anular_dosVeces_laSegundaDevuelve409() {
        UUID facturaId = crearFactura(new LineaFacturaRequest("A", BigDecimal.ONE, new BigDecimal("10.00"), BigDecimal.ZERO, BigDecimal.ZERO)).id();

        ResponseEntity<FacturaResponse> primeraAnulacion = restTemplate.exchange(
                "/api/empresas/" + empresaId + "/series/" + serieId + "/facturas/" + facturaId + "/anular",
                HttpMethod.PATCH, authenticated(adminToken), FacturaResponse.class);
        assertThat(primeraAnulacion.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(primeraAnulacion.getBody().estado().name()).isEqualTo("ANULADA");
        assertThat(primeraAnulacion.getBody().numero()).isEqualTo(1);

        ResponseEntity<String> segundaAnulacion = restTemplate.exchange(
                "/api/empresas/" + empresaId + "/series/" + serieId + "/facturas/" + facturaId + "/anular",
                HttpMethod.PATCH, authenticated(adminToken), String.class);
        assertThat(segundaAnulacion.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    void creacionConcurrente_enLaMismaSerie_noProduceHuecosNiDuplicados() throws InterruptedException {
        int hilos = 10;
        ExecutorService executor = Executors.newFixedThreadPool(hilos);
        CountDownLatch salida = new CountDownLatch(1);
        CountDownLatch listos = new CountDownLatch(hilos);
        List<ResponseEntity<FacturaResponse>> resultados = new java.util.concurrent.CopyOnWriteArrayList<>();

        for (int i = 0; i < hilos; i++) {
            executor.submit(() -> {
                listos.countDown();
                try {
                    salida.await();
                    resultados.add(restTemplate.exchange(
                            "/api/empresas/" + empresaId + "/series/" + serieId + "/facturas", HttpMethod.POST,
                            authenticated(adminToken, unaLineaRequest()), FacturaResponse.class));
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
        }

        listos.await(5, TimeUnit.SECONDS);
        salida.countDown();
        executor.shutdown();
        executor.awaitTermination(30, TimeUnit.SECONDS);

        assertThat(resultados).hasSize(hilos);
        assertThat(resultados).allSatisfy(r -> assertThat(r.getStatusCode()).isEqualTo(HttpStatus.CREATED));

        Set<Integer> numeros = resultados.stream().map(r -> r.getBody().numero()).collect(Collectors.toSet());
        assertThat(numeros).hasSize(hilos);
        assertThat(numeros).isEqualTo(IntStream.rangeClosed(1, hilos).boxed().collect(Collectors.toSet()));
    }

    private FacturaResponse crearFactura(LineaFacturaRequest linea) {
        return crear("/api/empresas/" + empresaId + "/series/" + serieId + "/facturas",
                new FacturaRequest(clienteId, List.of(linea)), FacturaResponse.class);
    }

    private FacturaRequest unaLineaRequest() {
        return new FacturaRequest(clienteId, List.of(
                new LineaFacturaRequest("Servicio", BigDecimal.ONE, new BigDecimal("10.00"), BigDecimal.ZERO, BigDecimal.ZERO)));
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
