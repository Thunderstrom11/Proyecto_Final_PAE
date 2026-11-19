(() => {
    'use strict';

    const API = '/api';
    const REFRESH_MS = 5000;
    const STORAGE = {
        token: 'fleetroute.token',
        operador: 'fleetroute.operador',
        empresa: 'fleetroute.empresa'
    };
    const ESTADOS = {
        PROGRAMADO: 'Programado',
        EN_CURSO: 'En curso',
        PAUSADO: 'En pausa',
        FINALIZADO: 'Finalizado',
        CANCELADO: 'Cancelado'
    };
    const SEMAFORO_NIVEL = {
        A_TIEMPO: 'ok',
        DESCANSO_PENDIENTE: 'warn',
        RETRASO_SLA: 'danger'
    };

    const state = {
        token: null,
        panel: null,
        config: null,
        loading: false,
        timer: null,
        toastTimer: null,
        map: null,
        mapLoaded: false,
        routeMarkers: [],
        userMarker: null,
        lastPosition: null,
        route: null,
        routeSignature: null,
        navActive: false,
        navPhase: null,
        routeTarget: null,
        arrivedOrigin: false,
        arrivedDestination: false,
        followUser: true,
        followButton: null,
        mapResizeObserver: null,
        lastRecalcAt: 0,
        recalculating: false,
        geoWatch: null,
        voiceEnabled: true,
        currentStep: 0,
        online: navigator.onLine,
        sendingAlert: false
    };

    const $ = id => document.getElementById(id);

    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', init);
    } else {
        init();
    }

    function init() {
        bindEvents();
        actualizarBotonPantallaCompleta();
        loadConfig();
        restoreSession();
    }

    async function loadConfig() {
        try {
            const config = await api('/config', { auth: false });
            state.config = config;
            if (config?.empresa) {
                $('login-company').textContent = config.empresa;
                document.title = `${config.empresa} · Cabina del operador`;
            }
        } catch (ignored) {
            state.config = null;
        }
    }

    function bindEvents() {
        $('login-form').addEventListener('submit', login);
        $('toggle-password').addEventListener('click', togglePassword);
        $('logout-button').addEventListener('click', logout);
        $('change-password').addEventListener('click', abrirModalPassword);
        $('close-password').addEventListener('click', cerrarModalPassword);
        $('cancel-password').addEventListener('click', cerrarModalPassword);
        $('password-form').addEventListener('submit', cambiarPassword);
        $('refresh-button').addEventListener('click', () => {
            state.routeSignature = null;
            loadPanel();
        });
        $('alert-button').addEventListener('click', enviarAlerta);
        $('btn-accion-iniciar').addEventListener('click', () => ejecutarJornada('iniciar'));
        $('btn-accion-pausar').addEventListener('click', () => ejecutarJornada('pausar'));
        $('btn-accion-reanudar').addEventListener('click', () => ejecutarJornada('reanudar'));
        $('btn-accion-confirmar').addEventListener('click', () => ejecutarJornada('confirmar'));
        $('nav-start').addEventListener('click', toggleNavigation);
        $('nav-voice').addEventListener('click', toggleVoice);
        $('toggle-trip').addEventListener('click', toggleTripPanel);
        $('nav-arrival-close').addEventListener('click', ocultarLlegada);
        const botonPantalla = $('fullscreen-toggle');
        if (botonPantalla) {
            botonPantalla.addEventListener('click', togglePantallaCompleta);
        }
        window.addEventListener('resize', () => {
            if (state.map) {
                state.map.resize();
            }
        });
        document.addEventListener('fullscreenchange', () => {
            if (document.fullscreenElement) {
                bloquearHorizontal();
            }
            actualizarBotonPantallaCompleta();
        });
        document.addEventListener('visibilitychange', () => {
            if (!document.hidden && state.token) {
                loadPanel({ silent: true });
            }
        });
        window.addEventListener('online', () => {
            state.online = true;
            setSessionStatus('Cabina lista.');
            if (state.token) {
                toast('Conexión restablecida. Actualizando datos…');
                loadPanel({ silent: true });
            }
        });
        window.addEventListener('offline', () => {
            state.online = false;
            setSessionStatus('Sin conexión');
            if (state.token) {
                toast('Sin conexión. La actualización se reanudará al volver la red.', true);
            }
        });
    }

    function restoreSession() {
        const token = storageGet(STORAGE.token);
        if (!token) {
            showLogin();
            return;
        }
        state.token = token;
        const operador = parseJson(storageGet(STORAGE.operador));
        const empresa = parseJson(storageGet(STORAGE.empresa));
        showDashboard({ operador, empresa });
        loadPanel();
    }

    async function login(event) {
        event.preventDefault();
        activarPantallaCompleta();
        hideLoginError();
        const usuario = $('usuario').value.trim();
        const password = $('password').value;

        if (!usuario || !password) {
            showLoginError('Escribe tu usuario y contraseña.');
            (!usuario ? $('usuario') : $('password')).focus();
            return;
        }

        setLoginLoading(true);
        try {
            const data = await api('/auth/login', {
                method: 'POST',
                body: { usuario, password },
                auth: false
            });
            saveSession(data);
            $('password').value = '';
            state.panel = null;
            showDashboard(data);
            await loadPanel();
        } catch (error) {
            salirPantallaCompleta();
            showLoginError(error.message || 'No se pudo iniciar sesión.');
        } finally {
            setLoginLoading(false);
        }
    }

    function activarPantallaCompleta() {
        const raiz = document.documentElement;
        if (document.fullscreenElement || typeof raiz.requestFullscreen !== 'function') {
            return;
        }
        try {
            const intento = raiz.requestFullscreen({ navigationUI: 'hide' });
            if (intento && typeof intento.catch === 'function') {
                intento.catch(() => {});
            }
        } catch (ignored) {
            // Algunos navegadores rechazan la solicitud aunque venga del gesto de login.
        }
    }

    function bloquearHorizontal() {
        if (!document.fullscreenElement || !screen.orientation
            || typeof screen.orientation.lock !== 'function') {
            return;
        }
        try {
            const intento = screen.orientation.lock('landscape');
            if (intento && typeof intento.catch === 'function') {
                intento.catch(() => {});
            }
        } catch (ignored) {
            // El bloqueo de orientación solo existe en algunos navegadores móviles.
        }
    }

    function salirPantallaCompleta() {
        try {
            if (document.fullscreenElement && typeof document.exitFullscreen === 'function') {
                const intento = document.exitFullscreen();
                if (intento && typeof intento.catch === 'function') {
                    intento.catch(() => {});
                }
            }
            if (screen.orientation && typeof screen.orientation.unlock === 'function') {
                screen.orientation.unlock();
            }
        } catch (ignored) {
            // Sin efecto cuando no hay pantalla completa activa.
        }
    }

    function togglePantallaCompleta() {
        if (document.fullscreenElement) {
            salirPantallaCompleta();
            return;
        }
        activarPantallaCompleta();
    }

    function actualizarBotonPantallaCompleta() {
        const boton = $('fullscreen-toggle');
        if (!boton) {
            return;
        }
        const activa = !!document.fullscreenElement;
        const etiqueta = activa ? 'Salir de pantalla completa' : 'Pantalla completa';
        boton.title = etiqueta;
        boton.setAttribute('aria-label', etiqueta);
        boton.setAttribute('aria-pressed', String(activa));
        const texto = boton.querySelector('span');
        if (texto) {
            texto.textContent = etiqueta;
        }
        const icono = $('fullscreen-toggle-icon');
        if (icono) {
            icono.setAttribute('href', activa ? '#i-fullscreen-exit' : '#i-fullscreen');
        }
    }

    async function logout() {
        try {
            await api('/auth/logout', { method: 'POST' });
        } catch (ignored) {
            // La sesión local se cierra aunque el servidor no responda.
        }
        clearSession();
        showLogin('Cerraste sesión correctamente.', 'info');
    }

    function saveSession(data) {
        state.token = data.token;
        storageSet(STORAGE.token, data.token);
        storageSet(STORAGE.operador, JSON.stringify(data.operador || {}));
        storageSet(STORAGE.empresa, JSON.stringify(data.empresa || {}));
    }

    function clearSession() {
        state.token = null;
        state.panel = null;
        storageRemove(STORAGE.token);
        storageRemove(STORAGE.operador);
        storageRemove(STORAGE.empresa);
    }

    function showLogin(message, tone = 'error') {
        stopTimers();
        resetMap();
        salirPantallaCompleta();
        $('alert-button').disabled = true;
        deshabilitarAcciones();
        $('dashboard-view').hidden = true;
        $('login-view').hidden = false;
        $('password').value = '';
        setLoginLoading(false);
        hideLoginError();
        if (message) {
            showLoginError(message, tone === 'info');
        }
        window.setTimeout(() => $('usuario').focus(), 40);
    }

    function showDashboard(data) {
        $('login-view').hidden = true;
        $('dashboard-view').hidden = false;
        hydrateHeader(data?.operador, data?.empresa);
        startTimers();
        window.scrollTo({ top: 0, behavior: 'smooth' });
    }

    function hydrateHeader(operador, empresa) {
        if (operador) {
            const nombre = operador.nombre || operador.username || 'Operador';
            $('operator-name').textContent = nombre;
        }
        if (empresa) {
            document.title = `${empresa.nombre || 'FleetRoute Pro'} · Cabina del operador`;
        }
    }

    function startTimers() {
        stopTimers();
        state.timer = window.setInterval(() => {
            if (!document.hidden && state.token && navigator.onLine) {
                loadPanel({ silent: true });
            }
        }, REFRESH_MS);
    }

    function stopTimers() {
        if (state.timer) {
            window.clearInterval(state.timer);
            state.timer = null;
        }
    }

    async function loadPanel({ silent = false } = {}) {
        if (!state.token || state.loading) {
            return;
        }
        if (!navigator.onLine) {
            state.online = false;
            setSessionStatus('Sin conexión');
            if (!silent) {
                toast('Sin conexión a internet. Se reintentará automáticamente.', true);
            }
            return;
        }
        state.online = true;
        state.loading = true;
        try {
            const data = await api('/operador/panel');
            state.panel = data;
            hydrateHeader(data.operador, data.empresa);
            setSessionStatus('Cabina lista.');
            renderPanel(data);
        } catch (error) {
            if (error.status === 401) {
                clearSession();
                showLogin('Tu sesión expiró. Inicia sesión nuevamente.');
            } else if (!silent) {
                if (!state.panel) {
                    renderEmpty(null);
                    toast(error.message || 'No se pudo cargar la cabina.', true);
                } else {
                    toast(error.message || 'No se pudo actualizar la información.', true);
                }
            } else {
                toast('No se pudo actualizar. Reintentaremos automáticamente.', true);
            }
        } finally {
            state.loading = false;
        }
    }

    function setSessionStatus(texto) {
        const estado = $('session-status');
        if (estado) {
            estado.textContent = texto;
        }
    }

    function deshabilitarAcciones() {
        ['iniciar', 'pausar', 'reanudar', 'confirmar'].forEach(accion => {
            const boton = $('btn-accion-' + accion);
            if (boton) {
                boton.disabled = true;
            }
        });
    }

    function habilitarAcciones(jornada) {
        const paso = jornada?.paso;
        const esperandoOrigen = debeEsperarLlegada();
        const botonIniciar = $('btn-accion-iniciar');
        botonIniciar.disabled = paso !== 'SIN_INICIAR' || esperandoOrigen;
        botonIniciar.title = esperandoOrigen ? 'Acércate al punto A para iniciar el trayecto.' : '';
        $('btn-accion-pausar').disabled = paso !== 'EN_MARCHA';
        $('btn-accion-reanudar').disabled = !(paso === 'EN_PAUSA' && jornada?.puedeReanudar);
        $('btn-accion-confirmar').disabled = paso !== 'EN_MARCHA';
    }

    function debeEsperarLlegada() {
        if (!state.navActive || state.navPhase !== 'hacia-origen'
                || state.arrivedOrigin || !state.lastPosition) {
            return false;
        }
        const origen = origenViaje();
        if (!origen) {
            return false;
        }
        return distanceMeters(state.lastPosition.lat, state.lastPosition.lng,
            origen[1], origen[0]) > 120;
    }

    function origenViaje() {
        const viaje = state.panel?.viaje;
        return viaje ? coordinatePair(viaje.latitudOrigen, viaje.longitudOrigen) : null;
    }

    function destinoViaje() {
        const viaje = state.panel?.viaje;
        return viaje ? coordinatePair(viaje.latitudDestino, viaje.longitudDestino) : null;
    }

    function toggleTripPanel() {
        const layout = $('cabina-layout');
        const colapsado = layout.classList.toggle('trip-collapsed');
        const boton = $('toggle-trip');
        const etiqueta = colapsado ? 'Mostrar viaje activo' : 'Ocultar viaje activo';
        boton.setAttribute('aria-expanded', String(!colapsado));
        boton.setAttribute('aria-label', etiqueta);
        boton.title = etiqueta;
        $('toggle-trip-icon').setAttribute('href', colapsado ? '#i-chevron-right' : '#i-chevron-left');
        window.setTimeout(() => {
            if (state.map) {
                state.map.resize();
            }
        }, 240);
    }

    async function ejecutarJornada(accion) {
        const viaje = state.panel?.viaje;
        if (!viaje) {
            toast('No hay un viaje activo.', true);
            return;
        }
        if (accion === 'confirmar'
                && !window.confirm('¿Confirmar la entrega del viaje #' + viaje.id + '?')) {
            return;
        }
        if (!navigator.onLine) {
            toast('Sin conexión a internet. No se pudo actualizar el viaje.', true);
            return;
        }

        deshabilitarAcciones();
        try {
            const respuesta = await api('/operador/jornada/' + accion, { method: 'POST' });
            state.routeSignature = null;
            await loadPanel();
            if (accion === 'iniciar' && state.navActive && state.navPhase === 'hacia-origen') {
                state.arrivedOrigin = true;
                state.navPhase = 'hacia-destino';
                state.routeTarget = null;
                ocultarLlegada();
                recalcularRutaNavegacion(true);
            }
            toast(respuesta?.mensaje || 'Jornada actualizada.');
        } catch (error) {
            if (error.status === 401) {
                clearSession();
                showLogin('Tu sesión expiró. Inicia sesión nuevamente.');
                return;
            }
            toast(error.message || 'No se pudo actualizar la jornada.', true);
            habilitarAcciones(state.panel?.jornada);
        }
    }

    function abrirModalPassword() {
        $('password-error').hidden = true;
        $('password-form').reset();
        $('password-modal').hidden = false;
        window.setTimeout(() => $('current-password').focus(), 30);
    }

    function cerrarModalPassword() {
        $('password-modal').hidden = true;
    }

    async function cambiarPassword(event) {
        event.preventDefault();
        const actual = $('current-password').value;
        const nueva = $('new-password').value;
        const confirmar = $('confirm-password').value;
        const error = $('password-error');

        const mostrarError = mensaje => {
            error.textContent = mensaje;
            error.hidden = false;
        };

        if (!actual || !nueva || !confirmar) {
            mostrarError('Complete los tres campos.');
            return;
        }
        if (nueva.length < 8) {
            mostrarError('La nueva contraseña debe tener al menos 8 caracteres.');
            return;
        }
        if (nueva !== confirmar) {
            mostrarError('Las contraseñas nuevas no coinciden.');
            return;
        }

        const boton = $('save-password');
        boton.disabled = true;
        try {
            await api('/auth/password', {
                method: 'POST',
                body: { actual, nueva }
            });
            cerrarModalPassword();
            toast('Contraseña actualizada correctamente.');
        } catch (err) {
            if (err.status === 401) {
                cerrarModalPassword();
                clearSession();
                showLogin('Tu sesión expiró. Inicia sesión nuevamente.');
                return;
            }
            mostrarError(err.message || 'No se pudo cambiar la contraseña.');
        } finally {
            boton.disabled = false;
        }
    }

    async function enviarAlerta() {
        if (state.sendingAlert) {
            return;
        }
        const viaje = state.panel?.viaje;
        if (!viaje) {
            toast('No hay un viaje activo para asociar a la alerta.', true);
            return;
        }
        if (!navigator.onLine) {
            toast('Sin conexión a internet. No se pudo enviar la alerta.', true);
            return;
        }
        if (!window.confirm('¿Enviar una alerta de emergencia a los administradores?')) {
            return;
        }

        state.sendingAlert = true;
        const boton = $('alert-button');
        boton.disabled = true;
        try {
            const posicion = await obtenerUbicacionActual();
            const latitud = posicion?.lat ?? viaje.latitudOrigen;
            const longitud = posicion?.lng ?? viaje.longitudOrigen;
            const ubicacion = posicion
                ? 'Ubicación GPS actual'
                : (viaje.origen ? 'Origen: ' + viaje.origen : null);
            await api('/operador/alerta', {
                method: 'POST',
                body: {
                    latitud,
                    longitud,
                    ubicacion,
                    mensaje: 'Alerta de emergencia enviada desde la cabina web'
                }
            });
            toast('Alerta enviada. Administración ya puede verla.');
        } catch (error) {
            if (error.status === 401) {
                clearSession();
                showLogin('Tu sesión expiró. Inicia sesión nuevamente.');
                return;
            }
            toast(error.message || 'No se pudo enviar la alerta.', true);
        } finally {
            state.sendingAlert = false;
            boton.disabled = false;
        }
    }

    function obtenerUbicacionActual() {
        if (state.lastPosition) {
            return Promise.resolve({ lat: state.lastPosition.lat, lng: state.lastPosition.lng });
        }
        if (!navigator.geolocation) {
            return Promise.resolve(null);
        }
        return new Promise(resolve => {
            navigator.geolocation.getCurrentPosition(
                pos => resolve({ lat: pos.coords.latitude, lng: pos.coords.longitude }),
                () => resolve(null),
                { enableHighAccuracy: true, timeout: 6000, maximumAge: 10000 }
            );
        });
    }

    function renderPanel(data) {
        if (!data.tieneViaje || !data.viaje) {
            renderEmpty(data);
            return;
        }
        renderTrip(data);
    }

    function renderEmpty(data) {
        $('trip-content').hidden = false;
        setText('trip-id', '');
        setText('route-origin', 'Sin viaje asignado');
        setText('route-destination', '');
        if ($('route-arrow')) {
            $('route-arrow').hidden = true;
        }
        if ($('route-destination')) {
            $('route-destination').hidden = true;
        }
        const status = $('trip-status');
        status.textContent = 'Sin viaje';
        status.dataset.status = '';
        setText('cargo-number', '—');
        setText('unit-number', '—');
        $('alert-button').disabled = true;
        setText('drive-percent', '0 %');
        $('drive-progress').style.width = '0%';
        setText('drive-time', '0 h 00 min');
        setText('drive-max', 'Máximo 8 h');
        setText('rest-time', '0 h 00 min');
        setText('rest-remain', '0 h 00 min');
        setText('eta', '—');
        setText('sla-full', '—');
        $('time-status').hidden = true;
        setText('step-badge', '—');
        $('pause-hint').hidden = true;
        setText('next-title', 'Sin siguiente viaje');
        $('next-content').hidden = true;
        $('next-empty').hidden = false;
        resetMap();
        showRouteFallback('Sin ruta activa', 'No hay un viaje activo para calcular la navegación.', false);
        updateUpdatedAt(data);
    }

    function renderTrip(data) {
        const viaje = data.viaje;
        const jornada = data.jornada || {};
        $('trip-content').hidden = false;
        $('alert-button').disabled = false;

        setText('trip-id', `#${viaje.id}`);
        setText('route-origin', viaje.origen);
        setText('route-destination', viaje.destino);
        if ($('route-arrow')) {
            $('route-arrow').hidden = false;
        }
        if ($('route-destination')) {
            $('route-destination').hidden = false;
        }
        setText('cargo-number', viaje.numeroCarga);
        setText('unit-number', viaje.numeroUnidad);
        setText('legend-origin', placeLabel(viaje.origen, viaje.departamentoOrigen, viaje.paisOrigen));
        setText('legend-destination', placeLabel(viaje.destino, viaje.departamentoDestino, viaje.paisDestino));

        const status = $('trip-status');
        status.textContent = ESTADOS[viaje.estado] || viaje.estado || 'Sin estado';
        status.dataset.status = viaje.estado || '';

        const nivel = SEMAFORO_NIVEL[jornada.semaforo] || 'ok';
        $('time-status').hidden = false;
        $('time-status').dataset.level = nivel;
        setText('time-status', jornada.semaforoEtiqueta || 'Sin datos');
        setText('step-badge', jornada.pasoEtiqueta || 'Sin iniciar');
        habilitarAcciones(jornada);

        const porcentaje = Number.isFinite(Number(jornada.porcentajeManejo))
            ? Math.max(0, Number(jornada.porcentajeManejo)) : 0;
        setText('drive-percent', `${porcentaje} %`);
        $('drive-progress').style.width = `${Math.min(100, porcentaje)}%`;
        setText('drive-time', jornada.manejoTexto || '0 h 00 min');
        setText('drive-max', viaje.horasMaxConduccion
            ? `Máximo ${formatNumber(viaje.horasMaxConduccion)} h` : 'Sin límite definido');
        setText('rest-time', jornada.descansoTexto || '0 h 00 min');
        setText('rest-remain', jornada.descansoRestanteTexto || '0 h 00 min');
        setText('eta', formatShortDate(jornada.llegadaProyectada));
        setText('sla-full', formatDate(viaje.fechaLimite));
        $('pause-hint').hidden = jornada.paso !== 'EN_PAUSA';

        renderNext(data.siguiente);
        updateMap(viaje);
        updateUpdatedAt(data);
    }

    function renderNext(siguiente) {
        const valido = siguiente && !['FINALIZADO', 'CANCELADO'].includes(siguiente.estado);
        $('next-content').hidden = !valido;
        $('next-empty').hidden = !!valido;
        setText('next-title', valido ? `Carga ${siguiente.numeroCarga}` : 'Sin siguiente viaje');
        if (!valido) {
            return;
        }
        setText('next-origin', siguiente.origen);
        setText('next-destination', siguiente.destino);
        setText('next-cargo', siguiente.numeroCarga);
        setText('next-unit', siguiente.numeroUnidad);
        setText('next-sla', formatDate(siguiente.fechaLimite));
    }

    function updateUpdatedAt(data) {
        const actualizado = data?.actualizado;
        const hora = actualizado ? formatTime(actualizado) : '—';
        setText('last-update', `Actualizado ${hora}`);
    }

    async function updateMap(viaje) {
        const ruta = `${viaje.origen || 'Origen'}  →  ${viaje.destino || 'Destino'}`;
        setText('route-fallback-route', ruta);

        const directions = buildDirectionsUrl(viaje);
        setNavigationLink('open-navigation', directions);
        setNavigationLink('fallback-navigation', directions);

        const origen = coordinatePair(viaje.latitudOrigen, viaje.longitudOrigen);
        const destino = coordinatePair(viaje.latitudDestino, viaje.longitudDestino);
        const signature = `${viaje.id}:${viaje.latitudOrigen},${viaje.longitudOrigen},${viaje.latitudDestino},${viaje.longitudDestino}`;

        if (state.routeSignature === signature) {
            return;
        }
        state.routeSignature = signature;
        state.route = null;
        state.currentStep = 0;
        stopNavigation();        clearSteps();
        $('nav-start').disabled = true;
        setText('nav-total', '—');
        setText('nav-instruction', 'Calculando la ruta…');
        setText('nav-step-distance', '—');
        showRouteFallback('Calculando ruta…', 'Estamos consultando OpenRouteService para obtener la ruta y las indicaciones.', false);
        ensureNavMap();
        $('nav-start').disabled = !(origen && destino);
        drawRouteBase(viaje, origen, destino);

        if (!origen || !destino) {
            showRouteFallback('Faltan coordenadas',
                'El viaje no tiene coordenadas completas de origen y destino. Puedes abrirlo en Google Maps.', true);
            return;
        }

        try {
            const respuesta = await api('/operador/ruta');
            state.route = respuesta;
            drawRoute(respuesta, origen, destino);
            renderSteps(respuesta);
            hideRouteFallback();
            $('nav-start').disabled = false;
        } catch (error) {
            if (error.status === 401) {
                clearSession();
                showLogin('Tu sesión expiró. Inicia sesión nuevamente.');
                return;
            }
            setText('nav-instruction', 'Navegación no disponible');
            setText('nav-step-distance', '—');
            showRouteFallback('Ruta no disponible',
                error.message || 'No se pudo calcular la ruta. Puedes abrir Google Maps mientras tanto.', true);
        }
    }

    function ensureNavMap() {
        if (state.map) {
            return;
        }
        state.map = new maplibregl.Map({
            container: 'nav-map',
            style: 'https://tiles.openfreemap.org/styles/liberty',
            center: [-85.5, 12.8],
            zoom: 7,
            pitch: 0,
            bearing: 0,
            antialias: true,
            renderWorldCopies: false,
            minZoom: 1.5
        });
        state.map.on('moveend', () => {
            if (!state.map) {
                return;
            }
            const centro = state.map.getCenter();
            const lng = Math.max(-180, Math.min(180, centro.lng));
            const lat = Math.max(-85, Math.min(85, centro.lat));
            if (Math.abs(centro.lng - lng) > 1e-6 || Math.abs(centro.lat - lat) > 1e-6) {
                state.map.panTo([lng, lat], { duration: 0 });
            }
        });
        state.map.addControl(new maplibregl.NavigationControl({
            visualizePitch: true,
            showCompass: true,
            showZoom: false
        }), 'bottom-right');
        agregarControlesZoom(state.map);
        agregarControlSeguir(state.map);
        state.map.on('dragstart', () => {
            state.followUser = false;
            actualizarBotonSeguir();
        });
        state.map.on('rotatestart', () => {
            state.followUser = false;
            actualizarBotonSeguir();
        });
        state.map.on('wheel', () => {
            state.followUser = false;
            actualizarBotonSeguir();
        });
        const brujula = state.map.getContainer().querySelector('.maplibregl-ctrl-compass');
        if (brujula) {
            brujula.addEventListener('click', () => seguirUsuario());
        }
        if (typeof ResizeObserver === 'function') {
            state.mapResizeObserver = new ResizeObserver(() => {
                if (state.map) {
                    state.map.resize();
                }
            });
            state.mapResizeObserver.observe(document.getElementById('nav-map'));
        }
        state.map.on('load', () => {
            state.mapLoaded = true;
            state.map.setSky({
                'sky-color': '#B8D9F2',
                'sky-horizon-blend': 0.55,
                'horizon-color': '#EAF2F8',
                'horizon-fog-blend': 0.5,
                'fog-color': '#EAF2F8',
                'fog-ground-blend': 0.72
            });
            if (state.route?.puntos?.length) {
                drawRouteLayers(state.route);
            }
        });
    }

    function withMapReady(accion) {
        ensureNavMap();
        if (!state.map) {
            return;
        }
        if (state.mapLoaded) {
            accion();
        } else {
            state.map.once('load', () => {
                if (state.map) {
                    accion();
                }
            });
        }
    }

    function agregarControlesZoom(map) {
        const grupo = document.createElement('div');
        grupo.className = 'maplibregl-ctrl maplibregl-ctrl-group maplibregl-ctrl-zoom-custom';
        const acercar = document.createElement('button');
        acercar.type = 'button';
        acercar.title = 'Acercar';
        acercar.setAttribute('aria-label', 'Acercar');
        acercar.innerHTML = '<svg viewBox="0 0 24 24"><path d="M12 5v14M5 12h14"/></svg>';
        acercar.addEventListener('click', () => {
            map.zoomTo(map.getZoom() + 1.5, { duration: 300 });
            seguirUsuario();
        });
        const alejar = document.createElement('button');
        alejar.type = 'button';
        alejar.title = 'Alejar';
        alejar.setAttribute('aria-label', 'Alejar');
        alejar.innerHTML = '<svg viewBox="0 0 24 24"><path d="M5 12h14"/></svg>';
        alejar.addEventListener('click', () => {
            map.zoomTo(map.getZoom() - 1.5, { duration: 300 });
            seguirUsuario();
        });
        grupo.append(acercar, alejar);
        map.addControl({
            onAdd: () => grupo,
            onRemove: () => grupo.remove()
        }, 'bottom-right');
    }

    function agregarControlSeguir(map) {
        const contenedor = document.createElement('div');
        contenedor.className = 'maplibregl-ctrl maplibregl-ctrl-group maplibregl-ctrl-follow';
        const boton = document.createElement('button');
        boton.type = 'button';
        boton.title = 'Centrar en mi ubicación';
        boton.setAttribute('aria-label', 'Centrar en mi ubicación');
        boton.innerHTML = '<svg viewBox="0 0 24 24"><use href="#i-target"></use></svg>';
        boton.addEventListener('click', () => seguirUsuario());
        contenedor.append(boton);
        map.addControl({
            onAdd: () => contenedor,
            onRemove: () => contenedor.remove()
        }, 'bottom-right');
        state.followButton = boton;
        actualizarBotonSeguir();
    }

    function seguirUsuario() {
        state.followUser = true;
        actualizarBotonSeguir();
        if (!state.lastPosition) {
            toast('Esperando la ubicación del GPS…');
            return;
        }
        if (!state.map) {
            return;
        }
        state.map.easeTo({
            center: [state.lastPosition.lng, state.lastPosition.lat],
            bearing: state.navActive ? (state.lastPosition.rumbo || 0) : state.map.getBearing(),
            pitch: state.navActive ? 58 : state.map.getPitch(),
            duration: 700
        });
    }

    function actualizarBotonSeguir() {
        if (state.followButton) {
            state.followButton.classList.toggle('active', !!state.followUser);
        }
    }

    function drawNavigationRoute(ruta) {
        withMapReady(() => {
            if (!ruta?.puntos?.length || !state.map) {
                return;
            }
            const geoJson = {
                type: 'Feature',
                properties: {},
                geometry: {
                    type: 'LineString',
                    coordinates: ruta.puntos.map(punto => [punto.lng, punto.lat])
                }
            };
            if (!state.map.getSource('ruta-navegacion')) {
                state.map.addSource('ruta-navegacion', { type: 'geojson', data: geoJson });
                state.map.addLayer({
                    id: 'ruta-navegacion-casing',
                    type: 'line',
                    source: 'ruta-navegacion',
                    layout: { 'line-cap': 'round', 'line-join': 'round' },
                    paint: { 'line-color': '#FFFFFF', 'line-width': 11, 'line-opacity': 0.9 }
                });
                state.map.addLayer({
                    id: 'ruta-navegacion-linea',
                    type: 'line',
                    source: 'ruta-navegacion',
                    layout: { 'line-cap': 'round', 'line-join': 'round' },
                    paint: { 'line-color': '#166534', 'line-width': 6, 'line-opacity': 0.97 }
                });
            } else {
                state.map.getSource('ruta-navegacion').setData(geoJson);
            }
            if (state.map.getLayer('ruta-operador-linea')) {
                state.map.setPaintProperty('ruta-operador-linea', 'line-opacity', 0.22);
            }
            if (state.map.getLayer('trayecto-base-linea')) {
                state.map.setLayoutProperty('trayecto-base-linea', 'visibility', 'none');
            }
        });
    }

    function limpiarRutaNavegacion() {
        if (!state.map) {
            return;
        }
        ['ruta-navegacion-linea', 'ruta-navegacion-casing'].forEach(id => {
            if (state.map.getLayer(id)) {
                state.map.removeLayer(id);
            }
        });
        if (state.map.getSource('ruta-navegacion')) {
            state.map.removeSource('ruta-navegacion');
        }
        if (state.map.getLayer('ruta-operador-linea')) {
            state.map.setPaintProperty('ruta-operador-linea', 'line-opacity', 0.95);
        }
        if (state.map.getLayer('trayecto-base-linea')) {
            state.map.setLayoutProperty('trayecto-base-linea', 'visibility', 'visible');
        }
    }

    function mostrarLlegada(titulo, mensaje) {
        setText('nav-arrival-title', titulo);
        setText('nav-arrival-message', mensaje);
        const overlay = $('nav-arrival');
        if (overlay) {
            overlay.hidden = false;
        }
    }

    function ocultarLlegada() {
        const overlay = $('nav-arrival');
        if (overlay) {
            overlay.hidden = true;
        }
    }

    async function recalcularRutaNavegacion(forzar = false) {
        if (!state.navActive || state.recalculating) {
            return;
        }
        const objetivo = state.navPhase === 'hacia-origen' ? 'origen' : 'destino';
        const ahora = Date.now();
        if (!forzar && ahora - state.lastRecalcAt < 20000) {
            return;
        }
        state.recalculating = true;
        state.lastRecalcAt = ahora;
        try {
            const posicion = state.lastPosition
                ? { lat: state.lastPosition.lat, lng: state.lastPosition.lng }
                : await obtenerUbicacionActual();
            if (!state.navActive) {
                return;
            }
            const params = new URLSearchParams({ hasta: objetivo });
            if (posicion) {
                params.set('lat', posicion.lat.toFixed(6));
                params.set('lon', posicion.lng.toFixed(6));
            }
            const respuesta = await api('/operador/ruta?' + params.toString());
            if (!state.navActive) {
                return;
            }
            state.route = respuesta;
            state.routeTarget = objetivo;
            drawNavigationRoute(respuesta);
            renderSteps(respuesta);
            hideRouteFallback();
        } catch (error) {
            if (error.status === 401) {
                clearSession();
                showLogin('Tu sesión expiró. Inicia sesión nuevamente.');
                return;
            }
            toast(error.message || 'No se pudo calcular la ruta de navegación.', true);
        } finally {
            state.recalculating = false;
        }
    }

    function manejarLlegada(objetivo) {
        if (objetivo === 'origen' && !state.arrivedOrigin) {
            state.arrivedOrigin = true;
            const nombre = state.panel?.viaje?.origen || 'el punto A';
            mostrarLlegada('Has llegado al punto A',
                `Estás en ${nombre}. Ya puedes iniciar el trayecto y comenzar a contar las horas de manejo.`);
            speakInstruction('Has llegado al punto de origen. Ya puedes iniciar el trayecto.');
            habilitarAcciones(state.panel?.jornada);
            return;
        }
        if (objetivo === 'destino' && !state.arrivedDestination) {
            state.arrivedDestination = true;
            const quien = state.panel?.operador?.nombre || 'El operador';
            mostrarLlegada(`${quien} ha llegado a su destino`,
                'Confirma la entrega para cerrar el viaje.');
            speakInstruction(`${quien} ha llegado a su destino.`);
        }
    }

    function drawRouteBase(viaje, origen, destino) {
        withMapReady(() => {
            state.routeMarkers.forEach(marker => marker.remove());
            state.routeMarkers = [];
            if (origen) {
                state.routeMarkers.push(createNodeMarker(viaje.origen || 'Origen', 'origin', origen));
            }
            if (destino) {
                state.routeMarkers.push(createNodeMarker(viaje.destino || 'Destino', 'destination', destino));
            }
            state.map.resize();
            if (origen && destino) {
                const bounds = new maplibregl.LngLatBounds();
                bounds.extend(origen);
                bounds.extend(destino);
                state.map.fitBounds(bounds, { padding: 55, maxZoom: 13, duration: 0 });
                state.map.easeTo({ pitch: 45, bearing: 0, duration: 0 });
            } else if (origen || destino) {
                state.map.easeTo({
                    center: origen || destino,
                    zoom: 12,
                    pitch: 45,
                    bearing: 0,
                    duration: 0
                });
            }
        });
        refrescarTrayecto();
    }

    function createNodeMarker(nombre, clase, lngLat) {
        const elemento = document.createElement('div');
        elemento.className = `map-node-marker ${clase}`.trim();
        const etiqueta = document.createElement('span');
        etiqueta.textContent = clase === 'destination' ? 'B' : 'A';
        elemento.append(etiqueta);
        elemento.title = nombre;
        return new maplibregl.Marker({ element: elemento, anchor: 'bottom' })
            .setLngLat(lngLat)
            .addTo(state.map);
    }

    function drawRoute(ruta, origen, destino) {
        withMapReady(() => {
            drawRouteLayers(ruta);
            if (ruta.puntos?.length) {
                const bounds = new maplibregl.LngLatBounds();
                ruta.puntos.forEach(punto => bounds.extend([punto.lng, punto.lat]));
                state.map.fitBounds(bounds, { padding: 55, maxZoom: 14, duration: 0 });
                state.map.easeTo({ pitch: 45, bearing: 0, duration: 0 });
            } else if (origen && destino) {
                const bounds = new maplibregl.LngLatBounds();
                bounds.extend(origen);
                bounds.extend(destino);
                state.map.fitBounds(bounds, { padding: 55, maxZoom: 13, duration: 0 });
                state.map.easeTo({ pitch: 45, bearing: 0, duration: 0 });
            }
        });
        refrescarTrayecto();
    }

    async function refrescarTrayecto() {
        if (!state.map || !state.mapLoaded || state.navActive) {
            return;
        }
        const origen = origenViaje();
        if (!origen) {
            return;
        }
        const posicion = state.lastPosition
            ? { lat: state.lastPosition.lat, lng: state.lastPosition.lng }
            : await obtenerUbicacionActual();
        if (!posicion || !state.map || state.navActive) {
            return;
        }
        state.lastPosition = { lat: posicion.lat, lng: posicion.lng, rumbo: state.lastPosition?.rumbo || 0 };
        ensureUserMarker(posicion.lng, posicion.lat, state.lastPosition.rumbo);
        const geojson = {
            type: 'Feature',
            properties: {},
            geometry: {
                type: 'LineString',
                coordinates: [[posicion.lng, posicion.lat], origen]
            }
        };
        if (state.map.getSource('trayecto-base')) {
            state.map.getSource('trayecto-base').setData(geojson);
            if (state.map.getLayer('trayecto-base-linea')) {
                state.map.setLayoutProperty('trayecto-base-linea', 'visibility', 'visible');
            }
            return;
        }
        state.map.addSource('trayecto-base', { type: 'geojson', data: geojson });
        state.map.addLayer({
            id: 'trayecto-base-linea',
            type: 'line',
            source: 'trayecto-base',
            layout: { 'line-cap': 'round', 'line-join': 'round' },
            paint: {
                'line-color': '#0E7490',
                'line-width': 3,
                'line-opacity': 0.75,
                'line-dasharray': [1.2, 1.6]
            }
        });
    }

    function drawRouteLayers(ruta) {
        if (!state.map || !ruta?.puntos?.length) {
            return;
        }
        const geoJson = {
            type: 'Feature',
            properties: {},
            geometry: {
                type: 'LineString',
                coordinates: ruta.puntos.map(punto => [punto.lng, punto.lat])
            }
        };
        if (state.map.getSource('ruta-operador')) {
            state.map.getSource('ruta-operador').setData(geoJson);
            return;
        }
        state.map.addSource('ruta-operador', { type: 'geojson', data: geoJson });
        state.map.addLayer({
            id: 'ruta-operador-casing',
            type: 'line',
            source: 'ruta-operador',
            layout: { 'line-cap': 'round', 'line-join': 'round' },
            paint: { 'line-color': '#FFFFFF', 'line-width': 10, 'line-opacity': 0.9 }
        });
        state.map.addLayer({
            id: 'ruta-operador-linea',
            type: 'line',
            source: 'ruta-operador',
            layout: { 'line-cap': 'round', 'line-join': 'round' },
            paint: { 'line-color': '#0E7490', 'line-width': 5, 'line-opacity': 0.95 }
        });
    }

    function renderSteps(ruta) {
        const pasos = Array.isArray(ruta.pasos) ? ruta.pasos : [];
        setText('nav-total', `${formatDistance(ruta.distanciaMetros)} · ${formatDuration(ruta.duracionSegundos)}`);
        setText('nav-step-count', pasos.length ? `Paso 1 de ${pasos.length}` : 'Sin indicaciones');

        if (pasos.length) {
            setCurrentStep(0, false);
        } else {
            setText('nav-instruction', 'Sin indicaciones de giro');
            setText('nav-step-distance', '—');
        }
    }

    function clearSteps() {
        setText('nav-step-count', '—');
    }

    function setCurrentStep(index, speak) {
        const pasos = state.route?.pasos || [];
        if (!pasos.length) {
            return;
        }
        const actual = Math.max(0, Math.min(index, pasos.length - 1));
        state.currentStep = actual;
        const paso = pasos[actual];
        setText('nav-instruction', paso.instruccion || 'Continúa por la ruta');
        setText('nav-step-distance', formatDistance(paso.distanciaMetros));
        setText('nav-step-count', `Paso ${actual + 1} de ${pasos.length}`);
        if (speak) {
            speakInstruction(paso.instruccion);
        }
    }

    function toggleNavigation() {
        if (state.navActive) {
            stopNavigation();
            if (state.panel?.viaje) {
                state.routeSignature = null;
                updateMap(state.panel.viaje);
            }
            return;
        }
        startNavigation();
    }

    function startNavigation() {
        if (!('geolocation' in navigator)) {
            toast('Este navegador no puede acceder a la ubicación.', true);
            return;
        }
        if (!window.isSecureContext) {
            toast('La navegación necesita HTTPS o localhost para usar el GPS.', true);
            return;
        }
        if (!origenViaje() || !destinoViaje()) {
            toast('El viaje no tiene coordenadas completas de origen y destino.', true);
            return;
        }
        stopNavigation();
        state.navActive = true;
        state.followUser = true;
        actualizarBotonSeguir();
        state.navPhase = state.panel?.jornada?.paso === 'SIN_INICIAR' ? 'hacia-origen' : 'hacia-destino';
        state.routeTarget = null;
        state.arrivedOrigin = false;
        state.arrivedDestination = false;
        state.lastRecalcAt = 0;
        ocultarLlegada();
        updateNavButton();
        if (state.map) {
            state.map.easeTo({ pitch: 58, zoom: Math.max(state.map.getZoom(), 16.5), duration: 800 });
        }
        state.geoWatch = navigator.geolocation.watchPosition(onPosition, onGeoError, {
            enableHighAccuracy: true,
            maximumAge: 1000,
            timeout: 12000
        });
        recalcularRutaNavegacion(true);
    }

    function stopNavigation() {
        if (state.geoWatch !== null && navigator.geolocation) {
            navigator.geolocation.clearWatch(state.geoWatch);
        }
        state.geoWatch = null;
        state.navActive = false;
        state.navPhase = null;
        state.routeTarget = null;
        state.arrivedOrigin = false;
        state.arrivedDestination = false;
        state.followUser = false;
        state.lastPosition = null;
        ocultarLlegada();
        actualizarBotonSeguir();
        limpiarRutaNavegacion();
        if (state.userMarker) {
            state.userMarker.remove();
            state.userMarker = null;
        }
        if (state.map) {
            state.map.easeTo({ pitch: 45, bearing: 0, duration: 500 });
        }
        updateNavButton();
    }

    function updateNavButton() {
        const btn = $('nav-start');
        if (!btn) {
            return;
        }
        const label = btn.querySelector('span');
        if (label) {
            label.textContent = state.navActive ? 'Detener navegación' : 'Iniciar navegación';
        }
    }

    function onPosition(position) {
        if (!state.navActive) {
            return;
        }
        const lat = position.coords.latitude;
        const lng = position.coords.longitude;
        const anterior = state.lastPosition;
        let rumbo = 0;
        if (anterior) {
            const recorrido = distanceMeters(anterior.lat, anterior.lng, lat, lng);
            if (recorrido > 2) {
                rumbo = bearingBetween(anterior.lat, anterior.lng, lat, lng);
            }
        }
        if (!anterior && state.route?.puntos?.length) {
            const cercanoInicial = nearestRoutePoint(lat, lng, state.route.puntos || []);
            if (cercanoInicial) {
                const siguiente = state.route.puntos[Math.min(cercanoInicial.indice + 2, state.route.puntos.length - 1)];
                if (siguiente) {
                    rumbo = bearingBetween(lat, lng, siguiente.lat, siguiente.lng);
                }
            }
        }
        if (anterior && anterior.rumbo != null && rumbo === 0) {
            rumbo = anterior.rumbo;
        }

        ensureUserMarker(lng, lat, rumbo);
        state.lastPosition = { lat, lng, rumbo };
        if (state.followUser && state.map) {
            state.map.easeTo({
                center: [lng, lat],
                bearing: rumbo,
                pitch: 58,
                duration: 600
            });
        }

        const objetivo = state.navPhase === 'hacia-origen' ? origenViaje() : destinoViaje();
        if (objetivo && distanceMeters(lat, lng, objetivo[1], objetivo[0]) < 90) {
            manejarLlegada(state.navPhase === 'hacia-origen' ? 'origen' : 'destino');
        }

        const objetivoActual = state.navPhase === 'hacia-origen' ? 'origen' : 'destino';
        const llegadaActual = objetivoActual === 'origen' ? state.arrivedOrigin : state.arrivedDestination;
        if (llegadaActual) {
            return;
        }
        if (!state.route || state.routeTarget !== objetivoActual) {
            recalcularRutaNavegacion();
            return;
        }
        const cercano = nearestRoutePoint(lat, lng, state.route.puntos || []);
        if (cercano) {
            let paso = state.route.pasos.findIndex(p => cercano.indice >= p.inicio && cercano.indice <= p.fin);
            if (paso < 0) {
                paso = state.route.pasos.reduce((mejor, p, i) =>
                    p.inicio <= cercano.indice && p.inicio > (state.route.pasos[mejor]?.inicio ?? -1) ? i : mejor, 0);
            }
            setCurrentStep(paso, paso !== state.currentStep);
            if (cercano.distanciaMetros > 90) {
                recalcularRutaNavegacion();
            }
        }
    }

    function ensureUserMarker(lng, lat, rumbo) {
        if (!state.map) {
            return;
        }
        if (!state.userMarker) {
            const elemento = document.createElement('div');
            elemento.className = 'nav-user-arrow';
            elemento.textContent = '^';
            state.userMarker = new maplibregl.Marker({
                element: elemento,
                anchor: 'center',
                rotationAlignment: 'map',
                pitchAlignment: 'map'
            }).setLngLat([lng, lat]).addTo(state.map);
        } else {
            state.userMarker.setLngLat([lng, lat]);
        }
        state.userMarker.setRotation(rumbo || 0);
    }

    function bearingBetween(lat1, lng1, lat2, lng2) {
        const rad = Math.PI / 180;
        const y = Math.sin((lng2 - lng1) * rad) * Math.cos(lat2 * rad);
        const x = Math.cos(lat1 * rad) * Math.sin(lat2 * rad)
            - Math.sin(lat1 * rad) * Math.cos(lat2 * rad) * Math.cos((lng2 - lng1) * rad);
        return (Math.atan2(y, x) * 180 / Math.PI + 360) % 360;
    }

    function onGeoError(error) {
        const mensaje = error.code === error.PERMISSION_DENIED
            ? 'Permiso de ubicación denegado.'
            : error.code === error.POSITION_UNAVAILABLE
                ? 'Ubicación no disponible.'
                : 'No se pudo obtener la ubicación.';
        toast(mensaje, true);
        stopNavigation();
    }

    function nearestRoutePoint(lat, lng, puntos) {
        let indice = -1;
        let distancia = Infinity;
        puntos.forEach((punto, i) => {
            const d = distanceMeters(lat, lng, punto.lat, punto.lng);
            if (d < distancia) {
                distancia = d;
                indice = i;
            }
        });
        return indice < 0 ? null : { indice, distanciaMetros: distancia };
    }

    function distanceMeters(lat1, lng1, lat2, lng2) {
        const radio = 6371000;
        const rad = Math.PI / 180;
        const dLat = (lat2 - lat1) * rad;
        const dLng = (lng2 - lng1) * rad;
        const a = Math.sin(dLat / 2) ** 2
            + Math.cos(lat1 * rad) * Math.cos(lat2 * rad) * Math.sin(dLng / 2) ** 2;
        return 2 * radio * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }

    function toggleVoice() {
        state.voiceEnabled = !state.voiceEnabled;
        const btn = $('nav-voice');
        btn.setAttribute('aria-pressed', String(state.voiceEnabled));
        btn.querySelector('span').textContent = state.voiceEnabled ? 'Voz activada' : 'Voz desactivada';
        if (!state.voiceEnabled && window.speechSynthesis) {
            window.speechSynthesis.cancel();
        }
    }

    function speakInstruction(texto) {
        if (!state.voiceEnabled || !texto || !window.speechSynthesis) {
            return;
        }
        const mensaje = new SpeechSynthesisUtterance(texto);
        mensaje.lang = 'es-NI';
        mensaje.rate = 1;
        window.speechSynthesis.cancel();
        window.speechSynthesis.speak(mensaje);
    }

    function showRouteFallback(titulo, mensaje, mostrarEnlace) {
        const fallback = $('route-fallback');
        if (!fallback) {
            return;
        }
        setText('route-fallback-title', titulo);
        setText('route-fallback-message', mensaje);
        fallback.hidden = false;
        const enlace = $('fallback-navigation');
        if (enlace) {
            enlace.hidden = !mostrarEnlace;
        }
        if (!state.route) {
            clearSteps();
        }
    }

    function hideRouteFallback() {
        if ($('route-fallback')) {
            $('route-fallback').hidden = true;
        }
    }

    function buildDirectionsUrl(viaje) {
        const origen = routePoint(viaje.latitudOrigen, viaje.longitudOrigen,
            viaje.origen, viaje.departamentoOrigen, viaje.paisOrigen);
        const destino = routePoint(viaje.latitudDestino, viaje.longitudDestino,
            viaje.destino, viaje.departamentoDestino, viaje.paisDestino);
        if (!origen || !destino) {
            return null;
        }
        const params = new URLSearchParams({
            api: '1',
            origin: origen,
            destination: destino,
            travelmode: 'driving',
            dir_action: 'navigate'
        });
        return `https://www.google.com/maps/dir/?${params.toString()}`;
    }

    function routePoint(lat, lng, nombre, departamento, pais) {
        const la = Number(lat);
        const ln = Number(lng);
        if (Number.isFinite(la) && Number.isFinite(ln)
                && la >= -90 && la <= 90 && ln >= -180 && ln <= 180) {
            return `${la},${ln}`;
        }
        const partes = [nombre, departamento, pais].filter(valor => valor && String(valor).trim());
        return partes.length ? partes.join(', ') : null;
    }

    function coordinatePair(lat, lng) {
        const la = Number(lat);
        const ln = Number(lng);
        if (!Number.isFinite(la) || !Number.isFinite(ln) || la < -90 || la > 90 || ln < -180 || ln > 180) {
            return null;
        }
        return [ln, la];
    }

    function setNavigationLink(id, url) {
        const enlace = $(id);
        if (!enlace) {
            return;
        }
        if (url) {
            enlace.href = url;
            enlace.classList.remove('is-disabled');
            enlace.removeAttribute('aria-disabled');
            enlace.removeAttribute('tabindex');
        } else {
            enlace.href = '#';
            enlace.classList.add('is-disabled');
            enlace.setAttribute('aria-disabled', 'true');
            enlace.tabIndex = -1;
        }
    }

    function resetMap() {
        stopNavigation();
        if (window.speechSynthesis) {
            window.speechSynthesis.cancel();
        }
        state.route = null;
        state.routeSignature = null;
        state.currentStep = 0;
        destroyNavMap();
        hideRouteFallback();
        setText('route-fallback-route', '—');
        setText('legend-origin', '—');
        setText('legend-destination', '—');
        setText('nav-instruction', 'Calcula la ruta para comenzar.');
        setText('nav-step-distance', '—');
        setText('nav-total', '—');
        setText('nav-step-count', '—');
        clearSteps();
        $('nav-start').disabled = true;
        setNavigationLink('open-navigation', null);
        setNavigationLink('fallback-navigation', null);
    }

    function destroyNavMap() {
        state.routeMarkers.forEach(marker => marker.remove());
        state.routeMarkers = [];
        if (state.userMarker) {
            state.userMarker.remove();
            state.userMarker = null;
        }
        if (state.mapResizeObserver) {
            state.mapResizeObserver.disconnect();
            state.mapResizeObserver = null;
        }
        if (state.map) {
            state.map.remove();
            state.map = null;
        }
        state.mapLoaded = false;
        state.followButton = null;
        state.lastPosition = null;
    }

    function formatDistance(metros) {
        const valor = Number(metros);
        if (!Number.isFinite(valor) || valor < 0) {
            return '—';
        }
        return valor < 1000
            ? `${Math.round(valor)} m`
            : `${(valor / 1000).toFixed(1)} km`;
    }

    function formatDuration(segundos) {
        const valor = Number(segundos);
        if (!Number.isFinite(valor) || valor < 0) {
            return '—';
        }
        const minutos = Math.round(valor / 60);
        if (minutos < 60) {
            return `${minutos} min`;
        }
        return `${Math.floor(minutos / 60)} h ${String(minutos % 60).padStart(2, '0')} min`;
    }

    function placeLabel(nombre, departamento, pais) {
        return [nombre, departamento, pais].filter(valor => valor && String(valor).trim()).join(', ');
    }

    function setText(id, value) {
        const elemento = $(id);
        if (elemento) {
            elemento.textContent = value == null || value === '' ? '—' : String(value);
        }
    }

    function setLoginLoading(loading) {
        const button = $('login-submit');
        button.disabled = loading;
        button.classList.toggle('is-loading', loading);
        button.querySelector('.btn-label').textContent = loading ? 'Ingresando…' : 'Ingresar';
        button.querySelector('.spinner').hidden = !loading;
        button.querySelector('.btn-arrow').hidden = loading;
    }

    function showLoginError(message, info = false) {
        const error = $('login-error');
        error.textContent = message;
        error.classList.toggle('info', info);
        error.hidden = false;
    }

    function hideLoginError() {
        const error = $('login-error');
        error.textContent = '';
        error.classList.remove('info');
        error.hidden = true;
    }

    function togglePassword() {
        const input = $('password');
        const button = $('toggle-password');
        const visible = input.type === 'text';
        input.type = visible ? 'password' : 'text';
        button.setAttribute('aria-pressed', String(!visible));
        button.setAttribute('aria-label', visible ? 'Mostrar contraseña' : 'Ocultar contraseña');
        $('toggle-password-icon').setAttribute('href', visible ? '#i-eye' : '#i-eye-off');
        input.focus();
    }

    function toast(message, error = false) {
        const elemento = $('toast');
        elemento.textContent = message;
        elemento.classList.toggle('error', error);
        elemento.classList.add('visible');
        window.clearTimeout(state.toastTimer);
        state.toastTimer = window.setTimeout(() => elemento.classList.remove('visible'), 4500);
    }

    async function api(path, { method = 'GET', body, auth = true } = {}) {
        const headers = { 'Accept': 'application/json' };
        if (body !== undefined) {
            headers['Content-Type'] = 'application/json';
        }
        if (auth && state.token) {
            headers['Authorization'] = `Bearer ${state.token}`;
        }
        const response = await fetch(`${API}${path}`, {
            method,
            headers,
            body: body === undefined ? undefined : JSON.stringify(body),
            credentials: 'same-origin'
        });
        if (response.status === 204) {
            return null;
        }
        let data = null;
        try {
            data = await response.json();
        } catch (ignored) {
            data = null;
        }
        if (!response.ok) {
            const error = new Error(data?.mensaje || 'No se pudo completar la solicitud.');
            error.status = response.status;
            throw error;
        }
        return data;
    }

    function formatDate(value, options = {}) {
        if (!value) {
            return '—';
        }
        const fecha = new Date(value);
        if (Number.isNaN(fecha.getTime())) {
            return '—';
        }
        try {
            return new Intl.DateTimeFormat('es-NI', {
                day: '2-digit',
                month: 'short',
                hour: '2-digit',
                minute: '2-digit',
                hourCycle: 'h23',
                timeZone: zonaHoraria(),
                ...options
            }).format(fecha);
        } catch (ignored) {
            return '—';
        }
    }

    function formatShortDate(value) {
        return formatDate(value);
    }

    function formatTime(value) {
        if (!value) {
            return '—';
        }
        const fecha = new Date(value);
        if (Number.isNaN(fecha.getTime())) {
            return '—';
        }
        try {
            return new Intl.DateTimeFormat('es-NI', {
                hour: '2-digit',
                minute: '2-digit',
                second: '2-digit',
                hourCycle: 'h23',
                timeZone: zonaHoraria()
            }).format(fecha);
        } catch (ignored) {
            return '—';
        }
    }

    function formatNumber(value) {
        const numero = Number(value);
        if (!Number.isFinite(numero)) {
            return '—';
        }
        return new Intl.NumberFormat('es-NI', {
            maximumFractionDigits: 2
        }).format(numero);
    }

    function zonaHoraria() {
        return state.panel?.empresa?.zonaHoraria || 'America/Managua';
    }

    function parseJson(value) {
        if (!value) {
            return null;
        }
        try {
            return JSON.parse(value);
        } catch (ignored) {
            return null;
        }
    }

    function storageGet(key) {
        try {
            return window.sessionStorage.getItem(key);
        } catch (ignored) {
            return null;
        }
    }

    function storageSet(key, value) {
        try {
            window.sessionStorage.setItem(key, value);
        } catch (ignored) {
            // La sesión funciona en memoria si el navegador bloquea sessionStorage.
        }
    }

    function storageRemove(key) {
        try {
            window.sessionStorage.removeItem(key);
        } catch (ignored) {
            // Sin acción.
        }
    }
})();
