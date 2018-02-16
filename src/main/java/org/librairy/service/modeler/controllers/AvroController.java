package org.librairy.service.modeler.controllers;

import org.librairy.service.modeler.facade.AvroServer;
import org.librairy.service.modeler.facade.model.ModelerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.io.IOException;

@Component
public class AvroController {

    @Autowired
    ModelerService service;

    @Value("#{environment['AVRO_PORT']?:${avro.port}}")
    Integer port;

    String host = "0.0.0.0";

    private AvroServer server;

    @PostConstruct
    public void setup() throws IOException {
        server = new AvroServer(service);
        server.open(host,port);
    }

    @PreDestroy
    public void destroy(){
        if (server != null) server.close();
    }

}
