package com.fleetroute.api.service;

import com.fleetroute.api.config.ApiException;
import com.fleetroute.api.dto.EmpresaInfo;
import com.fleetroute.api.dto.LoginResponse;
import com.fleetroute.api.dto.OperadorInfo;
import com.fleetroute.api.security.LoginAttemptService;
import com.fleetroute.api.security.Sesion;
import com.fleetroute.api.dto.CambioPasswordRequest;
import com.fleetroute.api.security.SesionService;
import org.mindrot.jbcrypt.BCrypt;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.time.ZoneId;
import java.util.List;
import java.util.Locale;

@Service
public class AuthService {

    private static final String CONSULTA_USUARIO = """
            select u.id_usuario, u.username, u.password_hash, u.rol, u.id_operador,
                   o.nombre as operador_nombre, o.cedula
              from usuarios u
              left join operadores o on o.id_operador = u.id_operador
             where u.username = ? and u.activo = true
            """;

    private static final String CONSULTA_EMPRESA = """
            select nombre, zona_horaria, moneda
              from empresa
             order by id_empresa
             limit 1
            """;

    private final JdbcTemplate jdbc;
    private final SesionService sesiones;
    private final LoginAttemptService intentos;

    public AuthService(JdbcTemplate jdbc, SesionService sesiones, LoginAttemptService intentos) {
        this.jdbc = jdbc;
        this.sesiones = sesiones;
        this.intentos = intentos;
    }

    public LoginResponse login(String usuario, String password) {
        String clave = usuario.toLowerCase(Locale.ROOT);
        long bloqueo = intentos.segundosBloqueo(clave);
        if (bloqueo > 0) {
            throw new ApiException(HttpStatus.TOO_MANY_REQUESTS,
                    "Demasiados intentos fallidos. Espere " + bloqueo + " segundos.");
        }

        List<UsuarioFila> filas = jdbc.query(CONSULTA_USUARIO, (rs, rowNum) -> new UsuarioFila(
                rs.getInt("id_usuario"),
                rs.getString("username"),
                rs.getString("password_hash"),
                rs.getString("rol"),
                (Integer) rs.getObject("id_operador"),
                rs.getString("operador_nombre"),
                rs.getString("cedula")), usuario);

        if (filas.isEmpty() || !passwordCorrecta(password, filas.get(0).passwordHash())) {
            intentos.registrarFallo(clave);
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Usuario o contraseña incorrectos.");
        }

        UsuarioFila fila = filas.get(0);
        if (!"OPERADOR".equals(fila.rol()) || fila.idOperador() == null) {
            throw new ApiException(HttpStatus.FORBIDDEN,
                    "Esta vista es solo para operadores. Use la aplicación de escritorio para administración.");
        }

        EmpresaInfo empresa = empresa();
        intentos.limpiar(clave);
        Sesion sesion = sesiones.crear(fila.idUsuario(), fila.idOperador(), fila.username(),
                fila.operadorNombre(), fila.cedula(), empresa.nombre(), empresa.zonaHoraria(), empresa.moneda());
        OperadorInfo operador = new OperadorInfo(fila.idOperador(), fila.username(),
                fila.operadorNombre(), fila.cedula());
        return new LoginResponse(sesion.token(),
                sesion.expira().atZone(ZoneId.of(empresa.zonaHoraria())).toOffsetDateTime(),
                operador, empresa);
    }

    public void cambiarPassword(Sesion sesion, CambioPasswordRequest solicitud) {
        String hash = jdbc.queryForObject(
                "select password_hash from usuarios where id_usuario = ?",
                String.class, sesion.idUsuario());
        if (hash == null || solicitud == null || solicitud.actual() == null
                || !BCrypt.checkpw(solicitud.actual(), hash)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "La contraseña actual no es correcta.");
        }
        if (solicitud.nueva() == null || solicitud.nueva().length() < 8) {
            throw new ApiException(HttpStatus.BAD_REQUEST,
                    "La nueva contraseña debe tener al menos 8 caracteres.");
        }
        jdbc.update("update usuarios set password_hash = ? where id_usuario = ?",
                BCrypt.hashpw(solicitud.nueva(), BCrypt.gensalt()), sesion.idUsuario());
    }

    private boolean passwordCorrecta(String password, String hash) {
        if (password == null || hash == null || hash.isBlank()) {
            return false;
        }
        try {
            return BCrypt.checkpw(password, hash);
        } catch (IllegalArgumentException ex) {
            return false;
        }
    }

    private EmpresaInfo empresa() {
        List<EmpresaInfo> filas = jdbc.query(CONSULTA_EMPRESA, (rs, rowNum) -> {
            String nombre = rs.getString("nombre");
            String zona = rs.getString("zona_horaria");
            String moneda = rs.getString("moneda");
            if (nombre == null || nombre.isBlank()) {
                nombre = "FleetRoute Pro";
            }
            if (zona == null || zona.isBlank()) {
                zona = "America/Managua";
            }
            if (moneda == null || moneda.isBlank()) {
                moneda = "NIO";
            }
            try {
                ZoneId.of(zona);
            } catch (RuntimeException ex) {
                zona = "America/Managua";
            }
            return new EmpresaInfo(nombre, zona, moneda);
        });
        return filas.isEmpty()
                ? new EmpresaInfo("FleetRoute Pro", "America/Managua", "NIO")
                : filas.get(0);
    }

    private record UsuarioFila(
            int idUsuario,
            String username,
            String passwordHash,
            String rol,
            Integer idOperador,
            String operadorNombre,
            String cedula) {
    }
}
