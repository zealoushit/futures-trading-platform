<template>
  <div class="trading-platform">
    <!-- 主要内容区域 -->
    <div class="main-content">
      <!-- 左侧投资者信息面板 -->
      <div class="left-panel">
        <div class="investor-info">
          <div class="investor-header">
            <h4>投资者信息</h4>
          </div>
          <div class="investor-form">
            <el-form label-width="80px" size="small">
              <el-form-item label="投资者">
                <el-input
                  v-model="investorCode"
                  placeholder="请输入投资者代码"
                  style="width: 150px"
                />
              </el-form-item>
              <el-form-item>
                <el-button type="primary" size="small" @click="queryInvestorInfo">
                  绑定查询
                </el-button>
              </el-form-item>
            </el-form>
          </div>
          <div class="fund-info" v-if="fundInfo">
            <div class="fund-item">
              <span class="label">可用资金:</span>
              <span class="value">{{ formatMoney(fundInfo.available) }}</span>
            </div>
            <div class="fund-item">
              <span class="label">冻结资金:</span>
              <span class="value">{{ formatMoney(fundInfo.frozen) }}</span>
            </div>
            <div class="fund-item">
              <span class="label">总资产:</span>
              <span class="value">{{ formatMoney(fundInfo.total) }}</span>
            </div>
            <div class="fund-item">
              <span class="label">持仓盈亏:</span>
              <span class="value" :class="fundInfo.profit >= 0 ? 'profit' : 'loss'">
                {{ formatMoney(fundInfo.profit) }}
              </span>
            </div>
            <div class="fund-item">
              <span class="label">风险度:</span>
              <span class="value" :class="getRiskClass(fundInfo.riskRatio)">
                {{ (fundInfo.riskRatio * 100).toFixed(2) }}%
              </span>
            </div>
          </div>
        </div>
      </div>

      <!-- 中间行情区域 -->
      <div class="center-panel">
        <div class="market-header">
          <div class="market-title">
            <h3>期货行情中心</h3>
          </div>
          <div class="market-filters">
            <el-select v-model="selectedExchange" placeholder="选择交易所" size="small" style="width: 150px">
              <el-option label="全部" value=""></el-option>
              <el-option
                v-for="exchange in exchanges"
                :key="exchange.name"
                :label="exchange.label"
                :value="exchange.name"
              ></el-option>
            </el-select>
            <el-select v-model="selectedCategory" placeholder="选择品种" size="small" style="width: 120px; margin-left: 10px">
              <el-option label="全部" value=""></el-option>
              <el-option
                v-for="category in categories"
                :key="category"
                :label="category"
                :value="category"
              ></el-option>
            </el-select>
          </div>
        </div>

        <div class="market-table">
          <el-table
            :data="filteredMarketData"
            style="width: 100%"
            height="400px"
            @row-click="handleRowClick"
            highlight-current-row
            :row-class-name="getRowClassName"
          >
            <el-table-column prop="symbol" label="合约" width="100" fixed></el-table-column>
            <el-table-column label="名称" width="120">
              <template #default="scope">
                {{ getContractName(scope.row.symbol) }}
              </template>
            </el-table-column>
            <el-table-column label="交易所" width="80">
              <template #default="scope">
                {{ getContractExchange(scope.row.symbol) }}
              </template>
            </el-table-column>
            <el-table-column label="最新价" width="100">
              <template #default="scope">
                <span :class="getPriceClass(scope.row.change)">
                  {{ formatPrice(scope.row.price) }}
                </span>
              </template>
            </el-table-column>
            <el-table-column label="涨跌" width="80">
              <template #default="scope">
                <span :class="getPriceClass(scope.row.change)">
                  {{ formatPriceChange(scope.row.change, scope.row.price) }}
                </span>
              </template>
            </el-table-column>
            <el-table-column label="涨跌幅" width="80">
              <template #default="scope">
                <span :class="getPriceClass(scope.row.change)">
                  {{ formatChange(scope.row.change) }}
                </span>
              </template>
            </el-table-column>
            <el-table-column prop="open" label="开盘" width="100">
              <template #default="scope">
                {{ formatPrice(scope.row.open) }}
              </template>
            </el-table-column>
            <el-table-column prop="high" label="最高" width="100">
              <template #default="scope">
                {{ formatPrice(scope.row.high) }}
              </template>
            </el-table-column>
            <el-table-column prop="low" label="最低" width="100">
              <template #default="scope">
                {{ formatPrice(scope.row.low) }}
              </template>
            </el-table-column>
            <el-table-column prop="volume" label="成交量" width="100">
              <template #default="scope">
                {{ formatVolume(scope.row.volume) }}
              </template>
            </el-table-column>
            <el-table-column prop="amount" label="成交额" width="120">
              <template #default="scope">
                {{ formatAmount(scope.row.amount) }}
              </template>
            </el-table-column>
          </el-table>
        </div>
      </div>

      <!-- 右侧下单面板 -->
      <div class="right-panel">
        <div class="order-panel">
          <div class="order-header">
            <h4>快速下单</h4>
          </div>
          <div class="order-form">
            <el-form :model="orderForm" label-width="60px" size="small">
              <el-form-item label="合约">
                <el-input v-model="orderForm.symbol" readonly style="width: 120px" />
              </el-form-item>
              <el-form-item label="方向">
                <el-radio-group v-model="orderForm.direction">
                  <el-radio-button label="buy" class="buy-btn">买入</el-radio-button>
                  <el-radio-button label="sell" class="sell-btn">卖出</el-radio-button>
                </el-radio-group>
              </el-form-item>
              <el-form-item label="价格">
                <el-input-number
                  v-model="orderForm.price"
                  :precision="2"
                  :step="1"
                  style="width: 120px"
                />
              </el-form-item>
              <el-form-item label="数量">
                <el-input-number
                  v-model="orderForm.quantity"
                  :min="1"
                  :step="1"
                  style="width: 120px"
                />
              </el-form-item>
              <el-form-item label="类型">
                <el-select v-model="orderForm.type" style="width: 120px">
                  <el-option label="限价单" value="limit"></el-option>
                  <el-option label="市价单" value="market"></el-option>
                </el-select>
              </el-form-item>
              <el-form-item>
                <el-button
                  type="primary"
                  @click="submitOrder"
                  :disabled="!orderForm.symbol"
                  style="width: 100%"
                >
                  提交订单
                </el-button>
              </el-form-item>
            </el-form>
          </div>
        </div>
      </div>
    </div>

    <!-- 下方标签栏区域 -->
    <div class="bottom-panel">
      <el-tabs v-model="activeTab" type="border-card">
        <el-tab-pane label="成交信息" name="trades">
          <el-table :data="tradesList" height="200px" size="small">
            <el-table-column prop="time" label="时间" width="100"></el-table-column>
            <el-table-column prop="symbol" label="合约" width="100"></el-table-column>
            <el-table-column prop="direction" label="方向" width="60">
              <template #default="scope">
                <span :class="scope.row.direction === 'buy' ? 'buy-text' : 'sell-text'">
                  {{ scope.row.direction === 'buy' ? '买' : '卖' }}
                </span>
              </template>
            </el-table-column>
            <el-table-column prop="price" label="价格" width="100"></el-table-column>
            <el-table-column prop="quantity" label="数量" width="80"></el-table-column>
            <el-table-column prop="amount" label="金额" width="120"></el-table-column>
            <el-table-column prop="status" label="状态" width="80"></el-table-column>
          </el-table>
        </el-tab-pane>

        <el-tab-pane label="委托信息" name="orders">
          <el-table :data="ordersList" height="200px" size="small">
            <el-table-column prop="time" label="时间" width="100"></el-table-column>
            <el-table-column prop="symbol" label="合约" width="100"></el-table-column>
            <el-table-column prop="direction" label="方向" width="60">
              <template #default="scope">
                <span :class="scope.row.direction === 'buy' ? 'buy-text' : 'sell-text'">
                  {{ scope.row.direction === 'buy' ? '买' : '卖' }}
                </span>
              </template>
            </el-table-column>
            <el-table-column prop="price" label="价格" width="100"></el-table-column>
            <el-table-column prop="quantity" label="数量" width="80"></el-table-column>
            <el-table-column prop="filled" label="已成交" width="80"></el-table-column>
            <el-table-column prop="status" label="状态" width="80"></el-table-column>
            <el-table-column label="操作" width="100">
              <template #default="scope">
                <el-button
                  v-if="scope.row.status === '未成交'"
                  type="danger"
                  size="mini"
                  @click="cancelOrder(scope.row)"
                >
                  撤单
                </el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>

        <el-tab-pane label="持仓信息" name="positions">
          <el-table :data="positionsList" height="200px" size="small">
            <el-table-column prop="symbol" label="合约" width="100"></el-table-column>
            <el-table-column prop="direction" label="方向" width="60">
              <template #default="scope">
                <span :class="scope.row.direction === 'long' ? 'buy-text' : 'sell-text'">
                  {{ scope.row.direction === 'long' ? '多' : '空' }}
                </span>
              </template>
            </el-table-column>
            <el-table-column prop="quantity" label="持仓量" width="80"></el-table-column>
            <el-table-column prop="avgPrice" label="均价" width="100"></el-table-column>
            <el-table-column prop="currentPrice" label="现价" width="100"></el-table-column>
            <el-table-column prop="profit" label="盈亏" width="100">
              <template #default="scope">
                <span :class="scope.row.profit >= 0 ? 'profit' : 'loss'">
                  {{ formatMoney(scope.row.profit) }}
                </span>
              </template>
            </el-table-column>
            <el-table-column prop="margin" label="保证金" width="120"></el-table-column>
          </el-table>
        </el-tab-pane>
      </el-tabs>
    </div>
  </div>
