import {
    FiHome,
    FiPackage,
    FiGrid,
    FiUsers,
    FiShoppingCart,
    FiBarChart2
} from 'react-icons/fi'

function MenuLateral({ paginaActual, onCambiarPagina }) {

    const opciones = [
        { nombre: 'Inicio', icono: <FiHome /> },
        { nombre: 'Productos', icono: <FiPackage /> },
        { nombre: 'Categorías', icono: <FiGrid /> },
        { nombre: 'Empleados', icono: <FiUsers /> },
        { nombre: 'Ventas', icono: <FiShoppingCart /> },
        { nombre: 'Reportes', icono: <FiBarChart2 /> }
    ]

    return (
        <aside className="menu-lateral">

            <div className="menu-marca">
                <h2>Natur Cosecha</h2>
                <p>Panel de Administración</p>
            </div>

            <nav className="menu-navegacion">

                {opciones.map((opcion) => (
                    <button
                        key={opcion.nombre}
                        className={
                            paginaActual === opcion.nombre
                                ? 'menu-opcion activa'
                                : 'menu-opcion'
                        }
                        onClick={() => onCambiarPagina(opcion.nombre)}
                    >
                        {opcion.icono}
                        <span>{opcion.nombre}</span>
                    </button>
                ))}

            </nav>

        </aside>
    )
}

export default MenuLateral