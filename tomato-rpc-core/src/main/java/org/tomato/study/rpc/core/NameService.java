/*
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.tomato.study.rpc.core;

import org.tomato.study.rpc.core.data.MetaData;
import org.tomato.study.rpc.core.spi.SpiInterface;

import java.net.URI;
import java.util.List;
import java.util.Optional;

/**
 * register service and routing
 * @author Tomato
 * Created on 2021.03.31
 */
@SpiInterface(paramName = "nameService")
public interface NameService {

    /**
     * connect to name server
     * @param nameServiceURI name service connection uri
     */
    void connect(String nameServiceURI);

    /**
     * disconnect from name server
     */
    void disconnect();

    /**
     * register service vip
     * @param metaData service identification、service address
     */
    void registerService(MetaData metaData);

    /**
     * get service address
     * @param serviceVIP service identification
     * @return service address
     * @throws Exception any exception during look up
     */
    Optional<URI> lookupService(String serviceVIP) throws Exception;
}
