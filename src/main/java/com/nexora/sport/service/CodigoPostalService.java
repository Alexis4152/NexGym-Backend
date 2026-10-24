package com.nexora.sport.service;

import com.nexora.sport.dto.CodigoPostalDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.zip.GZIPInputStream;

/** Catalogo Nacional de Codigos Postales (SEPOMEX / Correos de Mexico), empaquetado como
 * recurso: resources/sepomex/codigos-postales.psv.gz, una linea por asentamiento con
 * "cp|asentamiento|tipo|municipio|estado". Se carga en memoria la primera vez que se usa.
 * Para actualizarlo: descargar el TXT completo de correosdemexico.gob.mx y regenerar el archivo. */
@Service
public class CodigoPostalService {

    private static final Logger log = LoggerFactory.getLogger(CodigoPostalService.class);
    private static final String RECURSO = "sepomex/codigos-postales.psv.gz";

    private volatile Map<String, List<CodigoPostalDto.Colonia>> catalogo;

    public Optional<CodigoPostalDto> buscar(String cp) {
        if (cp == null || !cp.matches("\\d{5}")) return Optional.empty();
        List<CodigoPostalDto.Colonia> colonias = catalogo().get(cp);
        if (colonias == null || colonias.isEmpty()) return Optional.empty();
        List<String> estados = colonias.stream().map(CodigoPostalDto.Colonia::estado).distinct().toList();
        List<String> municipios = colonias.stream().map(CodigoPostalDto.Colonia::municipio).distinct().toList();
        return Optional.of(new CodigoPostalDto(cp, estados, municipios, colonias));
    }

    /** true si el catalogo esta disponible y el C.P. no existe en el (si el recurso no se pudo
     * cargar no se bloquea a nadie: se devuelve false). */
    public boolean esInexistente(String cp) {
        Map<String, List<CodigoPostalDto.Colonia>> c = catalogo();
        return !c.isEmpty() && !c.containsKey(cp);
    }

    private Map<String, List<CodigoPostalDto.Colonia>> catalogo() {
        Map<String, List<CodigoPostalDto.Colonia>> c = catalogo;
        if (c == null) {
            synchronized (this) {
                if (catalogo == null) catalogo = cargar();
                c = catalogo;
            }
        }
        return c;
    }

    private Map<String, List<CodigoPostalDto.Colonia>> cargar() {
        Map<String, List<CodigoPostalDto.Colonia>> mapa = new HashMap<>(40_000);
        Map<String, String> pool = new HashMap<>(); // municipios/estados/tipos se repiten miles de veces
        try (BufferedReader in = new BufferedReader(new InputStreamReader(
                new GZIPInputStream(new ClassPathResource(RECURSO).getInputStream()), StandardCharsets.UTF_8))) {
            String linea;
            while ((linea = in.readLine()) != null) {
                String[] p = linea.split("\\|", -1);
                if (p.length < 5) continue;
                mapa.computeIfAbsent(p[0], k -> new ArrayList<>()).add(new CodigoPostalDto.Colonia(
                        p[1], pool.computeIfAbsent(p[2], k -> k), pool.computeIfAbsent(p[3], k -> k),
                        pool.computeIfAbsent(p[4], k -> k)));
            }
            mapa.values().forEach(l -> l.sort(Comparator.comparing(CodigoPostalDto.Colonia::nombre)));
            log.info("Catalogo SEPOMEX cargado: {} codigos postales", mapa.size());
        } catch (Exception e) {
            log.error("No se pudo cargar el catalogo de codigos postales ({}): la busqueda por C.P. no estara disponible", RECURSO, e);
            return Map.of();
        }
        return mapa;
    }
}
