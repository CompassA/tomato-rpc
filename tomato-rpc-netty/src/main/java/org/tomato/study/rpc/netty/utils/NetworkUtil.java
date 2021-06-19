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

package org.tomato.study.rpc.netty.utils;

import org.apache.commons.lang3.StringUtils;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;
import java.util.regex.Pattern;

/**
 * @author Tomato
 * Created on 2021.06.12
 */
public final class NetworkUtil {

    private static final Pattern IP_PATTERN = Pattern.compile("\\d{1,3}(\\.\\d{1,3}){3,5}$");

    private static final String LOCAL_HOST = "127.0.0.1";

    private static final String ANY_HOST = "0.0.0.0";

    private NetworkUtil() {
    }

    public static String getLocalHost() {
        InetAddress address = null;
        try {
            address = getLocalAddress();
        } catch (SocketException e) {
            e.printStackTrace();
        }
        return address == null ? LOCAL_HOST : address.getHostAddress();
    }

    public static InetAddress getLocalAddress() throws SocketException {
        Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
        while (interfaces.hasMoreElements()) {
            NetworkInterface networkInterface = interfaces.nextElement();
            if (networkInterface == null || networkInterface.isLoopback()
                    || networkInterface.isVirtual() || !networkInterface.isUp()) {
                continue;
            }
            Enumeration<InetAddress> inetAddresses = networkInterface.getInetAddresses();
            while (inetAddresses.hasMoreElements()) {
                InetAddress inetAddress = inetAddresses.nextElement();
                if (inetAddress == null || inetAddress.isLoopbackAddress()) {
                    continue;
                }
                String hostAddress = inetAddress.getHostAddress();
                if (!StringUtils.isBlank(hostAddress) && IP_PATTERN.matcher(hostAddress).matches()
                        && !ANY_HOST.equals(hostAddress)) {
                    return inetAddress;
                }
            }
        }
        return null;
    }

}
