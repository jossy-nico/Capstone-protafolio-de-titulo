
import { useEffect, useMemo, useState } from 'react'
import {
    FiEye,
    FiDownload,
    FiRefreshCw,
    FiSearch,
    FiX,
    FiShoppingCart,
    FiTruck,
    FiCheckCircle,
    FiExternalLink
} from 'react-icons/fi'

// ==========================================
// CONFIGURACIÓN API
// ==========================================

const API_VENTAS = 'http://localhost:8383/api/ventas'

const formatoMoneda = new Intl.NumberFormat('es-CL', {
    style: 'currency',
    currency: 'CLP',
    maximumFractionDigits: 0
})

const NOMBRES_ESTADOS = {
    PENDIENTE_PAGO: 'Pendiente de pago',
    PAGADO: 'Pagado',
    EN_PREPARACION: 'En preparación',
    ENVIADO: 'Enviado',
    ENTREGADO: 'Entregado',
    CANCELADO: 'Cancelado'
}

// ENVIADO se gestiona por el endpoint de despacho.
const SIGUIENTE_ESTADO = {
    PAGADO: 'EN_PREPARACION',
    ENVIADO: 'ENTREGADO'
}

// ==========================================
// FUNCIONES AUXILIARES
// ==========================================

function formatearDinero(valor) {
    const numero = Number(valor)

    return formatoMoneda.format(
        Number.isFinite(numero) ? numero : 0
    )
}

function formatearFecha(fecha) {
    if (!fecha) return 'Sin registrar'

    const valor = Array.isArray(fecha)
        ? new Date(
            fecha[0],
            fecha[1] - 1,
            fecha[2],
            fecha[3] || 0,
            fecha[4] || 0,
            fecha[5] || 0
        )
        : new Date(fecha)

    return Number.isNaN(valor.getTime())
        ? String(fecha)
        : valor.toLocaleString('es-CL')
}

function obtenerCliente(venta) {
    const nombreInvitado = [
        venta.nombreInvitado,
        venta.apellidoInvitado
    ].filter(Boolean).join(' ')

    if (nombreInvitado) return nombreInvitado

    if (venta.clienteId != null) {
        return `Cliente #${venta.clienteId}`
    }

    return 'Sin identificar'
}

function nombreEstado(estado) {
    return NOMBRES_ESTADOS[estado] || 'Sin estado'
}

function colorEstado(estado) {
    switch (estado) {
        case 'PAGADO':
        case 'ENTREGADO':
            return '#198754'

        case 'EN_PREPARACION':
            return '#b7791f'

        case 'ENVIADO':
            return '#2563eb'

        case 'CANCELADO':
            return '#dc3545'

        default:
            return '#64748b'
    }
}

function etiquetaEstado(estado) {
    const color = colorEstado(estado)

    return (
        <span
            style={{
                display: 'inline-block',
                padding: '6px 11px',
                borderRadius: 20,
                border: `1px solid ${color}`,
                color,
                fontSize: 12,
                fontWeight: 600,
                whiteSpace: 'nowrap'
            }}
        >
            {nombreEstado(estado)}
        </span>
    )
}


async function consultarJson(
    url,
    opciones = {},
    token,
    onSesionExpirada
) {
    const headers = new Headers(opciones.headers || {})

    headers.set('Authorization', `Bearer ${token}`)

    const respuesta = await fetch(url, {
        ...opciones,
        headers
    })

    if (respuesta.status === 401) {
        onSesionExpirada?.()
        throw new Error(
            'Tu sesión venció. Inicia sesión nuevamente.'
        )
    }

    if (respuesta.status === 403) {
        throw new Error(
            'No tienes permisos para realizar esta operación.'
        )
    }

    if (!respuesta.ok) {
        let mensaje = `Error HTTP ${respuesta.status}`

        try {
            const contenido = await respuesta.json()
            mensaje = contenido.error || mensaje
        } catch {
            // Mantener el mensaje HTTP original.
        }

        throw new Error(mensaje)
    }

    return respuesta.json()
}


