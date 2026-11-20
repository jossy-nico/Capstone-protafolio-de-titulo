
import { useState } from 'react'

import Encabezado from './components/Encabezado.jsx'
import MenuLateral from './components/MenuLateral.jsx'

import Productos from './pages/Productos.jsx'
import Categorias from './pages/Categorias.jsx'
import Empleados from './pages/Empleados.jsx'
import Ventas from './pages/Ventas.jsx'
import Reportes from './pages/Reportes.jsx'


import './App.css'

// ==========================================
// NATUR COSECHA - AUTENTICACIÓN JWT
// ==========================================

const USUARIO_API = 'http://localhost:8181'

function App() {

  // ==========================================
  // NAVEGACIÓN
  // ==========================================

  const [paginaActual, setPaginaActual] =
    useState('Inicio')

  // ==========================================
  // SESIÓN ADMINISTRATIVA
  // ==========================================

  const [sesion, setSesion] = useState(null)

  const [correo, setCorreo] = useState('')
  const [password, setPassword] = useState('')

  const [cargando, setCargando] = useState(false)
  const [errorLogin, setErrorLogin] = useState('')

  // ==========================================
  // CERRAR SESIÓN
  // ==========================================

  const cerrarSesion = () => {

    setSesion(null)
    setPaginaActual('Inicio')

    setCorreo('')
    setPassword('')

    setErrorLogin('')
  }

  // ==========================================
  // INICIAR SESIÓN
  // ==========================================

  const iniciarSesion = async (event) => {

    event.preventDefault()

    setErrorLogin('')

    const correoLimpio = correo.trim().toLowerCase()

    if (!correoLimpio || !password) {
      setErrorLogin(
        'Ingresa tu correo y contraseña.'
      )
      return
    }

    setCargando(true)

    try {

      const respuesta = await fetch(
        `${USUARIO_API}/api/usuarios/login-token`,
        {
          method: 'POST',

          headers: {
            'Content-Type': 'application/json'
          },

          body: JSON.stringify({
            correo: correoLimpio,
            password
          })
        }
      )

      if (!respuesta.ok) {

        if (
          respuesta.status === 401 ||
          respuesta.status === 403
        ) {
          throw new Error(
            'Credenciales incorrectas o cuenta sin acceso.'
          )
        }

        throw new Error(
          'No fue posible iniciar sesión.'
        )
      }

      const datos = await respuesta.json()

      const usuario = datos?.usuario
      const token = datos?.token

      if (
        !usuario ||
        typeof token !== 'string' ||
        !token
      ) {
        throw new Error(
          'El servidor no entregó una sesión válida.'
        )
      }

      // Solo cuentas activas.
      if (usuario.estado !== 'ACTIVO') {
        throw new Error(
          'Esta cuenta no está activa.'
        )
      }

      // Solo administradores.
      if (usuario.tipoUsuario !== 'ADMIN') {
        throw new Error(
          'No tienes permisos para ingresar al panel administrativo.'
        )
      }

      // Bloqueo de cambio de contraseña pendiente.
      if (usuario.debeCambiarPassword !== false) {
        throw new Error(
          'Debes completar el cambio obligatorio de contraseña antes de ingresar.'
        )
      }

      const duracion =
        Number(datos.expiresIn) || 900

      const expiraEn =
        Date.now() + duracion * 1000

      setSesion({
        token,
        usuario,
        expiraEn
      })

      setPaginaActual('Inicio')
      setPassword('')
      setErrorLogin('')

    } catch (error) {

      setSesion(null)

      setErrorLogin(
        error.message ||
        'No fue posible iniciar sesión.'
      )

    } finally {
      setCargando(false)
    }
  }

  // ==========================================
  // FORMULARIO DE LOGIN
  // ==========================================

  if (!sesion) {

    return (
      <div
        style={{
          minHeight: '100vh',
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'center',
          padding: '24px',
          background:
            'linear-gradient(135deg, #101c18 0%, #1b3428 55%, #234936 100%)',
          fontFamily: 'inherit'
        }}
      >

        <form
          onSubmit={iniciarSesion}
          style={{
            width: '100%',
            maxWidth: '420px',
            padding: '36px',
            borderRadius: '18px',
            background: '#ffffff',
            boxShadow:
              '0 20px 65px rgba(0,0,0,0.25)',
            display: 'flex',
            flexDirection: 'column',
            gap: '16px'
          }}
        >

          <div style={{ textAlign: 'center' }}>

            <div
              style={{
                fontSize: '38px',
                marginBottom: '10px'
              }}
            >
              🌿
            </div>

            <h1
              style={{
                margin: 0,
                color: '#224b36',
                fontSize: '28px'
              }}
            >
              Natur Cosecha
            </h1>

            <p
              style={{
                color: '#68776d',
                marginTop: '8px',
                marginBottom: '20px'
              }}
            >
              Panel de administración
            </p>

          </div>

          <label
            htmlFor="correo-admin"
            style={{
              fontWeight: 600,
              color: '#263a2f'
            }}
          >
            Correo electrónico
          </label>

          <input
            id="correo-admin"
            type="email"
            value={correo}
            onChange={(event) =>
              setCorreo(event.target.value)
            }
            placeholder="administrador@correo.cl"
            autoComplete="username"
            required
            disabled={cargando}
            style={{
              width: '100%',
              boxSizing: 'border-box',
              padding: '13px',
              borderRadius: '9px',
              border: '1px solid #cbd5ce',
              fontSize: '15px',
              color: '#263a2f',
              background: '#ffffff'
            }}
          />

          <label
            htmlFor="password-admin"
            style={{
              fontWeight: 600,
              color: '#263a2f'
            }}
          >
            Contraseña
          </label>

          <input
            id="password-admin"
            type="password"
            value={password}
            onChange={(event) =>
              setPassword(event.target.value)
            }
            placeholder="Ingresa tu contraseña"
            autoComplete="current-password"
            required
            disabled={cargando}
            style={{
              width: '100%',
              boxSizing: 'border-box',
              padding: '13px',
              borderRadius: '9px',
              border: '1px solid #cbd5ce',
              fontSize: '15px',
              color: '#263a2f',
              background: '#ffffff'
            }}
          />

          {errorLogin && (
            <div
              role="alert"
              style={{
                background: '#fff0f0',
                color: '#a32929',
                padding: '12px',
                borderRadius: '9px',
                fontSize: '14px'
              }}
            >
              {errorLogin}
            </div>
          )}

          <button
            type="submit"
            disabled={cargando}
            style={{
              padding: '14px',
              borderRadius: '9px',
              border: 'none',
              background: cargando
                ? '#769584'
                : '#246844',
              color: 'white',
              fontSize: '15px',
              fontWeight: 700,
              cursor: cargando
                ? 'wait'
                : 'pointer'
            }}
          >
            {cargando
              ? 'Validando credenciales...'
              : 'Ingresar al panel'}
          </button>

          <p
            style={{
              textAlign: 'center',
              fontSize: '12px',
              color: '#849087',
              margin: 0
            }}
          >
            Acceso exclusivo para administradores autorizados.
          </p>

        </form>

      </div>
    )
  }

  // ==========================================
  // DATOS DE LA SESIÓN
  // ==========================================

  const { token, usuario, expiraEn } = sesion

  // ==========================================
  // VERIFICAR VENCIMIENTO
  // ==========================================

  if (Date.now() >= expiraEn) {

    return (
      <div
        style={{
          minHeight: '100vh',
          display: 'flex',
          justifyContent: 'center',
          alignItems: 'center',
          background: '#eef3ef',
          padding: '24px'
        }}
      >
        <section
          style={{
            background: 'white',
            padding: '32px',
            borderRadius: '14px',
            textAlign: 'center',
            maxWidth: '420px'
          }}
        >
          <h2>Sesión vencida</h2>

          <p>
            Por seguridad, debes iniciar sesión nuevamente.
          </p>

          <button
            onClick={cerrarSesion}
            style={{
              padding: '12px 20px',
              border: 0,
              borderRadius: '8px',
              background: '#246844',
              color: 'white',
              cursor: 'pointer'
            }}
          >
            Volver al inicio de sesión
          </button>
        </section>
      </div>
    )
  }

  // ==========================================
  // MOSTRAR PÁGINAS
  // ==========================================

  const mostrarPagina = () => {

    switch (paginaActual) {

      case 'Productos':
        return (
          <Productos
            token={token}
            usuario={usuario}
            onSesionExpirada={cerrarSesion}
          />
        )

      case 'Categorías':
        return (
          <Categorias
            token={token}
            usuario={usuario}
            onSesionExpirada={cerrarSesion}
          />
        )

      case 'Empleados':
        return (
          <Empleados
            token={token}
            usuario={usuario}
            onSesionExpirada={cerrarSesion}
          />
        )

      case 'Ventas':
        return (
          <Ventas
            token={token}
            usuario={usuario}
            onSesionExpirada={cerrarSesion}
          />
        )

      case 'Pedidos y despachos':
        return (
          <Ventas
            token={token}
            usuario={usuario}
            onSesionExpirada={cerrarSesion}
            modo="despachos"
          />
        )



      // ==========================================
      // HU-32: GESTIÓN DE PEDIDOS Y DESPACHOS
      // ==========================================

      case 'Pedidos y despachos':
        return (
          <PedidosDespachos
            token={token}
            usuario={usuario}
            onSesionExpirada={cerrarSesion}
          />
        )


      case 'Reportes':
        return (
          <Reportes
            token={token}
            usuario={usuario}
            onSesionExpirada={cerrarSesion}
          />
        )

      default:
        return (
          <section className="tarjeta-bienvenida">

            <p className="titulo-seccion">
              PANEL DE ADMINISTRACIÓN
            </p>

            <h1>Inicio</h1>

            <p className="texto-bienvenida">
              Bienvenido al sistema administrativo,
              {' '}
              {usuario.nombre}.
            </p>

            <p className="texto-bienvenida">
              Has iniciado sesión como administrador
              de Natur Cosecha.
            </p>

          </section>
        )
    }
  }

  // ==========================================
  // PANEL ADMINISTRATIVO AUTENTICADO
  // ==========================================

  return (
    <div className="app">

      <MenuLateral
        paginaActual={paginaActual}
        onCambiarPagina={setPaginaActual}
      />

      <div className="area-principal">

        <Encabezado />

        <main className="contenido-principal">

          <div
            style={{
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'flex-end',
              flexWrap: 'wrap',
              gap: '12px',
              marginBottom: '18px'
            }}
          >

            <span
              style={{
                fontSize: '13px',
                color: '#52665a'
              }}
            >
              {usuario.nombre} {usuario.apellido}
              {' · '}
              {usuario.tipoUsuario}
            </span>

            <button
              type="button"
              onClick={cerrarSesion}
              style={{
                padding: '9px 15px',
                border: '1px solid #b9c9bc',
                borderRadius: '8px',
                background: '#ffffff',
                color: '#24543a',
                fontWeight: 600,
                cursor: 'pointer'
              }}
            >
              Cerrar sesión
            </button>

          </div>

          {mostrarPagina()}

        </main>

      </div>

    </div>
  )
}

export default App