</template>

<script>
import { ref, computed, onMounted } from 'vue'
import { useStore } from 'vuex'
import { ElMessage } from 'element-plus'

export default {
  name: 'MarketView',
  setup() {
    const store = useStore()
    const futuresList = ref([])
    const selectedExchange = ref('')
    const selectedCategory = ref('')
    const selectedContract = ref(null)
    const activeTab = ref('trades')

    // 投资者信息
    const investorCode = ref('123456789')
    const fundInfo = ref(null)

    // 下单表单
    const orderForm = ref({
      symbol: '',
      direction: 'buy',
      price: 0,
      quantity: 1,
      type: 'limit'
    })

    // 成交、委托、持仓数据
    const tradesList = ref([])
    const ordersList = ref([])
    const positionsList = ref([])

    // 交易所分组
    const exchanges = ref([
      { name: 'SHFE', label: '上海期货交易所' },
      { name: 'DCE', label: '大连商品交易所' },
      { name: 'CZCE', label: '郑州商品交易所' },
      { name: 'CFFEX', label: '中国金融期货交易所' },
      { name: 'INE', label: '上海国际能源交易中心' }
    ])

    // 产品类别
    const categories = computed(() => {
      const cats = new Set()
      futuresList.value.forEach(item => cats.add(item.category))
      return Array.from(cats)
    })

    // 过滤后的行情数据
    const filteredMarketData = computed(() => {
      let data = store.getters.marketDataList

      if (selectedExchange.value) {
        data = data.filter(item => {
          const contract = futuresList.value.find(c => c.symbol === item.symbol)
          return contract && contract.exchange === selectedExchange.value
        })
      }

      if (selectedCategory.value) {
        data = data.filter(item => {
          const contract = futuresList.value.find(c => c.symbol === item.symbol)
          return contract && contract.category === selectedCategory.value
        })
      }

      return data
    })

    // 生成大量模拟数据
    const generateMockData = () => {
      const contracts = []
      const months = ['2401', '2402', '2403', '2404', '2405', '2406', '2407', '2408', '2409', '2410', '2411', '2412']

      // 上海期货交易所 (SHFE) - 扩展到更多品种和月份
      const shfeProducts = [
        { code: 'CU', name: '沪铜', category: '有色金属', basePrice: 68000, range: 5000 },
        { code: 'AL', name: '沪铝', category: '有色金属', basePrice: 18000, range: 2000 },
        { code: 'ZN', name: '沪锌', category: '有色金属', basePrice: 23000, range: 2000 },
        { code: 'PB', name: '沪铅', category: '有色金属', basePrice: 15000, range: 1500 },
        { code: 'NI', name: '沪镍', category: '有色金属', basePrice: 130000, range: 10000 },
        { code: 'SN', name: '沪锡', category: '有色金属', basePrice: 150000, range: 15000 },
        { code: 'AU', name: '沪金', category: '贵金属', basePrice: 380, range: 20 },
        { code: 'AG', name: '沪银', category: '贵金属', basePrice: 4500, range: 300 },
        { code: 'RB', name: '螺纹钢', category: '黑色金属', basePrice: 3800, range: 300 },
        { code: 'HC', name: '热卷', category: '黑色金属', basePrice: 3600, range: 300 },
        { code: 'WR', name: '线材', category: '黑色金属', basePrice: 4000, range: 300 },
        { code: 'FU', name: '燃料油', category: '能源化工', basePrice: 2800, range: 200 },
        { code: 'RU', name: '天然橡胶', category: '能源化工', basePrice: 12000, range: 1000 },
        { code: 'BU', name: '沥青', category: '能源化工', basePrice: 3200, range: 300 },
        { code: 'SS', name: '不锈钢', category: '黑色金属', basePrice: 14000, range: 1000 }
      ]

      // 大连商品交易所 (DCE)
      const dceProducts = [
        { code: 'I', name: '铁矿石', category: '黑色金属', basePrice: 800, range: 80 },
        { code: 'J', name: '焦炭', category: '黑色金属', basePrice: 2200, range: 200 },
        { code: 'JM', name: '焦煤', category: '黑色金属', basePrice: 1400, range: 150 },
        { code: 'M', name: '豆粕', category: '农产品', basePrice: 3200, range: 200 },
        { code: 'Y', name: '豆油', category: '农产品', basePrice: 8500, range: 500 },
        { code: 'A', name: '豆一', category: '农产品', basePrice: 4200, range: 300 },
        { code: 'C', name: '玉米', category: '农产品', basePrice: 2600, range: 200 },
        { code: 'CS', name: '玉米淀粉', category: '农产品', basePrice: 2800, range: 200 },
        { code: 'P', name: '棕榈油', category: '农产品', basePrice: 7500, range: 500 },
        { code: 'V', name: 'PVC', category: '化工', basePrice: 6800, range: 400 },
        { code: 'PP', name: 'PP', category: '化工', basePrice: 8200, range: 500 },
        { code: 'L', name: '聚乙烯', category: '化工', basePrice: 8800, range: 600 },
        { code: 'EB', name: '苯乙烯', category: '化工', basePrice: 8000, range: 500 },
        { code: 'EG', name: '乙二醇', category: '化工', basePrice: 4500, range: 300 },
        { code: 'PG', name: '液化石油气', category: '化工', basePrice: 3800, range: 300 }
      ]

      // 郑州商品交易所 (CZCE)
      const czceProducts = [
        { code: 'CF', name: '棉花', category: '农产品', basePrice: 15000, range: 1000 },
        { code: 'SR', name: '白糖', category: '农产品', basePrice: 5800, range: 400 },
        { code: 'TA', name: 'PTA', category: '化工', basePrice: 5500, range: 400 },
        { code: 'MA', name: '甲醇', category: '化工', basePrice: 2400, range: 200 },
        { code: 'FG', name: '玻璃', category: '建材', basePrice: 1800, range: 150 },
        { code: 'RM', name: '菜粕', category: '农产品', basePrice: 2600, range: 200 },
        { code: 'OI', name: '菜油', category: '农产品', basePrice: 7200, range: 500 },
        { code: 'ZC', name: '动力煤', category: '能源', basePrice: 800, range: 80 },
        { code: 'SA', name: '纯碱', category: '化工', basePrice: 2200, range: 200 },
        { code: 'UR', name: '尿素', category: '化工', basePrice: 2300, range: 200 },
        { code: 'SF', name: '硅铁', category: '黑色金属', basePrice: 7500, range: 500 },
        { code: 'SM', name: '锰硅', category: '黑色金属', basePrice: 6800, range: 400 }
      ]

      // 中国金融期货交易所 (CFFEX)
      const cffexProducts = [
        { code: 'IF', name: '沪深300股指', category: '金融', basePrice: 4200, range: 300 },
        { code: 'IC', name: '中证500股指', category: '金融', basePrice: 6800, range: 400 },
        { code: 'IH', name: '上证50股指', category: '金融', basePrice: 3200, range: 250 },
        { code: 'T', name: '10年期国债', category: '金融', basePrice: 98, range: 2 },
        { code: 'TF', name: '5年期国债', category: '金融', basePrice: 99, range: 1 },
        { code: 'TS', name: '2年期国债', category: '金融', basePrice: 99.5, range: 0.5 }
      ]

      // 上海国际能源交易中心 (INE)
      const ineProducts = [
        { code: 'SC', name: '原油', category: '能源', basePrice: 520, range: 50 },
        { code: 'NR', name: '20号胶', category: '化工', basePrice: 11000, range: 800 },
        { code: 'LU', name: '低硫燃料油', category: '能源', basePrice: 3500, range: 300 },
        { code: 'BC', name: '国际铜', category: '有色金属', basePrice: 68000, range: 5000 }
      ]

      // 生成所有合约
      const allProducts = [
        ...shfeProducts.map(p => ({ ...p, exchange: 'SHFE' })),
        ...dceProducts.map(p => ({ ...p, exchange: 'DCE' })),
        ...czceProducts.map(p => ({ ...p, exchange: 'CZCE' })),
        ...cffexProducts.map(p => ({ ...p, exchange: 'CFFEX' })),
        ...ineProducts.map(p => ({ ...p, exchange: 'INE' }))
      ]

      allProducts.forEach(product => {
        months.forEach(month => {
          contracts.push({
            symbol: `${product.code}${month}`,
            name: `${product.name}${month}`,
            exchange: product.exchange,
            category: product.category,
            basePrice: product.basePrice,
            range: product.range
          })
        })
      })

      return contracts
    }

    // 初始化
    onMounted(async () => {
      try {
        // 生成大量模拟数据
        futuresList.value = generateMockData()

        // 初始化投资者资金信息
        fundInfo.value = {
          available: 1000000,
          frozen: 50000,
          total: 1050000,
          profit: 25000,
          riskRatio: 0.15
        }

        // 生成模拟行情数据
        setTimeout(() => {
          futuresList.value.forEach((item) => {
            const currentPrice = item.basePrice + (Math.random() - 0.5) * item.range
            const openPrice = item.basePrice + (Math.random() - 0.5) * item.range
            const highPrice = Math.max(currentPrice, openPrice) + Math.random() * (item.range * 0.3)
            const lowPrice = Math.min(currentPrice, openPrice) - Math.random() * (item.range * 0.3)

            const mockData = {
              symbol: item.symbol,
              price: currentPrice,
              change: (Math.random() - 0.5) * 8,
              open: openPrice,
              high: highPrice,
              low: lowPrice,
              volume: Math.floor(Math.random() * 100000) + 10000,
              amount: Math.floor(Math.random() * 1000000000) + 100000000,
              timestamp: Date.now()
            }
            store.dispatch('updateMarketData', mockData)
          })
        }, 500)

        // 生成模拟成交数据
        tradesList.value = [
          { time: '09:30:15', symbol: 'CU2401', direction: 'buy', price: '68500', quantity: '10', amount: '685000', status: '已成交' },
          { time: '09:31:22', symbol: 'AL2401', direction: 'sell', price: '18200', quantity: '5', amount: '91000', status: '已成交' },
          { time: '09:32:08', symbol: 'RB2401', direction: 'buy', price: '3850', quantity: '20', amount: '77000', status: '已成交' }
        ]

        // 生成模拟委托数据
        ordersList.value = [
          { time: '09:30:00', symbol: 'CU2401', direction: 'buy', price: '68400', quantity: '10', filled: '0', status: '未成交' },
          { time: '09:31:00', symbol: 'AL2401', direction: 'sell', price: '18300', quantity: '5', filled: '3', status: '部分成交' },
          { time: '09:32:00', symbol: 'RB2401', direction: 'buy', price: '3800', quantity: '20', filled: '20', status: '已成交' }
        ]

        // 生成模拟持仓数据
        positionsList.value = [
          { symbol: 'CU2401', direction: 'long', quantity: '10', avgPrice: '68000', currentPrice: '68500', profit: '5000', margin: '136000' },
          { symbol: 'AL2401', direction: 'short', quantity: '5', avgPrice: '18500', currentPrice: '18200', profit: '1500', margin: '36400' },
          { symbol: 'RB2401', direction: 'long', quantity: '20', avgPrice: '3800', currentPrice: '3850', profit: '1000', margin: '15400' }
        ]

        // 启动实时数据更新
        startRealTimeUpdate()
      } catch (error) {
        console.error('初始化失败:', error)
      }
    })

    // 启动实时数据更新
    const startRealTimeUpdate = () => {
      setInterval(() => {
        // 随机更新一些合约的价格
        const randomContracts = futuresList.value.slice(0, 50) // 只更新前50个合约以提高性能
        randomContracts.forEach(contract => {
          const currentData = store.state.marketData[contract.symbol]
          if (currentData) {
            const priceChange = (Math.random() - 0.5) * (contract.range * 0.1)
            const newPrice = currentData.price + priceChange
            const changePercent = ((newPrice - currentData.open) / currentData.open) * 100

            const updatedData = {
              ...currentData,
              price: newPrice,
              change: changePercent,
              high: Math.max(currentData.high, newPrice),
              low: Math.min(currentData.low, newPrice),
              timestamp: Date.now()
            }
            store.dispatch('updateMarketData', updatedData)
          }
        })
      }, 3000) // 每3秒更新一次
    }

    // 查询投资者信息
    const queryInvestorInfo = () => {
      ElMessage.success('投资者信息查询成功')
      // 这里可以根据投资者代码查询真实的资金信息
    }

    // 处理行情表格行点击
    const handleRowClick = (row) => {
      selectedContract.value = row
      orderForm.value.symbol = row.symbol
      orderForm.value.price = row.price
    }

    // 获取行样式类名
    const getRowClassName = ({ row }) => {
      if (selectedContract.value && selectedContract.value.symbol === row.symbol) {
        return 'selected-row'
      }
      return ''
    }

    // 提交订单
    const submitOrder = () => {
      if (!orderForm.value.symbol) {
        ElMessage.warning('请选择合约')
        return
      }

      const newOrder = {
        time: new Date().toLocaleTimeString(),
        symbol: orderForm.value.symbol,
        direction: orderForm.value.direction,
        price: orderForm.value.price.toFixed(2),
        quantity: orderForm.value.quantity.toString(),
        filled: '0',
        status: '未成交'
      }

      ordersList.value.unshift(newOrder)
      ElMessage.success('订单提交成功')
    }

    // 撤销订单
    const cancelOrder = (order) => {
      const index = ordersList.value.findIndex(o => o === order)
      if (index !== -1) {
        ordersList.value[index].status = '已撤销'
        ElMessage.success('撤单成功')
      }
    }

    // 格式化价格
    const formatPrice = (price) => {
      return price ? price.toFixed(2) : '0.00'
    }

    // 格式化价格变动
    const formatPriceChange = (change, price) => {
      if (!change || !price) return '0.00'
      const changeValue = (price * change) / 100
      return (changeValue > 0 ? '+' : '') + changeValue.toFixed(2)
    }

    // 格式化涨跌幅
    const formatChange = (change) => {
      return change ? (change > 0 ? '+' : '') + change.toFixed(2) + '%' : '0.00%'
    }

    // 格式化成交量
    const formatVolume = (volume) => {
      return volume ? volume.toLocaleString() : '0'
    }

    // 格式化成交额
    const formatAmount = (amount) => {
      if (!amount) return '0'
      if (amount >= 100000000) {
        return (amount / 100000000).toFixed(2) + '亿'
      } else if (amount >= 10000) {
        return (amount / 10000).toFixed(2) + '万'
      } else {
        return amount.toLocaleString()
      }
    }

    // 格式化金额
    const formatMoney = (money) => {
      if (!money) return '0.00'
      return money.toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
    }

    // 获取价格样式类
    const getPriceClass = (change) => {
      if (!change) return ''
      return change > 0 ? 'price-up' : change < 0 ? 'price-down' : ''
    }

    // 获取风险度样式类
    const getRiskClass = (ratio) => {
      if (ratio > 0.8) return 'risk-high'
      if (ratio > 0.5) return 'risk-medium'
      return 'risk-low'
    }

    // 根据合约代码获取合约名称
    const getContractName = (symbol) => {
      const contract = futuresList.value.find(item => item.symbol === symbol)
      return contract ? contract.name : symbol
    }

    // 根据合约代码获取交易所
    const getContractExchange = (symbol) => {
      const contract = futuresList.value.find(item => item.symbol === symbol)
      return contract ? contract.exchange : ''
    }

    return {
      // 数据
      futuresList,
      selectedExchange,
      selectedCategory,
      selectedContract,
      activeTab,
      investorCode,
      fundInfo,
      orderForm,
      tradesList,
      ordersList,
      positionsList,
      exchanges,
      categories,
      filteredMarketData,

      // 方法
      queryInvestorInfo,
      handleRowClick,
      getRowClassName,
      submitOrder,
      cancelOrder,
      formatPrice,
      formatPriceChange,
      formatChange,
      formatVolume,
      formatAmount,
      formatMoney,
      getPriceClass,
      getRiskClass,
      getContractName,
      getContractExchange
    }
  }
}
</script>

