
import { useEffect, useMemo, useState } from 'react'
import {
    FiBarChart2,
    FiCalendar,
    FiDownload,
    FiRefreshCw,
    FiShoppingBag,
    FiTrendingUp,
    FiDollarSign,
    FiAlertCircle
} from 'react-icons/fi'

const API_VENTAS = '/api/ventas'
const API_PRODUCTOS = '/api/productos'

const formatoCLP = new Intl.NumberFormat('es-CL', {
    style: 'currency',
    currency: 'CLP',
    maximumFractionDigits: 0
})

function dinero(valor) {
    return formatoCLP.format(Number(valor) || 0)
}

function fechaLocal(valor) {
    if (!valor) return ''

    if (Array.isArray(valor)) {
        const [anio, mes, dia] = valor
        return `${anio}-${String(mes).padStart(2, '0')}-${String(dia).padStart(2, '0')}`
    }

    return String(valor).slice(0, 10)
}

function mesDeFecha(valor) {
    return fechaLocal(valor).slice(0, 7)
}

function etiquetaMes(mes) {
    if (!mes) return ''

    const [anio, numeroMes] = mes.split('-').map(Number)

    const fecha = new Date(anio, numeroMes - 1, 1)

    return fecha.toLocaleDateString('es-CL', {
        month: 'short',
        year: 'numeric'
    })
}

function exportarCSV(nombre, filas) {
    const escapar = (valor) => {
        const texto = String(valor ?? '')
        return `"${texto.replaceAll('"', '""')}"`
    }

    const contenido = filas
        .map((fila) => fila.map(escapar).join(';'))
        .join('\r\n')

    const archivo = new Blob(
        ['\uFEFF' + contenido],
        { type: 'text/csv;charset=utf-8;' }
    )

    const url = URL.createObjectURL(archivo)
    const enlace = document.createElement('a')

    enlace.href = url
    enlace.download = nombre

    document.body.appendChild(enlace)
    enlace.click()
    enlace.remove()

    setTimeout(() => URL.revokeObjectURL(url), 1000)
}

