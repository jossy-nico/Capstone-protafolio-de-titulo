import Encabezado from './components/Encabezado.jsx'
import MenuLateral from './components/MenuLateral.jsx'
import './App.css'

function App() {
  return (
    <div className="app">
      <MenuLateral />

      <div className="area-principal">
        <Encabezado />

        <main className="contenido-principal">

          <section className="tarjeta-bienvenida">
            <p className="titulo-seccion">
              PANEL DE ADMINISTRACIÓN
            </p>

            <h1>Inicio</h1>

            <p className="texto-bienvenida">
              Bienvenido al sistema administrativo.
            </p>
          </section>

        </main>
      </div>
    </div>
  )
}

export default App