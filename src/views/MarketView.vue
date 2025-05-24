<template>
  <div class="market-container">
    <div class="market-header">
      <div class="market-title">
        <h3>期货行情中心</h3>
      </div>
      <div class="market-actions">
        <el-select
          v-model="selectedSymbols"
          multiple
          collapse-tags
          placeholder="请选择合约"
          @change="handleSymbolsChange"
        >
          <el-option
            v-for="item in futuresList"
            :key="item.symbol"
            :label="`${item.symbol} - ${item.name}`"
            :value="item.symbol"
          ></el-option>
        </el-select>
      </div>
    </div>

    <div class="market-content">
      <el-table
        :data="marketDataList"
        style="width: 100%"
        :default-sort="{ prop: 'change', order: 'descending' }"
        height="calc(100vh - 250px)"
      >
        <el-table-column prop="symbol" label="合约" width="100" fixed></el-table-column>
        <el-table-column label="最新价" width="120">
          <template #default="scope">
            <span :class="getPriceClass(scope.row.change)">
              {{ formatPrice(scope.row.price) }}
            </span>
          </template>
        </el-table-column>
        <el-table-column label="涨跌幅" width="120" sortable prop="change">
          <template #default="scope">
            <span :class="getPriceClass(scope.row.change)">
              {{ formatChange(scope.row.change) }}
            </span>
          </template>
        </el-table-column>
        <el-table-column prop="open" label="开盘价" width="120">
          <template #default="scope">
            {{ formatPrice(scope.row.open) }}
          </template>
        </el-table-column>
        <el-table-column prop="high" label="最高价" width="120">
          <template #default="scope">
            {{ formatPrice(scope.row.high) }}
          </template>
        </el-table-column>
        <el-table-column prop="low" label="最低价" width="120">
          <template #default="scope">
            {{ formatPrice(scope.row.low) }}
          </template>
        </el-table-column>
        <el-table-column prop="volume" label="成交量" width="120">
          <template #default="scope">
            {{ formatVolume(scope.row.volume) }}
          </template>
        </el-table-column>
        <el-table-column prop="amount" label="成交额" width="150">
          <template #default="scope">
            {{ formatAmount(scope.row.amount) }}
          </template>
        </el-table-column>
        <el-table-column prop="timestamp" label="更新时间" width="180">
          <template #default="scope">
            {{ formatTime(scope.row.timestamp) }}
          </template>
        </el-table-column>
      </el-table>
    </div>

    <div class="market-chart" v-if="selectedSymbol">
      <div class="chart-header">
        <h4>{{ selectedSymbol }} 价格走势</h4>
      </div>
      <div ref="chartRef" class="price-chart"></div>
    </div>
  </div>
</template>

<script>
import { ref, computed, onMounted, onUnmounted, watch } from 'vue'
import { useStore } from 'vuex'
import * as echarts from 'echarts'

