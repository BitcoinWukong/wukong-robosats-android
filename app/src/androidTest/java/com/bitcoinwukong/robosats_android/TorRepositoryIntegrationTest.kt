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
import com.bitcoinwukong.robosats_android.utils.PgpKeyGenerator
import com.bitcoinwukong.robosats_android.utils.generateSecureToken
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.FixMethodOrder
import org.junit.Test
import org.junit.runners.MethodSorters

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
class TorRepositoryIntegrationTest {
    companion object {
        val robotToken = "C2etfi7nPeUD7rCcwAOy4XoLvEAxbTRGSK6H"
        val publicKey = "-----BEGIN PGP PUBLIC KEY BLOCK-----\n" +
                "\n" +
                "mDMEZVO9bxYJKwYBBAHaRw8BAQdAVyePBQK63FB2r5ZpIqO998WaqZjmro+LFNH+\n" +
                "sw2raQC0TFJvYm9TYXRzIElEIGVkN2QzYjJiMmU1ODlhYjI2NzIwNjA1ZTc0MTRh\n" +
                "YjRmYmNhMjFjYjRiMzFlNWI0ZTYyYTZmYTUxYzI0YTllYWKIjAQQFgoAPgWCZVO9\n" +
                "bwQLCQcICZAuNFtLSY2XJAMVCAoEFgACAQIZAQKbAwIeARYhBDIhViOFpzWovPuw\n" +
                "vC40W0tJjZckAACTeAEA+AdXmA8p6I+FFqXaFVRh5JRa5ZoO4xhGb+QY00kgZisB\n" +
                "AJee8XdW6FHBj2J3b4M9AYqufdpvuj+lLmaVAshN9U4MuDgEZVO9bxIKKwYBBAGX\n" +
                "VQEFAQEHQORkbvSesg9oJeCRKigTNdQ5tkgmVGXfdz/+vwBIl3E3AwEIB4h4BBgW\n" +
                "CAAqBYJlU71vCZAuNFtLSY2XJAKbDBYhBDIhViOFpzWovPuwvC40W0tJjZckAABZ\n" +
                "1AD/RIJM/WNb28pYqtq4XmeOaqLCrbQs2ua8mXpGBZSl8E0BALWSlbHICYTNy9L6\n" +
                "KV0a5pXbxcXpzejcjpJmVwzuWz8P\n" +
                "=32+r\n" +
                "-----END PGP PUBLIC KEY BLOCK-----"
        val encryptedPrivateKey="-----BEGIN PGP PRIVATE KEY BLOCK-----\n" +
                "\n" +
                "xYYEZVO9bxYJKwYBBAHaRw8BAQdAVyePBQK63FB2r5ZpIqO998WaqZjmro+L\n" +
                "FNH+sw2raQD+CQMIHkZZZnDa6d/gHioGTKf6JevirkCBWwz8tFLGFs5DFwjD\n" +
                "tI4ew9CJd09AUxfMq2WvTilhMNrdw2nmqtmAoaIyIo43azVT1VQoxSDnWxFv\n" +
                "Tc1MUm9ib1NhdHMgSUQgZWQ3ZDNiMmIyZTU4OWFiMjY3MjA2MDVlNzQxNGFi\n" +
                "NGZiY2EyMWNiNGIzMWU1YjRlNjJhNmZhNTFjMjRhOWVhYsKMBBAWCgA+BYJl\n" +
                "U71vBAsJBwgJkC40W0tJjZckAxUICgQWAAIBAhkBApsDAh4BFiEEMiFWI4Wn\n" +
                "Nai8+7C8LjRbS0mNlyQAAJN4AQD4B1eYDynoj4UWpdoVVGHklFrlmg7jGEZv\n" +
                "5BjTSSBmKwEAl57xd1boUcGPYndvgz0Biq592m+6P6UuZpUCyE31TgzHiwRl\n" +
                "U71vEgorBgEEAZdVAQUBAQdA5GRu9J6yD2gl4JEqKBM11Dm2SCZUZd93P/6/\n" +
                "AEiXcTcDAQgH/gkDCGSRul0JyboW4JZSQVlHNVlx2mrfE1gRTh2R5hJWU9Kg\n" +
                "aw2gET8OwWDYU4F8wKTo/s7BGn+HN4jrZeLw1k/etKUKLzuPC06KUXhj3rMF\n" +
                "Ti3CeAQYFggAKgWCZVO9bwmQLjRbS0mNlyQCmwwWIQQyIVYjhac1qLz7sLwu\n" +
                "NFtLSY2XJAAAWdQA/0SCTP1jW9vKWKrauF5njmqiwq20LNrmvJl6RgWUpfBN\n" +
                "AQC1kpWxyAmEzcvS+ildGuaV28XF6c3o3I6SZlcM7ls/Dw==\n" +
                "=YAfZ\n" +
                "-----END PGP PRIVATE KEY BLOCK-----"

        private var orderId: Int? = null
    }

