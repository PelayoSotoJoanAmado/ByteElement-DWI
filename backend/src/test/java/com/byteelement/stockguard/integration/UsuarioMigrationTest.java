package com.byteelement.stockguard.integration;

import static org.junit.jupiter.api.Assertions.*;

import com.zaxxer.hikari.HikariDataSource;
import java.util.UUID;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationVersion;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

class UsuarioMigrationTest {
  @Test
  void migrarV3RelacionaAutoresExistentesSinAlterarHistorial() {
    try (var fuente = new HikariDataSource()) {
      fuente.setJdbcUrl(
          "jdbc:h2:mem:upgrade-"
              + UUID.randomUUID()
              + ";MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1");
      fuente.setUsername("sa");
      fuente.setPassword("");
      fuente.setMaximumPoolSize(3);
      Flyway.configure()
          .dataSource(fuente)
          .locations("classpath:db/migration")
          .target(MigrationVersion.fromVersion("3"))
          .load()
          .migrate();
      var jdbc = new JdbcTemplate(fuente);
      jdbc.update(
          "insert into usuario(username,password_hash,rol,activo) values"
              + " ('autor-historico','hash-no-utilizable','ALMACEN',false)");
      jdbc.update(
          "insert into usuario(username,password_hash,rol,activo) values"
              + " ('supervisor-historico','hash-no-utilizable','SUPERVISOR',false)");
      jdbc.update(
          "insert into producto(sku,categoria,marca,modelo,especificacion,critico) values"
              + " ('LEGACY','RAM','Marca','Modelo','Especificacion',true)");
      long producto = jdbc.queryForObject("select id from producto where sku='LEGACY'", Long.class);
      long almacen = jdbc.queryForObject("select id from almacen where codigo='A01'", Long.class);
      long autor =
          jdbc.queryForObject(
              "select id from usuario where username='autor-historico'", Long.class);
      long supervisor =
          jdbc.queryForObject(
              "select id from usuario where username='supervisor-historico'", Long.class);
      jdbc.update(
          "insert into existencia(producto_id,almacen_id,cantidad) values (?,?,7)",
          producto,
          almacen);
      jdbc.update(
          "insert into"
              + " movimiento(producto_id,almacen_id,tipo,cantidad,autor,fecha,motivo,saldo_resultante,referencia)"
              + " values (?,?,'ENTRADA',7,'autor-historico',TIMESTAMP WITH TIME ZONE '2026-10-10"
              + " 12:00:00+00','Historial conservado',7,'referencia-original')",
          producto,
          almacen);
      jdbc.update(
          "insert into"
              + " conteo(producto_id,almacen_id,cantidad_fisica,saldo_corte,fecha_corte,autor,motivo,confirmado,confirmado_por,confirmado_en)"
              + " values (?,?,7,7,TIMESTAMP WITH TIME ZONE '2026-10-10"
              + " 13:00:00+00','autor-historico','Coincide',true,'supervisor-historico',TIMESTAMP"
              + " WITH TIME ZONE '2026-10-10 14:00:00+00')",
          producto,
          almacen);
      jdbc.update(
          "insert into"
              + " conteo(producto_id,almacen_id,cantidad_fisica,saldo_corte,fecha_corte,autor,motivo)"
              + " values (?,?,6,7,TIMESTAMP WITH TIME ZONE '2026-10-10"
              + " 15:00:00+00','autor-historico','Pendiente')",
          producto,
          almacen);
      jdbc.update(
          "insert into"
              + " corte_stock(lote,fecha,autor,producto_id,almacen_id,sku,critico,cantidad,minimo,maximo)"
              + " values ('lote-original',TIMESTAMP WITH TIME ZONE '2026-10-10"
              + " 16:00:00+00','supervisor-historico',?,?,'LEGACY',true,7,2,10)",
          producto,
          almacen);
      Flyway.configure().dataSource(fuente).locations("classpath:db/migration").load().migrate();
      assertEquals(autor, jdbc.queryForObject("select usuario_id from movimiento", Long.class));
      assertEquals(
          2,
          jdbc.queryForObject(
              "select count(*) from conteo where usuario_id=?", Integer.class, autor));
      assertEquals(
          supervisor,
          jdbc.queryForObject(
              "select confirmado_por_id from conteo where confirmado=true", Long.class));
      assertNull(
          jdbc.queryForObject(
              "select confirmado_por_id from conteo where confirmado=false", Long.class));
      assertEquals(
          supervisor, jdbc.queryForObject("select usuario_id from corte_stock", Long.class));
      assertEquals(7, jdbc.queryForObject("select cantidad from existencia", Long.class));
      assertEquals(
          "Historial conservado",
          jdbc.queryForObject("select motivo from movimiento", String.class));
      assertEquals(
          "referencia-original",
          jdbc.queryForObject("select referencia from movimiento", String.class));
      assertEquals(
          4,
          jdbc.queryForObject(
              "select count(*) from information_schema.table_constraints where constraint_name in"
                  + " ('fk_movimiento_usuario','fk_conteo_usuario','fk_conteo_confirmado_por','fk_corte_stock_usuario')",
              Integer.class));
    }
  }

  @Test
  void autorHistoricoSinCuentaDetieneMigracionSinBorrarNiInventarUsuarios() {
    try (var fuente = new HikariDataSource()) {
      fuente.setJdbcUrl(
          "jdbc:h2:mem:orphan-"
              + UUID.randomUUID()
              + ";MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1");
      fuente.setUsername("sa");
      fuente.setPassword("");
      fuente.setMaximumPoolSize(3);
      Flyway.configure()
          .dataSource(fuente)
          .locations("classpath:db/migration")
          .target(MigrationVersion.fromVersion("3"))
          .load()
          .migrate();
      var jdbc = new JdbcTemplate(fuente);
      jdbc.update(
          "insert into producto(sku,categoria,marca,modelo,especificacion,critico) values"
              + " ('ORPHAN','RAM','Marca','Modelo','Especificacion',true)");
      long producto = jdbc.queryForObject("select id from producto where sku='ORPHAN'", Long.class);
      long almacen = jdbc.queryForObject("select id from almacen where codigo='A01'", Long.class);
      jdbc.update(
          "insert into"
              + " movimiento(producto_id,almacen_id,tipo,cantidad,autor,fecha,motivo,saldo_resultante,referencia)"
              + " values (?,?,'ENTRADA',7,'sin-correspondencia',CURRENT_TIMESTAMP,'No"
              + " eliminar',7,'referencia-preservada')",
          producto,
          almacen);
      assertThrows(
          org.flywaydb.core.api.FlywayException.class,
          () ->
              Flyway.configure()
                  .dataSource(fuente)
                  .locations("classpath:db/migration")
                  .load()
                  .migrate());
      assertEquals(1, jdbc.queryForObject("select count(*) from movimiento", Integer.class));
      assertEquals(
          "sin-correspondencia", jdbc.queryForObject("select autor from movimiento", String.class));
      assertEquals(
          "No eliminar", jdbc.queryForObject("select motivo from movimiento", String.class));
      assertEquals(0, jdbc.queryForObject("select count(*) from usuario", Integer.class));
    }
  }
}
