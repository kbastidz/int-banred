package com.bolivariano.microservice.recbanred.core.constants;

public class Defaults {

    private Defaults() {
    }

    public static final String TRAZABILIDAD_UNICIDAD = "0000000000";
    public static final String TERMINALID = "2K010203";
    public static final int BILL_SERV_CODE = 1;
    public static final String FORMA_PAGO = "1";
    public static final boolean REVERSAR = true;
    public static final String EMPTY = "";
    public static final String SUCCESS_BANRED = "00000";
    public static final String TIMEOUT_CODES_BANRED = "50091,50076,60091";
    public static final String DATE_FORMAT_V2 = "ddMMyyyy";
    public static final String DATE_FORMAT_V1 = "yyyyMMdd"; //USAR POR DEFAULT EL FORMATO AL REVES, DADO A QUE SE AGREGA EL AÑO DENTRO DEL SISTEMA
    public static final String TIME_FORMAT = "HHmmss";
    public static final String FULLDATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ss";
    public static final String SUCCESS_INTEGRATOR = "0";
    public static final String REFERENCIA = "REFERENCIA";
}