export default {
  name: 'MarketView',
  setup() {
    const store = useStore()
    const futuresList = ref([])
    const selectedSymbols = ref([])
    const selectedSymbol = ref('')
    const chartRef = ref(null)
    const chart = ref(null)
    const chartData = ref([])
    let unsubscribe = null

    // 从store获取行情数据列表，并根据选中的合约进行过滤
    const marketDataList = computed(() => {
      const allData = store.getters.marketDataList
      if (selectedSymbols.value.length === 0) {
        return []
      }
      return allData.filter(item => selectedSymbols.value.includes(item.symbol))
    })

    // 初始化
    onMounted(async () => {
      try {
        // 获取期货合约列表 - 如果electronAPI不可用，使用模拟数据
        if (window.electronAPI && window.electronAPI.getFuturesList) {
          futuresList.value = await window.electronAPI.getFuturesList()
        } else {
          // 模拟数据
          futuresList.value = [
            { symbol: 'CU2401', name: '沪铜2401' },
            { symbol: 'AL2401', name: '沪铝2401' },
            { symbol: 'ZN2401', name: '沪锌2401' },
            { symbol: 'AU2401', name: '沪金2401' },
            { symbol: 'AG2401', name: '沪银2401' },
            { symbol: 'RB2401', name: '螺纹钢2401' },
            { symbol: 'HC2401', name: '热卷2401' },
            { symbol: 'I2401', name: '铁矿石2401' }
          ]

          // 添加模拟行情数据
          setTimeout(() => {
            futuresList.value.forEach((item) => {
              const mockData = {
                symbol: item.symbol,
                price: 50000 + Math.random() * 20000,
                change: (Math.random() - 0.5) * 10,
                open: 50000 + Math.random() * 20000,
                high: 50000 + Math.random() * 20000,
                low: 50000 + Math.random() * 20000,
                volume: Math.floor(Math.random() * 100000),
                amount: Math.floor(Math.random() * 1000000000),
                timestamp: Date.now()
              }
              store.dispatch('updateMarketData', mockData)
            })
          }, 500)
        }

        // 默认选择前5个合约
        selectedSymbols.value = futuresList.value.slice(0, 5).map(item => item.symbol)

        // 默认选择第一个合约作为图表显示
        if (selectedSymbols.value.length > 0) {
          selectedSymbol.value = selectedSymbols.value[0]
        }

        // 订阅行情数据
        subscribeMarketData()

        // 初始化图表
        initChart()
      } catch (error) {
        console.error('初始化失败:', error)
      }
    })

    // 清理
    onUnmounted(() => {
      if (unsubscribe) {
        unsubscribe()
      }

      if (chart.value) {
        chart.value.dispose()
      }
    })

    // 监听选中合约变化
    watch(selectedSymbols, () => {
      // 更新store中的选中合约
      store.dispatch('selectSymbols', selectedSymbols.value)

      // 重新订阅行情数据
      if (unsubscribe) {
        unsubscribe()
        unsubscribe = null
      }

      // 如果当前选中的图表合约不在选中列表中，则重置
      if (selectedSymbols.value.length > 0) {
        if (!selectedSymbols.value.includes(selectedSymbol.value)) {
          selectedSymbol.value = selectedSymbols.value[0]
        }
      } else {
        selectedSymbol.value = ''
      }

      // 清空图表数据
      chartData.value = []

      // 重新订阅（只有在有选中合约时才订阅）
      if (selectedSymbols.value.length > 0) {
        subscribeMarketData()
      }
    })

    // 监听选中的图表合约变化
    watch(selectedSymbol, () => {
      // 清空图表数据
      chartData.value = []

      // 更新图表
      updateChart()
    })

    // 订阅行情数据
    const subscribeMarketData = () => {
      if (selectedSymbols.value.length === 0) return

      if (window.electronAPI && window.electronAPI.subscribeMarketData) {
        unsubscribe = window.electronAPI.subscribeMarketData(
          selectedSymbols.value,
          (data) => {
            // 更新store中的行情数据
            store.dispatch('updateMarketData', data)

            // 如果是当前选中的合约，更新图表数据
            if (data.symbol === selectedSymbol.value) {
              // 限制图表数据点数量，保持性能
              if (chartData.value.length > 100) {
                chartData.value.shift()
              }

              chartData.value.push({
                time: new Date(data.timestamp),
                price: data.price
              })

              // 更新图表
              updateChart()
            }
          }
        )
      } else {
        // 模拟实时数据更新
        const interval = setInterval(() => {
          selectedSymbols.value.forEach(symbol => {
            const currentData = store.state.marketData[symbol]
            if (currentData) {
              const mockData = {
                ...currentData,
                price: currentData.price + (Math.random() - 0.5) * 100,
                change: (Math.random() - 0.5) * 5,
                timestamp: Date.now()
              }

              store.dispatch('updateMarketData', mockData)

              // 如果是当前选中的合约，更新图表数据
              if (symbol === selectedSymbol.value) {
                // 限制图表数据点数量，保持性能
                if (chartData.value.length > 100) {
                  chartData.value.shift()
                }

                chartData.value.push({
                  time: new Date(mockData.timestamp),
                  price: mockData.price
                })

                // 更新图表
                updateChart()
              }
            }
          })
        }, 2000) // 每2秒更新一次

        unsubscribe = () => clearInterval(interval)
      }
    }

    // 初始化图表
    const initChart = () => {
      if (chartRef.value) {
        chart.value = echarts.init(chartRef.value)
        updateChart()
      }
    }

    // 更新图表
    const updateChart = () => {
      if (!chart.value || !selectedSymbol.value) return

      const option = {
        title: {
          text: `${selectedSymbol.value} 实时价格`,
          left: 'center'
        },
        tooltip: {
          trigger: 'axis',
          formatter: function(params) {
            const data = params[0].data
            return `
              时间: ${new Date(data[0]).toLocaleTimeString()}<br/>
              价格: ${data[1].toFixed(2)}
            `
          }
        },
        xAxis: {
          type: 'time',
          splitLine: {
            show: false
          }
        },
        yAxis: {
          type: 'value',
          scale: true,
          splitLine: {
            show: true
          }
        },
        series: [
          {
            name: '价格',
            type: 'line',
            showSymbol: false,
            data: chartData.value.map(item => [item.time, item.price]),
            lineStyle: {
              width: 2
            }
          }
        ],
        grid: {
          left: '3%',
          right: '4%',
          bottom: '3%',
          containLabel: true
        }
      }

      chart.value.setOption(option)
    }

    // 处理合约选择变化
    const handleSymbolsChange = (value) => {
      selectedSymbols.value = value
    }

    // 格式化价格
    const formatPrice = (price) => {
      return price ? price.toFixed(2) : '0.00'
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

    // 格式化时间
    const formatTime = (timestamp) => {
      return timestamp ? new Date(timestamp).toLocaleTimeString() : ''
    }

    // 获取价格样式类
    const getPriceClass = (change) => {
      if (!change) return ''
      return change > 0 ? 'price-up' : change < 0 ? 'price-down' : ''
    }

    return {
      futuresList,
      selectedSymbols,
      selectedSymbol,
      marketDataList,
      chartRef,
      handleSymbolsChange,
      formatPrice,
      formatChange,
      formatVolume,
      formatAmount,
      formatTime,
      getPriceClass
    }
  }
}
</script>

<style scoped>
.market-container {
  padding: 20px;
  height: calc(100vh - 60px);
  display: flex;
  flex-direction: column;
}

.market-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.market-content {
  flex: 1;
}

.market-chart {
  height: 300px;
  margin-top: 20px;
  border: 1px solid #ebeef5;
  border-radius: 4px;
  padding: 10px;
}

.chart-header {
  margin-bottom: 10px;
}

.price-chart {
  height: 250px;
}

.price-up {
  color: #f56c6c;
}

.price-down {
  color: #67c23a;
}
</style>