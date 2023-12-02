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
import com.bitcoinwukong.robosats_android.model.errorRobot
import com.bitcoinwukong.robosats_android.repository.TorRepository
import com.bitcoinwukong.robosats_android.utils.convertExpirationTimeToExpirationSeconds
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDateTime

class SharedViewModel(
    private val torRepository: TorRepository,
    private val sharedPreferences: SharedPreferences
) : ViewModel(), ISharedViewModel {
    companion object {
        private val TAG = SharedViewModel::class.java.simpleName
    }

    override val torManagerEvents: LiveData<String> = torRepository.torManager.events

    private val _orders = MutableLiveData<List<OrderData>>(emptyList())
    override val orders: LiveData<List<OrderData>> get() = _orders

    private val _lastUpdated = MutableLiveData<LocalDateTime>()
    override val lastUpdated: LiveData<LocalDateTime> get() = _lastUpdated

    override val isUpdating: LiveData<Boolean> get() = torRepository.isUpdating
    override val isTorReady: LiveData<Boolean> get() = torRepository.isTorReady

    override val loadingRobots: LiveData<Set<Robot>> get() = torRepository.loadingRobots

    private var _robotTokens = MutableLiveData<Set<String>>(emptySet())
    override val robotTokens: LiveData<Set<String>> get() = _robotTokens

    private val _robotsInfoMap = MutableLiveData<Map<String, Robot>>(emptyMap())
    override val robotsInfoMap: LiveData<Map<String, Robot>> get() = _robotsInfoMap


    private var _selectedToken = MutableLiveData("")
    override val selectedToken: LiveData<String> get() = _selectedToken
    private var _selectedRobot = MutableLiveData<Robot?>(null)
    override val selectedRobot: LiveData<Robot?> get() = _selectedRobot

    private var _activeOrder = MutableLiveData<OrderData?>(null)
    override val activeOrder: LiveData<OrderData?> get() = _activeOrder

    private var _chatMessages = MutableLiveData<List<String>>(emptyList())

    override val chatMessages: LiveData<List<String>> get() = _chatMessages

    private val _orderIdPeerPublicKeyMap: MutableMap<Int, String> = mutableMapOf()

    private val _ordersCache = mutableMapOf<Int, OrderData>()

    override fun restartTor() {
        torRepository.restartTor()
    }

    init {
        startAutoFetch()
    }

    private fun startAutoFetch() {
        viewModelScope.launch {
            while (isActive) {
                refreshRobotsInfo()
                fetchOrders()
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
                Log.d(TAG, "Fetched robot info: ${robot.nickname}")
            }.onFailure { e ->
                val errorMessageRobot =
                    errorRobot(token, "Error fetching robot info: ${e.message}")
                updateRobotInfoInMap(token, errorMessageRobot)
                Log.e(TAG, "Error fetching robot info: ${e.message}")
            }
        }
    }

    private fun updateRobotInfoInMap(token: String, robot: Robot?) {
        val currentInfo = _robotsInfoMap.value ?: mapOf()
        val updatedInfo = currentInfo.toMutableMap().apply {
            if (robot != null) {
                put(token, robot)
                robot.activeOrderId?.let { orderId ->
                    getOrderDetails(robot, orderId)
                }
            } else {
                remove(token) // Remove the robot info if robot is null
            }
        }
        _robotsInfoMap.postValue(updatedInfo)

        if (_selectedToken.value == token) {
            updateSelectedRobotInternal(robot)
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

    override fun createOrder(createOrderParams: CreateOrderParams) {
        val robot = _selectedRobot.value ?: return
        updateRobotInfoInMap(robot.token, null) // Clear robot info cache
        viewModelScope.launch {
            // Todo: update view model and UI base on the order creation result
            val result = torRepository.makeOrder(
                robot.token,
                createOrderParams.orderType,
                createOrderParams.currency,
                amount = createOrderParams.amount,
                paymentMethod = createOrderParams.paymentMethod,
                premium = createOrderParams.premium,
                publicDuration = convertExpirationTimeToExpirationSeconds(createOrderParams.expirationTime)
            )
            result.onSuccess {
                fetchRobotInfo(robot.token)
            }.onFailure { e ->
                Log.e(TAG, "Error in createOrder: ${e.message}")
            }
        }
    }

    override fun takeOrder(orderData: OrderData) {
        val robot = _selectedRobot.value ?: return
        viewModelScope.launch {
            val result = torRepository.takeOrder(
                robot.token,
                orderData.id!!
            )

            result.onSuccess {
                fetchRobotInfo(robot.token)
            }.onFailure { e ->
                Log.e(TAG, "Error in takeOrder: ${e.message}")
            }
        }
    }

    override fun confirmOrderFiatReceived(orderData: OrderData) {
        val robot = _selectedRobot.value ?: return
        viewModelScope.launch {
            val result = torRepository.confirmOrderFiatReceived(
                robot.token,
                orderData.id!!
            )

            result.onSuccess {
                fetchRobotInfo(robot.token)
            }.onFailure { e ->
                Log.e(TAG, "Error in confirmOrderFiatReceived: ${e.message}")
            }
        }
    }

    override fun getOrderDetails(robot: Robot, orderId: Int, resetCache: Boolean) {
        Log.d(TAG, "getting order details: $orderId")

        val activeOrderId = _selectedRobot.value?.activeOrderId
        if (resetCache) {
            _ordersCache.remove(orderId)
            if (activeOrderId == orderId) {
                _activeOrder.value = null
            }
        } else if (activeOrderId == orderId) {
            _activeOrder.value = _ordersCache[orderId]
        }

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
        Log.d(TAG, "pauseResumeOrder: $orderId")
        // Invalidate the order data
        invalidateOrder(orderId)

        viewModelScope.launch {
            torRepository.pauseResumeOrder(robot.token, orderId)
            getOrderDetails(robot, orderId)
        }
    }

    private fun invalidateOrder(orderId: Int) {
        _ordersCache.remove(orderId)
        _chatMessages.postValue(emptyList())
        if (_activeOrder.value?.id == orderId) {
            _activeOrder.postValue(null)
        }
    }

    override fun cancelOrder(robot: Robot, orderId: Int) {
        Log.d(TAG, "cancelOrder: $orderId")
        invalidateOrder(orderId)

        viewModelScope.launch {
            torRepository.cancelOrder(robot.token, orderId)
            fetchRobotInfo(robot.token)
        }
    }

    override fun getChatMessages(robot: Robot, orderId: Int) {
        viewModelScope.launch {
            val result = torRepository.getChatMessages(robot, orderId)
            result.onSuccess { chatMessagesResponse ->
                Log.d(TAG, "getChatMessages succeeded: ")

                _orderIdPeerPublicKeyMap[orderId] = chatMessagesResponse.peerPublicKey

                val messages = chatMessagesResponse.messages
                // Sort messages by index incrementally
                val sortedMessages = messages.sortedBy { it.index }
                val decryptedMessagesDeferred = sortedMessages.map { message ->
                    async(Dispatchers.IO) {
                        robot.decryptMessage(message.message).also { decryptedMessage ->
                            withContext(Dispatchers.Main) {
                                torRepository.torManager.addLine("Message: ${message.time}, ${message.nick}, ${message.index}: $decryptedMessage")
                                Log.d(
                                    TAG,
                                    "Message: ${message.time}, ${message.nick}, ${message.index}: $decryptedMessage"
                                )
                            }
                        }
                    }
                }

                // Await all the decrypted messages and then post them
                val decryptedMessagesList = decryptedMessagesDeferred.awaitAll()
                _chatMessages.postValue(decryptedMessagesList)
            }.onFailure { e ->
                Log.e(TAG, "getChatMessages failed: ${e.message}")
            }
        }
    }

    override fun sendChatMessage(robot: Robot, orderId: Int, message: String) {
        _orderIdPeerPublicKeyMap[orderId]?.let { peerPublicKey ->
            // If the peer public key is not null, send the chat message
            viewModelScope.launch {
                val result = torRepository.sendChatMessage(robot, orderId, peerPublicKey, message)
                result.onSuccess { json ->
                    Log.d(TAG, "sendChatMessage succeeded: $json")

                    getChatMessages(robot, orderId)
                }
            }
        } ?: run {
            // If the peer public key is null, log an error
            Log.e(TAG, "missing peer public key for order: $orderId")
        }
    }

    private fun updateSelectedRobot(token: String?) {
        val safeToken = token.orEmpty()
        _selectedToken.value = safeToken
        val robot = _robotsInfoMap.value?.get(safeToken)

        if (safeToken.isNotEmpty()) {
            if (robot == null || robot.errorMessage != null) {
                fetchRobotInfo(safeToken)  // safeToken is guaranteed to be non-null here
            }
        }
        updateSelectedRobotInternal(robot)
    }

    private fun updateSelectedRobotInternal(robot: Robot?) {
        val previousValue = _selectedRobot.value
        _selectedRobot.value = robot

        if (robot != previousValue && robot?.activeOrderId != null) {
            _activeOrder.value = _ordersCache[robot.activeOrderId]
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
