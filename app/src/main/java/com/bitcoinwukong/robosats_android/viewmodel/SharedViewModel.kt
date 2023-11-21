package com.bitcoinwukong.robosats_android.viewmodel

import android.content.SharedPreferences
import android.util.Log
import androidx.core.content.edit
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bitcoinwukong.robosats_android.model.OrderData
import com.bitcoinwukong.robosats_android.model.Robot
import com.bitcoinwukong.robosats_android.repository.TorRepository
import io.matthewnelson.kmp.tor.manager.common.state.TorState
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.time.LocalDateTime

class SharedViewModel(
    private val torRepository: TorRepository,
    private val sharedPreferences: SharedPreferences
) : ViewModel(), ISharedViewModel {
    companion object {
        private val TAG = SharedViewModel::class.java.simpleName
    }

    override val torManagerEvents: LiveData<String> = torRepository.torManager.events

    private val _orders = MutableLiveData<List<OrderData>>()
    override val orders: LiveData<List<OrderData>> get() = _orders

    private val _lastUpdated = MutableLiveData<LocalDateTime>()
    override val lastUpdated: LiveData<LocalDateTime> get() = _lastUpdated

    override val isUpdating: LiveData<Boolean> get() = torRepository.isUpdating
    override val torState: LiveData<TorState> get() = torRepository.torState

    private var _robotTokens = MutableLiveData<Set<String>>()
    override val robotTokens: LiveData<Set<String>> get() = _robotTokens

    private val _robotsInfoMap = MutableLiveData<Map<String, Robot>>(mapOf())
    override val robotsInfoMap: LiveData<Map<String, Robot>> get() = _robotsInfoMap


    private var _selectedToken = MutableLiveData("")
    override val selectedToken: LiveData<String> get() = _selectedToken
    private var _selectedRobot = MutableLiveData<Robot?>(null)
    override val selectedRobot: LiveData<Robot?> get() = _selectedRobot

    private var _activeOrder = MutableLiveData<OrderData?>(null)
    override val activeOrder: LiveData<OrderData?> get() = _activeOrder

    private val _ordersCache = mutableMapOf<Int, OrderData>()

    override fun restartTor() {
        torRepository.restartTor()
    }

    init {
        refreshRobotsInfo()
        startAutoFetch()
    }

    private fun startAutoFetch() {
        viewModelScope.launch {
            while (isActive) {
                fetchOrders()
                if (_selectedRobot.value != null) {
                    fetchRobotInfo(_selectedRobot.value!!.token)
                }
                delay(60000) // 1 minute delay
            }
        }
    }

    override fun fetchOrders() {
        viewModelScope.launch {
            val result = torRepository.listOrders()
            result.onSuccess { ordersList ->
                _orders.postValue(ordersList)
                _lastUpdated.postValue(LocalDateTime.now())
                Log.d("SharedViewModel", "Orders update completed: $ordersList")
            }.onFailure { e ->
                Log.e("SharedViewModel", "Error fetching orders: ${e.message}")
            }
        }
    }

    override fun updateOrders(newOrders: List<OrderData>) {
        _orders.postValue(newOrders)
        _lastUpdated.postValue(LocalDateTime.now())
    }

    override fun refreshRobotsInfo() {
        Log.d(TAG, "refreshRobotInfo called")

        val tokens = loadTokens()
        if (_selectedToken.value?.isEmpty() == true) {
            selectRobot(tokens.firstOrNull() ?: "")
        }
        _robotTokens.postValue(loadTokens())
        // Fetch all robot info
        tokens.forEach { token ->
            fetchRobotInfo(token)
        }
    }

    private fun fetchRobotInfo(token: String) {
        Log.d(TAG, "fetching robot info for $token")
        viewModelScope.launch {
            val result = torRepository.getRobotInfo(token)

            result.onSuccess { robot ->
                updateRobotInfoInMap(token, robot)
                Log.d(TAG, "Fetched robot info: $robot")
            }.onFailure { e ->
                val errorMessageRobot =
                    Robot(token = token, errorMessage = "Error fetching robot info: ${e.message}")
                updateRobotInfoInMap(token, errorMessageRobot)
                Log.e(TAG, "Error fetching robot info: ${e.message}")
            }
        }
    }

    private fun updateRobotInfoInMap(token: String, robot: Robot) {
        val currentInfo = _robotsInfoMap.value ?: mapOf()
        val updatedInfo = currentInfo.toMutableMap().apply { put(token, robot) }
        _robotsInfoMap.postValue(updatedInfo)

        if (_selectedToken.value == robot.token) {
            updateSelectedRobotInternal(robot)
        }
        if (robot.activeOrderId != null) {
            getOrderDetails(robot, robot.activeOrderId)
        }
    }

    private fun loadTokens(): Set<String> {
        val tokens = sharedPreferences.getStringSet("robot_tokens", setOf()) ?: setOf()
        Log.d(TAG, "Robot tokens loaded: $tokens")
        return tokens
    }

    override fun addRobot(token: String) {
        saveTokens((_robotTokens.value ?: setOf()).plus(token))
        fetchRobotInfo(token)
        updateSelectedRobot(token)
    }

    override fun removeRobot(token: String) {
        val tokens = (_robotTokens.value ?: setOf()).minus(token)
        saveTokens(tokens)

        val updatedInfoMap = _robotsInfoMap.value?.toMutableMap() ?: mutableMapOf()
        updatedInfoMap.remove(token)
        _robotsInfoMap.postValue(updatedInfoMap)

        if (_selectedToken.value == token) {
            updateSelectedRobot(tokens.firstOrNull())
        }
    }

    override fun selectRobot(token: String) {
        updateSelectedRobot(token)
    }

    override fun createOrder(
        orderData: OrderData
    ) {
        val robot = _selectedRobot.value ?: return
        viewModelScope.launch {
            // Todo: update view model and UI base on the orde creation result
            val result = torRepository.makeOrder(
                robot.token,
                orderData.type,
                orderData.currency,
                amount = orderData.amount.toString(),
                paymentMethod = orderData.paymentMethod,
                premium = orderData.premium?.toString() ?: "",
            )
            result.onSuccess {
                fetchRobotInfo(robot.token)


            }.onFailure { e ->
                Log.e(TAG, "Error in createOrder: ${e.message}")
            }
        }
    }

    override fun getOrderDetails(robot: Robot, orderId: Int) {
        Log.d(TAG, "getting order details: $orderId")
        _activeOrder.value = _ordersCache[orderId]

        viewModelScope.launch {
            val result = torRepository.getOrderDetails(robot.token, orderId)
            result.onSuccess { orderData ->
                _ordersCache[orderId] = orderData
                if (_selectedRobot.value?.activeOrderId == orderId) {
                    _activeOrder.postValue(orderData)
                }
                Log.d("SharedViewModel", "getOrderDetails completed: $orderData")
            }.onFailure { e ->
                Log.e("SharedViewModel", "getOrderDetails failed: ${e.message}")
            }
        }
    }

    override fun pauseResumeOrder(robot: Robot, orderId: Int) {
        _activeOrder.value = null
        viewModelScope.launch {
            val result = torRepository.performOrderAction(robot.token, orderId, "pause")
            result.onSuccess {
                getOrderDetails(robot, orderId)
            }.onFailure { _ ->
                getOrderDetails(robot, orderId)
            }
        }
    }

    override fun cancelOrder(onResult: (Boolean, String?) -> Unit) {
        val robot = _selectedRobot.value ?: return
        val orderId = robot.activeOrderId ?: return

        viewModelScope.launch {
            val result = torRepository.performOrderAction(robot.token, orderId, "cancel")

            result.onSuccess {
                fetchRobotInfo(robot.token)
                onResult(true, null)
            }.onFailure { e ->
                Log.e("SharedViewModel", "Error in cancel orderr: ${e.message}")
                fetchRobotInfo(robot.token)
                onResult(false, e.message)
            }
        }
    }

    private fun updateSelectedRobot(token: String?) {
        _selectedToken.value = token ?: ""
        val robot = token?.let { _robotsInfoMap.value?.get(it) }
        updateSelectedRobotInternal(robot)
    }

    private fun updateSelectedRobotInternal(robot: Robot?) {
        val previousValue = _selectedRobot.value
        _selectedRobot.value = robot

        if (robot != previousValue && robot?.activeOrderId != null) {
            getOrderDetails(robot, robot.activeOrderId)
        }
    }

    private fun saveTokens(tokens: Set<String>) {
        _robotTokens.postValue(tokens)

        sharedPreferences.edit {
            putStringSet("robot_tokens", tokens)
            apply()
        }
    }
}
