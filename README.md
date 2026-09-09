# NombreMicro

Arquetipo version 1

Framework seguridad properties
=============

```properties
framework-seguridad.wsdl=https://frameworkdes.bbframework.com/WSFrameWork/ServicioSeguridadSCL.asmx?wsdl
framework-seguridad.version=2 # 1 --> version micro, 2 -->  Version micro , por defecto es 2
framework-seguridad.id-application=<id>
framework-seguridad.id-user=<user>
framework-seguridad.key-user=<key-user> #valor entero del key de usuario ej: bib_spring.datasource.username
framework-seguridad.key-password=<key-user> #valor entero del key de password ej: bib_spring.datasource.password
```

SSL Configuration properties
=============

```properties
security.ssl.keystore.trust.path=/secret/truststore.jks
security.ssl.keystore.trust.password=changeit
```

Sybase DB properties
=============

```properties
dbconnection.sybase.datasource.url=jdbc:sybase:Tds:172.16.23.22:2050/cob_internet
dbconnection.sybase.datasource.driver-class=com.sybase.jdbc4.jdbc.SybDriver
dbconnection.sybase.datasource.test-on-borrow=true
dbconnection.sybase.datasource.remove-abandoned=true
dbconnection.sybase.datasource.validation-query=select 1
dbconnection.sybase.datasource.validation-interval=1800000
dbconnection.sybase.datasource.max-active=2
dbconnection.sybase.datasource.min-idle=1
```

> En caso de requerir conexion manual al datasource utilizar

```java
package com.bolivariano.microservice.arquetipo.services;

import com.bolivariano.commons.config.FrameworkSeguridadConfig;
import com.bolivariano.commons.config.SslConfig;
import com.bolivariano.commons.service.framework.FrameworkCredential;
import com.bolivariano.commons.service.framework.FrameworkService;
import jakarta.annotation.PostConstruct;
import org.apache.tomcat.jdbc.pool.DataSource;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.stereotype.Component;

@Component
public class MiBeans {
    private FrameworkSeguridadConfig config;
    private SslConfig ssl;

    public MiBeans(FrameworkSeguridadConfig config, SslConfig ssl) {
        this.config = config;
        this.ssl = ssl;
    }

    @PostConstruct
    public DataSource crateDatasource() {
        ssl.initialize();
        FrameworkCredential credenciales = FrameworkService.loadCredential(config);
        DataSourceBuilder<DataSource> builder = (DataSourceBuilder<DataSource>) DataSourceBuilder.create();

        if (credenciales.hasError()) {
            builder.username("");
            builder.password("");
            return builder.build();
        }

        //Colocar demas propiedades del datasource ...
        builder.username(credenciales.getUsuario());
        builder.password(credenciales.getPassword());
        return builder.build();
    }
}
```

SMTP Configuration properties
=============
`application.properties`

```properties
mailer.host=localhost
mailer.from=johnmazzi@hotmail.com
```

`mibeans.java`

```java
package com.bolivariano.microservice.arquetipo.services;

import com.bolivariano.commons.service.mail.SmtpService;
import org.springframework.stereotype.Component;

@Component
public class MiBeans {
    private SmtpService smtpService;

    public MiBeans(SmtpService smtpService) {
        this.smtpService = smtpService;
    }

    public void sendMail() {
        smtpService.sendMail("destino@mail.com", "Mi subject", "Mi content");
    }
}
```

