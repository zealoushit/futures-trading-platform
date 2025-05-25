package com.trading.controller;

import com.trading.model.ApiResponse;
import com.trading.service.MarketService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * 行情控制器 - Java 8兼容
 * 提供行情相关的REST API接口
 */
@RestController
@RequestMapping("/api/market")
@CrossOrigin(origins = "*") // 允许跨域，适配Electron前端
public class MarketController {

    private static final Logger logger = LoggerFactory.getLogger(MarketController.class);

    @Autowired
    private MarketService marketService;

    /**
     * 获取行情连接状态
     */
    @GetMapping("/status")
    public ApiResponse<Map<String, Object>> getStatus() {
        Map<String, Object> status = new HashMap<>();
        status.put("connected", marketService.isConnected());
        status.put("loggedIn", marketService.isLoggedIn());
        status.put("subscribedInstruments", marketService.getSubscribedInstruments());
        status.put("timestamp", System.currentTimeMillis());

        return ApiResponse.success("获取行情状态成功", status);
    }

    /**
     * 行情用户登录
     */
    @PostMapping("/login")
    public CompletableFuture<ApiResponse<Object>> login() {
        logger.info("收到行情登录请求");
        return marketService.login();
    }

    /**
     * 行情用户登出
     */
    @PostMapping("/logout")
    public CompletableFuture<ApiResponse<Object>> logout() {
        logger.info("收到行情登出请求");
        return marketService.logout();
    }

    /**
     * 订阅行情
     */
    @PostMapping("/subscribe")
    public CompletableFuture<ApiResponse<Object>> subscribeMarketData(@RequestBody Map<String, Object> request) {
        logger.info("收到订阅行情请求: {}", request);

        try {
            Object instrumentsObj = request.get("instruments");
            String[] instruments;

            if (instrumentsObj instanceof String[]) {
                instruments = (String[]) instrumentsObj;
            } else if (instrumentsObj instanceof String) {
                instruments = new String[]{(String) instrumentsObj};
            } else if (instrumentsObj instanceof java.util.List) {
                @SuppressWarnings("unchecked")
                java.util.List<String> list = (java.util.List<String>) instrumentsObj;
                instruments = list.toArray(new String[0]);
            } else {
                return CompletableFuture.completedFuture(
                    ApiResponse.error("合约代码参数格式错误，支持格式：字符串、字符串数组或字符串列表"));
            }

            if (instruments.length == 0) {
                return CompletableFuture.completedFuture(
                    ApiResponse.error("合约代码不能为空"));
            }

            return marketService.subscribeMarket(instruments);

        } catch (Exception e) {
            logger.error("订阅行情请求参数解析失败", e);
            return CompletableFuture.completedFuture(
                ApiResponse.error("订阅行情请求参数错误: " + e.getMessage()));
        }
    }

    /**
     * 退订行情
     */
    @PostMapping("/unsubscribe")
    public CompletableFuture<ApiResponse<Object>> unsubscribeMarketData(@RequestBody Map<String, Object> request) {
        logger.info("收到退订行情请求: {}", request);

        try {
            Object instrumentsObj = request.get("instruments");
            String[] instruments;

            if (instrumentsObj instanceof String[]) {
                instruments = (String[]) instrumentsObj;
            } else if (instrumentsObj instanceof String) {
                instruments = new String[]{(String) instrumentsObj};
            } else if (instrumentsObj instanceof java.util.List) {
                @SuppressWarnings("unchecked")
                java.util.List<String> list = (java.util.List<String>) instrumentsObj;
                instruments = list.toArray(new String[0]);
            } else {
                return CompletableFuture.completedFuture(
                    ApiResponse.error("合约代码参数格式错误，支持格式：字符串、字符串数组或字符串列表"));
            }

            if (instruments.length == 0) {
                return CompletableFuture.completedFuture(
                    ApiResponse.error("合约代码不能为空"));
            }

            return marketService.unsubscribeMarket(instruments);

        } catch (Exception e) {
            logger.error("退订行情请求参数解析失败", e);
            return CompletableFuture.completedFuture(
                ApiResponse.error("退订行情请求参数错误: " + e.getMessage()));
        }
    }

    /**
     * 查询合约信息
     */
    @GetMapping("/instrument/{instrumentId}")
    public CompletableFuture<ApiResponse<Object>> queryInstrument(@PathVariable String instrumentId) {
        logger.info("收到查询合约请求: {}", instrumentId);

        if (instrumentId == null || instrumentId.trim().isEmpty()) {
            return CompletableFuture.completedFuture(
                ApiResponse.error("合约代码不能为空"));
        }

        return marketService.queryInstrument(instrumentId);
    }

    /**
     * 批量查询合约信息
     */
    @PostMapping("/instruments")
    public CompletableFuture<ApiResponse<Object>> queryInstruments(@RequestBody Map<String, Object> request) {
        logger.info("收到批量查询合约请求: {}", request);

        try {
            Object instrumentsObj = request.get("instruments");
            String[] instruments;

            if (instrumentsObj instanceof String[]) {
                instruments = (String[]) instrumentsObj;
            } else {
                return CompletableFuture.completedFuture(
                    ApiResponse.error("合约代码参数格式错误"));
            }

            if (instruments.length == 0) {
                return CompletableFuture.completedFuture(
                    ApiResponse.error("合约代码不能为空"));
            }

            // 批量查询，返回第一个合约的查询结果
            // 实际应用中可能需要循环查询所有合约
            return marketService.queryInstrument(instruments[0]);

        } catch (Exception e) {
            logger.error("批量查询合约请求参数解析失败", e);
            return CompletableFuture.completedFuture(
                ApiResponse.error("批量查询合约请求参数错误: " + e.getMessage()));
        }
    }

    /**
     * 获取已订阅的合约列表
     */
    @GetMapping("/subscriptions")
    public ApiResponse<Object> getSubscriptions() {
        return ApiResponse.success("获取订阅列表成功", marketService.getSubscribedInstruments());
    }

    /**
     * 行情健康检查
     */
    @GetMapping("/health")
    public ApiResponse<String> health() {
        return ApiResponse.success("行情服务运行正常", "OK");
    }
}
