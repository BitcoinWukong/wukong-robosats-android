package com.bitcoinwukong.robosats_android.viewmodel

import androidx.lifecycle.LiveData
import com.bitcoinwukong.robosats_android.model.OrderData
import com.bitcoinwukong.robosats_android.model.Robot
import io.matthewnelson.kmp.tor.manager.common.state.TorState
import java.time.LocalDateTime

interface ISharedViewModel {
    val torManagerEvents: LiveData<String>

    val orders: LiveData<List<OrderData>>
    val lastUpdated: LiveData<LocalDateTime>

    val isUpdating: LiveData<Boolean>
    val torState: LiveData<TorState>

    val robotTokens: LiveData<Set<String>>
    val robotsInfoMap: LiveData<Map<String, Robot>>
    val selectedToken: LiveData<String>
    val selectedRobot: LiveData<Robot?>

    val activeOrder: LiveData<OrderData?>

    fun restartTor()
    fun fetchOrders()
    fun updateOrders(newOrders: List<OrderData>)


    fun selectRobot(token: String)
    fun refreshRobotsInfo()
    fun addRobot(token: String)
    fun removeRobot(token: String)

    fun createOrder(orderData: OrderData)

    fun getOrderDetails(robot: Robot, orderId: Int)
    fun pauseResumeOrder(robot: Robot, orderId: Int)

    fun cancelOrder(onResult: (Boolean, String?) -> Unit)
}
