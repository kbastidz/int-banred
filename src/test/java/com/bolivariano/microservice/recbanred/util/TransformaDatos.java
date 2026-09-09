package com.bolivariano.microservice.recbanred.util;

import com.google.gson.Gson;

import com.google.gson.GsonBuilder;
import com.google.gson.ToNumberPolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TransformaDatos {

    private static final Logger logger = LoggerFactory.getLogger(TransformaDatos.class);

    private TransformaDatos() {

    }

    public static <T> T transformarJsonObjeto(String nombreArchivoJson, Class<T> kls) {
        String json = UtilsDatos.leerArchivoJson(nombreArchivoJson);

        Gson gson = new GsonBuilder()
                .setObjectToNumberStrategy(ToNumberPolicy.LONG_OR_DOUBLE)
                .create();

        if (json == null) {
            logger.debug("ERROR: No se cargo la información del archivo JSON");
            return null;
        }
        try {
            return gson.fromJson(json, kls);
        } catch (Exception ex) {
            logger.debug("No se pudo transformar el archivo JSON --> Clase: {}, debido a {}", kls, ex.getMessage());
        }
        return null;
    }

    public static String transformarObjetoAJson(Object kls) {
        Gson gson = new Gson();

        try {
            return gson.toJson(kls);
        } catch (Exception ex) {
            logger.debug("No se pudo transformar la Clase: {} --> archivo JSON debido a {}", kls, ex.getMessage());

        }
        return null;
    }
}