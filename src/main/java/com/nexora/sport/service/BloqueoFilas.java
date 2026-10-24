package com.nexora.sport.service;

import com.nexora.sport.exception.ResourceNotFoundException;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Bloqueo de filas (SELECT ... FOR UPDATE) para operaciones concurrentes sobre stock y estados
 * (homologado con DemoPV#findByIdForUpdate): la segunda operacion espera a que la primera
 * termine su transaccion y despues ve los datos ya actualizados.
 *
 * Usa refresh(entidad, PESSIMISTIC_WRITE) en vez de una consulta con @Lock: si la entidad ya
 * estaba cargada en la misma transaccion, una consulta bloquea la fila pero Hibernate NO
 * sobrescribe el objeto en memoria (seguiria con el stock viejo); refresh bloquea Y relee.
 */
@Component
public class BloqueoFilas {

    @PersistenceContext
    private EntityManager em;

    /** La entidad bloqueada y con su estado actual de la base. Debe llamarse dentro de una transaccion. */
    public <T> T bloquear(Class<T> tipo, Long id, String mensajeNoEncontrado) {
        T entidad = em.find(tipo, id);
        if (entidad == null) throw new ResourceNotFoundException(mensajeNoEncontrado);
        em.refresh(entidad, LockModeType.PESSIMISTIC_WRITE);
        return entidad;
    }

    /** Bloquea varias filas SIEMPRE en orden ascendente de id: dos operaciones sobre los mismos
     * registros nunca se esperan en orden cruzado (evita deadlocks). */
    public <T> Map<Long, T> bloquearEnOrden(Class<T> tipo, Collection<Long> ids, String mensajeNoEncontrado) {
        Map<Long, T> bloqueados = new LinkedHashMap<>();
        ids.stream().filter(Objects::nonNull).distinct().sorted()
                .forEach(id -> bloqueados.put(id, bloquear(tipo, id, mensajeNoEncontrado)));
        return bloqueados;
    }
}