    @Test
    fun testDecryptPrivateKey() {
        val robotToken = "C2etfi7nPeUD7rCcwAOy4XoLvEAxbTRGSK6H"
        val encPrivKey = "-----BEGIN PGP PRIVATE KEY BLOCK-----\n" +
                "\n" +
                "xYYEZVO9bxYJKwYBBAHaRw8BAQdAVyePBQK63FB2r5ZpIqO998WaqZjmro+L\n" +
                "FNH+sw2raQD+CQMIHkZZZnDa6d/gHioGTKf6JevirkCBWwz8tFLGFs5DFwjD\n" +
                "tI4ew9CJd09AUxfMq2WvTilhMNrdw2nmqtmAoaIyIo43azVT1VQoxSDnWxFv\n" +
                "Tc1MUm9ib1NhdHMgSUQgZWQ3ZDNiMmIyZTU4OWFiMjY3MjA2MDVlNzQxNGFi\n" +
                "NGZiY2EyMWNiNGIzMWU1YjRlNjJhNmZhNTFjMjRhOWVhYsKMBBAWCgA+BYJl\n" +
                "U71vBAsJBwgJkC40W0tJjZckAxUICgQWAAIBAhkBApsDAh4BFiEEMiFWI4Wn\n" +
                "Nai8+7C8LjRbS0mNlyQAAJN4AQD4B1eYDynoj4UWpdoVVGHklFrlmg7jGEZv\n" +
                "5BjTSSBmKwEAl57xd1boUcGPYndvgz0Biq592m+6P6UuZpUCyE31TgzHiwRl\n" +
                "U71vEgorBgEEAZdVAQUBAQdA5GRu9J6yD2gl4JEqKBM11Dm2SCZUZd93P/6/\n" +
                "AEiXcTcDAQgH/gkDCGSRul0JyboW4JZSQVlHNVlx2mrfE1gRTh2R5hJWU9Kg\n" +
                "aw2gET8OwWDYU4F8wKTo/s7BGn+HN4jrZeLw1k/etKUKLzuPC06KUXhj3rMF\n" +
                "Ti3CeAQYFggAKgWCZVO9bwmQLjRbS0mNlyQCmwwWIQQyIVYjhac1qLz7sLwu\n" +
                "NFtLSY2XJAAAWdQA/0SCTP1jW9vKWKrauF5njmqiwq20LNrmvJl6RgWUpfBN\n" +
                "AQC1kpWxyAmEzcvS+ildGuaV28XF6c3o3I6SZlcM7ls/Dw==\n" +
                "=YAfZ\n" +
                "-----END PGP PRIVATE KEY BLOCK-----"
        val keyId = 7088936486162781302
        val pgpPrivateKey = PgpKeyGenerator.decryptPrivateKey(encPrivKey, robotToken)
        assertEquals(keyId, pgpPrivateKey!!.keyID)
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
    fun testGenerateRobot() {
        val token = generateSecureToken()
        val (pubkey, encPrivKey) = PgpKeyGenerator.generateKeyPair("12345", token)
        runBlocking {
            // Context of the app under test.
            val appContext = InstrumentationRegistry.getInstrumentation().targetContext
            assertEquals("com.bitcoinwukong.robosats_android", appContext.packageName)

            // Cast the context to Application.
            val application = appContext.applicationContext as Application

            val torManager = TorManager(application)

            val torRepository = TorRepository(torManager)
            val result = torRepository.getRobotInfo(token, pubkey, encPrivKey)

            result.onSuccess { robotInfo ->
                assertEquals(pubkey, robotInfo.publicKey)
                assertEquals(encPrivKey, robotInfo.encryptedPrivateKey)
            }.onFailure { e ->
                fail("Post request failed: ${e.message}")
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
            val result = torRepository.getRobotInfo(robotToken, publicKey, encryptedPrivateKey)

            result.onSuccess { robotInfo ->
                assertEquals("UptightPub730", robotInfo.nickname)
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
