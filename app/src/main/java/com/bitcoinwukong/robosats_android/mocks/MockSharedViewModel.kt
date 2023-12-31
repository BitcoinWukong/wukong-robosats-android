package com.bitcoinwukong.robosats_android.mocks

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.bitcoinwukong.robosats_android.model.MessageData
import com.bitcoinwukong.robosats_android.model.OrderData
import com.bitcoinwukong.robosats_android.model.Robot
import com.bitcoinwukong.robosats_android.viewmodel.CreateOrderParams
import com.bitcoinwukong.robosats_android.viewmodel.ISharedViewModel
import java.time.LocalDateTime

class MockSharedViewModel(
    ordersList: List<OrderData> = emptyList(),
    isUpdating: Boolean = false,
    robotTokens: Set<String> = emptySet(),
    robotsInfoMap: Map<String, Robot> = emptyMap(),
    selectedToken: String? = null,
    selectedRobot: Robot? = null,
    activeOrder: OrderData? = null,
    isTorReady: Boolean = true,
    chatMessages: List<MessageData> = emptyList(),
) : ISharedViewModel {
    override val orders = MutableLiveData(ordersList)

    override val robotTokens: LiveData<Set<String>> = MutableLiveData(robotTokens)
    override val robotsInfoMap: LiveData<Map<String, Robot>> = MutableLiveData(robotsInfoMap)
    override val selectedToken: LiveData<String> =
        MutableLiveData(selectedToken ?: selectedRobot?.token ?: "")
    override val selectedRobot: LiveData<Robot?> = MutableLiveData(selectedRobot)
    override val activeOrder: LiveData<OrderData?> = MutableLiveData(activeOrder)
    override val chatMessages: LiveData<List<MessageData>> = MutableLiveData(chatMessages)
    override val torManagerEvents: LiveData<String> = MutableLiveData("")
    override val lastUpdated: LiveData<LocalDateTime> = MutableLiveData(LocalDateTime.now())
    override val isUpdating: LiveData<Boolean> = MutableLiveData(isUpdating)
    override val isTorReady: LiveData<Boolean> = MutableLiveData(isTorReady)
    override val loadingRobots: LiveData<Set<Robot>> = MutableLiveData(emptySet())

    override fun restartTor() {
        Log.d("MockSharedViewModel", "restartTor called")
    }

    override fun fetchOrders() {
        Log.d("MockSharedViewModel", "fetchOrders called")
    }

    override fun updateOrders(newOrders: List<OrderData>) {
        Log.d("MockSharedViewModel", "updateOrders called")
    }

    override fun selectRobot(token: String) {
        Log.d("MockSharedViewModel", "selectRobot called")
    }

    override fun refreshRobotsInfo() {
        Log.d("MockSharedViewModel", "refreshRobotInfo called")
    }

    override fun addRobot(token: String) {
        Log.d("MockSharedViewModel", "addRobot called")
    }

    override fun removeRobot(token: String) {
        Log.d("MockSharedViewModel", "removeRobot called")
    }

    override fun createOrder(createOrderParams: CreateOrderParams) {
        Log.d("MockSharedViewModel", "createOrder called")
    }

    override fun takeOrder(orderData: OrderData) {
        Log.d("MockSharedViewModel", "takeOrder called")
    }

    override fun confirmOrderFiatReceived(orderData: OrderData) {
        Log.d("MockSharedViewModel", "confirmOrderFiatReceived called")
    }

    override fun getOrderDetails(robot: Robot, orderId: Int, resetCache: Boolean) {
        Log.d("MockSharedViewModel", "getOrderDetails called")
    }

    override fun pauseResumeOrder(robot: Robot, orderId: Int) {
        Log.d("MockSharedViewModel", "pauseResumeOrder called")
    }

    override fun getChatMessages(robot: Robot, orderId: Int) {
        Log.d("MockSharedViewModel", "getChatMessages called")
    }

    override fun sendChatMessage(robot: Robot, orderId: Int, message: String) {
        TODO("Not yet implemented")
    }

    override fun cancelOrder(robot: Robot, orderId: Int) {
        Log.d("MockSharedViewModel", "cancelOrder called")
    }
}
