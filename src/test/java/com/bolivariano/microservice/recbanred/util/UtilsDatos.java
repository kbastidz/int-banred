package com.bolivariano.microservice.recbanred.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;

import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

public class UtilsDatos {

    private static final Logger logger = LoggerFactory.getLogger(UtilsDatos.class);
    private static final String STR_ERROR = "ERROR: {}";

    private UtilsDatos() {
    }

    public static String obtenerDirectorioActual() {
        String directorioActual = "";
        File miDir = new File(".");
        try {
            directorioActual = miDir.getCanonicalPath();
        } catch (Exception ex) {
            logger.error(STR_ERROR, ex.getMessage());
            return null;
        }
        return directorioActual;
    }

    public static String leerArchivoJson(String nombreArchivoJson) {

        StringBuilder contenidoJson = new StringBuilder();
        Resource resource = new ClassPathResource(nombreArchivoJson);
        try (Scanner input = new Scanner(resource.getFile(), StandardCharsets.UTF_8)) {
            while (input.hasNextLine()) {
                String linea = input.nextLine();
                contenidoJson.append(linea);
            }
        } catch (Exception ex) {
            logger.error(STR_ERROR, ex.getMessage());
            return null;
        }
        return contenidoJson.toString();
    }

    public static <T> T given(Class<T> clase, String path, String filename) {

        // Given
        try {
            return TransformaDatos.transformarJsonObjeto(path + filename, clase);
        } catch (Exception ex) {
            logger.debug("ERROR: Test-given --> Clase: {}, Message: {}", clase, ex.getMessage());
            return null;
        }
    }

    public static void then(ResultActions response, String jsonStringRes, String status) {
        try {
            response.andDo(print())
                    .andExpect(jsonPath("$.response.code").value(status))
                    .andExpect(MockMvcResultMatchers.content().string(jsonStringRes));
        } catch (Exception ex) {
            logger.debug("ERROR: Test-then --> Clase: {}, Message: {}", response.getClass(), ex.getMessage());
        }
    }
}