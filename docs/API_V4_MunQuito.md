# API V4 – Recaudación de Impuestos Municipales  
## Municipio del Distrito Metropolitano de Quito

**Versión:** 1.0.0  
**Fecha:** 2026-09-28  
**Protocolo:** ISO 8583 sobre TCP directo  
**Endpoint HTTP del microservicio:** `POST /banred/procesar`

---

## Índice

1. [Descripción general](#1-descripción-general)
2. [Configuración](#2-configuración)
3. [Operaciones](#3-operaciones)
   - [3.1 Consulta](#31-consulta)
   - [3.2 Pago](#32-pago)
   - [3.3 Reverso](#33-reverso)
4. [Estructura del request HTTP](#4-estructura-del-request-http)
5. [Estructura del response HTTP](#5-estructura-del-response-http)
6. [Datos adicionales de entrada](#6-datos-adicionales-de-entrada)
7. [Datos adicionales de salida](#7-datos-adicionales-de-salida)
8. [Códigos de resultado](#8-códigos-de-resultado)
9. [Grupos de impuesto](#9-grupos-de-impuesto)
10. [Indicadores de reverso](#10-indicadores-de-reverso)
11. [Ejemplos de tramas ISO generadas](#11-ejemplos-de-tramas-iso-generadas)
12. [Flujo completo](#12-flujo-completo)
13. [Consideraciones importantes](#13-consideraciones-importantes)

---

## 1. Descripción general

La V4 integra la recaudación de impuestos del Municipio del Distrito Metropolitano de Quito a través del protocolo **ISO 8583** transportado sobre **TCP directo** (sin capa SOAP/HTTP).

A diferencia de las versiones V1/V2/V3 que utilizan SOAP sobre HTTPS, la V4:
- Abre una conexión TCP por transacción
- Envía la trama ISO en texto plano (UTF-8)
- Lee la respuesta completa y la parsea
- Cierra la conexión

El microservicio expone **el mismo endpoint HTTP** `/banred/procesar` para el consumidor. La diferenciación de versión es automática por el campo `e_band_autorizador` en `datosAdicionales`.

**Tipos de transacción soportados:**

| Operación | MTI Request | MTI Response | Tipo Tx |
|-----------|-------------|--------------|---------|
| Consulta  | `0200`      | `0210`       | `310004` |
| Pago      | `0200`      | `0210`       | `010005` |
| Reverso   | `0420`      | `0430`       | `010005` |

---

## 2. Configuración

En `application.yml`:

```yaml
mun-quito:
  host: ""                    # IP o hostname del servidor TCP del Municipio
  port: 0                     # Puerto TCP asignado por el Municipio
  companies: "2300"           # BillCompanyCode asignado por Banred al Municipio de Quito
  acquirer-aba: "06597777"    # Bit 32: ABA institución adquirente
  product-code: "0010031004"  # Bit 52: código de producto (fijo por ficha técnica)
  header-prefix: "ISO01400007"# Prefijo del header de la trama
  header-institution: "3"     # Dígito de institución en el header
```

> **Nota:** `host` y `port` deben completarse con los valores asignados por el Municipio de Quito. El campo `companies` debe actualizarse con el código real que Banred asigne.

---

## 3. Operaciones

### 3.1 Consulta

Permite consultar las deudas pendientes de un contribuyente por número de predio, patente o título de crédito.

**Flujo ISO:**
```
IF → [0200 tipo 310004] → TCP → Municipio
Municipio → [0210 respuesta con deudas] → TCP → IF
```

**Datos que retorna:**
- Nombre del contribuyente
- Lista de deudas (código impuesto, descripción, año, monto)
- Detalle de títulos (número de título, prioridad, índice, obra, dividendo)
- Dirección del contribuyente
- Documento de identidad del propietario

---

### 3.2 Pago

Permite registrar el pago de un impuesto municipal. **No acepta pagos parciales** — siempre es el monto total.

**Prerrequisito:** ejecutar la consulta primero para obtener los arreglos Bit 67 y Bit 68.

**Flujo ISO:**
```
IF → [0200 tipo 010005 + arreglo deudas] → TCP → Municipio
Municipio → [0210 con autorización + desglose] → TCP → IF
```

**Datos que retorna:**
- Número de autorización del Municipio (Bit 119)
- Valor total, rubro, intereses, descuento, recargo, costas
- Detalle de rubros (descripción + valor)
- Fecha de emisión, título de crédito, avalúo comercial

---

### 3.3 Reverso

Anula el pago del mismo día. **No acepta reversas parciales** — siempre es el monto total del pago original.

**Restricciones:**
- Solo se puede reversar el mismo día de la recaudación
- Requiere los datos de la transacción original (secuencial, fecha, hora, terminal)

**Flujo ISO:**
```
IF → [0420 tipo 010005 + datos tx original] → TCP → Municipio
Municipio → [0430 confirmación] → TCP → IF
```

---

## 4. Estructura del request HTTP

**URL:** `POST https://{host}:{port}/banred/procesar`  
**Content-Type:** `application/json`

### 4.1 Consulta

```json
{
  "tipoFlujo": "CONSULTA",
  "mensajeEntradaConsultarDeuda": {
    "canal": "VEN",
    "secuencial": "000001",
    "servicio": {
      "codTipoServicio": "PAG",
      "codigoConvenio": "0",
      "codigoEmpresa": "2300",
      "identificador": "53189",
      "datosAdicionales": {
        "datoAdicional": [
          { "codigo": "e_band_autorizador", "valor": "2300" },
          { "codigo": "grupo_impuesto",     "valor": "001" },
          { "codigo": "e_term",             "valor": "RECCAJ         " },
          { "codigo": "e_operador",         "valor": "023300" }
        ]
      }
    }
  }
}
```

**Campo `identificador`:** número de predio (PREDIOS), número de patente (PATENTES) o número de título (VARIOS).

---

### 4.2 Pago

```json
{
  "tipoFlujo": "PAGO",
  "mensajeEntradaEjecutarPago": {
    "canal": "VEN",
    "secuencial": "000056",
    "valorPago": 897.02,
    "nombreCliente": "UNTUNA ESCOBAR HENRY DAVID",
    "servicio": {
      "codTipoServicio": "PAG",
      "codigoConvenio": "0",
      "codigoEmpresa": "2300",
      "identificador": "454217",
      "datosAdicionales": {
        "datoAdicional": [
          { "codigo": "e_band_autorizador", "valor": "2300" },
          { "codigo": "grupo_impuesto",     "valor": "003" },
          { "codigo": "e_term",             "valor": "RECCAJ         " },
          { "codigo": "e_operador",         "valor": "023300" },
          { "codigo": "arreglo1",           "valor": "<BIT67_DE_CONSULTA>" },
          { "codigo": "arreglo2",           "valor": "<BIT68_DE_CONSULTA>" }
        ]
      }
    }
  }
}
```

> `arreglo1` y `arreglo2` son los bits 67 y 68 recibidos en la respuesta de consulta. El consumidor debe almacenarlos y reenviarlos en el pago.

---

### 4.3 Reverso

```json
{
  "tipoFlujo": "REVERSO",
  "mensajeEntradaEjecutarReverso": {
    "canal": "VEN",
    "secuencial": "000099",
    "valorPago": 897.02,
    "servicio": {
      "codTipoServicio": "PAG",
      "codigoConvenio": "0",
      "codigoEmpresa": "2300",
      "identificador": "454217",
      "datosAdicionales": {
        "datoAdicional": [
          { "codigo": "e_band_autorizador", "valor": "2300" },
          { "codigo": "e_reverso",          "valor": "M" },
          { "codigo": "grupo_impuesto",     "valor": "003" },
          { "codigo": "e_term",             "valor": "RECCAJ         " },
          { "codigo": "e_operador",         "valor": "023300" },
          { "codigo": "e_reverso_ind",      "valor": "02" },
          { "codigo": "e_ssn_corr",         "valor": "000056" },
          { "codigo": "e_fecha_orig",       "valor": "20260922" },
          { "codigo": "e_hora_orig",        "valor": "170410" }
        ]
      }
    }
  }
}
```

---

## 5. Estructura del response HTTP

**Content-Type:** `application/json`

### 5.1 Response de Consulta exitosa

```json
{
  "codigo": "00",
  "estado": "OK",
  "mensajeUsuario": "CONSULTA REALIZADA",
  "mensajeSalidaConsultarDeuda": {
    "codigoError": "00",
    "nombreCliente": "UNTUNA ESCOBAR HENRY DAVID",
    "montoTotal": 897.02,
    "limiteMontoMaximo": 897.02,
    "limiteMontoMinimo": 0,
    "fechaVencimiento": "20260922",
    "identificadorDeuda": "53189",
    "recibos": {
      "recibo": [
        {
          "comprobante": "039",
          "concepto": "260CEM",
          "fecha": "2026",
          "totalAPagar": 897.02,
          "valor": 897.02
        }
      ]
    },
    "datosAdicionales": {
      "datoAdicional": [
        { "codigo": "nombre_contribuyente",    "valor": "UNTUNA ESCOBAR HENRY DAVID" },
        { "codigo": "documento",               "valor": "9406597777" },
        { "codigo": "secuencial_banred",       "valor": "050244" },
        { "codigo": "fecha_local",             "valor": "20260922" },
        { "codigo": "hora_local",              "valor": "170349" },
        { "codigo": "institucion_autorizadora","valor": "06730607" },
        { "codigo": "codigo_resultado",        "valor": "00" }
      ]
    }
  }
}
```

---

### 5.2 Response de Pago exitoso

```json
{
  "codigo": "00",
  "estado": "OK",
  "mensajeUsuario": "PAGO REALIZADO",
  "mensajeSalidaEjecutarPago": {
    "codigoError": "00",
    "referencia": "7251421704",
    "montoTotal": 897.02,
    "fechaPago": "2026-09-22 17:04:10",
    "fechaDebito": "2026-09-22 17:04:10",
    "datosAdicionales": {
      "datoAdicional": [
        { "codigo": "numero_autorizacion", "valor": "7251421704" },
        { "codigo": "nombre_contribuyente","valor": "UNTUNA ESCOBAR HENRY DAVID" },
        { "codigo": "valor_total",         "valor": "000000089700" },
        { "codigo": "intereses",           "valor": "000000000000" },
        { "codigo": "descuento",           "valor": "000000000000" },
        { "codigo": "fecha_emision",       "valor": "20251231" },
        { "codigo": "detalle_rubros",      "valor": "OBRAS EN EL DISTRITO  000000897" },
        { "codigo": "secuencial_banred",   "valor": "072514" }
      ]
    }
  }
}
```

---

### 5.3 Response de Reverso exitoso

```json
{
  "codigo": "00",
  "estado": "OK",
  "mensajeUsuario": "REVERSO REALIZADO",
  "mensajeSalidaEjecutarPago": {
    "codigoError": "00",
    "referencia": "725053",
    "datosAdicionales": {
      "datoAdicional": [
        { "codigo": "codigo_resultado",       "valor": "00" },
        { "codigo": "secuencial_banred",      "valor": "725053" },
        { "codigo": "secuencial_adquirente",  "valor": "443577" },
        { "codigo": "institucion_autorizadora","valor": "06730607" },
        { "codigo": "fecha_local",            "valor": "20260922" },
        { "codigo": "hora_local",             "valor": "165729" }
      ]
    }
  }
}
```

---

### 5.4 Response de error

```json
{
  "codigo": "05",
  "estado": "ERROR",
  "mensajeUsuario": "ESTA RECAUDACION NO SE ENCUENTRA DISPONIBLE POR EL MOMENTO"
}
```

---

## 6. Datos adicionales de entrada

Todos se envían como elementos del arreglo `datosAdicionales.datoAdicional`.

### Campos comunes (todas las operaciones)

| Código | Tipo | Requerido | Descripción |
|--------|------|-----------|-------------|
| `e_band_autorizador` | String | **Sí** | BillCompanyCode del Municipio de Quito. Valor: `"2300"` |
| `grupo_impuesto` | String(3) | **Sí** | Grupo de impuesto. Ver [sección 9](#9-grupos-de-impuesto) |
| `e_term` | String(16) | No | Número de terminal. Default: valor de `banred.accpIdTellerCode` |
| `e_operador` | String(6) | No | Código operador. Default: valor de `banred.accpIdTellerCode` |

### Campos adicionales para Pago

| Código | Tipo | Requerido | Descripción |
|--------|------|-----------|-------------|
| `arreglo1` | String | **Sí** | Bit 67 recibido en la respuesta de consulta (detalle deudas) |
| `arreglo2` | String | **Sí** | Bit 68 recibido en la respuesta de consulta (detalle títulos) |

### Campos adicionales para Reverso

| Código | Tipo | Requerido | Descripción |
|--------|------|-----------|-------------|
| `e_reverso` | String | **Sí** | Tipo de reverso: `M` (manual) o `A` (automático) |
| `e_reverso_ind` | String(2) | No | Indicador de causa del reverso. Ver [sección 10](#10-indicadores-de-reverso). Default: `"02"` |
| `e_ssn_corr` | String(6) | **Sí** | Secuencial del pago original |
| `e_fecha_orig` | String(8) | **Sí** | Fecha del pago original en formato `YYYYMMDD` |
| `e_hora_orig` | String(6) | **Sí** | Hora del pago original en formato `HHMMSS` |

---

## 7. Datos adicionales de salida

### Respuesta de Consulta

| Código | Descripción |
|--------|-------------|
| `nombre_contribuyente` | Nombre y apellidos del contribuyente |
| `documento` | Número de cédula/RUC del propietario |
| `direccion` | Dirección del inmueble |
| `secuencial_banred` | Número secuencial asignado por el switch Banred |
| `fecha_local` | Fecha de la transacción `YYYYMMDD` |
| `hora_local` | Hora de la transacción `HHMMSS` |
| `institucion_autorizadora` | ABA de la institución autorizadora (Municipio) |
| `codigo_resultado` | Código de resultado ISO (`00` = aprobado) |

### Respuesta de Pago

| Código | Descripción |
|--------|-------------|
| `numero_autorizacion` | Código de autorización del Municipio (Bit 119) |
| `nombre_contribuyente` | Nombre del contribuyente |
| `valor_total` | Valor total pagado en centavos (12N) |
| `valor_rubro` | Valor del rubro en centavos |
| `intereses` | Intereses en centavos |
| `descuento` | Descuento aplicado en centavos |
| `recargo` | Recargo aplicado en centavos |
| `costas` | Costas judiciales en centavos |
| `fecha_emision` | Fecha de emisión del título `YYYYMMDD` |
| `titulo_credito` | Código del título de crédito |
| `avaluo_comercial` | Valor del avalúo comercial |
| `secuencial_banred` | Secuencial asignado por el switch |
| `fecha_local` | Fecha de la transacción |
| `hora_local` | Hora de la transacción |
| `detalle_rubros` | Desglose de rubros (descripción + valor, hasta 10) |

### Respuesta de Reverso

| Código | Descripción |
|--------|-------------|
| `codigo_resultado` | `00` = reverso aprobado |
| `secuencial_banred` | Secuencial del reverso asignado por el switch |
| `secuencial_adquirente` | Secuencial original del adquirente |
| `institucion_autorizadora` | ABA de la institución autorizadora |
| `fecha_local` | Fecha del reverso |
| `hora_local` | Hora del reverso |

---

## 8. Códigos de resultado

Definidos en la ficha técnica Anexo 2 (Municipio de Quito):

| Código | Mensaje | Descripción |
|--------|---------|-------------|
| `00` | Transacción Aprobada Exitosamente | OK |
| `01` | Pago ya efectuado | El número de referencia ya tiene pago registrado |
| `02` | No existe Código Catastral | Código catastral no encontrado |
| `03` | No existe Nombre del Contribuyente | No existe en catastro para el año indicado |
| `04` | Error en tasa de recaudación | Inconsistencia en tasa de matrícula |
| `05` | Error por default | Situación de error no declarada |
| `06` | Existe juicio coactivo | Debe acercarse al Municipio |
| `07` | Predio ya fue Pagado | Año y semestre ya pagados |
| `10` | No existe transacción a reversar | Datos inconsistentes en reverso |
| `11` | Existen abonos para este código | Debe pagar en la Municipalidad |
| `12` | Código de transacción inválido | TX no definida |
| `13` | Valores inválidos | Valores inconsistentes |
| `30` | Error en formato del mensaje | Formato de trama incorrecto |
| `89` | Transacción no disponible | Problemas en Base de Datos |
| `91` | Problemas de comunicaciones con Autorizador | BD desconectada del switch |
| `92` | Time-out del Autorizador | BD no responde al switch |

---

## 9. Grupos de impuesto

Corresponde al **Bit 109** de la trama ISO y al campo `grupo_impuesto` en `datosAdicionales`:

| Código | Grupo | Identificador del contribuyente |
|--------|-------|---------------------------------|
| `001` | PREDIOS (Predial y Obras) | Número de Predio |
| `002` | PATENTES | Número de Patente |
| `003` | VARIOS (Multas, alcabalas, utilidad, etc.) | Número de Título (empieza con 6100) |

---

## 10. Indicadores de reverso

Corresponde al **Bit 25** de la trama ISO 0420 y al campo `e_reverso_ind`:

| Código | Descripción |
|--------|-------------|
| `01` | Error de comunicaciones en Institución Adquirente |
| `02` | Anulación del Pago realizada por el operador *(default)* |
| `03` | Time-out del dispositivo Adquirente |
| `04` | Time-out del Autorizador |

---

## 11. Ejemplos de tramas ISO generadas

### Consulta (0200)
```
ISO014000073
0200
E23A00018880900000000000018000000000
0553189
310004
20260922170349
000001
170349
20260922
20260922
0806597777
023300
000001
RECCAJ          
840
0010031004
03001
03001
```
*(en una sola línea sin saltos, mostrado aquí por legibilidad)*

### Pago (0200)
```
ISO014000073 0200 [bitmaps] [Bit2: llave] 010005 [valor] [fechas] [institución] [terminal] 840 0010031004 [arreglo1] [arreglo2] [canal] [grupo]
```

### Reverso (0420)
```
ISO014000073 0420 [bitmaps] [Bit2: llave] 010005 [valor] [fechas] [Bit25: indicador] [institución] [terminal] 840 0010031004 [Bit56: datos tx original] [canal] [grupo]
```

---

## 12. Flujo completo

```
┌─────────────────────────────────────────────────────────────────┐
│                    CONSUMIDOR (IF/Canal)                         │
└─────────────────────┬───────────────────────────────────────────┘
                      │ POST /banred/procesar
                      │ tipoFlujo: CONSULTA
                      ▼
┌─────────────────────────────────────────────────────────────────┐
│              IntegradorController                                │
│  → InquiryBanred.processInquiry()                               │
│  → BanredService.executeInquiry()                               │
│  → getVersionByEnterprise(e_band_autorizador) → V4              │
│  → BanredServiceV4.executeConsulta()                            │
│  → IsoMunQuitoBuilder.buildConsulta() → trama ISO               │
│  → MunQuitoTcpClient.enviar(trama)                              │
└─────────────────────┬───────────────────────────────────────────┘
                      │ TCP: trama 0200/310004
                      ▼
              ┌───────────────┐
              │  Municipio    │
              │  de Quito     │
              │  (TCP Server) │
              └───────┬───────┘
                      │ TCP: respuesta 0210
                      ▼
┌─────────────────────────────────────────────────────────────────┐
│  MunQuitoTcpClient recibe respuesta                             │
│  → IsoMunQuitoParser.parsear() → RespuestaIso                   │
│  → InquiryBanred.mapV4Consulta() → MensajeSalidaConsultarDeuda  │
│  → MensajeSalidaProcesar.successInquiry()                       │
└─────────────────────┬───────────────────────────────────────────┘
                      │ JSON response
                      ▼
              CONSUMIDOR (recibe deudas + recibos)

─── [CONSUMIDOR almacena arreglo1/arreglo2 de la respuesta] ──────

                      │ POST /banred/procesar
                      │ tipoFlujo: PAGO (con arreglo1 + arreglo2)
                      ▼
              [mismo flujo → BanredServiceV4.executePago()]
              [→ trama 0200/010005 con arreglos]
              [← respuesta 0210 con numero_autorizacion]

─── [REVERSO si es necesario, mismo día] ─────────────────────────

                      │ POST /banred/procesar
                      │ tipoFlujo: REVERSO (con e_ssn_corr + fechas)
                      ▼
              [→ trama 0420 con Bit56 datos tx original]
              [← respuesta 0430 confirmación]
```

---

## 13. Consideraciones importantes

### Restricciones del protocolo
- **Sin pagos parciales:** la transacción de pago debe ser por el valor total exacto retornado en la consulta.
- **Sin reversas parciales:** el reverso debe ser por el valor total del pago original.
- **Reversas solo el mismo día:** no se aceptan reversas de días anteriores.
- **Consulta obligatoria antes del pago:** los arreglos Bit 67 y Bit 68 de la consulta deben reenvíarse en el pago.

### Canal TCP
- Una conexión TCP por transacción (no persistente).
- Timeout de conexión y lectura: **30 segundos** (configurable en `banred.connectionTimeout` y `banred.readTimeout`).
- En caso de timeout, el microservicio retorna el error genérico configurado en `banred.genericErrorResponse`.

### Horario de servicio
Según ficha técnica Banred: **lunes a domingo y feriados de 08h00 a 20h00**.  
Las transacciones de fin de semana y feriados se contabilizan el siguiente día laborable.

### Identificación de versión
La V4 se activa automáticamente cuando el campo `e_band_autorizador` en `datosAdicionales` coincide con los códigos configurados en `mun-quito.companies`. No requiere ningún cambio en la URL ni en el contrato del endpoint.

### Canales soportados

| Canal (`canal` en request) | Código ISO (Bit 108) |
|---------------------------|----------------------|
| `VEN` – Ventanilla         | `001` |
| `KIO` – Kiosko             | `002` |
| `ATM` – Cajero automático  | `003` |
| `IBK` – Banca virtual      | `004` |
| `SAT` – SAT                | `004` |
| `IVR` – IVR                | `005` |
| `WAP` – Banca móvil        | `006` |
| `CNB` – CNB                | `007` |