function obtenerUrlSegura(url) {
    if (!url) return null

    try {
        const resultado = new URL(url)

        if (resultado.protocol !== 'https:') {
            return null
        }

        if (
            resultado.username ||
            resultado.password
        ) {
            return null
        }

        return resultado.href
    } catch {
        return null
    }
}

// ==========================================
// COMPONENTE PRINCIPAL
// ==========================================

function Ventas({
    token,
    onSesionExpirada,
    modo = 'ventas'
}) {

    const [ventas, setVentas] = useState([])
    const [busqueda, setBusqueda] = useState('')
    const [cargando, setCargando] = useState(true)
    const [error, setError] = useState('')
    const [mensaje, setMensaje] = useState('')

    const [
        ventaSeleccionada,
        setVentaSeleccionada
    ] = useState(null)

    const [detalles, setDetalles] = useState([])
    const [
        cargandoDetalles,
        setCargandoDetalles
    ] = useState(false)

    const [descargandoId, setDescargandoId] =
        useState(null)

    const [
        actualizandoEstado,
        setActualizandoEstado
    ] = useState(false)

    // ==========================================
    // HU-32: FORMULARIO DE DESPACHO
    // ==========================================

    const [tipoDespacho, setTipoDespacho] =
        useState('PROPIO')

    const [transportista, setTransportista] =
        useState('')

    const [
        numeroSeguimiento,
        setNumeroSeguimiento
    ] = useState('')

    const [urlSeguimiento, setUrlSeguimiento] =
        useState('')

    function limpiarFormularioDespacho() {
        setTipoDespacho('PROPIO')
        setTransportista('')
        setNumeroSeguimiento('')
        setUrlSeguimiento('')
    }

    // ==========================================
    // CARGAR VENTAS
    // ==========================================

    async function cargarVentas() {
        setCargando(true)
        setError('')

        try {

            const resultado = await consultarJson(
                API_VENTAS,
                {},
                token,
                onSesionExpirada
            )


            if (!Array.isArray(resultado)) {
                throw new Error(
                    'La API no devolvió una lista de ventas.'
                )
            }

            setVentas(resultado)

            setVentaSeleccionada((actual) => {
                if (!actual) return null

                return resultado.find(
                    (venta) =>
                        venta.idVenta === actual.idVenta
                ) || actual
            })

        } catch (err) {
            console.error(err)

            setError(
                'No fue posible cargar las ventas. ' +
                err.message
            )
        } finally {
            setCargando(false)
        }
    }

    useEffect(() => {
        cargarVentas()
    }, [])

    // ==========================================
    // BUSCAR Y ORDENAR
    // ==========================================

    const ventasFiltradas = useMemo(() => {
        const texto = busqueda.trim().toLowerCase()

        const resultado = ventas.filter((venta) => {
            if (!texto) return true

            return [
                venta.idVenta,
                venta.clienteId,
                venta.nombreInvitado,
                venta.apellidoInvitado,
                venta.correoInvitado,
                venta.fechaVenta,
                venta.estadoPedido,
                nombreEstado(venta.estadoPedido),
                venta.transportista
            ].some((valor) =>
                String(valor ?? '')
                    .toLowerCase()
                    .includes(texto)
            )
        })

        return [...resultado].sort((a, b) => {
            const fechaA =
                new Date(a.fechaVenta || 0).getTime() || 0

            const fechaB =
                new Date(b.fechaVenta || 0).getTime() || 0

            return fechaB - fechaA
        })

    }, [ventas, busqueda])

    // ==========================================
    // CONSULTAR DETALLE
    // ==========================================

    async function verDetalle(venta) {
        setVentaSeleccionada(venta)
        setDetalles([])
        setCargandoDetalles(true)
        setError('')
        setMensaje('')

        limpiarFormularioDespacho()

        try {

            const resultado = await consultarJson(
                `${API_VENTAS}/${venta.idVenta}/detalles`,
                {},
                token,
                onSesionExpirada
            )


            if (!Array.isArray(resultado)) {
                throw new Error(
                    'Formato de detalles no válido.'
                )
            }

            setDetalles(resultado)

        } catch (err) {
            console.error(err)

            setError(
                'No fue posible consultar el detalle: ' +
                err.message
            )
        } finally {
            setCargandoDetalles(false)
        }
    }

    function cerrarDetalle() {
        if (actualizandoEstado) return

        setVentaSeleccionada(null)
        limpiarFormularioDespacho()
    }

    // ==========================================
    // DESCARGAR COMPROBANTE PDF
    // ==========================================

    async function descargarComprobante(venta) {
        setDescargandoId(venta.idVenta)
        setError('')
        setMensaje('')

        try {

            const respuesta = await fetch(
                `${API_VENTAS}/${venta.idVenta}/comprobante/pdf`,
                {
                    headers: {
                        Authorization: `Bearer ${token}`
                    }
                }
            )

            if (respuesta.status === 401) {
                onSesionExpirada?.()
                throw new Error(
                    'Tu sesión venció. Inicia sesión nuevamente.'
                )
            }

            if (respuesta.status === 403) {
                throw new Error(
                    'No tienes permisos para descargar este comprobante.'
                )
            }


            if (!respuesta.ok) {
                throw new Error(
                    `Error HTTP ${respuesta.status}`
                )
            }

            const contenido = await respuesta.blob()

            if (!contenido.size) {
                throw new Error(
                    'El comprobante está vacío.'
                )
            }

            const archivo = new Blob(
                [contenido],
                { type: 'application/pdf' }
            )

            const url = URL.createObjectURL(archivo)
            const enlace = document.createElement('a')

            enlace.href = url
            enlace.download =
                `comprobante-venta-${venta.idVenta}.pdf`

            document.body.appendChild(enlace)
            enlace.click()
            enlace.remove()

            setTimeout(() => {
                URL.revokeObjectURL(url)
            }, 1000)

            setMensaje(
                `Descarga iniciada para la venta #${venta.idVenta}.`
            )

        } catch (err) {
            console.error(err)

            setError(
                'No se pudo descargar el comprobante PDF. ' +
                err.message
            )
        } finally {
            setDescargandoId(null)
        }
    }

    // ==========================================
    // ACTUALIZAR TABLA Y MODAL
    // ==========================================

    function sincronizarVenta(ventaActualizada) {
        setVentas((anteriores) =>
            anteriores.map((venta) =>
                venta.idVenta === ventaActualizada.idVenta
                    ? ventaActualizada
                    : venta
            )
        )

        setVentaSeleccionada(ventaActualizada)
    }

    // ==========================================
    // HU-32: CAMBIAR ESTADOS GENERALES
    // ==========================================

    async function avanzarEstadoPedido() {
        if (
            !ventaSeleccionada ||
            actualizandoEstado
        ) {
            return
        }

        const estadoActual =
            ventaSeleccionada.estadoPedido

        const nuevoEstado =
            SIGUIENTE_ESTADO[estadoActual]

        if (!nuevoEstado) {
            setError(
                'No existe una transición disponible.'
            )
            return
        }

        const confirmado = window.confirm(
            `¿Confirmas el cambio del pedido ` +
            `#${ventaSeleccionada.idVenta}?\n\n` +
            `${nombreEstado(estadoActual)} → ` +
            `${nombreEstado(nuevoEstado)}`
        )

        if (!confirmado) return

        setActualizandoEstado(true)
        setError('')
        setMensaje('')

        try {

            const ventaActualizada = await consultarJson(
                `${API_VENTAS}/${ventaSeleccionada.idVenta}/estado`,
                {
                    method: 'PATCH',
                    headers: {
                        'Content-Type': 'application/json'
                    },
                    body: JSON.stringify({
                        estado: nuevoEstado
                    })
                },
                token,
                onSesionExpirada
            )


            sincronizarVenta(ventaActualizada)

            setMensaje(
                `Pedido #${ventaActualizada.idVenta} ` +
                `actualizado a ${nombreEstado(nuevoEstado)}.`
            )

        } catch (err) {
            console.error(err)

            setError(
                'No se pudo actualizar el pedido: ' +
                err.message
            )
        } finally {
            setActualizandoEstado(false)
        }
    }

    // ==========================================
    // HU-32: REGISTRAR DESPACHO
    // ==========================================

    async function registrarDespacho(evento) {
        evento.preventDefault()

        if (
            !ventaSeleccionada ||
            actualizandoEstado
        ) {
            return
        }

        if (
            ventaSeleccionada.estadoPedido !==
            'EN_PREPARACION'
        ) {
            setError(
                'Solo se pueden despachar pedidos en preparación.'
            )
            return
        }

        const esPropio =
            tipoDespacho === 'PROPIO'

        const nombreTransportista = esPropio
            ? 'DESPACHO_PROPIO'
            : transportista.trim()

        const numero = esPropio
            ? null
            : numeroSeguimiento.trim()

        const enlace = esPropio
            ? null
            : urlSeguimiento.trim() || null

        // ======================================
        // VALIDACIONES DEL FORMULARIO
        // ======================================

        if (!nombreTransportista) {
            setError(
                'Debes ingresar la empresa de transporte.'
            )
            return
        }

        if (nombreTransportista.length > 100) {
            setError(
                'El nombre del transportista supera 100 caracteres.'
            )
            return
        }

        if (!esPropio && !numero) {
            setError(
                'Debes ingresar el número de seguimiento.'
            )
            return
        }

        if (numero && numero.length > 150) {
            setError(
                'El número de seguimiento supera 150 caracteres.'
            )
            return
        }

        if (enlace && enlace.length > 1000) {
            setError(
                'El enlace de seguimiento supera 1000 caracteres.'
            )
            return
        }

        if (enlace && !obtenerUrlSegura(enlace)) {
            setError(
                'El enlace debe ser una URL HTTPS válida.'
            )
            return
        }

        const confirmado = window.confirm(
            `¿Confirmas el despacho del pedido ` +
            `#${ventaSeleccionada.idVenta}?\n\n` +
            `Transportista: ${esPropio
                ? 'Despacho propio'
                : nombreTransportista}\n` +
            `Estado nuevo: ENVIADO`
        )

        if (!confirmado) return

        setActualizandoEstado(true)
        setError('')
        setMensaje('')

        try {

            const ventaActualizada = await consultarJson(
                `${API_VENTAS}/${ventaSeleccionada.idVenta}/despacho`,
                {
                    method: 'PATCH',
                    headers: {
                        'Content-Type': 'application/json'
                    },
                    body: JSON.stringify({
                        transportista: nombreTransportista,
                        numeroSeguimientoTransporte: numero,
                        urlSeguimientoTransporte: enlace
                    })
                },
                token,
                onSesionExpirada
            )


            sincronizarVenta(ventaActualizada)
            limpiarFormularioDespacho()

            setMensaje(
                `Despacho del pedido #${ventaActualizada.idVenta} ` +
                'registrado correctamente.'
            )

        } catch (err) {
            console.error(err)

            setError(
                'No fue posible registrar el despacho: ' +
                err.message
            )
        } finally {
            setActualizandoEstado(false)
        }
    }

    // ==========================================
    // ESTADO DEL PEDIDO SELECCIONADO
    // ==========================================

    const estadoSeleccionado =
        ventaSeleccionada?.estadoPedido

    const siguienteEstado =
        SIGUIENTE_ESTADO[estadoSeleccionado]

    const puedeRegistrarDespacho =
        estadoSeleccionado === 'EN_PREPARACION'

    const urlExterna = obtenerUrlSegura(
        ventaSeleccionada?.urlSeguimientoTransporte
    )

    // ==========================================
    // INTERFAZ
    // ==========================================

    return (
        <section className="pagina-productos pagina-ventas">

            {/* ENCABEZADO */}

            <div className="pagina-cabecera">
                <div>
                    <p className="titulo-seccion">
                        PANEL DE ADMINISTRACIÓN
                    </p>

                    <h1>
                        {modo === 'despachos'
                            ? 'Pedidos y despachos'
                            : 'Ventas'}
                    </h1>

                    <p className="pagina-descripcion">
                        Consulta ventas, comprobantes y
                        seguimiento de pedidos.
                    </p>
                </div>

                <button
                    type="button"
                    className="boton-primario"
                    onClick={cargarVentas}
                    disabled={cargando}
                >
                    <FiRefreshCw />
                    Actualizar
                </button>
            </div>

            {/* MENSAJES */}

            {mensaje && (
                <div className="mensaje-exito">
                    {mensaje}
                </div>
            )}

            {error && (
                <div className="mensaje-error">
                    {error}
                </div>
            )}

            {/* RESUMEN */}

            <div className="ventas-resumen">

                <div className="ventas-resumen-tarjeta">
                    <span>Ventas registradas</span>
                    <strong>{ventas.length}</strong>
                </div>

                <div className="ventas-resumen-tarjeta">
                    <span>En preparación</span>
                    <strong>
                        {ventas.filter(
                            (v) =>
                                v.estadoPedido ===
                                'EN_PREPARACION'
                        ).length}
                    </strong>
                </div>

                <div className="ventas-resumen-tarjeta">
                    <span>Enviados</span>
                    <strong>
                        {ventas.filter(
                            (v) =>
                                v.estadoPedido === 'ENVIADO'
                        ).length}
                    </strong>
                </div>

                <div className="ventas-resumen-tarjeta">
                    <span>Entregados</span>
                    <strong>
                        {ventas.filter(
                            (v) =>
                                v.estadoPedido === 'ENTREGADO'
                        ).length}
                    </strong>
                </div>

            </div>

            {/* BUSCADOR */}

            <div className="ventas-buscador">
                <FiSearch />

                <input
                    type="search"
                    placeholder="Buscar venta, cliente, correo o estado..."
                    value={busqueda}
                    onChange={(evento) =>
                        setBusqueda(evento.target.value)
                    }
                    aria-label="Buscar ventas"
                />
            </div>

            {/* TABLA DE VENTAS */}

            {cargando ? (
                <div className="estado-carga">
                    Cargando ventas...
                </div>
            ) : (
                <div className="tabla-contenedor">
                    <table className="tabla-admin">

                        <thead>
                            <tr>
                                <th>N.º venta</th>
                                <th>Fecha</th>
                                <th>Cliente</th>
                                <th>Total</th>
                                <th>Estado</th>
                                <th>Acciones</th>
                            </tr>
                        </thead>

                        <tbody>
                            {ventasFiltradas.length === 0 ? (
                                <tr>
                                    <td
                                        colSpan="6"
                                        className="tabla-vacia"
                                    >
                                        No se encontraron ventas.
                                    </td>
                                </tr>
                            ) : (
                                ventasFiltradas.map((venta) => (
                                    <tr key={venta.idVenta}>

                                        <td>
                                            #{venta.idVenta}
                                        </td>

                                        <td>
                                            {formatearFecha(
                                                venta.fechaVenta
                                            )}
                                        </td>

                                        <td>
                                            {obtenerCliente(venta)}
                                        </td>

                                        <td>
                                            <strong>
                                                {formatearDinero(
                                                    venta.totalVenta
                                                )}
                                            </strong>
                                        </td>

                                        <td>
                                            {etiquetaEstado(
                                                venta.estadoPedido
                                            )}
                                        </td>

                                        <td>
                                            <div className="acciones-tabla">

                                                <button
                                                    type="button"
                                                    className="boton-icono editar"
                                                    title="Ver detalle y gestionar pedido"
                                                    aria-label={
                                                        `Ver venta ${venta.idVenta}`
                                                    }
                                                    onClick={() =>
                                                        verDetalle(venta)
                                                    }
                                                >
                                                    <FiEye />
                                                </button>

                                                <button
                                                    type="button"
                                                    className="boton-icono"
                                                    title="Descargar comprobante PDF"
                                                    aria-label={
                                                        `Descargar comprobante ${venta.idVenta}`
                                                    }
                                                    disabled={
                                                        descargandoId ===
                                                        venta.idVenta
                                                    }
                                                    onClick={() =>
                                                        descargarComprobante(
                                                            venta
                                                        )
                                                    }
                                                >
                                                    <FiDownload />
                                                </button>

                                            </div>
                                        </td>
                                    </tr>
                                ))
                            )}
                        </tbody>
                    </table>
                </div>
            )}

            {/* MODAL DETALLE DE VENTA */}

            {ventaSeleccionada && (
                <div
                    className="modal-fondo"
                    onClick={cerrarDetalle}
                >
                    <div
                        className="modal-producto ventas-modal"
                        role="dialog"
                        aria-modal="true"
                        aria-label={
                            `Detalle de venta ${ventaSeleccionada.idVenta}`
                        }
                        onClick={(evento) =>
                            evento.stopPropagation()
                        }
                    >

                        {/* CABECERA MODAL */}

                        <div className="modal-cabecera">

                            <div>
                                <p className="titulo-seccion">
                                    DETALLE DE VENTA
                                </p>

                                <h2>
                                    Venta #{ventaSeleccionada.idVenta}
                                </h2>
                            </div>

                            <button
                                type="button"
                                className="modal-cerrar"
                                aria-label="Cerrar detalle"
                                disabled={actualizandoEstado}
                                onClick={cerrarDetalle}
                            >
                                <FiX />
                            </button>

                        </div>

                        {/* INFORMACIÓN DE VENTA */}

                        <div className="ventas-detalle-info">

                            <p>
                                <strong>Cliente: </strong>
                                {obtenerCliente(
                                    ventaSeleccionada
                                )}
                            </p>

                            <p>
                                <strong>Fecha: </strong>
                                {formatearFecha(
                                    ventaSeleccionada.fechaVenta
                                )}
                            </p>

                            <p>
                                <strong>Total: </strong>
                                {formatearDinero(
                                    ventaSeleccionada.totalVenta
                                )}
                            </p>

                            <p>
                                <strong>Estado actual: </strong>
                                {etiquetaEstado(
                                    ventaSeleccionada.estadoPedido
                                )}
                            </p>

                        </div>

                        {/* SEGUIMIENTO DEL PEDIDO */}

                        <div
                            style={{
                                marginTop: 20,
                                marginBottom: 20,
                                padding: 16,
                                border: '1px solid #ddd',
                                borderRadius: 12
                            }}
                        >

                            <h3>
                                <FiTruck />
                                {' '}Seguimiento del pedido
                            </h3>

                            <div className="ventas-detalle-info">

                                <p>
                                    <strong>Pago confirmado: </strong>
                                    {formatearFecha(
                                        ventaSeleccionada.fechaPago
                                    )}
                                </p>

                                <p>
                                    <strong>En preparación: </strong>
                                    {formatearFecha(
                                        ventaSeleccionada.fechaPreparacion
                                    )}
                                </p>

                                <p>
                                    <strong>Enviado: </strong>
                                    {formatearFecha(
                                        ventaSeleccionada.fechaEnvio
                                    )}
                                </p>

                                <p>
                                    <strong>Entregado: </strong>
                                    {formatearFecha(
                                        ventaSeleccionada.fechaEntrega
                                    )}
                                </p>

                                {ventaSeleccionada.fechaCancelacion && (
                                    <p>
                                        <strong>Cancelado: </strong>
                                        {formatearFecha(
                                            ventaSeleccionada.fechaCancelacion
                                        )}
                                    </p>
                                )}

                                {ventaSeleccionada.transportista && (
                                    <p>
                                        <strong>Transportista: </strong>
                                        {ventaSeleccionada.transportista ===
                                            'DESPACHO_PROPIO'
                                            ? 'Despacho propio'
                                            : ventaSeleccionada.transportista}
                                    </p>
                                )}

                                {ventaSeleccionada.numeroSeguimientoTransporte && (
                                    <p>
                                        <strong>Número de seguimiento: </strong>
                                        {ventaSeleccionada.numeroSeguimientoTransporte}
                                    </p>
                                )}

                                {urlExterna && (
                                    <p>
                                        <strong>Seguimiento externo: </strong>
                                        <a
                                            href={urlExterna}
                                            target="_blank"
                                            rel="noopener noreferrer"
                                        >
                                            Abrir seguimiento
                                            {' '}<FiExternalLink />
                                        </a>
                                    </p>
                                )}

                            </div>

                            {/* FORMULARIO DESPACHO */}

                            {puedeRegistrarDespacho && (
                                <form
                                    onSubmit={registrarDespacho}
                                    style={{
                                        display: 'grid',
                                        gap: 14,
                                        padding: 16,
                                        marginTop: 18,
                                        border: '1px solid #ddd',
                                        borderRadius: 10
                                    }}
                                >

                                    <h3>
                                        Registrar despacho
                                    </h3>

                                    <p>
                                        Selecciona cómo será enviado
                                        el pedido al cliente.
                                    </p>

                                    <label
                                        htmlFor="tipo-despacho"
                                    >
                                        Tipo de transporte
                                    </label>

                                    <select
                                        id="tipo-despacho"
                                        value={tipoDespacho}
                                        disabled={actualizandoEstado}
                                        onChange={(evento) =>
                                            setTipoDespacho(
                                                evento.target.value
                                            )
                                        }
                                        style={{
                                            padding: 10,
                                            borderRadius: 7,
                                            border: '1px solid #ccc',
                                            width: '100%'
                                        }}
                                    >
                                        <option value="PROPIO">
                                            Despacho propio
                                        </option>

                                        <option value="EXTERNO">
                                            Transporte externo
                                        </option>
                                    </select>

                                    {tipoDespacho === 'EXTERNO' && (
                                        <>
                                            <label
                                                htmlFor="transportista"
                                            >
                                                Empresa de transporte
                                            </label>

                                            <input
                                                id="transportista"
                                                type="text"
                                                maxLength={100}
                                                required
                                                placeholder="Ej. Chilexpress"
                                                value={transportista}
                                                disabled={actualizandoEstado}
                                                onChange={(evento) =>
                                                    setTransportista(
                                                        evento.target.value
                                                    )
                                                }
                                                style={{
                                                    padding: 10,
                                                    borderRadius: 7,
                                                    border: '1px solid #ccc',
                                                    width: '100%'
                                                }}
                                            />

                                            <label
                                                htmlFor="numero-seguimiento"
                                            >
                                                Número de seguimiento
                                            </label>

                                            <input
                                                id="numero-seguimiento"
                                                type="text"
                                                maxLength={150}
                                                required
                                                placeholder="Código de rastreo"
                                                value={numeroSeguimiento}
                                                disabled={actualizandoEstado}
                                                onChange={(evento) =>
                                                    setNumeroSeguimiento(
                                                        evento.target.value
                                                    )
                                                }
                                                style={{
                                                    padding: 10,
                                                    borderRadius: 7,
                                                    border: '1px solid #ccc',
                                                    width: '100%'
                                                }}
                                            />

                                            <label
                                                htmlFor="url-seguimiento"
                                            >
                                                URL de seguimiento
                                                (opcional)
                                            </label>

                                            <input
                                                id="url-seguimiento"
                                                type="url"
                                                maxLength={1000}
                                                placeholder="https://..."
                                                value={urlSeguimiento}
                                                disabled={actualizandoEstado}
                                                onChange={(evento) =>
                                                    setUrlSeguimiento(
                                                        evento.target.value
                                                    )
                                                }
                                                style={{
                                                    padding: 10,
                                                    borderRadius: 7,
                                                    border: '1px solid #ccc',
                                                    width: '100%'
                                                }}
                                            />
                                        </>
                                    )}

                                    <button
                                        type="submit"
                                        className="boton-primario"
                                        disabled={actualizandoEstado}
                                    >
                                        <FiTruck />

                                        {actualizandoEstado
                                            ? 'Registrando despacho...'
                                            : 'Confirmar despacho'}
                                    </button>

                                </form>
                            )}

                            {/* TRANSICIONES GENERALES */}

                            {siguienteEstado ? (
                                <button
                                    type="button"
                                    className="boton-primario"
                                    disabled={actualizandoEstado}
                                    onClick={avanzarEstadoPedido}
                                    style={{
                                        marginTop: 15
                                    }}
                                >
                                    {siguienteEstado === 'ENTREGADO'
                                        ? <FiCheckCircle />
                                        : <FiTruck />
                                    }

                                    {actualizandoEstado
                                        ? 'Actualizando...'
                                        : `Marcar como ${nombreEstado(siguienteEstado)}`
                                    }
                                </button>
                            ) : (
                                !puedeRegistrarDespacho && (
                                    <p>
                                        No hay cambios administrativos
                                        disponibles para este estado.
                                    </p>
                                )
                            )}

                        </div>

                        {/* PRODUCTOS COMPRADOS */}

                        <h3>
                            <FiShoppingCart />
                            {' '}Productos de la venta
                        </h3>

                        {cargandoDetalles ? (
                            <p>Cargando detalle...</p>
                        ) : detalles.length === 0 ? (
                            <p>No hay detalles disponibles.</p>
                        ) : (
                            <div className="tabla-contenedor">
                                <table className="tabla-admin">

                                    <thead>
                                        <tr>
                                            <th>ID producto</th>
                                            <th>Cantidad</th>
                                            <th>Precio unitario</th>
                                            <th>Subtotal</th>
                                        </tr>
                                    </thead>

                                    <tbody>
                                        {detalles.map(
                                            (detalle, indice) => (
                                                <tr
                                                    key={
                                                        detalle.id?.productoId
                                                        ?? indice
                                                    }
                                                >

                                                    <td>
                                                        {detalle.id?.productoId ?? '—'}
                                                    </td>

                                                    <td>
                                                        {detalle.cantidad ?? '—'} g
                                                    </td>

                                                    <td>
                                                        {formatearDinero(
                                                            detalle.precioUnitario
                                                        )}
                                                    </td>

                                                    <td>
                                                        {formatearDinero(
                                                            detalle.subtotal
                                                        )}
                                                    </td>

                                                </tr>
                                            )
                                        )}
                                    </tbody>

                                </table>
                            </div>
                        )}

                        {/* ACCIONES DEL MODAL */}

                        <div className="ventas-modal-acciones">

                            <button
                                type="button"
                                className="boton-secundario"
                                disabled={actualizandoEstado}
                                onClick={cerrarDetalle}
                            >
                                Cerrar
                            </button>

                            <button
                                type="button"
                                className="boton-primario"
                                disabled={
                                    descargandoId ===
                                    ventaSeleccionada.idVenta
                                }
                                onClick={() =>
                                    descargarComprobante(
                                        ventaSeleccionada
                                    )
                                }
                            >
                                <FiDownload />
                                Descargar PDF
                            </button>

                        </div>

                    </div>
                </div>
            )}
        </section>
    )
}

export default Ventas
