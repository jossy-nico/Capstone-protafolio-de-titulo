import { useEffect, useState } from 'react'
import {
    FiPlus,
    FiEdit2,
    FiTrash2,
    FiX
} from 'react-icons/fi'

const API_CATEGORIAS = 'http://localhost:8080/api/categorias'

function Categorias() {

    const [categorias, setCategorias] = useState([])
    const [nombre, setNombre] = useState('')
    const [categoriaEditando, setCategoriaEditando] = useState(null)

    const [mostrarFormulario, setMostrarFormulario] = useState(false)
    const [cargando, setCargando] = useState(true)
    const [guardando, setGuardando] = useState(false)

    const [mensaje, setMensaje] = useState('')
    const [error, setError] = useState('')

    useEffect(() => {
        cargarCategorias()
    }, [])

    const cargarCategorias = async () => {

        setCargando(true)
        setError('')

        try {

            const respuesta = await fetch(API_CATEGORIAS)

            if (!respuesta.ok) {
                throw new Error('No fue posible cargar las categorías.')
            }

            const datos = await respuesta.json()

            setCategorias(datos)

        } catch (error) {

            console.error(error)

            setError(
                'No se pudo conectar con producto-api. Verifica que esté ejecutándose en el puerto 8080.'
            )

        } finally {
            setCargando(false)
        }
    }

    const abrirNuevaCategoria = () => {

        setCategoriaEditando(null)
        setNombre('')
        setMensaje('')
        setError('')
        setMostrarFormulario(true)
    }

    const abrirEditarCategoria = (categoria) => {

        setCategoriaEditando(categoria)
        setNombre(categoria.nomCategoria)
        setMensaje('')
        setError('')
        setMostrarFormulario(true)
    }

    const cerrarFormulario = () => {

        setMostrarFormulario(false)
        setCategoriaEditando(null)
        setNombre('')
    }

    const guardarCategoria = async (evento) => {

        evento.preventDefault()

        setGuardando(true)
        setMensaje('')
        setError('')

        try {

            const categoria = {
                nomCategoria: nombre.trim()
            }

            if (!categoria.nomCategoria) {
                throw new Error('Debes ingresar el nombre de la categoría.')
            }

            let respuesta

            if (categoriaEditando) {

                respuesta = await fetch(
                    `${API_CATEGORIAS}/${categoriaEditando.idCategoria}`,
                    {
                        method: 'PUT',
                        headers: {
                            'Content-Type': 'application/json'
                        },
                        body: JSON.stringify(categoria)
                    }
                )

            } else {

                respuesta = await fetch(
                    API_CATEGORIAS,
                    {
                        method: 'POST',
                        headers: {
                            'Content-Type': 'application/json'
                        },
                        body: JSON.stringify(categoria)
                    }
                )
            }

            if (!respuesta.ok) {

                if (respuesta.status === 409) {
                    throw new Error('La categoría ya existe.')
                }

                throw new Error('No fue posible guardar la categoría.')
            }

            await cargarCategorias()

            setMostrarFormulario(false)
            setNombre('')
            setCategoriaEditando(null)

            setMensaje(
                categoriaEditando
                    ? 'Categoría actualizada correctamente.'
                    : 'Categoría creada correctamente.'
            )

        } catch (error) {

            console.error(error)
            setError(error.message)

        } finally {
            setGuardando(false)
        }
    }

    const eliminarCategoria = async (categoria) => {

        const confirmar = window.confirm(
            `¿Deseas eliminar la categoría "${categoria.nomCategoria}"?`
        )

        if (!confirmar) {
            return
        }

        setMensaje('')
        setError('')

        try {

            const respuesta = await fetch(
                `${API_CATEGORIAS}/${categoria.idCategoria}`,
                {
                    method: 'DELETE'
                }
            )

            if (!respuesta.ok) {
                throw new Error(
                    'No fue posible eliminar la categoría. Puede tener productos asociados.'
                )
            }

            await cargarCategorias()

            setMensaje('Categoría eliminada correctamente.')

        } catch (error) {

            console.error(error)
            setError(error.message)
        }
    }

    return (
        <section className="pagina-productos">

            <div className="pagina-cabecera">

                <div>

                    <p className="titulo-seccion">
                        PANEL DE ADMINISTRACIÓN
                    </p>

                    <h1>Categorías</h1>

                    <p className="pagina-descripcion">
                        Administra las categorías utilizadas en el catálogo de productos.
                    </p>

                </div>

                <button
                    className="boton-primario"
                    onClick={abrirNuevaCategoria}
                >
                    <FiPlus />
                    Nueva categoría
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
                    Cargando categorías...
                </div>

            ) : (

                <div className="tabla-contenedor">

                    <table className="tabla-admin">

                        <thead>
                            <tr>
                                <th>ID</th>
                                <th>Categoría</th>
                                <th>Acciones</th>
                            </tr>
                        </thead>

                        <tbody>

                            {categorias.length === 0 ? (

                                <tr>
                                    <td
                                        colSpan="3"
                                        className="tabla-vacia"
                                    >
                                        No existen categorías registradas.
                                    </td>
                                </tr>

                            ) : (

                                categorias.map((categoria) => (

                                    <tr key={categoria.idCategoria}>

                                        <td>
                                            #{categoria.idCategoria}
                                        </td>

                                        <td>
                                            <strong>
                                                {categoria.nomCategoria}
                                            </strong>
                                        </td>

                                        <td>

                                            <div className="acciones-tabla">

                                                <button
                                                    className="boton-icono editar"
                                                    title="Editar"
                                                    onClick={() =>
                                                        abrirEditarCategoria(categoria)
                                                    }
                                                >
                                                    <FiEdit2 />
                                                </button>

                                                <button
                                                    className="boton-icono eliminar"
                                                    title="Eliminar"
                                                    onClick={() =>
                                                        eliminarCategoria(categoria)
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

                    <div className="modal-categoria">

                        <div className="modal-cabecera">

                            <div>

                                <p className="titulo-seccion">
                                    CATEGORÍAS
                                </p>

                                <h2>
                                    {categoriaEditando
                                        ? 'Editar categoría'
                                        : 'Nueva categoría'}
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

                        <form onSubmit={guardarCategoria}>

                            <div className="campo">

                                <label>
                                    Nombre de la categoría
                                </label>

                                <input
                                    type="text"
                                    value={nombre}
                                    onChange={(evento) =>
                                        setNombre(evento.target.value)
                                    }
                                    maxLength="50"
                                    placeholder="Ej: Nueces"
                                    autoFocus
                                    required
                                />

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
                                        : categoriaEditando
                                            ? 'Guardar cambios'
                                            : 'Crear categoría'}
                                </button>

                            </div>

                        </form>

                    </div>

                </div>
            )}

        </section>
    )
}

export default Categorias