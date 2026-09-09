package com.bolivariano.microservice.recbanred.core.payloads.output;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import java.io.Serial;
import java.io.Serializable;

@JsonPropertyOrder({"codigo", "estado", "mensajeUsuario", "mensajeSalidaConsultarDeuda", "mensajeSalidaEjecutarPago"})
public class MensajeSalidaProcesar implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private String codigo;
    private String estado;
    private String mensajeUsuario;
    private transient MensajeSalidaEjecutarPago mensajeSalidaEjecutarPago;
    private transient MensajeSalidaConsultarDeuda mensajeSalidaConsultarDeuda;

    public MensajeSalidaProcesar() {
        // no-args constructor requerido por Jackson
    }

    // -------------------------------------------------------------------------
    // Getters — exponen los campos al serializador JSON (Jackson)
    // -------------------------------------------------------------------------

    public String getCodigo() {
        return codigo;
    }

    public String getEstado() {
        return estado;
    }

    public String getMensajeUsuario() {
        return mensajeUsuario;
    }

    public MensajeSalidaEjecutarPago getMensajeSalidaEjecutarPago() {
        return mensajeSalidaEjecutarPago;
    }

    public MensajeSalidaConsultarDeuda getMensajeSalidaConsultarDeuda() {
        return mensajeSalidaConsultarDeuda;
    }

    // -------------------------------------------------------------------------
    // Setters
    // -------------------------------------------------------------------------

    public void setCodigo(String codigo) {
        this.codigo = codigo;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    public void setMensajeUsuario(String mensajeUsuario) {
        this.mensajeUsuario = mensajeUsuario;
    }

    public void setMensajeSalidaEjecutarPago(MensajeSalidaEjecutarPago mensajeSalidaEjecutarPago) {
        this.mensajeSalidaEjecutarPago = mensajeSalidaEjecutarPago;
    }

    public void setMensajeSalidaConsultarDeuda(MensajeSalidaConsultarDeuda mensajeSalidaConsultarDeuda) {
        this.mensajeSalidaConsultarDeuda = mensajeSalidaConsultarDeuda;
    }

    // -------------------------------------------------------------------------
    // Métodos de negocio — construcción fluida del DTO de salida
    // -------------------------------------------------------------------------

    private void init(String codigo, String estado, String mensajeUsuario,
                      MensajeSalidaConsultarDeuda mensajeSalidaConsultarDeuda,
                      MensajeSalidaEjecutarPago mensajeSalidaEjecutarPago) {
        this.codigo = codigo;
        this.estado = estado;
        this.mensajeUsuario = mensajeUsuario;
        this.mensajeSalidaConsultarDeuda = mensajeSalidaConsultarDeuda;
        this.mensajeSalidaEjecutarPago = mensajeSalidaEjecutarPago;
    }

    public MensajeSalidaProcesar success() {
        init("0", "OK", "TRANSACCION REALIZADA CON EXITO", null, null);
        return this;
    }

    public MensajeSalidaProcesar successAutomaticReversal(String mensajeUsuario, MensajeSalidaEjecutarPago ejecutarPago) {
        init("0", "OK", mensajeUsuario, null, ejecutarPago);
        return this;
    }

    public MensajeSalidaProcesar successInquiry(MensajeSalidaConsultarDeuda consultarDeuda) {
        init(consultarDeuda.getCodigoError(), "OK", "CONSULTA REALIZADA", consultarDeuda, null);
        return this;
    }

    public MensajeSalidaProcesar successPayment(MensajeSalidaEjecutarPago ejecutarPago) {
        init(ejecutarPago.getCodigoError(), "OK", "PAGO REALIZADO", null, ejecutarPago);
        return this;
    }

    public MensajeSalidaProcesar successReversal(MensajeSalidaEjecutarPago ejecutarPago) {
        init(ejecutarPago.getCodigoError(), "OK", "REVERSO REALIZADO", null, ejecutarPago);
        return this;
    }

    public MensajeSalidaProcesar error(String codigo, String mensajeUsuario,
                                       MensajeSalidaConsultarDeuda mensajeSalidaConsultarDeuda,
                                       MensajeSalidaEjecutarPago mensajeSalidaEjecutarPago) {
        this.mensajeSalidaConsultarDeuda = mensajeSalidaConsultarDeuda;
        this.mensajeSalidaEjecutarPago = mensajeSalidaEjecutarPago;
        return errorGeneric(codigo, mensajeUsuario);
    }

    public MensajeSalidaProcesar errorGeneric(String codigo, String mensajeUsuario) {
        init(codigo, "ERROR", mensajeUsuario, this.mensajeSalidaConsultarDeuda, this.mensajeSalidaEjecutarPago);
        return this;
    }
}
