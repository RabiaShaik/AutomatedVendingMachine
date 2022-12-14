package com.vending.controller;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;


@ResponseStatus(HttpStatus.NOT_FOUND)
public class MachineNotFoundException extends RuntimeException {
    /**
     * Machine not found exception
     *
     * @param machineId the machine name
     */
    public MachineNotFoundException(String machineId) {
        super("could not find machine '" + machineId + "'.");
    }
}
