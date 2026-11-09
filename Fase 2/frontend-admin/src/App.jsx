import { useState } from 'react'

import Encabezado from './components/Encabezado.jsx'
import MenuLateral from './components/MenuLateral.jsx'

import Productos from './pages/Productos.jsx'
import Categorias from './pages/Categorias.jsx'
import Empleados from './pages/Empleados.jsx'

import './App.css'

function App() {

  const [paginaActual, setPaginaActual] = useState('Inicio')

  const mostrarPagina = () => {

    switch (paginaActual) {

      case 'Productos':
        return <Productos />

      case 'Categorías':
        return <Categorias />

      case 'Empleados':
        return <Empleados />

      case 'Ventas':
        return (
          <section className="tarjeta-bienvenida">
            <p className="titulo-seccion">
              PANEL DE ADMINISTRACIÓN
            </p>

            <h1>Ventas</h1>

            <p className="texto-bienvenida">
              Administración de ventas y pedidos.
            </p>
          </section>
        )

      case 'Reportes':
        return (
          <section className="tarjeta-bienvenida">
            <p className="titulo-seccion">
              PANEL DE ADMINISTRACIÓN
            </p>

            <h1>Reportes</h1>

            <p className="texto-bienvenida">
              Reportes del sistema.
            </p>
          </section>
        )

      default:
        return (
          <section className="tarjeta-bienvenida">

            <p className="titulo-seccion">
              PANEL DE ADMINISTRACIÓN
            </p>

            <h1>Inicio</h1>

            <p className="texto-bienvenida">
              Bienvenido al sistema administrativo.
            </p>

          </section>
        )
    }
  }

  return (
    <div className="app">

      <MenuLateral
        paginaActual={paginaActual}
        onCambiarPagina={setPaginaActual}
      />

      <div className="area-principal">

        <Encabezado />

        <main className="contenido-principal">
          {mostrarPagina()}
        </main>

      </div>

    </div>
  )
}

export default App