<style scoped>
.trading-platform {
  height: calc(100vh - 60px);
  display: flex;
  flex-direction: column;
  padding: 10px;
  gap: 10px;
}

.main-content {
  display: flex;
  flex: 1;
  gap: 10px;
  min-height: 0;
}

/* 左侧投资者信息面板 */
.left-panel {
  width: 250px;
  background: #f8f9fa;
  border: 1px solid #e9ecef;
  border-radius: 6px;
  padding: 15px;
}

.investor-info {
  height: 100%;
}

.investor-header h4 {
  margin: 0 0 15px 0;
  color: #333;
  font-size: 16px;
}

.investor-form {
  margin-bottom: 20px;
}

.fund-info {
  background: white;
  border-radius: 4px;
  padding: 15px;
  border: 1px solid #e9ecef;
}

.fund-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 10px;
  font-size: 14px;
}

.fund-item:last-child {
  margin-bottom: 0;
}

.fund-item .label {
  color: #666;
}

.fund-item .value {
  font-weight: 500;
  color: #333;
}

.fund-item .value.profit {
  color: #f56c6c;
}

.fund-item .value.loss {
  color: #67c23a;
}

.fund-item .value.risk-low {
  color: #67c23a;
}

.fund-item .value.risk-medium {
  color: #e6a23c;
}

