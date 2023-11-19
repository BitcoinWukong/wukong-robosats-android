package com.bitcoinwukong.robosats_android

import android.app.Application
import android.util.Log
import androidx.test.platform.app.InstrumentationRegistry
import com.bitcoinwukong.robosats_android.model.Currency
import com.bitcoinwukong.robosats_android.model.OrderData
import com.bitcoinwukong.robosats_android.model.OrderStatus
import com.bitcoinwukong.robosats_android.model.OrderType
import com.bitcoinwukong.robosats_android.model.PaymentMethod
import com.bitcoinwukong.robosats_android.network.TorManager
import com.bitcoinwukong.robosats_android.repository.TorRepository
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.FixMethodOrder
import org.junit.Test
import org.junit.runners.MethodSorters

const val TEST_ROBOT_TOKEN = "XWdQIua1zwlK60rw00IVd64fvwbk0DyJC8ye"

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
class TorRepositoryIntegrationTest {
    companion object {
        private var robotToken = TEST_ROBOT_TOKEN
        private var orderId: Int? = null
    }

    @Test
    fun testGetInfo() {
        runBlocking {
            // Context of the app under test.
            val appContext = InstrumentationRegistry.getInstrumentation().targetContext
            assertEquals("com.bitcoinwukong.robosats_android", appContext.packageName)

            // Cast the context to Application.
            val application = appContext.applicationContext as Application

            val torManager = TorManager(application)

            val torRepository = TorRepository(torManager)
            val result = torRepository.getInfo()

            result.onSuccess { json ->
                val formattedJson = json.toString(4)
                Log.d("TorRepositoryIntegrationTest", "Formatted JSON Response:\n$formattedJson")

                val numPublicBuyOrders = json.optInt("num_public_buy_orders", 0)
                assertTrue(numPublicBuyOrders > 0)
            }.onFailure { e ->
                fail("testGetInfo failed: ${e.message}")
            }
        }
    }

    @Test
    fun test1GetRobotInfo() {
        runBlocking {
            // Context of the app under test.
            val appContext = InstrumentationRegistry.getInstrumentation().targetContext
            assertEquals("com.bitcoinwukong.robosats_android", appContext.packageName)

            // Cast the context to Application.
            val application = appContext.applicationContext as Application

            val torManager = TorManager(application)

            val torRepository = TorRepository(torManager)
            val result = torRepository.getRobotInfo(robotToken)

            result.onSuccess { robotInfo ->
                assertEquals("AffectedSiren114", robotInfo.nickname)
                if (robotInfo.activeOrderId != null) {
                    val cancelResult = torRepository.performOrderAction(robotToken,
                        robotInfo.activeOrderId!!, "cancel")
                    cancelResult.onSuccess { jsonObject ->
                        Log.d("TorRepositoryIntegrationTest", "Formatted JSON Response:\n${jsonObject.toString(4)}")
                        val status = jsonObject.getInt("status")
                        assertEquals(OrderStatus.CANCELLED.code, status)
                    }.onFailure { e ->
                        if (e.message != "Failed with response code: 400, Bad Request") {
                            fail("Post request failed: ${e.message}")
                        } else {
                            Log.d("TorRepositoryIntegrationTest", "Ignoring known error: ${e.message}")
                        }
                    }
                }
            }.onFailure { e ->
                fail("Post request failed: ${e.message}")
            }
        }
    }

    @Test
    fun test2MakeOrder() {
        runBlocking {
            // Context of the app under test.
            val appContext = InstrumentationRegistry.getInstrumentation().targetContext
            assertEquals("com.bitcoinwukong.robosats_android", appContext.packageName)

            // Cast the context to Application.
            val application = appContext.applicationContext as Application

            val torManager = TorManager(application)

            val torRepository = TorRepository(torManager)
            val result = torRepository.makeOrder(robotToken, OrderType.BUY, Currency.USD, hasRange = true,
                minAmount = "21", maxAmount = "101.7", paymentMethod = PaymentMethod.ADVCASH)

            result.onSuccess { json ->
                val orderData = OrderData.fromJson(json)

                // Print formatted JSON using Log.d
                val formattedJson = json.toString(4)
                Log.d("TorRepositoryIntegrationTest", "Formatted JSON Response:\n$formattedJson")

                assertEquals(OrderStatus.WAITING_FOR_MAKER_BOND, orderData.status)
                orderId = orderData.id
            }.onFailure { e ->
                fail("Post request failed: ${e.message}")
            }
        }
    }

    @Test
    fun test3CancelOrder() {
        runBlocking {
            // Context of the app under test.
            val appContext = InstrumentationRegistry.getInstrumentation().targetContext
            assertEquals("com.bitcoinwukong.robosats_android", appContext.packageName)

            // Cast the context to Application.
            val application = appContext.applicationContext as Application

            val torManager = TorManager(application)

            val torRepository = TorRepository(torManager)
            val result = torRepository.performOrderAction(robotToken, orderId!!, "cancel")
            result.onSuccess { orderInfo ->
                // Print formatted JSON using Log.d
                val formattedJson = orderInfo.toString(4)
                Log.d("TorRepositoryIntegrationTest", "Formatted JSON Response:\n$formattedJson")

                val status = orderInfo.getInt("status")
                assertEquals(OrderStatus.CANCELLED.code, status)
            }.onFailure { e ->
                if (e.message != "Failed with response code: 400, Bad Request") {
                    fail("Post request failed: ${e.message}")
                } else {
                    Log.d("TorRepositoryIntegrationTest", "Ignoring known error: ${e.message}")
                }
            }
        }
    }

//    @Test
//    fun testPauseResumeOrder() {
//        runBlocking {
//            // Context of the app under test.
//            val appContext = InstrumentationRegistry.getInstrumentation().targetContext
//            assertEquals("com.bitcoinwukong.robosats_android", appContext.packageName)
//
//            // Cast the context to Application.
//            val application = appContext.applicationContext as Application
//
//            val torManager = TorManager(application)
//
//            val torRepository = TorRepository(torManager)
//            val result = torRepository.performOrderAction("xQuys13Xg81X7oFxHj86SdkqJbBsvCq9iwQW", 92019, "nonsense")
//
//            result.onSuccess { orderInfo ->
//                // Print formatted JSON using Log.d
//                val formattedJson = orderInfo.toString(4)
//                Log.d("InfoResponse", "Formatted orderInfo Response:\n$formattedJson")
//
//                val status = orderInfo.getInt("status")
//                assertEquals(OrderStatus.PAUSED.code, status)
//            }.onFailure { e ->
//                fail("Get info failed: ${e.message}")
//            }
//        }
//    }
}
