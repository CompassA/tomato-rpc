package org.tomato.study.rpc.core;

import java.net.URI;

/**
 * register service and routing
 * @author Tomato
 * Created on 2021.03.31
 */
public interface NameService {

    /**
     * register service vip
     * @param serviceVIP service identification
     * @param serviceURI service address
     */
    void registerService(String serviceVIP, URI serviceURI);

    /**
     * get service address
     * @param serviceVIP service identification
     * @return service address
     * @throws Exception any exception during look up
     */
    URI lookupService(String serviceVIP) throws Exception;
}