.fund-item .value.risk-high {
  color: #f56c6c;
}

/* 中间行情区域 */
.center-panel {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-width: 0;
}

.market-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 10px;
  padding: 0 10px;
}

.market-title h3 {
  margin: 0;
  color: #333;
  font-size: 18px;
}

.market-filters {
  display: flex;
  align-items: center;
}

.market-table {
  flex: 1;
  border: 1px solid #e9ecef;
  border-radius: 6px;
  overflow: hidden;
}

/* 右侧下单面板 */
.right-panel {
  width: 220px;
  background: #f8f9fa;
  border: 1px solid #e9ecef;
  border-radius: 6px;
  padding: 15px;
}

.order-panel {
  height: 100%;
}

.order-header h4 {
  margin: 0 0 15px 0;
  color: #333;
  font-size: 16px;
}

.order-form {
  background: white;
  border-radius: 4px;
  padding: 15px;
  border: 1px solid #e9ecef;
}

/* 下方标签栏区域 */
.bottom-panel {
  height: 250px;
  border: 1px solid #e9ecef;
  border-radius: 6px;
  overflow: hidden;
}

/* 表格样式 */
.selected-row {
  background-color: #e6f7ff !important;
}

.price-up {
  color: #f56c6c;
  font-weight: 500;
}

.price-down {
  color: #67c23a;
  font-weight: 500;
}

.buy-text {
  color: #f56c6c;
  font-weight: 500;
}

.sell-text {
  color: #67c23a;
  font-weight: 500;
}

.profit {
  color: #f56c6c;
  font-weight: 500;
}

.loss {
  color: #67c23a;
  font-weight: 500;
}

/* 下单按钮样式 */
.buy-btn {
  background-color: #f56c6c !important;
  border-color: #f56c6c !important;
  color: white !important;
}

.sell-btn {
  background-color: #67c23a !important;
  border-color: #67c23a !important;
  color: white !important;
}

/* 响应式设计 */
@media (max-width: 1200px) {
  .left-panel {
    width: 200px;
  }

  .right-panel {
    width: 180px;
  }
}

@media (max-width: 992px) {
  .main-content {
    flex-direction: column;
  }

  .left-panel,
  .right-panel {
    width: 100%;
    height: auto;
  }

  .bottom-panel {
    height: 200px;
  }
}
</style>