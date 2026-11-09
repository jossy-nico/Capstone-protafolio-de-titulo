import { useEffect, useState } from 'react'
import {
    FiPlus,
    FiEdit2,
    FiTrash2,
    FiX,
    FiImage
} from 'react-icons/fi'

const API_PRODUCTOS = 'http://localhost:8080/api/productos'
const API_CATEGORIAS = 'http://localhost:8080/api/categorias'

const formularioInicial = {
    nomProducto: '',
    descripcion: '',
    stockGramos: '',
    precioKilo: '',
    precio250g: '',
    idCategoria: '',
    tablaNutricional: '',
    ingredientes: '',
    alergenos: ''
}

function Productos() {

    const [productos, setProductos] = useState([])
    const [categorias, setCategorias] = useState([])
    const [formulario, setFormulario] = useState(formularioInicial)
    const [productoEditando, setProductoEditando] = useState(null)
    const [imagen, setImagen] = useState(null)

    const [mostrarFormulario, setMostrarFormulario] = useState(false)
    const [cargando, setCargando] = useState(true)
    const [guardando, setGuardando] = useState(false)
    const [mensaje, setMensaje] = useState('')
    const [error, setError] = useState('')

    useEffect(() => {
        cargarDatos()
    }, [])

    const cargarDatos = async () => {

        setCargando(true)
        setError('')

        try {

            const [respuestaProductos, respuestaCategorias] =
                await Promise.all([
                    fetch(API_PRODUCTOS),
                    fetch(API_CATEGORIAS)
                ])

            if (!respuestaProductos.ok) {
                throw new Error('No fue posible cargar los productos.')
            }

            if (!respuestaCategorias.ok) {
                throw new Error('No fue posible cargar las categorías.')
            }

            const datosProductos = await respuestaProductos.json()
            const datosCategorias = await respuestaCategorias.json()

            setProductos(datosProductos)
            setCategorias(datosCategorias)

        } catch (error) {

            console.error(error)
            setError(
                'No se pudo conectar con producto-api. Verifica que el servicio esté ejecutándose en el puerto 8080.'
            )

        } finally {
            setCargando(false)
        }
    }

    const manejarCambio = (evento) => {

        const { name, value } = evento.target

        setFormulario({
            ...formulario,
            [name]: value
        })
    }

    const abrirNuevoProducto = () => {

        setProductoEditando(null)
        setFormulario(formularioInicial)
        setImagen(null)
        setMensaje('')
        setError('')
        setMostrarFormulario(true)
    }

    const abrirEditarProducto = (producto) => {

        setProductoEditando(producto)

        setFormulario({
            nomProducto: producto.nomProducto ?? '',
            descripcion: producto.descripcion ?? '',
            stockGramos: producto.stockGramos ?? '',
            precioKilo: producto.precioKilo ?? '',
            precio250g: producto.precio250g ?? '',
            idCategoria: producto.categoria?.idCategoria ?? '',
            tablaNutricional: producto.tablaNutricional ?? '',
            ingredientes: producto.ingredientes ?? '',
            alergenos: producto.alergenos ?? ''
        })

        setImagen(null)
        setMensaje('')
        setError('')
        setMostrarFormulario(true)
    }

    const cerrarFormulario = () => {

        setMostrarFormulario(false)
        setProductoEditando(null)
        setFormulario(formularioInicial)
        setImagen(null)
    }

    const subirImagen = async (idProducto, archivo) => {

        if (!archivo) {
            return
        }

        const datosImagen = new FormData()

        datosImagen.append('file', archivo)

        const respuesta = await fetch(
            `${API_PRODUCTOS}/${idProducto}/imagen`,
            {
                method: 'POST',
                body: datosImagen
            }
        )

        if (!respuesta.ok) {
            throw new Error('El producto fue guardado, pero ocurrió un error al subir la imagen.')
        }
    }

    const guardarProducto = async (evento) => {

        evento.preventDefault()

        setGuardando(true)
        setMensaje('')
        setError('')

        try {

            if (!formulario.idCategoria) {
                throw new Error('Debes seleccionar una categoría.')
            }

            const producto = {
                nomProducto: formulario.nomProducto.trim(),
                descripcion: formulario.descripcion.trim(),

                stockGramos:
                    formulario.stockGramos === ''
                        ? 0
                        : Number(formulario.stockGramos),

                precioKilo:
                    formulario.precioKilo === ''
                        ? 0
                        : Number(formulario.precioKilo),

                categoria: {
                    idCategoria: Number(formulario.idCategoria)
                },

                tablaNutricional: formulario.tablaNutricional.trim(),
                ingredientes: formulario.ingredientes.trim(),
                alergenos: formulario.alergenos.trim()
            }

            let respuesta

            if (productoEditando) {

                respuesta = await fetch(
                    `${API_PRODUCTOS}/${productoEditando.idProductos}`,
                    {
                        method: 'PUT',
                        headers: {
                            'Content-Type': 'application/json'
                        },
                        body: JSON.stringify(producto)
                    }
                )

            } else {

                respuesta = await fetch(
                    API_PRODUCTOS,
                    {
                        method: 'POST',
                        headers: {
                            'Content-Type': 'application/json'
                        },
                        body: JSON.stringify(producto)
                    }
                )
            }

            if (!respuesta.ok) {
                throw new Error('No fue posible guardar el producto.')
            }

            const productoGuardado = await respuesta.json()

            if (imagen) {
                await subirImagen(productoGuardado.idProductos, imagen)
            }

            await cargarDatos()

            setMostrarFormulario(false)
            setProductoEditando(null)
            setFormulario(formularioInicial)
            setImagen(null)

            setMensaje(
                productoEditando
                    ? 'Producto actualizado correctamente.'
                    : 'Producto creado correctamente.'
            )

        } catch (error) {

            console.error(error)

            setError(
                error.message || 'Ocurrió un error al guardar el producto.'
            )

        } finally {
            setGuardando(false)
        }
    }

    const eliminarProducto = async (producto) => {

        const confirmar = window.confirm(
            `¿Deseas eliminar el producto "${producto.nomProducto}"?`
        )

        if (!confirmar) {
            return
        }

        setMensaje('')
        setError('')

        try {

            const respuesta = await fetch(
                `${API_PRODUCTOS}/${producto.idProductos}`,
                {
                    method: 'DELETE'
                }
            )

            if (!respuesta.ok) {
                throw new Error('No fue posible eliminar el producto.')
            }

            await cargarDatos()

            setMensaje('Producto eliminado correctamente.')

        } catch (error) {

            console.error(error)
            setError(error.message)
        }
    }

    const formatearPrecio = (precio) => {

        if (precio === null || precio === undefined) {
            return '$0'
        }

        return new Intl.NumberFormat(
            'es-CL',
            {
                style: 'currency',
                currency: 'CLP',
                maximumFractionDigits: 0
            }
        ).format(precio)
    }

    return (
        <section className="pagina-productos">

            <div className="pagina-cabecera">

                <div>
                    <p className="titulo-seccion">
                        PANEL DE ADMINISTRACIÓN
                    </p>

                    <h1>Productos</h1>

                    <p className="pagina-descripcion">
                        Administra el catálogo, precios, stock e imágenes de los productos.
                    </p>
                </div>

                <button
                    className="boton-primario"
                    onClick={abrirNuevoProducto}
                >
                    <FiPlus />
                    Nuevo producto
                </button>

            </div>

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

            {cargando ? (

                <div className="estado-carga">
                    Cargando productos...
                </div>

            ) : (

                <div className="tabla-contenedor">

                    <table className="tabla-admin">

                        <thead>
                            <tr>
                                <th>Imagen</th>
                                <th>Producto</th>
                                <th>Categoría</th>
                                <th>250 g</th>
                                <th>Kilo</th>
                                <th>Stock</th>
                                <th>Acciones</th>
                            </tr>
                        </thead>

                        <tbody>

                            {productos.length === 0 ? (

                                <tr>
                                    <td
                                        colSpan="7"
                                        className="tabla-vacia"
                                    >
                                        No existen productos registrados.
                                    </td>
                                </tr>

                            ) : (

                                productos.map((producto) => (

                                    <tr key={producto.idProductos}>

                                        <td>

                                            {producto.imageUrl ? (

                                                <img
                                                    src={producto.imageUrl}
                                                    alt={producto.nomProducto}
                                                    className="producto-imagen"
                                                />

                                            ) : (

                                                <div className="producto-sin-imagen">
                                                    <FiImage />
                                                </div>
                                            )}

                                        </td>

                                        <td>
                                            <strong>
                                                {producto.nomProducto}
                                            </strong>

                                            <span className="producto-descripcion">
                                                {producto.descripcion}
                                            </span>
                                        </td>

                                        <td>
                                            {producto.categoria?.nomCategoria || 'Sin categoría'}
                                        </td>

                                        <td>
                                            {formatearPrecio(producto.precio250g)}
                                        </td>

                                        <td>
                                            {formatearPrecio(producto.precioKilo)}
                                        </td>

                                        <td>
                                            <span
                                                className={
                                                    producto.stockGramos <= 1000
                                                        ? 'stock-bajo'
                                                        : 'stock-normal'
                                                }
                                            >
                                                {producto.stockGramos ?? 0} g
                                            </span>
                                        </td>

                                        <td>

                                            <div className="acciones-tabla">

                                                <button
                                                    className="boton-icono editar"
                                                    title="Editar"
                                                    onClick={() =>
                                                        abrirEditarProducto(producto)
                                                    }
                                                >
                                                    <FiEdit2 />
                                                </button>

                                                <button
                                                    className="boton-icono eliminar"
                                                    title="Eliminar"
                                                    onClick={() =>
                                                        eliminarProducto(producto)
                                                    }
                                                >
                                                    <FiTrash2 />
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

            {mostrarFormulario && (

                <div className="modal-fondo">

                    <div className="modal-producto">

                        <div className="modal-cabecera">

                            <div>
                                <p className="titulo-seccion">
                                    PRODUCTOS
                                </p>

                                <h2>
                                    {productoEditando
                                        ? 'Editar producto'
                                        : 'Nuevo producto'}
                                </h2>
                            </div>

                            <button
                                type="button"
                                className="modal-cerrar"
                                onClick={cerrarFormulario}
                            >
                                <FiX />
                            </button>

                        </div>

                        <form
                            className="formulario-producto"
                            onSubmit={guardarProducto}
                        >

                            <div className="formulario-grid">

                                <div className="campo campo-completo">
                                    <label>Nombre</label>

                                    <input
                                        type="text"
                                        name="nomProducto"
                                        value={formulario.nomProducto}
                                        onChange={manejarCambio}
                                        maxLength="50"
                                        required
                                    />
                                </div>

                                <div className="campo campo-completo">
                                    <label>Descripción</label>

                                    <textarea
                                        name="descripcion"
                                        value={formulario.descripcion}
                                        onChange={manejarCambio}
                                        maxLength="200"
                                        required
                                    />
                                </div>

                                <div className="campo">
                                    <label>Categoría</label>

                                    <select
                                        name="idCategoria"
                                        value={formulario.idCategoria}
                                        onChange={manejarCambio}
                                        required
                                    >
                                        <option value="">
                                            Seleccionar
                                        </option>

                                        {categorias.map((categoria) => (
                                            <option
                                                key={categoria.idCategoria}
                                                value={categoria.idCategoria}
                                            >
                                                {categoria.nomCategoria}
                                            </option>
                                        ))}

                                    </select>
                                </div>

                                <div className="campo">
                                    <label>Stock (gramos)</label>

                                    <input
                                        type="number"
                                        name="stockGramos"
                                        value={formulario.stockGramos}
                                        onChange={manejarCambio}
                                        min="0"
                                        required
                                    />
                                </div>

                                <div className="campo">
                                    <label>Precio kilo</label>

                                    <input
                                        type="number"
                                        name="precioKilo"
                                        value={formulario.precioKilo}
                                        onChange={manejarCambio}
                                        min="0"
                                        step="1"
                                        required
                                    />
                                </div>

                                <div className="campo">
                                    <label>Precio 250 g (automático)</label>

                                    <input
                                        type="text"
                                        value={
                                            formulario.precioKilo
                                                ? `$${Math.round(Number(formulario.precioKilo) / 4).toLocaleString('es-CL')}`
                                                : '$0'
                                        }
                                        disabled
                                    />

                                    <small>
                                        Se calcula automáticamente a partir del precio por kilo.
                                    </small>
                                </div>

                                <div className="campo campo-completo">
                                    <label>Ingredientes</label>

                                    <textarea
                                        name="ingredientes"
                                        value={formulario.ingredientes}
                                        onChange={manejarCambio}
                                    />
                                </div>

                                <div className="campo campo-completo">
                                    <label>Alérgenos</label>

                                    <textarea
                                        name="alergenos"
                                        value={formulario.alergenos}
                                        onChange={manejarCambio}
                                    />
                                </div>

                                <div className="campo campo-completo">
                                    <label>Tabla nutricional</label>

                                    <textarea
                                        name="tablaNutricional"
                                        value={formulario.tablaNutricional}
                                        onChange={manejarCambio}
                                    />
                                </div>

                                <div className="campo campo-completo">
                                    <label>Imagen del producto</label>

                                    <input
                                        type="file"
                                        accept="image/*"
                                        onChange={(evento) =>
                                            setImagen(evento.target.files[0])
                                        }
                                    />

                                    {productoEditando?.imageUrl && !imagen && (
                                        <small>
                                            Si no seleccionas una nueva imagen,
                                            se mantendrá la actual.
                                        </small>
                                    )}

                                    {imagen && (
                                        <small>
                                            Imagen seleccionada: {imagen.name}
                                        </small>
                                    )}

                                </div>

                            </div>

                            <div className="modal-acciones">

                                <button
                                    type="button"
                                    className="boton-secundario"
                                    onClick={cerrarFormulario}
                                    disabled={guardando}
                                >
                                    Cancelar
                                </button>

                                <button
                                    type="submit"
                                    className="boton-primario"
                                    disabled={guardando}
                                >
                                    {guardando
                                        ? 'Guardando...'
                                        : productoEditando
                                            ? 'Guardar cambios'
                                            : 'Crear producto'}
                                </button>

                            </div>

                        </form>

                    </div>

                </div>
            )}

        </section>
    )
}

export default Productos