function Reportes({ token, onSesionExpirada }) {
    const [ventas, setVentas] = useState([])
    const [detallesPorVenta, setDetallesPorVenta] = useState({})
    const [nombresProductos, setNombresProductos] = useState({})

    const [desde, setDesde] = useState('')
    const [hasta, setHasta] = useState('')

    const [cargando, setCargando] = useState(true)
    const [cargandoDetalles, setCargandoDetalles] = useState(false)
    const [detallesIncompletos, setDetallesIncompletos] = useState(false)
    const [error, setError] = useState('')

    // ==========================================
    // NATUR COSECHA - PETICIONES JWT
    // ==========================================

    async function consultarConJwt(url) {

        if (!token) {
            onSesionExpirada?.()

            throw new Error(
                'No existe una sesión administrativa activa.'
            )
        }

        const respuesta = await fetch(url, {
            method: 'GET',
            headers: {
                Authorization: `Bearer ${token}`
            }
        })

        if (respuesta.status === 401) {
            onSesionExpirada?.()

            throw new Error(
                'Tu sesión ha vencido. Inicia sesión nuevamente.'
            )
        }

        if (respuesta.status === 403) {
            throw new Error(
                'No tienes permisos para consultar esta información.'
            )
        }

        if (!respuesta.ok) {
            throw new Error(
                `Error HTTP ${respuesta.status}`
            )
        }

        return respuesta
    }


    async function cargarReportes() {
        setCargando(true)
        setError('')
        setDetallesPorVenta({})
        setDetallesIncompletos(false)
        setCargandoDetalles(false)

        try {
            const respuesta = await consultarConJwt(API_VENTAS)

            if (!respuesta.ok) {
                throw new Error(`Error HTTP ${respuesta.status}`)
            }

            const datos = await respuesta.json()

            if (!Array.isArray(datos)) {
                throw new Error('La API no devolvió una lista de ventas.')
            }

            setVentas(datos)
            setCargando(false)

            // Los nombres se obtienen del catálogo real.
            try {
                const respuestaProductos = await fetch(API_PRODUCTOS)

                if (respuestaProductos.ok) {
                    const productos = await respuestaProductos.json()

                    if (Array.isArray(productos)) {
                        const nombres = {}

                        productos.forEach((producto) => {
                            nombres[producto.idProductos] =
                                producto.nomProducto
                        })

                        setNombresProductos(nombres)
                    }
                }
            } catch (err) {
                console.warn('Catálogo no disponible:', err)
            }

            if (datos.length === 0) return

            setCargandoDetalles(true)

            // Consultar los detalles de las ventas registradas.
            const resultados = await Promise.allSettled(
                datos.map(async (venta) => {

                    const respuestaDetalle = await consultarConJwt(
                        `${API_VENTAS}/${venta.idVenta}/detalles`
                    )


                    if (!respuestaDetalle.ok) {
                        throw new Error(
                            `No fue posible consultar venta ${venta.idVenta}`
                        )
                    }

                    const detalles = await respuestaDetalle.json()

                    if (!Array.isArray(detalles)) {
                        throw new Error('Detalle de venta inválido.')
                    }

                    return {
                        idVenta: venta.idVenta,
                        detalles
                    }
                })
            )

            const mapa = {}
            let incompletos = false

            resultados.forEach((resultado) => {
                if (resultado.status === 'fulfilled') {
                    mapa[resultado.value.idVenta] =
                        resultado.value.detalles
                } else {
                    incompletos = true
                }
            })

            setDetallesPorVenta(mapa)
            setDetallesIncompletos(incompletos)

        } catch (err) {
            console.error('Error consultando reportes:', err)

            setError(
                'No fue posible consultar los reportes. ' +
                err.message
            )

            setVentas([])
        }
        finally {
            setCargando(false)
            setCargandoDetalles(false)
        }
    }

    useEffect(() => {
        cargarReportes()
    }, [])

    const ventasFiltradas = useMemo(() => {
        return ventas.filter((venta) => {
            const fecha = fechaLocal(venta.fechaVenta)

            if (desde && fecha < desde) return false
            if (hasta && fecha > hasta) return false

            return true
        })
    }, [ventas, desde, hasta])

    const resumen = useMemo(() => {
        const total = ventasFiltradas.reduce(
            (acumulado, venta) =>
                acumulado + Number(venta.totalVenta || 0),
            0
        )

        const cantidad = ventasFiltradas.length

        return {
            cantidad,
            total,
            promedio: cantidad > 0 ? total / cantidad : 0
        }
    }, [ventasFiltradas])

    const evolucionMensual = useMemo(() => {
        const agrupadas = {}

        ventasFiltradas.forEach((venta) => {
            const mes = mesDeFecha(venta.fechaVenta)

            if (!mes) return

            if (!agrupadas[mes]) {
                agrupadas[mes] = {
                    mes,
                    cantidad: 0,
                    total: 0
                }
            }

            agrupadas[mes].cantidad += 1
            agrupadas[mes].total += Number(venta.totalVenta || 0)
        })

        return Object.values(agrupadas).sort(
            (a, b) => a.mes.localeCompare(b.mes)
        )
    }, [ventasFiltradas])

    const maximoMensual = Math.max(
        1,
        ...evolucionMensual.map((item) => item.total)
    )

    const productosMasVendidos = useMemo(() => {
        const agrupados = {}

        ventasFiltradas.forEach((venta) => {
            const detalles = detallesPorVenta[venta.idVenta] || []

            detalles.forEach((detalle) => {
                const productoId = detalle.id?.productoId

                if (productoId == null) return

                if (!agrupados[productoId]) {
                    agrupados[productoId] = {
                        productoId,
                        cantidadGramos: 0,
                        total: 0
                    }
                }

                agrupados[productoId].cantidadGramos +=
                    Number(detalle.cantidad || 0)

                agrupados[productoId].total +=
                    Number(detalle.subtotal || 0)
            })
        })

        return Object.values(agrupados)
            .sort((a, b) => b.cantidadGramos - a.cantidadGramos)
            .slice(0, 10)
    }, [ventasFiltradas, detallesPorVenta])

    function descargarReporte() {
        const filas = [
            ['ID venta', 'Fecha', 'Cliente ID', 'Cliente invitado',
                'Monto registrado CLP'],
            ...ventasFiltradas.map((venta) => [
                venta.idVenta,
                fechaLocal(venta.fechaVenta),
                venta.clienteId,
                [
                    venta.nombreInvitado,
                    venta.apellidoInvitado
                ].filter(Boolean).join(' '),
                venta.totalVenta
            ])
        ]

        exportarCSV('reporte-ventas-natur-cosecha.csv', filas)
    }

    const periodoInvalido = Boolean(
        desde && hasta && desde > hasta
    )

    return (
        <section className="pagina-productos pagina-reportes">
            <div className="pagina-cabecera">
                <div>
                    <p className="titulo-seccion">
                        PANEL DE ADMINISTRACIÓN
                    </p>

                    <h1>Reportes</h1>

                    <p className="pagina-descripcion">
                        Estadísticas y análisis de las ventas registradas
                        en Natur Cosecha.
                    </p>
                </div>

                <button
                    type="button"
                    className="boton-primario"
                    onClick={cargarReportes}
                    disabled={cargando || cargandoDetalles}
                >
                    <FiRefreshCw />
                    Actualizar
                </button>
            </div>

            {error && (
                <div className="mensaje-error">{error}</div>
            )}

            <div className="reportes-filtros">
                <div className="reportes-filtro-titulo">
                    <FiCalendar />
                    <strong>Filtrar por período</strong>
                </div>

                <label>
                    Desde
                    <input
                        type="date"
                        value={desde}
                        onChange={(e) => setDesde(e.target.value)}
                    />
                </label>

                <label>
                    Hasta
                    <input
                        type="date"
                        value={hasta}
                        onChange={(e) => setHasta(e.target.value)}
                    />
                </label>

                <button
                    type="button"
                    className="boton-secundario"
                    onClick={() => {
                        setDesde('')
                        setHasta('')
                    }}
                >
                    Limpiar
                </button>
            </div>

            {periodoInvalido && (
                <div className="mensaje-error">
                    La fecha inicial no puede ser posterior a la final.
                </div>
            )}

            {cargando ? (
                <div className="estado-carga">
                    Cargando información de reportes...
                </div>
            ) : periodoInvalido ? null : (
                <>
                    <div className="reportes-indicadores">
                        <article className="reportes-indicador">
                            <FiShoppingBag />
                            <span>Ventas registradas</span>
                            <strong>{resumen.cantidad}</strong>
                        </article>

                        <article className="reportes-indicador">
                            <FiDollarSign />
                            <span>Monto total registrado</span>
                            <strong>{dinero(resumen.total)}</strong>
                        </article>

                        <article className="reportes-indicador">
                            <FiTrendingUp />
                            <span>Promedio por venta</span>
                            <strong>{dinero(resumen.promedio)}</strong>
                        </article>
                    </div>

                    <div className="reportes-aviso">
                        <FiAlertCircle />
                        <span>
                            Los montos corresponden a ventas registradas.
                            No representan necesariamente pagos autorizados
                            por Webpay.
                        </span>
                    </div>

                    <div className="reportes-panel">
                        <h2>
                            <FiBarChart2 />
                            Evolución mensual de ventas
                        </h2>

                        {evolucionMensual.length === 0 ? (
                            <p>No existen ventas para el período seleccionado.</p>
                        ) : (
                            <div className="reportes-grafico">
                                {evolucionMensual.map((item) => (
                                    <div
                                        className="reportes-barra-fila"
                                        key={item.mes}
                                    >
                                        <span>{etiquetaMes(item.mes)}</span>

                                        <div className="reportes-barra-fondo">
                                            <div
                                                className="reportes-barra-valor"
                                                style={{
                                                    width:
                                                        `${(item.total / maximoMensual) * 100}%`
                                                }}
                                            />
                                        </div>

                                        <strong>{dinero(item.total)}</strong>
                                    </div>
                                ))}
                            </div>
                        )}
                    </div>

                    <div className="reportes-panel">
                        <h2>Productos más vendidos</h2>

                        {cargandoDetalles ? (
                            <p>Calculando productos vendidos...</p>
                        ) : (
                            <>
                                {detallesIncompletos && (
                                    <div className="mensaje-error">
                                        No fue posible consultar todos los detalles.
                                        Este ranking puede estar incompleto.
                                    </div>
                                )}

                                <div className="tabla-contenedor">
                                    <table className="tabla-admin">
                                        <thead>
                                            <tr>
                                                <th>Producto</th>
                                                <th>Cantidad vendida</th>
                                                <th>Monto registrado</th>
                                            </tr>
                                        </thead>

                                        <tbody>
                                            {productosMasVendidos.length === 0 ? (
                                                <tr>
                                                    <td colSpan="3" className="tabla-vacia">
                                                        Sin productos para mostrar.
                                                    </td>
                                                </tr>
                                            ) : (
                                                productosMasVendidos.map((producto) => (
                                                    <tr key={producto.productoId}>
                                                        <td>
                                                            {nombresProductos[producto.productoId]
                                                                || `Producto #${producto.productoId}`}
                                                        </td>

                                                        <td>
                                                            {producto.cantidadGramos
                                                                .toLocaleString('es-CL')} g
                                                        </td>

                                                        <td>{dinero(producto.total)}</td>
                                                    </tr>
                                                ))
                                            )}
                                        </tbody>
                                    </table>
                                </div>
                            </>
                        )}
                    </div>

                    <div className="reportes-exportar">
                        <button
                            type="button"
                            className="boton-primario"
                            disabled={ventasFiltradas.length === 0}
                            onClick={descargarReporte}
                        >
                            <FiDownload />
                            Exportar ventas a CSV
                        </button>
                    </div>
                </>
            )}
        </section>
    )
}

export default Reportes
