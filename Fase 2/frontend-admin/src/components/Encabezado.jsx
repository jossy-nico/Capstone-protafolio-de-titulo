function Encabezado() {
    return (
        <header className="encabezado">
            <button className="boton-menu" aria-label="Abrir menú">
                ☰
            </button>

            <div className="usuario-admin">
                <div className="usuario-avatar">
                    A
                </div>

                <span>Administrador</span>
            </div>
        </header>
    )
}

export default Encabezado