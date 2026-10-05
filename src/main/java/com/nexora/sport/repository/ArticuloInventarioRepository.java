package com.nexora.sport.repository;

import com.nexora.sport.model.ArticuloInventario;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ArticuloInventarioRepository extends JpaRepository<ArticuloInventario, Long> {
    Page<ArticuloInventario> findByCentroIdAndDeletedAtIsNull(Long centroId, Pageable pageable);

    Page<ArticuloInventario> findByCentroIdAndDeletedAtIsNullAndNombreContainingIgnoreCase(
            Long centroId, String nombre, Pageable pageable);

    Optional<ArticuloInventario> findByCentroIdAndCodigoBarras(Long centroId, String codigoBarras);

    /** Filtros homologados con DemoPV#Inventory (buscador por nombre/codigo + categoria + stock bajo, todos opcionales),
     * mas sucursal (tambien opcional: null = todas las sucursales del centro, vista global del Dueno). */
    // cast(:q as string): sin el cast, con :q null Postgres no logra inferir su tipo
    // dentro de lower(...) y truena "no existe la funcion lower(bytea)".
    @Query("select distinct a from ArticuloInventario a left join a.categorias c where a.centro.id = :centroId and a.deletedAt is null " +
           "and (cast(:sucursalId as long) is null or a.sucursal.id = :sucursalId) " +
           "and (cast(:q as string) is null or lower(a.nombre) like lower(concat('%', cast(:q as string), '%')) or lower(a.codigoBarras) like lower(concat('%', cast(:q as string), '%'))) " +
           "and (cast(:categoriaId as long) is null or c.id = :categoriaId) " +
           "and (:soloStockBajo = false or a.stock <= a.stockMinimo)")
    Page<ArticuloInventario> buscar(@Param("centroId") Long centroId, @Param("sucursalId") Long sucursalId, @Param("q") String q,
                                     @Param("categoriaId") Long categoriaId, @Param("soloStockBajo") boolean soloStockBajo,
                                     Pageable pageable);

    List<ArticuloInventario> findByCentroIdAndVendibleTrueAndActivoTrueAndDeletedAtIsNull(Long centroId);

    List<ArticuloInventario> findByCentroIdAndReservableTrueAndActivoTrueAndDeletedAtIsNull(Long centroId);

    @Query("select a from ArticuloInventario a where a.centro.id = :centroId and a.deletedAt is null " +
           "and (cast(:sucursalId as long) is null or a.sucursal.id = :sucursalId) " +
           "and a.stock <= a.stockMinimo")
    List<ArticuloInventario> findConStockBajo(@Param("centroId") Long centroId, @Param("sucursalId") Long sucursalId);

    /** Para "buscar en otras sucursales" (seccion 31 del encargo): mismo nombre o codigo de
     * barras, en el mismo centro, pero en CUALQUIER sucursal menos la del que busca -- de
     * solo lectura, nunca para vender (ver VentaService#crear, que SI exige la sucursal exacta). */
    @Query("select a from ArticuloInventario a where a.centro.id = :centroId and a.deletedAt is null and a.activo = true " +
           "and a.sucursal.id <> :sucursalId " +
           "and (lower(a.nombre) like lower(concat('%', :q, '%')) or lower(a.codigoBarras) = lower(:q))")
    List<ArticuloInventario> buscarEnOtrasSucursales(@Param("centroId") Long centroId, @Param("sucursalId") Long sucursalId, @Param("q") String q);
}
