package com.enlaza.common.hash;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/**
 * Estandar Java (MessageDigest), sin dependencias extra -- ver decision D10
 * del modelo de datos. Usado por SerieFacturacion (semilla genesis) y
 * Factura (cadena de hashes real).
 */
public final class Sha256 {

    private Sha256() {
    }

    public static String hex(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 no disponible en esta JVM", e);
        }
    }
}
