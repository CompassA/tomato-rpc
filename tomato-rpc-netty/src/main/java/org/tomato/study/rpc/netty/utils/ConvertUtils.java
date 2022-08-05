/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.tomato.study.rpc.netty.utils;

import org.apache.commons.lang3.StringUtils;
import org.tomato.study.rpc.core.error.TomatoRpcRuntimeException;
import org.tomato.study.rpc.netty.data.RpcRequestDTO;
import org.tomato.study.rpc.netty.data.RpcRequestModel;
import org.tomato.study.rpc.netty.error.NettyRpcErrorEnum;

import java.util.HashMap;
import java.util.Map;

/**
 * Invocation对象转化
 * @author Tomato
 * Created on 2021.09.30
 */
public final class ConvertUtils {

    private static final Map<String, Class<?>> CLASS_CACHE = new HashMap<>(0);

    private ConvertUtils() {
        throw new IllegalStateException("illegal access");
    }

    public static RpcRequestModel convert(RpcRequestDTO dto) throws ClassNotFoundException {
        String[] argsTypes = dto.getArgsTypes();
        Object[] parameters = dto.getArgs();

        RpcRequestModel.RpcRequestModelBuilder builder = RpcRequestModel.builder()
                .microServiceId(dto.getMicroServiceId())
                .rpcInterFace(getClass(dto.getInterfaceName()))
                .methodName(dto.getMethodName())
                .apiId(dto.getApiId())
                .returnType(StringUtils.isBlank(dto.getReturnType())
                        ? void.class : getClass(dto.getReturnType()));

        if (argsTypes == null && parameters == null) {
            return builder.argsType(new Class<?>[0])
                    .parameters(new Object[0])
                    .build();
        }
        if (argsTypes == null || parameters == null || parameters.length != argsTypes.length) {
            throw new TomatoRpcRuntimeException(
                    NettyRpcErrorEnum.MODEL_DTO_CONVERT_ERROR.create("parameter error"));
        }
        Class<?>[] argsTypeModels = new Class<?>[argsTypes.length];
        for (int i = 0; i < argsTypes.length; ++i) {
            argsTypeModels[i] = getClass(argsTypes[i]);
        }
        return builder.argsType(argsTypeModels)
                .parameters(dto.getArgs())
                .build();
    }

    public static RpcRequestDTO convert(RpcRequestModel model) {
        Object[] args = model.getParameters();
        Class<?>[] argsType = model.getArgsType();

        RpcRequestDTO.RpcRequestDTOBuilder builder = RpcRequestDTO.builder()
                .microServiceId(model.getMicroServiceId())
                .interfaceName(model.getRpcInterFace().getName())
                .returnType(model.getReturnType() == null
                        ? void.class.getName()
                        : model.getReturnType().getName())
                .contextParameterMap(model.getContextParameterMap())
                .methodName(model.getMethodName());

        if (args == null && argsType == null) {
            return builder.args(new Object[0])
                    .argsTypes(new String[0])
                    .build();
        }
        if (args == null || argsType == null || argsType.length != args.length) {
            throw new TomatoRpcRuntimeException(
                    NettyRpcErrorEnum.MODEL_DTO_CONVERT_ERROR.create("parameter error"));
        }
        String[] argsNames = new String[args.length];
        for (int i = 0; i < argsType.length; ++i) {
            argsNames[i] = argsType[i].getName();
        }
        return builder.argsTypes(argsNames)
                .args(args)
                .build();
    }

    private static Class<?> getClass(String className) throws ClassNotFoundException {
        Class<?> clazz = CLASS_CACHE.get(className);
        if (clazz == null) {
            synchronized (CLASS_CACHE) {
                clazz = CLASS_CACHE.get(className);
                if (clazz == null) {
                    clazz = Class.forName(className);
                    CLASS_CACHE.put(className, clazz);
                }
            }
        }
        return clazz;
    }
}
