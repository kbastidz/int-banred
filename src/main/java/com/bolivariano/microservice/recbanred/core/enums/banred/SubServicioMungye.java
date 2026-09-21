package com.bolivariano.microservice.recbanred.core.enums.banred;

/**
 * Sub-servicios del biller MUNGYE (Municipio de Guayaquil). Cada uno maneja
 * su propio formato de token Q0/Q1, identificado por BillServiceCode.
 */
public enum SubServicioMungye {

    CEP(1),
    MILOTE(2),
    PREDIOS(3),
    MERCADOS(4);

    private final int billServiceCode;

    SubServicioMungye(int billServiceCode) {
        this.billServiceCode = billServiceCode;
    }

    public int getBillServiceCode() {
        return billServiceCode;
    }

    public static SubServicioMungye fromBillServiceCode(int billServiceCode) {
        for (SubServicioMungye value : values()) {
            if (value.billServiceCode == billServiceCode)
                return value;
        }
        throw new IllegalArgumentException("BillServiceCode no soportado para MUNGYE: " + billServiceCode);
    }
}
