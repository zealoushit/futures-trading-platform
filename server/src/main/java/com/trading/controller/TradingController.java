package com.trading.controller;

import com.trading.model.ApiResponse;
import com.trading.service.TradingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * 交易控制器 - Java 8兼容
 * 提供REST API接口供前端调用
 */
@RestController
@RequestMapping("/api/trading")
@CrossOrigin(origins = "*") // 允许跨域，适配Electron前端
public class TradingController {

    private static final Logger logger = LoggerFactory.getLogger(TradingController.class);

    @Autowired
    private TradingService tradingService;

    /**
     * 获取连接状态
     */
    @GetMapping("/status")
    public ApiResponse<Map<String, Object>> getStatus() {
        Map<String, Object> status = new HashMap<>();
        status.put("connected", tradingService.isConnected());
        status.put("loggedIn", tradingService.isLoggedIn());
        status.put("timestamp", System.currentTimeMillis());

        return ApiResponse.success("获取状态成功", status);
    }

    /**
     * 用户登录
     */
    @PostMapping("/login")
    public CompletableFuture<ApiResponse<Object>> login() {
        logger.info("收到登录请求");
        return tradingService.login();
    }

    /**
     * 用户登出
     */
    @PostMapping("/logout")
    public CompletableFuture<ApiResponse<Object>> logout() {
        logger.info("收到登出请求");
        return tradingService.logout();
    }

    /**
     * 下单
     */
    @PostMapping("/order")
    public CompletableFuture<ApiResponse<Object>> placeOrder(@RequestBody Map<String, Object> orderRequest) {
        logger.info("收到下单请求: {}", orderRequest);

        try {
            String instrumentId = (String) orderRequest.get("instrumentId");
            String directionStr = (String) orderRequest.get("direction");
            String offsetFlagStr = (String) orderRequest.get("offsetFlag");
            Double price = Double.valueOf(orderRequest.get("price").toString());
            Integer volume = Integer.valueOf(orderRequest.get("volume").toString());

            // 参数验证
            if (instrumentId == null || instrumentId.trim().isEmpty()) {
                return CompletableFuture.completedFuture(
                    ApiResponse.error("合约代码不能为空"));
            }

            if (directionStr == null || directionStr.length() != 1) {
                return CompletableFuture.completedFuture(
                    ApiResponse.error("买卖方向参数错误"));
            }

            if (offsetFlagStr == null || offsetFlagStr.length() != 1) {
                return CompletableFuture.completedFuture(
                    ApiResponse.error("开平标志参数错误"));
            }

            if (price == null || price <= 0) {
                return CompletableFuture.completedFuture(
                    ApiResponse.error("价格参数错误"));
            }

            if (volume == null || volume <= 0) {
                return CompletableFuture.completedFuture(
                    ApiResponse.error("数量参数错误"));
            }

            char direction = directionStr.charAt(0);
            char offsetFlag = offsetFlagStr.charAt(0);

            return tradingService.placeOrder(instrumentId, direction, offsetFlag, price, volume);

        } catch (Exception e) {
            logger.error("下单请求参数解析失败", e);
            return CompletableFuture.completedFuture(
                ApiResponse.error("下单请求参数错误: " + e.getMessage()));
        }
    }

    /**
     * 撤单
     */
    @PostMapping("/cancel")
    public CompletableFuture<ApiResponse<Object>> cancelOrder(@RequestBody Map<String, Object> cancelRequest) {
        logger.info("收到撤单请求: {}", cancelRequest);

        try {
            String orderRef = (String) cancelRequest.get("orderRef");
            Integer frontId = Integer.valueOf(cancelRequest.get("frontId").toString());
            Integer sessionId = Integer.valueOf(cancelRequest.get("sessionId").toString());

            // 参数验证
            if (orderRef == null || orderRef.trim().isEmpty()) {
                return CompletableFuture.completedFuture(
                    ApiResponse.error("报单引用不能为空"));
            }

            if (frontId == null) {
                return CompletableFuture.completedFuture(
                    ApiResponse.error("前置编号不能为空"));
            }

            if (sessionId == null) {
                return CompletableFuture.completedFuture(
                    ApiResponse.error("会话编号不能为空"));
            }

            return tradingService.cancelOrder(orderRef, frontId, sessionId);

        } catch (Exception e) {
            logger.error("撤单请求参数解析失败", e);
            return CompletableFuture.completedFuture(
                ApiResponse.error("撤单请求参数错误: " + e.getMessage()));
        }
    }

    /**
     * 健康检查
     */
    @GetMapping("/health")
    public ApiResponse<String> health() {
        return ApiResponse.success("服务运行正常", "OK");
    }

    /**
     * 查询持仓
     */
    @GetMapping("/position")
    public CompletableFuture<ApiResponse<Object>> queryPosition(@RequestParam(required = false) String instrumentId) {
        logger.info("收到查询持仓请求: {}", instrumentId);
        return tradingService.queryPosition(instrumentId);
    }

    /**
     * 查询资金账户
     */
    @GetMapping("/account")
    public CompletableFuture<ApiResponse<Object>> queryAccount() {
        logger.info("收到查询资金账户请求");
        return tradingService.queryAccount();
    }

    /**
     * 查询报单
     */
    @GetMapping("/orders")
    public CompletableFuture<ApiResponse<Object>> queryOrders(@RequestParam(required = false) String instrumentId) {
        logger.info("收到查询报单请求: {}", instrumentId);
        return tradingService.queryOrders(instrumentId);
    }

    /**
     * 查询成交
     */
    @GetMapping("/trades")
    public CompletableFuture<ApiResponse<Object>> queryTrades(@RequestParam(required = false) String instrumentId) {
        logger.info("收到查询成交请求: {}", instrumentId);
        return tradingService.queryTrades(instrumentId);
    }

    /**
     * 查询合约
     */
    @GetMapping("/instrument/{instrumentId}")
    public CompletableFuture<ApiResponse<Object>> queryInstrument(@PathVariable String instrumentId) {
        logger.info("收到查询合约请求: {}", instrumentId);

        if (instrumentId == null || instrumentId.trim().isEmpty()) {
            return CompletableFuture.completedFuture(
                ApiResponse.error("合约代码不能为空"));
        }

        return tradingService.queryInstrument(instrumentId);
    }

    /**
     * 设置投资者ID
     */
    @PostMapping("/config/investor")
    public ApiResponse<String> setInvestorId(@RequestBody Map<String, String> request) {
        logger.info("收到设置投资者ID请求: {}", request);

        String investorId = request.get("investorId");
        if (investorId == null || investorId.trim().isEmpty()) {
            return ApiResponse.error("投资者ID不能为空");
        }

        // 这里可以设置到配置中或服务中
        // 暂时返回成功，实际应用中需要持久化
        return ApiResponse.success("投资者ID设置成功: " + investorId, investorId);
    }

    /**
     * 获取当前投资者ID
     */
    @GetMapping("/config/investor")
    public ApiResponse<String> getInvestorId() {
        // 这里应该从配置中获取，暂时返回默认值
        String investorId = "default_investor";
        return ApiResponse.success("获取投资者ID成功", investorId);
    }

    /**
     * 获取API版本信息
     */
    @GetMapping("/version")
    public ApiResponse<Map<String, String>> getVersion() {
        Map<String, String> version = new HashMap<>();
        version.put("service", "Trading Middleware");
        version.put("version", "1.0.0");
        version.put("api", "Femas V1.32");
        version.put("java", System.getProperty("java.version"));

        return ApiResponse.success("获取版本信息成功", version);
    }
}
