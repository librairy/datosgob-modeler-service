package org.librairy.service.modeler.controllers;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import org.apache.avro.AvroRemoteException;
import org.librairy.service.modeler.facade.model.ModelerService;
import org.librairy.service.modeler.facade.rest.model.InferenceRequest;
import org.librairy.service.modeler.facade.rest.model.InferenceResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

@RestController
@RequestMapping("/inference")
@Api(tags="/inference", description="topics distribution for a given text")
public class RestInferenceController {

    private static final Logger LOG = LoggerFactory.getLogger(RestInferenceController.class);

    @Autowired
    ModelerService service;

    @PostConstruct
    public void setup(){

    }

    @PreDestroy
    public void destroy(){

    }

    @ApiOperation(value = "confirmation", nickname = "postInference", response=InferenceResult.class)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Success", response = InferenceResult.class),
    })
    @RequestMapping(method = RequestMethod.POST, produces = "application/json")
    public InferenceResult inference(@RequestBody InferenceRequest request)  {
        try {
            return new InferenceResult(service.inference(request.getText()));
        } catch (AvroRemoteException e) {
            throw new RuntimeException(e);
        }
    }

}
