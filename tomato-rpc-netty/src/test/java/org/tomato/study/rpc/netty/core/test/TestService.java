package org.tomato.study.rpc.netty.core.test;

import java.util.List;

/**
 * @author Tomato
 * Created on 2021.04.17
 */
public interface TestService {

    Integer sum(List<Integer> nums);
}
