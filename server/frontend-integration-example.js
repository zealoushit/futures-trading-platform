/**
 * 前端集成示例 - Vue + Electron
 * 展示如何与Java中台服务进行交互
 */

// WebSocket连接管理
class TradingWebSocket {
    constructor(baseUrl = 'http://localhost:8080') {
        this.baseUrl = baseUrl;
        this.socket = null;
        this.stompClient = null;
        this.isConnected = false;
        this.callbacks = new Map();
    }

    // 连接WebSocket
    connect() {
        return new Promise((resolve, reject) => {
            try {
                this.socket = new SockJS(`${this.baseUrl}/ws`);
                this.stompClient = Stomp.over(this.socket);
                
                this.stompClient.connect({}, (frame) => {
                    console.log('WebSocket连接成功:', frame);
                    this.isConnected = true;
                    this.setupSubscriptions();
                    resolve(frame);
                }, (error) => {
                    console.error('WebSocket连接失败:', error);
                    this.isConnected = false;
                    reject(error);
                });
            } catch (error) {
                reject(error);
            }
        });
    }

    // 设置订阅
    setupSubscriptions() {
        // 订阅连接状态
        this.stompClient.subscribe('/topic/connection', (message) => {
            const data = JSON.parse(message.body);
            this.handleCallback('connection', data);
        });

        // 订阅登录状态
        this.stompClient.subscribe('/topic/login', (message) => {
            const data = JSON.parse(message.body);
            this.handleCallback('login', data);
        });

        // 订阅报单回报
        this.stompClient.subscribe('/topic/orders', (message) => {
            const data = JSON.parse(message.body);
            this.handleCallback('orders', data);
        });

        // 订阅成交回报
        this.stompClient.subscribe('/topic/trades', (message) => {
            const data = JSON.parse(message.body);
            this.handleCallback('trades', data);
        });

        // 订阅行情连接状态
        this.stompClient.subscribe('/topic/market/connection', (message) => {
            const data = JSON.parse(message.body);
            this.handleCallback('marketConnection', data);
        });

        // 订阅行情数据
        this.stompClient.subscribe('/topic/market/data', (message) => {
            const data = JSON.parse(message.body);
            this.handleCallback('marketData', data);
        });
    }

    // 注册回调
    on(event, callback) {
        if (!this.callbacks.has(event)) {
            this.callbacks.set(event, []);
        }
        this.callbacks.get(event).push(callback);
    }

    // 处理回调
    handleCallback(event, data) {
        const callbacks = this.callbacks.get(event);
        if (callbacks) {
            callbacks.forEach(callback => callback(data));
        }
    }

    // 断开连接
    disconnect() {
        if (this.stompClient && this.isConnected) {
            this.stompClient.disconnect();
            this.isConnected = false;
        }
    }
}

// REST API客户端
class TradingApiClient {
    constructor(baseUrl = 'http://localhost:8080/api') {
        this.baseUrl = baseUrl;
    }

    // 通用请求方法
    async request(method, url, data = null) {
        const options = {
            method,
            headers: {
                'Content-Type': 'application/json',
            },
        };

        if (data) {
            options.body = JSON.stringify(data);
        }

        try {
            const response = await fetch(`${this.baseUrl}${url}`, options);
            const result = await response.json();
            
            if (!result.success) {
                throw new Error(result.message || '请求失败');
            }
            
            return result;
        } catch (error) {
            console.error('API请求失败:', error);
            throw error;
        }
    }

    // 交易相关API
    async getStatus() {
        return this.request('GET', '/trading/status');
    }

    async login() {
        return this.request('POST', '/trading/login');
    }

    async logout() {
        return this.request('POST', '/trading/logout');
    }

    async placeOrder(orderData) {
        return this.request('POST', '/trading/order', orderData);
    }

    async cancelOrder(cancelData) {
        return this.request('POST', '/trading/cancel', cancelData);
    }

    async queryPosition(instrumentId = null) {
        const url = instrumentId ? 
            `/trading/position?instrumentId=${instrumentId}` : 
            '/trading/position';
        return this.request('GET', url);
    }

    async queryAccount() {
        return this.request('GET', '/trading/account');
    }

    async queryOrders(instrumentId = null) {
        const url = instrumentId ? 
            `/trading/orders?instrumentId=${instrumentId}` : 
            '/trading/orders';
        return this.request('GET', url);
    }

    async queryTrades(instrumentId = null) {
        const url = instrumentId ? 
            `/trading/trades?instrumentId=${instrumentId}` : 
            '/trading/trades';
        return this.request('GET', url);
    }

    // 行情相关API
    async getMarketStatus() {
        return this.request('GET', '/market/status');
    }

    async marketLogin() {
        return this.request('POST', '/market/login');
    }

    async subscribeMarketData(instruments) {
        return this.request('POST', '/market/subscribe', { instruments });
    }

    async unsubscribeMarketData(instruments) {
        return this.request('POST', '/market/unsubscribe', { instruments });
    }

    async queryInstrument(instrumentId) {
        return this.request('GET', `/market/instrument/${instrumentId}`);
    }
}

