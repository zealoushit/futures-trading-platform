package com.trading;

import com.trading.controller.TradingController;
import com.trading.model.ApiResponse;
import com.trading.service.TradingService;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.Map;

import static org.junit.Assert.assertNotNull;

/**
 * 交易应用测试类 - Java 8兼容
 */
@RunWith(SpringRunner.class)
@SpringBootTest
public class TradingApplicationTests {

    @Autowired
    private TradingController tradingController;

    @Autowired
    private TradingService tradingService;

    @Test
    public void contextLoads() {
        // 验证Spring上下文加载成功
        assertNotNull("TradingController should not be null", tradingController);
        assertNotNull("TradingService should not be null", tradingService);
    }

    @Test
    public void testHealthEndpoint() {
        // 测试健康检查接口
        ApiResponse<String> response = tradingController.health();
        assertNotNull("Health response should not be null", response);
        assert(response.isSuccess());
    }

    @Test
    public void testVersionEndpoint() {
        // 测试版本信息接口
        ApiResponse<Map<String, String>> response = tradingController.getVersion();
        assertNotNull("Version response should not be null", response);
        assert(response.isSuccess());
        assertNotNull("Version data should not be null", response.getData());
    }

    @Test
    public void testStatusEndpoint() {
        // 测试状态接口
        ApiResponse<Map<String, Object>> response = tradingController.getStatus();
        assertNotNull("Status response should not be null", response);
        assert(response.isSuccess());
        assertNotNull("Status data should not be null", response.getData());
    }
}
