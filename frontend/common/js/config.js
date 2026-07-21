// Punto unico de configuracion del origen de la API. Se carga con un
// <script> normal (no modulo) ANTES que el resto, para que cualquier
// pagina pueda pisar window.ENLAZA_API_BASE antes de que apiClient.js
// lo lea, sin tocar el codigo fuente en cada despliegue.
window.ENLAZA_API_BASE = window.ENLAZA_API_BASE || 'http://localhost:8080/api';
