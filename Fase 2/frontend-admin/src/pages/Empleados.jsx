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

function Empleados() {
    const [empleados, setEmpleados] = useState([]);
    const [cargando, setCargando] = useState(true);
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

    useEffect(() => {
        cargarEmpleados();
    }, []);

    const cargarEmpleados = async () => {
        try {
            setCargando(true);
            setError("");

            const response = await fetch(API_EMPLEADOS);

            if (!response.ok) {
                throw new Error("No fue posible cargar los empleados");
            }

            const data = await response.json();
            setEmpleados(data);
        } catch (err) {
            setError(err.message);
        } finally {
            setCargando(false);
        }
    };

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
            nombre: empleado.nombre,
            apellido: empleado.apellido,
            correo: empleado.correo,
            password: "",
            puesto: empleado.puesto,
            fechaContrato: empleado.fechaContrato,
            sueldo: empleado.sueldo,
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

    const guardarEmpleado = async (e) => {
        e.preventDefault();

        try {
            setError("");
            setMensaje("");

            let response;

            if (modoEdicion) {
                response = await fetch(
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
                response = await fetch(`${API_EMPLEADOS}/completo`, {
                    method: "POST",
                    headers: {
                        "Content-Type": "application/json",
                    },
                    body: JSON.stringify({
                        nombre: formulario.nombre.trim(),
                        apellido: formulario.apellido.trim(),
                        correo: formulario.correo.trim().toLowerCase(),
                        password: formulario.password,
                        puesto: formulario.puesto,
                        fechaContrato: formulario.fechaContrato,
                        sueldo: Number(formulario.sueldo),
                    }),
                });
            }

            if (!response.ok) {
                const texto = await response.text();

                throw new Error(
                    texto || "No fue posible guardar el empleado"
                );
            }

            setMensaje(
                modoEdicion
                    ? "Empleado actualizado correctamente."
                    : "Empleado creado correctamente."
            );

            cerrarFormulario();
            await cargarEmpleados();
        } catch (err) {
            setError(err.message);
        }
    };

    const cambiarEstado = async (empleado) => {
        const suspender = empleado.estado === "ACTIVO";

        const accion = suspender ? "suspender" : "reactivar";

        const confirmar = window.confirm(
            suspender
                ? `¿Deseas suspender la cuenta de ${empleado.nombre} ${empleado.apellido}?`
                : `¿Deseas reactivar la cuenta de ${empleado.nombre} ${empleado.apellido}?`
        );

        if (!confirmar) {
            return;
        }

        try {
            setError("");
            setMensaje("");

            const response = await fetch(
                `${API_USUARIOS}/${empleado.idUsuario}/${accion}`,
                {
                    method: "PUT",
                }
            );

            if (!response.ok) {
                const texto = await response.text();

                throw new Error(
                    texto || "No fue posible cambiar el estado del empleado"
                );
            }

            setMensaje(
                suspender
                    ? "Empleado suspendido correctamente."
                    : "Empleado reactivado correctamente."
            );

            await cargarEmpleados();
        } catch (err) {
            setError(err.message);
        }
    };

    return (
        <div className="empleados-page">

            <div className="empleados-header">
                <div>
                    <h1>Empleados</h1>
                    <p>
                        Administra las cuentas y datos laborales de los empleados.
                    </p>
                </div>

                <button
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
                <div className="mensaje-error">
                    {error}
                </div>
            )}

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
                                    El empleado deberá cambiar su contraseña
                                    temporal en su primer ingreso.
                                </p>
                            )}
                        </div>

                        <button
                            type="button"
                            className="btn-cerrar"
                            onClick={cerrarFormulario}
                        >
                            <FaTimes />
                        </button>
                    </div>

                    <form onSubmit={guardarEmpleado}>

                        {!modoEdicion && (
                            <>
                                <div className="form-grid">

                                    <div className="campo">
                                        <label>Nombre</label>
                                        <input
                                            type="text"
                                            name="nombre"
                                            value={formulario.nombre}
                                            onChange={manejarCambio}
                                            required
                                        />
                                    </div>

                                    <div className="campo">
                                        <label>Apellido</label>
                                        <input
                                            type="text"
                                            name="apellido"
                                            value={formulario.apellido}
                                            onChange={manejarCambio}
                                            required
                                        />
                                    </div>

                                </div>

                                <div className="form-grid">

                                    <div className="campo">
                                        <label>Correo corporativo</label>
                                        <input
                                            type="email"
                                            name="correo"
                                            placeholder="nombre@frutossecos.cl"
                                            value={formulario.correo}
                                            onChange={manejarCambio}
                                            required
                                        />
                                    </div>

                                    <div className="campo">
                                        <label>Contraseña temporal</label>
                                        <input
                                            type="password"
                                            name="password"
                                            value={formulario.password}
                                            onChange={manejarCambio}
                                            required
                                        />
                                    </div>

                                </div>
                            </>
                        )}

                        {modoEdicion && (
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
                                <label>Puesto</label>

                                <select
                                    name="puesto"
                                    value={formulario.puesto}
                                    onChange={manejarCambio}
                                    required
                                >
                                    <option value="VENTAS">Ventas</option>
                                    <option value="INVENTARIO">Inventario</option>
                                    <option value="BODEGA">Bodega</option>
                                    <option value="CATALOGO">Catálogo</option>
                                    <option value="SUPERVISOR">Supervisor</option>
                                </select>
                            </div>

                            <div className="campo">
                                <label>Fecha de contrato</label>

                                <input
                                    type="date"
                                    name="fechaContrato"
                                    value={formulario.fechaContrato}
                                    onChange={manejarCambio}
                                    required
                                />
                            </div>

                            <div className="campo">
                                <label>Sueldo</label>

                                <input
                                    type="number"
                                    name="sueldo"
                                    min="1"
                                    value={formulario.sueldo}
                                    onChange={manejarCambio}
                                    required
                                />
                            </div>

                        </div>

                        <div className="form-actions">

                            <button
                                type="button"
                                className="btn-cancelar"
                                onClick={cerrarFormulario}
                            >
                                Cancelar
                            </button>

                            <button
                                type="submit"
                                className="btn-guardar"
                            >
                                {modoEdicion
                                    ? "Guardar cambios"
                                    : "Crear empleado"}
                            </button>

                        </div>

                    </form>
                </div>
            )}

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

                                        <td>{empleado.correo}</td>

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
                                                    className="btn-editar"
                                                    title="Editar empleado"
                                                    onClick={() =>
                                                        abrirEditarEmpleado(empleado)
                                                    }
                                                >
                                                    <FaPen />
                                                </button>

                                                <button
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