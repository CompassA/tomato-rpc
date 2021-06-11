package org.tomato.study.rpc.netty.utils;

import org.apache.commons.lang3.StringUtils;
import org.junit.Assert;
import org.junit.Test;

/**
 * @author Tomato
 * Created on 2021.06.12
 */
public class UtilTest {

    @Test
    public void networkUtilTest() {
        String localHost = NetworkUtil.getLocalHost();
        Assert.assertTrue(StringUtils.isNotBlank(localHost) && !"127.0.0.1".equals(localHost));
    }
}
