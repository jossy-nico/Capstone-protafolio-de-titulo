
import { useEffect, useState } from "react";
import {
    FaPlus,
    FaPen,
    FaBan,
    FaCheck,
    FaTimes,
    FaUserTie,
} from "react-icons/fa";

const API_EMPLEADOS = "http://localhost:8181/api/empleados";
const API_USUARIOS = "http://localhost:8181/api/usuarios";

// ==========================================
// NATUR COSECHA - EMPLEADOS
// AUTENTICACIÓN JWT
// ==========================================

function Empleados({ token, onSesionExpirada }) {

    const [empleados, setEmpleados] = useState([]);
    const [cargando, setCargando] = useState(true);
    const [guardando, setGuardando] = useState(false);
    const [procesandoEstado, setProcesandoEstado] = useState(null);

    const [mostrarFormulario, setMostrarFormulario] = useState(false);
    const [modoEdicion, setModoEdicion] = useState(false);
    const [empleadoEditando, setEmpleadoEditando] = useState(null);

    const [mensaje, setMensaje] = useState("");
    const [error, setError] = useState("");

    const formularioInicial = {
        nombre: "",
        apellido: "",
        correo: "",
        password: "",
        puesto: "VENTAS",
        fechaContrato: "",
        sueldo: "",
    };

    const [formulario, setFormulario] = useState(formularioInicial);

    // ==========================================
    // PETICIONES AUTENTICADAS
    // ==========================================

    const peticionAutenticada = async (url, opciones = {}) => {

        if (!token) {
            onSesionExpirada?.();
            throw new Error(
                "No existe una sesión administrativa activa."
            );
        }

        const headers = new Headers(opciones.headers || {});

        headers.set(
            "Authorization",
            `Bearer ${token}`
        );

        const response = await fetch(url, {
            ...opciones,
            headers,
        });

        if (response.status === 401) {
            onSesionExpirada?.();

            throw new Error(
                "Tu sesión ha vencido. Debes iniciar sesión nuevamente."
            );
        }

        if (response.status === 403) {
            throw new Error(
                "No tienes permisos para realizar esta operación."
            );
        }

        return response;
    };

    // ==========================================
    // CONSULTAR EMPLEADOS
    // ==========================================

    const cargarEmpleados = async () => {

        try {
            setCargando(true);
            setError("");

            const response = await peticionAutenticada(
                API_EMPLEADOS
            );

            if (!response.ok) {
                throw new Error(
                    `No fue posible cargar los empleados. HTTP ${response.status}`
                );
            }

            const data = await response.json();

            if (!Array.isArray(data)) {
                throw new Error(
                    "La API no devolvió una lista válida de empleados."
                );
            }

            setEmpleados(data);

        } catch (err) {
            console.error("Error consultando empleados:", err);
            setError(err.message);

        } finally {
            setCargando(false);
        }
    };

    useEffect(() => {
        cargarEmpleados();
        // La instancia se monta al ingresar a esta página.
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [token]);

    // ==========================================
    // FORMULARIO
    // ==========================================

    const manejarCambio = (e) => {

        const { name, value } = e.target;

        setFormulario((anterior) => ({
            ...anterior,
            [name]: value,
        }));
    };

    const abrirNuevoEmpleado = () => {

        setModoEdicion(false);
        setEmpleadoEditando(null);
        setFormulario(formularioInicial);

        setMensaje("");
        setError("");
        setMostrarFormulario(true);
    };

    const abrirEditarEmpleado = (empleado) => {

        setModoEdicion(true);
        setEmpleadoEditando(empleado);

        setFormulario({
            nombre: empleado.nombre ?? "",
            apellido: empleado.apellido ?? "",
            correo: empleado.correo ?? "",
            password: "",
            puesto: empleado.puesto ?? "VENTAS",
            fechaContrato: empleado.fechaContrato ?? "",
            sueldo: empleado.sueldo ?? "",
        });

        setMensaje("");
        setError("");
        setMostrarFormulario(true);
    };

    const cerrarFormulario = () => {

        setMostrarFormulario(false);
        setModoEdicion(false);
        setEmpleadoEditando(null);
        setFormulario(formularioInicial);
    };

    // ==========================================
    // CREAR / EDITAR EMPLEADO
    // ==========================================

    const guardarEmpleado = async (e) => {

        e.preventDefault();

        if (guardando) return;

        try {
            setGuardando(true);
            setError("");
            setMensaje("");

            let response;

            if (modoEdicion) {

                response = await peticionAutenticada(
                    `${API_EMPLEADOS}/${empleadoEditando.idUsuario}`,
                    {
                        method: "PUT",
                        headers: {
                            "Content-Type": "application/json",
                        },
                        body: JSON.stringify({
                            puesto: formulario.puesto,
                            fechaContrato: formulario.fechaContrato,
                            sueldo: Number(formulario.sueldo),
                        }),
                    }
                );

            } else {

                if (!formulario.password.trim()) {
                    throw new Error(
                        "Debes ingresar una contraseña temporal."
                    );
                }

                response = await peticionAutenticada(
                    `${API_EMPLEADOS}/completo`,
                    {
                        method: "POST",
                        headers: {
                            "Content-Type": "application/json",
                        },
                        body: JSON.stringify({
                            nombre: formulario.nombre.trim(),
                            apellido: formulario.apellido.trim(),
                            correo: formulario.correo
                                .trim()
                                .toLowerCase(),
                            password: formulario.password,
                            puesto: formulario.puesto,
                            fechaContrato: formulario.fechaContrato,
                            sueldo: Number(formulario.sueldo),
                        }),
                    }
                );
            }

            if (!response.ok) {

                const texto = await response.text();

                throw new Error(
                    texto ||
                    `No fue posible guardar el empleado. HTTP ${response.status}`
                );
            }

            const mensajeExito = modoEdicion
                ? "Empleado actualizado correctamente."
                : "Empleado creado correctamente.";

            cerrarFormulario();

            await cargarEmpleados();

            setMensaje(mensajeExito);

        } catch (err) {

            console.error("Error guardando empleado:", err);
            setError(err.message);

        } finally {
            setGuardando(false);
        }
    };

    // ==========================================
    // SUSPENDER / REACTIVAR EMPLEADO
    // ==========================================

    const cambiarEstado = async (empleado) => {

        if (procesandoEstado !== null) return;

        const suspender = empleado.estado === "ACTIVO";

        const accion = suspender
            ? "suspender"
            : "reactivar";

        const confirmar = window.confirm(
            suspender
                ? `¿Deseas suspender la cuenta de ${empleado.nombre} ${empleado.apellido}?`
                : `¿Deseas reactivar la cuenta de ${empleado.nombre} ${empleado.apellido}?`
        );

        if (!confirmar) return;

        try {
            setProcesandoEstado(empleado.idUsuario);

            setError("");
            setMensaje("");

            const response = await peticionAutenticada(
                `${API_USUARIOS}/${empleado.idUsuario}/${accion}`,
                {
                    method: "PUT",
                }
            );

            if (!response.ok) {

                const texto = await response.text();

                throw new Error(
                    texto ||
                    `No fue posible cambiar el estado del empleado. HTTP ${response.status}`
                );
            }

            await cargarEmpleados();

            setMensaje(
                suspender
                    ? "Empleado suspendido correctamente."
                    : "Empleado reactivado correctamente."
            );

        } catch (err) {

            console.error("Error cambiando estado:", err);
            setError(err.message);

        } finally {
            setProcesandoEstado(null);
        }
    };

    // ==========================================
    // INTERFAZ DE EMPLEADOS
    // ==========================================

    return (
        <div className="empleados-page">

            <div className="empleados-header">
                <div>
                    <h1>Empleados</h1>
                    <p>
                        Administra las cuentas y datos laborales
                        de los empleados.
                    </p>
                </div>

                <button
                    type="button"
                    className="btn-nuevo-empleado"
                    onClick={abrirNuevoEmpleado}
                >
                    <FaPlus />
                    Nuevo empleado
                </button>
            </div>

            {mensaje && (
                <div className="mensaje-exito">
                    {mensaje}
                </div>
            )}

            {error && (
                <div className="mensaje-error" role="alert">
                    {error}
                </div>
            )}

            {/* ==================================
                FORMULARIO DE EMPLEADOS
            ================================== */}

            {mostrarFormulario && (
                <div className="empleado-form-card">

                    <div className="form-header">
                        <div>
                            <h2>
                                {modoEdicion
                                    ? "Editar empleado"
                                    : "Nuevo empleado"}
                            </h2>

                            {!modoEdicion && (
                                <p>
                                    El empleado deberá cambiar su
                                    contraseña temporal en su primer ingreso.
                                </p>
                            )}
                        </div>

                        <button
                            type="button"
                            className="btn-cerrar"
                            onClick={cerrarFormulario}
                            disabled={guardando}
                            aria-label="Cerrar formulario"
                        >
                            <FaTimes />
                        </button>
                    </div>

                    <form onSubmit={guardarEmpleado}>

                        {!modoEdicion && (
                            <>
                                <div className="form-grid">

                                    <div className="campo">
                                        <label htmlFor="empleado-nombre">
                                            Nombre
                                        </label>

                                        <input
                                            id="empleado-nombre"
                                            type="text"
                                            name="nombre"
                                            value={formulario.nombre}
                                            onChange={manejarCambio}
                                            disabled={guardando}
                                            required
                                        />
                                    </div>

                                    <div className="campo">
                                        <label htmlFor="empleado-apellido">
                                            Apellido
                                        </label>

                                        <input
                                            id="empleado-apellido"
                                            type="text"
                                            name="apellido"
                                            value={formulario.apellido}
                                            onChange={manejarCambio}
                                            disabled={guardando}
                                            required
                                        />
                                    </div>

                                </div>

                                <div className="form-grid">

                                    <div className="campo">
                                        <label htmlFor="empleado-correo">
                                            Correo corporativo
                                        </label>

                                        <input
                                            id="empleado-correo"
                                            type="email"
                                            name="correo"
                                            placeholder="nombre@frutossecos.cl"
                                            value={formulario.correo}
                                            onChange={manejarCambio}
                                            disabled={guardando}
                                            autoComplete="off"
                                            required
                                        />
                                    </div>

                                    <div className="campo">
                                        <label htmlFor="empleado-password">
                                            Contraseña temporal
                                        </label>

                                        <input
                                            id="empleado-password"
                                            type="password"
                                            name="password"
                                            value={formulario.password}
                                            onChange={manejarCambio}
                                            disabled={guardando}
                                            autoComplete="new-password"
                                            required
                                        />
                                    </div>

                                </div>
                            </>
                        )}

                        {modoEdicion && empleadoEditando && (
                            <div className="empleado-identidad">
                                <FaUserTie />

                                <div>
                                    <strong>
                                        {empleadoEditando.nombre}{" "}
                                        {empleadoEditando.apellido}
                                    </strong>

                                    <span>
                                        {empleadoEditando.correo}
                                    </span>
                                </div>
                            </div>
                        )}

                        <div className="form-grid">

                            <div className="campo">
                                <label htmlFor="empleado-puesto">
                                    Puesto
                                </label>

                                <select
                                    id="empleado-puesto"
                                    name="puesto"
                                    value={formulario.puesto}
                                    onChange={manejarCambio}
                                    disabled={guardando}
                                    required
                                >
                                    <option value="VENTAS">
                                        Ventas
                                    </option>

                                    <option value="INVENTARIO">
                                        Inventario
                                    </option>

                                    <option value="BODEGA">
                                        Bodega
                                    </option>

                                    <option value="CATALOGO">
                                        Catálogo
                                    </option>

                                    <option value="SUPERVISOR">
                                        Supervisor
                                    </option>
                                </select>
                            </div>

                            <div className="campo">
                                <label htmlFor="empleado-contrato">
                                    Fecha de contrato
                                </label>

                                <input
                                    id="empleado-contrato"
                                    type="date"
                                    name="fechaContrato"
                                    value={formulario.fechaContrato}
                                    onChange={manejarCambio}
                                    disabled={guardando}
                                    required
                                />
                            </div>

                            <div className="campo">
                                <label htmlFor="empleado-sueldo">
                                    Sueldo
                                </label>

                                <input
                                    id="empleado-sueldo"
                                    type="number"
                                    name="sueldo"
                                    min="1"
                                    value={formulario.sueldo}
                                    onChange={manejarCambio}
                                    disabled={guardando}
                                    required
                                />
                            </div>

                        </div>

                        <div className="form-actions">

                            <button
                                type="button"
                                className="btn-cancelar"
                                onClick={cerrarFormulario}
                                disabled={guardando}
                            >
                                Cancelar
                            </button>

                            <button
                                type="submit"
                                className="btn-guardar"
                                disabled={guardando}
                            >
                                {guardando
                                    ? "Guardando..."
                                    : modoEdicion
                                        ? "Guardar cambios"
                                        : "Crear empleado"}
                            </button>

                        </div>

                    </form>
                </div>
            )}

            {/* ==================================
                TABLA DE EMPLEADOS
            ================================== */}

            <div className="empleados-table-card">

                {cargando ? (
                    <p>Cargando empleados...</p>

                ) : empleados.length === 0 ? (
                    <p>No existen empleados registrados.</p>

                ) : (
                    <div className="table-responsive">

                        <table className="empleados-table">

                            <thead>
                                <tr>
                                    <th>Empleado</th>
                                    <th>Correo</th>
                                    <th>Puesto</th>
                                    <th>Contrato</th>
                                    <th>Sueldo</th>
                                    <th>Estado</th>
                                    <th>Contraseña</th>
                                    <th>Acciones</th>
                                </tr>
                            </thead>

                            <tbody>

                                {empleados.map((empleado) => (
                                    <tr key={empleado.idUsuario}>

                                        <td>
                                            <div className="empleado-nombre">

                                                <div className="empleado-avatar">
                                                    <FaUserTie />
                                                </div>

                                                <div>
                                                    <strong>
                                                        {empleado.nombre}{" "}
                                                        {empleado.apellido}
                                                    </strong>

                                                    <span>
                                                        ID #{empleado.idUsuario}
                                                    </span>
                                                </div>

                                            </div>
                                        </td>

                                        <td>
                                            {empleado.correo}
                                        </td>

                                        <td>
                                            {empleado.puesto}
                                        </td>

                                        <td>
                                            {empleado.fechaContrato}
                                        </td>

                                        <td>
                                            $
                                            {Number(
                                                empleado.sueldo
                                            ).toLocaleString("es-CL")}
                                        </td>

                                        <td>
                                            <span
                                                className={
                                                    empleado.estado === "ACTIVO"
                                                        ? "estado activo"
                                                        : "estado suspendido"
                                                }
                                            >
                                                {empleado.estado}
                                            </span>
                                        </td>

                                        <td>
                                            {empleado.debeCambiarPassword ? (
                                                <span className="password-pendiente">
                                                    Cambio pendiente
                                                </span>
                                            ) : (
                                                <span className="password-ok">
                                                    <FaCheck /> Actualizada
                                                </span>
                                            )}
                                        </td>

                                        <td>
                                            <div className="acciones-empleado">

                                                <button
                                                    type="button"
                                                    className="btn-editar"
                                                    title="Editar empleado"
                                                    aria-label={`Editar ${empleado.nombre}`}
                                                    disabled={
                                                        procesandoEstado !== null
                                                    }
                                                    onClick={() =>
                                                        abrirEditarEmpleado(empleado)
                                                    }
                                                >
                                                    <FaPen />
                                                </button>

                                                <button
                                                    type="button"
                                                    className={
                                                        empleado.estado === "ACTIVO"
                                                            ? "btn-suspender"
                                                            : "btn-reactivar"
                                                    }
                                                    title={
                                                        empleado.estado === "ACTIVO"
                                                            ? "Suspender empleado"
                                                            : "Reactivar empleado"
                                                    }
                                                    aria-label={
                                                        empleado.estado === "ACTIVO"
                                                            ? `Suspender ${empleado.nombre}`
                                                            : `Reactivar ${empleado.nombre}`
                                                    }
                                                    disabled={
                                                        procesandoEstado !== null
                                                    }
                                                    onClick={() =>
                                                        cambiarEstado(empleado)
                                                    }
                                                >
                                                    {empleado.estado === "ACTIVO" ? (
                                                        <FaBan />
                                                    ) : (
                                                        <FaCheck />
                                                    )}
                                                </button>

                                            </div>
                                        </td>

                                    </tr>
                                ))}

                            </tbody>
                        </table>

                    </div>
                )}

            </div>

        </div>
    );
}

export default Empleados;
