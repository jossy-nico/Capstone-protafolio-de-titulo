function MenuLateral() {
    return (
        <aside className="menu-lateral">

            <div className="menu-marca">
                <h2>Natur Cosecha</h2>
                <p>Panel de Administración</p>
            </div>

            <nav className="menu-navegacion">
                <button className="menu-opcion activa">
                    Inicio
                </button>

                <button className="menu-opcion">
                    Productos
                </button>

                <button className="menu-opcion">
                    Categorías
                </button>

                <button className="menu-opcion">
                    Empleados
                </button>

                <button className="menu-opcion">
                    Ventas
                </button>

                <button className="menu-opcion">
                    Reportes
                </button>
            </nav>

        </aside>
    )
}

export default MenuLateral