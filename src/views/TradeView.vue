<template>
  <div class="trade-container">
    <div class="trade-header">
      <h3>期货交易</h3>
    </div>

    <div class="trade-content">
      <el-row :gutter="20">
        <!-- 左侧：交易面板 -->
        <el-col :span="8">
          <el-card class="trade-panel">
            <template #header>
              <div class="card-header">
                <span>交易面板</span>
              </div>
            </template>

            <el-form :model="tradeForm" label-width="80px">
              <el-form-item label="合约">
                <el-select v-model="tradeForm.symbol" placeholder="请选择合约">
                  <el-option
                    v-for="item in futuresList"
                    :key="item.symbol"
                    :label="`${item.symbol} - ${item.name}`"
                    :value="item.symbol"
                  ></el-option>
                </el-select>
              </el-form-item>

              <el-form-item label="方向">
                <el-radio-group v-model="tradeForm.direction">
                  <el-radio label="buy" class="buy-radio">买入</el-radio>
                  <el-radio label="sell" class="sell-radio">卖出</el-radio>
                </el-radio-group>
              </el-form-item>

              <el-form-item label="数量">
                <el-input-number
                  v-model="tradeForm.quantity"
                  :min="1"
                  :max="1000"
                  placeholder="请输入数量"
                ></el-input-number>
              </el-form-item>

              <el-form-item label="价格">
                <el-input-number
                  v-model="tradeForm.price"
                  :precision="2"
                  :step="0.01"
                  placeholder="请输入价格"
                ></el-input-number>
              </el-form-item>

              <el-form-item>
                <el-button
                  type="primary"
                  @click="submitTrade"
                  :loading="trading"
                  style="width: 100%"
                >
                  {{ trading ? '提交中...' : '提交订单' }}
                </el-button>
              </el-form-item>
            </el-form>
          </el-card>
        </el-col>

        <!-- 中间：持仓信息 -->
        <el-col :span="8">
          <el-card class="position-panel">
            <template #header>
              <div class="card-header">
                <span>持仓信息</span>
              </div>
            </template>

            <el-table :data="positions" style="width: 100%" height="400">
              <el-table-column prop="symbol" label="合约" width="80"></el-table-column>
              <el-table-column prop="direction" label="方向" width="60">
                <template #default="scope">
                  <span :class="scope.row.direction === 'buy' ? 'buy-text' : 'sell-text'">
                    {{ scope.row.direction === 'buy' ? '多' : '空' }}
                  </span>
                </template>
              </el-table-column>
              <el-table-column prop="quantity" label="数量" width="80"></el-table-column>
              <el-table-column prop="avgPrice" label="均价" width="80">
                <template #default="scope">
                  {{ scope.row.avgPrice.toFixed(2) }}
                </template>
              </el-table-column>
              <el-table-column prop="pnl" label="盈亏" width="80">
                <template #default="scope">
                  <span :class="scope.row.pnl >= 0 ? 'profit-text' : 'loss-text'">
                    {{ scope.row.pnl.toFixed(2) }}
                  </span>
                </template>
              </el-table-column>
            </el-table>
          </el-card>
        </el-col>

        <!-- 右侧：订单历史 -->
        <el-col :span="8">
          <el-card class="order-panel">
            <template #header>
              <div class="card-header">
                <span>订单历史</span>
              </div>
            </template>

            <el-table :data="orders" style="width: 100%" height="400">
              <el-table-column prop="symbol" label="合约" width="80"></el-table-column>
              <el-table-column prop="direction" label="方向" width="60">
                <template #default="scope">
                  <span :class="scope.row.direction === 'buy' ? 'buy-text' : 'sell-text'">
                    {{ scope.row.direction === 'buy' ? '买' : '卖' }}
                  </span>
                </template>
              </el-table-column>
              <el-table-column prop="quantity" label="数量" width="60"></el-table-column>
              <el-table-column prop="price" label="价格" width="80">
                <template #default="scope">
                  {{ scope.row.price.toFixed(2) }}
                </template>
              </el-table-column>
              <el-table-column prop="status" label="状态" width="80">
                <template #default="scope">
                  <el-tag :type="getStatusType(scope.row.status)">
                    {{ getStatusText(scope.row.status) }}
                  </el-tag>
                </template>
              </el-table-column>
            </el-table>
          </el-card>
        </el-col>
      </el-row>
    </div>
  </div>