// Vue组件示例
const TradingTerminal = {
    data() {
        return {
            // 连接状态
            isConnected: false,
            isLoggedIn: false,
            isMarketConnected: false,
            isMarketLoggedIn: false,
            
            // 交易数据
            positions: [],
            orders: [],
            trades: [],
            account: null,
            
            // 行情数据
            marketData: new Map(),
            subscribedInstruments: [],
            
            // 表单数据
            orderForm: {
                instrumentId: '',
                direction: '0', // 0-买, 1-卖
                offsetFlag: '0', // 0-开仓, 1-平仓
                price: 0,
                volume: 1
            },
            
            // 客户端实例
            apiClient: null,
            wsClient: null
        };
    },

    async mounted() {
        // 初始化客户端
        this.apiClient = new TradingApiClient();
        this.wsClient = new TradingWebSocket();
        
        // 设置WebSocket回调
        this.setupWebSocketCallbacks();
        
        // 连接WebSocket
        try {
            await this.wsClient.connect();
            console.log('WebSocket连接成功');
        } catch (error) {
            console.error('WebSocket连接失败:', error);
        }
        
        // 获取初始状态
        await this.refreshStatus();
    },

    methods: {
        // 设置WebSocket回调
        setupWebSocketCallbacks() {
            this.wsClient.on('connection', (data) => {
                console.log('交易连接状态:', data);
                this.isConnected = data.success;
            });

            this.wsClient.on('login', (data) => {
                console.log('交易登录状态:', data);
                this.isLoggedIn = data.success;
                if (data.success) {
                    this.refreshData();
                }
            });

            this.wsClient.on('orders', (data) => {
                console.log('报单回报:', data);
                this.updateOrder(data.data);
            });

            this.wsClient.on('trades', (data) => {
                console.log('成交回报:', data);
                this.updateTrade(data.data);
            });

            this.wsClient.on('marketConnection', (data) => {
                console.log('行情连接状态:', data);
                this.isMarketConnected = data.success;
            });

            this.wsClient.on('marketData', (data) => {
                console.log('行情数据:', data);
                this.updateMarketData(data.data);
            });
        },

        // 刷新状态
        async refreshStatus() {
            try {
                const [tradingStatus, marketStatus] = await Promise.all([
                    this.apiClient.getStatus(),
                    this.apiClient.getMarketStatus()
                ]);
                
                this.isConnected = tradingStatus.data.connected;
                this.isLoggedIn = tradingStatus.data.loggedIn;
                this.isMarketConnected = marketStatus.data.connected;
                this.isMarketLoggedIn = marketStatus.data.loggedIn;
                this.subscribedInstruments = marketStatus.data.subscribedInstruments || [];
            } catch (error) {
                console.error('刷新状态失败:', error);
            }
        },

        // 刷新数据
        async refreshData() {
            try {
                const [positions, orders, trades, account] = await Promise.all([
                    this.apiClient.queryPosition(),
                    this.apiClient.queryOrders(),
                    this.apiClient.queryTrades(),
                    this.apiClient.queryAccount()
                ]);
                
                this.positions = positions.data || [];
                this.orders = orders.data || [];
                this.trades = trades.data || [];
                this.account = account.data;
            } catch (error) {
                console.error('刷新数据失败:', error);
            }
        },

        // 登录
        async login() {
            try {
                await this.apiClient.login();
                await this.apiClient.marketLogin();
                console.log('登录成功');
            } catch (error) {
                console.error('登录失败:', error);
                alert('登录失败: ' + error.message);
            }
        },

        // 下单
        async placeOrder() {
            try {
                await this.apiClient.placeOrder(this.orderForm);
                console.log('下单成功');
                this.orderForm = {
                    instrumentId: '',
                    direction: '0',
                    offsetFlag: '0',
                    price: 0,
                    volume: 1
                };
            } catch (error) {
                console.error('下单失败:', error);
                alert('下单失败: ' + error.message);
            }
        },

        // 订阅行情
        async subscribeMarket(instrumentId) {
            try {
                await this.apiClient.subscribeMarketData([instrumentId]);
                console.log('订阅行情成功:', instrumentId);
            } catch (error) {
                console.error('订阅行情失败:', error);
                alert('订阅行情失败: ' + error.message);
            }
        },

        // 更新报单
        updateOrder(orderData) {
            const index = this.orders.findIndex(o => o.orderSysId === orderData.orderSysId);
            if (index >= 0) {
                this.orders.splice(index, 1, orderData);
            } else {
                this.orders.unshift(orderData);
            }
        },

        // 更新成交
        updateTrade(tradeData) {
            this.trades.unshift(tradeData);
        },

        // 更新行情数据
        updateMarketData(data) {
            this.marketData.set(data.instrumentId, data);
        }
    },

    beforeDestroy() {
        if (this.wsClient) {
            this.wsClient.disconnect();
        }
    }
};

// 导出供Electron使用
if (typeof module !== 'undefined' && module.exports) {
    module.exports = {
        TradingWebSocket,
        TradingApiClient,
        TradingTerminal
    };
}
