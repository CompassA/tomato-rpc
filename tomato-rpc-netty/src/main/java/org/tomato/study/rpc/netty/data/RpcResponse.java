package org.tomato.study.rpc.netty.data;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * @author Tomato
 * Created on 2021.06.19
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RpcResponse {

    public int code;

    public String message;

    public Object data;

    public static RpcResponse success(Object data) {
        return new RpcResponse(Code.SUCCESS.getCode(), Code.SUCCESS.getMsg(), data);
    }

    public static RpcResponse fail(Object data) {
        return fail(data, Code.FAIL.getMsg());
    }

    public static RpcResponse fail(Object data, String message) {
        return new RpcResponse(Code.FAIL.getCode(), message, data);
    }
}
