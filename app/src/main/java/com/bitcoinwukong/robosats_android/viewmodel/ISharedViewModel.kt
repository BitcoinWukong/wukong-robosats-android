package com.bitcoinwukong.robosats_android.viewmodel

import androidx.lifecycle.LiveData
import com.bitcoinwukong.robosats_android.model.Currency
import com.bitcoinwukong.robosats_android.model.OrderData
import com.bitcoinwukong.robosats_android.model.OrderType
import com.bitcoinwukong.robosats_android.model.PaymentMethod
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

    fun createOrder(orderType: OrderType, currency: Currency, amount: Double, paymentMethod: PaymentMethod)

    fun getOrderDetails(robot: Robot, orderId: Int)
    fun pauseResumeOrder(robot: Robot, orderId: Int)

    fun cancelOrder(onResult: (Boolean, String?) -> Unit)
}


//2023-11-16 02:36:51.325 10487-10546 Network                 com.bitcoinwukong.robosats_android   D  pauseResumeOrder result: {"id":91724,"status":1,"created_at":"2023-11-15T17:57:45.927197Z","expires_at":"2023-11-16T17:56:45.927197Z","type":1,"currency":1,"amount":"100.00000000","has_range":false,"min_amount":null,"max_amount":null,"payment_method":"Amazon USA GiftCard","is_explicit":false,"premium":"10.00","satoshis":null,"maker":277126,"taker":null,"escrow_duration":10800,"bond_size":"3.00","latitude":null,"longitude":null,"total_secs_exp":86340,"is_maker":true,"is_taker":false,"is_participant":true,"maker_nick":"OmniscientQuad377","maker_status":"Active","price_now":41251.0,"premium_now":10.0,"satoshis_now":249131,"premium_percentile":0.0,"num_similar_orders":14,"is_buyer":false,"is_seller":true,"taker_nick":"None","status_message":"Public","is_fiat_sent":false,"is_disputed":false,"ur_nick":"OmniscientQuad377","maker_locked":true,"taker_locked":false,"escrow_locked":false}
//2023-11-16 02:36:51.326 10487-10487 TorListener             com.bitcoinwukong.robosats_android   D  Pause/Resume order result: true