</template>

<script>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'

export default {
  name: 'TradeView',
  setup() {
    const futuresList = ref([])
    const trading = ref(false)
    const positions = ref([])
    const orders = ref([])

    const tradeForm = reactive({
      symbol: '',
      direction: 'buy',
      quantity: 1,
      price: 0
    })

    // 初始化
    onMounted(async () => {
      try {
        // 获取期货合约列表
        futuresList.value = [
          { symbol: 'CU2401', name: '沪铜2401' },
          { symbol: 'AL2401', name: '沪铝2401' },
          { symbol: 'ZN2401', name: '沪锌2401' },
          { symbol: 'AU2401', name: '沪金2401' },
          { symbol: 'AG2401', name: '沪银2401' }
        ]

        // 模拟持仓数据
        positions.value = [
          {
            symbol: 'CU2401',
            direction: 'buy',
            quantity: 10,
            avgPrice: 68500.00,
            pnl: 1250.00
          },
          {
            symbol: 'AL2401',
            direction: 'sell',
            quantity: 5,
            avgPrice: 18200.00,
            pnl: -320.00
          }
        ]

        // 模拟订单历史
        orders.value = [
          {
            symbol: 'CU2401',
            direction: 'buy',
            quantity: 10,
            price: 68500.00,
            status: 'filled'
          },
          {
            symbol: 'AL2401',
            direction: 'sell',
            quantity: 5,
            price: 18200.00,
            status: 'filled'
          },
          {
            symbol: 'ZN2401',
            direction: 'buy',
            quantity: 3,
            price: 25800.00,
            status: 'cancelled'
          }
        ]

        // 默认选择第一个合约
        if (futuresList.value.length > 0) {
          tradeForm.symbol = futuresList.value[0].symbol
        }
      } catch (error) {
        console.error('初始化失败:', error)
        ElMessage.error('初始化失败')
      }
    })

    // 提交交易
    const submitTrade = async () => {
      if (!tradeForm.symbol || !tradeForm.quantity || !tradeForm.price) {
        ElMessage.warning('请填写完整的交易信息')
        return
      }

      trading.value = true

      try {
        // 模拟交易提交
        await new Promise(resolve => setTimeout(resolve, 1000))

        // 添加到订单历史
        orders.value.unshift({
          symbol: tradeForm.symbol,
          direction: tradeForm.direction,
          quantity: tradeForm.quantity,
          price: tradeForm.price,
          status: 'filled'
        })

        ElMessage.success('订单提交成功')

        // 重置表单
        tradeForm.quantity = 1
        tradeForm.price = 0
      } catch (error) {
        console.error('交易失败:', error)
        ElMessage.error('交易失败')
      } finally {
        trading.value = false
      }
    }

    // 获取状态类型
    const getStatusType = (status) => {
      switch (status) {
        case 'filled': return 'success'
        case 'cancelled': return 'danger'
        case 'pending': return 'warning'
        default: return 'info'
      }
    }

    // 获取状态文本
    const getStatusText = (status) => {
      switch (status) {
        case 'filled': return '已成交'
        case 'cancelled': return '已取消'
        case 'pending': return '待成交'
        default: return '未知'
      }
    }

    return {
      futuresList,
      trading,
      positions,
      orders,
      tradeForm,
      submitTrade,
      getStatusType,
      getStatusText
    }
  }
}
</script>

<style scoped>
.trade-container {
  padding: 20px;
  height: calc(100vh - 60px);
}

.trade-header {
  margin-bottom: 20px;
}

.trade-content {
  height: calc(100vh - 100px);
}

.trade-panel,
.position-panel,
.order-panel {
  height: 100%;
}

.card-header {
  font-weight: bold;
}

.buy-radio {
  color: #f56c6c;
}

.sell-radio {
  color: #67c23a;
}

.buy-text {
  color: #f56c6c;
  font-weight: bold;
}

.sell-text {
  color: #67c23a;
  font-weight: bold;
}

.profit-text {
  color: #f56c6c;
  font-weight: bold;
}

.loss-text {
  color: #67c23a;
  font-weight: bold;
}
</style>
