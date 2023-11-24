package com.bitcoinwukong.robosats_android.repository

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.bitcoinwukong.robosats_android.model.Currency
import com.bitcoinwukong.robosats_android.model.Message
import com.bitcoinwukong.robosats_android.model.OrderData
import com.bitcoinwukong.robosats_android.model.OrderType
import com.bitcoinwukong.robosats_android.model.PaymentMethod
import com.bitcoinwukong.robosats_android.model.Robot
import com.bitcoinwukong.robosats_android.network.ITorManager
import com.bitcoinwukong.robosats_android.utils.ROBOSATS_MAINNET
import com.bitcoinwukong.robosats_android.utils.ROBOSATS_TESTNET
import com.bitcoinwukong.robosats_android.utils.TOR_SOCKS_PORT
import com.bitcoinwukong.robosats_android.utils.hashTokenAsBase91
import io.matthewnelson.kmp.tor.manager.common.state.isStarting
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.net.InetSocketAddress
import java.net.Proxy
import java.net.SocketTimeoutException
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

class TorRepository(val torManager: ITorManager) {
    private val isCurrentlyUpdating = AtomicBoolean(false)
    private val _isUpdating = MutableLiveData(false)
    val isUpdating: LiveData<Boolean> get() = _isUpdating

    // Shared Deferred for waiting for Tor
    private var waitingForTor: Deferred<Unit>? = null

    private var _isTorReady = MutableLiveData(false)
    val isTorReady: LiveData<Boolean> get() = _isTorReady

    fun restartTor() {
        torManager.restart()
    }

    companion object {
        private val TAG = TorRepository::class.java.simpleName
    }

    private fun createHttpClient(): OkHttpClient {
        return OkHttpClient.Builder()
            .proxy(Proxy(Proxy.Type.SOCKS, InetSocketAddress("127.0.0.1", TOR_SOCKS_PORT)))
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    private suspend fun makeApiRequest(
        url: HttpUrl,
        headers: Map<String, String> = emptyMap(),
        requestBody: RequestBody? = null,
        method: String = "GET",
        onSuccess: (String) -> Unit,
        onFailure: (String) -> Unit,
        maxRetries: Int = 2,
        retryDelayMillis: Long = 5000, // 5 seconds,
        checkTorConnection: Boolean = true
    ) {
        Log.d(TAG, "Preparing to make API request to $url with headers: $headers")

        if (checkTorConnection) {
            Log.d(TAG, "Waiting for tor for API request to $url")
            waitForTor()
        }

        val httpClient = createHttpClient()

        val requestBuilder = Request.Builder().url(url).apply {
            headers.forEach { addHeader(it.key, it.value) }
            if (method == "POST" && requestBody != null) {
                post(requestBody)
                Log.d(TAG, "Setting request method to POST with body: $requestBody")
            }
        }
        val request = requestBuilder.build()
        Log.i(TAG, "Request built: $request")
        torManager.addLine("Request built: $request")

        repeat(maxRetries) {
            try {
                httpClient.newCall(request).execute().use { response ->
                    Log.i(TAG, "makeApiRequest response received: $response")
                    torManager.addLine("makeApiRequest response received: $response")
                    if (response.isSuccessful) {
                        val responseBody = response.body?.string().orEmpty()
                        onSuccess(responseBody)
                        return
                    } else {
                        val errorMessage =
                            "Failed with response code: ${response.code}, ${response.message}"
                        val responseBody = response.body?.string().orEmpty()
                        Log.e(TAG, errorMessage)
                        torManager.addLine(errorMessage)
                        Log.e(TAG, "response body: $responseBody")
                        onFailure(errorMessage)
                        return
                    }
                }
            } catch (e: IOException) {
                Log.e(TAG, "IOException during API request: ${e.message}")
                if (!isRetryableError(e)) {
                    onFailure(e.message ?: "Unknown error")
                    return
                }
                Log.d(TAG, "Retrying API request... Attempt: ${it + 1}")
                delay(retryDelayMillis) // Wait before retrying
            }
        }
        onFailure("Failed after $maxRetries attempts")
    }

    private suspend fun makeGeneralRequest(
        api: String,
        token: String? = null,
        pubKey: String? = null,
        encPrivKey: String? = null,
        queryParams: Map<String, String> = emptyMap(),
        formBodyParams: Map<String, String> = emptyMap(),
        method: String = "GET",
        testNet: Boolean = false,
        checkTorConnection: Boolean = true
    ): Result<JSONObject> = withContext(Dispatchers.IO) {
        val host = if (testNet) ROBOSATS_TESTNET else ROBOSATS_MAINNET

        try {
            var jsonObject: JSONObject? = null

            // Build the URL
            val urlBuilder = HttpUrl.Builder()
                .scheme("http")
                .host(host)
                .addPathSegment("api")
                .addPathSegment(api)
                .addPathSegment("")
            for ((key, value) in queryParams) {
                urlBuilder.addQueryParameter(key, value)
            }
            val url = urlBuilder.build()

            // Build the headers
            val headers = mutableMapOf<String, String>()
            token?.let { tokenValue ->
                val hashedToken = hashTokenAsBase91(tokenValue)
                val authValue = buildString {
                    append("Token $hashedToken")
                    if (pubKey != null && encPrivKey != null) {
                        append(" | Public ${pubKey.replace("\n", "\\")} | Private ${encPrivKey.replace("\n", "\\")}")
                    }
                }
                headers["Authorization"] = authValue
            }

            // Build the body
            val formBodyBuilder = FormBody.Builder()
            for ((key, value) in formBodyParams) {
                formBodyBuilder.add(key, value)
            }
            val formBody = formBodyBuilder.build()

            // Add "Content-Type" header only if formBody is not empty
            if (formBodyParams.isNotEmpty()) {
                headers["Content-Type"] = "application/x-www-form-urlencoded"
            }

            makeApiRequest(
                url = url,
                headers = headers,
                requestBody = formBody,
                method = method,
                onSuccess = { responseData ->
                    Log.d(TAG, "makeGeneralRequest response received: $responseData")
                    jsonObject = JSONObject(responseData)
                },
                onFailure = { errorMessage ->
                    Log.e(TAG, "Error in makeGeneralRequest: $errorMessage")
                    throw IOException(errorMessage)
                },
                checkTorConnection = checkTorConnection
            )
            Result.success(jsonObject ?: throw IllegalStateException("No response data"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getInfo(checkTorConnection: Boolean = true): Result<JSONObject> =
        withContext(Dispatchers.IO) {
            makeGeneralRequest(
                api = "info",
                checkTorConnection = checkTorConnection
            )
        }

    suspend fun getRobotInfo(token: String, publicKey: String?=null, encPrivKey: String?=null): Result<Robot> = withContext(Dispatchers.IO) {
        Log.d(TAG, "getRobotInfo: $token, $publicKey, $encPrivKey")
        makeGeneralRequest(
            api = "robot",
            token = token,
            pubKey = publicKey,
            encPrivKey = encPrivKey
        ).fold(
            onSuccess = { jsonObject ->
                val robot = Robot.fromTokenAndJson(token, jsonObject)
                Log.d(TAG, "getRobotInfo succeeded: ${robot.token} ${robot.encryptedPrivateKey}, ${robot.publicKey}")
                Result.success(robot)
            },
            onFailure = { e ->
                Result.failure(e)
            }
        )
    }

    suspend fun getChatMessages(
        token: String,
        orderId: Int
    ): Result<List<Message>> = withContext(Dispatchers.IO) {
        val queryParams = mapOf(
            "order_id" to orderId.toString(),
        )

        makeGeneralRequest(
            api = "chat",
            token = token,
            queryParams = queryParams,
        ).fold(
            onSuccess = { jsonObject ->
                val messagesJsonArray = jsonObject.optJSONArray("messages")
                val messages = mutableListOf<Message>()

                if (messagesJsonArray != null) {
                    for (i in 0 until messagesJsonArray.length()) {
                        val messageJsonObject = messagesJsonArray.optJSONObject(i)
                        messageJsonObject?.let {
                            val message = Message(
                                index = it.optInt("index"),
                                time = it.optString("time"),
                                message = it.optString("message"),
                                nick = it.optString("nick")
                            )
                            messages.add(message)
                        }
                    }
                }

                Result.success(messages)
            },
            onFailure = { e ->
                Result.failure(e)
            }
        )
    }

    suspend fun makeOrder(
        token: String,
        type: OrderType,
        currency: Currency,
        amount: String? = null, // Required if has_range is false
        hasRange: Boolean = false,
        minAmount: String? = null, // Required if has_range is true
        maxAmount: String? = null, // Required if has_range is true
        paymentMethod: PaymentMethod,
        isExplicit: Boolean = false,
        premium: String = "0",
        satoshis: Int? = null, // Required if isExplicit is true
        publicDuration: Int = 86400,
        escrowDuration: Int = 1800,
        bondSize: String = "3.0",
        latitude: String? = null,
        longitude: String? = null
    ): Result<JSONObject> = withContext(Dispatchers.IO) {
        val formBodyParams = mutableMapOf(
            "type" to type.value.toString(),
            "currency" to currency.id.toString(),
            "has_range" to hasRange.toString(),
            "payment_method" to paymentMethod.methodName,
            "is_explicit" to isExplicit.toString(),
            "premium" to premium,
            "public_duration" to publicDuration.toString(),
            "escrow_duration" to escrowDuration.toString(),
            "bond_size" to bondSize
        )

        if (hasRange) {
            formBodyParams["min_amount"] = minAmount
                ?: throw IllegalArgumentException("minAmount is required when hasRange is true")
            formBodyParams["max_amount"] = maxAmount
                ?: throw IllegalArgumentException("maxAmount is required when hasRange is true")
        } else {
            formBodyParams["amount"] = amount
                ?: throw IllegalArgumentException("amount is required when hasRange is false")
        }
        if (isExplicit) {
            formBodyParams["satoshis"] = satoshis?.toString()
                ?: throw IllegalArgumentException("satoshis is required when isExplicit is true")
        }

        latitude?.let { formBodyParams["latitude"] = it }
        longitude?.let { formBodyParams["longitude"] = it }

        makeGeneralRequest(
            api = "make",
            token = token,
            formBodyParams = formBodyParams,
            method = "POST"
        )
    }

    suspend fun getOrderDetails(
        token: String,
        orderId: Int
    ): Result<OrderData> = withContext(Dispatchers.IO) {
        Log.d(TAG, "getOrderDetails: $orderId")
        makeGeneralRequest(
            api = "order",
            token = token,
            queryParams = mapOf("order_id" to orderId.toString()),
        ).fold(
            onSuccess = { jsonObject ->
                Result.success(OrderData.fromJson(jsonObject))
            },
            onFailure = { e ->
                Result.failure(e)
            }
        )
    }

    suspend fun pauseResumeOrder(
        token: String,
        orderId: Int,
    ): Result<JSONObject> = withContext(Dispatchers.IO) {
        Log.d(TAG, "pauseResumeOrder: $orderId")
        performOrderAction(token, orderId, "pause")
    }

    suspend fun performOrderAction(
        token: String,
        orderId: Int,
        action: String,
        invoice: String? = null,
        routingBudgetPpm: Int? = null,
        address: String? = null,
        statement: String? = null,
        rating: String? = null,
        amount: String? = null,
        miningFeeRate: String? = null
    ): Result<JSONObject> = withContext(Dispatchers.IO) {
        try {
            val formBodyParams = mutableMapOf(
                "action" to action,
                "invoice" to invoice,
                "routing_budget_ppm" to routingBudgetPpm?.toString(),
                "address" to address,
                "statement" to statement,
                "rating" to rating,
                "amount" to amount,
                "mining_fee_rate" to miningFeeRate
            ).filterValues { it != null }.mapValues { it.value!! }

            makeGeneralRequest(
                api = "order",
                token = token,
                queryParams = mapOf("order_id" to orderId.toString()),
                formBodyParams = formBodyParams,
                method = "POST"
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun listOrders(): Result<List<OrderData>> = withContext(Dispatchers.IO) {
        if (isCurrentlyUpdating.getAndSet(true)) {
            Log.d(TAG, "Update already in progress. Fetch aborted.")
            return@withContext Result.failure(IllegalStateException("Update in progress"))
        }
        _isUpdating.postValue(true)

        try {
            var resultOrders = listOf<OrderData>()
            val url = HttpUrl.Builder()
                .scheme("http")
                .host("robosats6tkf3eva7x2voqso3a5wcorsnw34jveyxfqi2fu7oyheasid.onion")
                .addPathSegment("api")
                .addPathSegment("book")
                .addQueryParameter("currency", "1")
                .addQueryParameter("type", "2")
                .build()
            Log.d("Network", "listing orders....")
            makeApiRequest(
                url = url,
                onSuccess = { responseData ->
                    val jsonArray = JSONArray(responseData)
                    val buyOrders = mutableListOf<OrderData>()
                    val sellOrders = mutableListOf<OrderData>()

                    for (i in 0 until jsonArray.length()) {
                        val orderJson = jsonArray.getJSONObject(i)
                        val orderData = OrderData.fromJson(orderJson)
                        if (orderData.type == OrderType.BUY) buyOrders.add(orderData) else sellOrders.add(
                            orderData
                        )
                    }

                    // Sorting by premium in descending order
                    val comparator = compareByDescending<OrderData> { it.premium }
                    buyOrders.sortWith(comparator)
                    sellOrders.sortWith(comparator)

                    resultOrders = buyOrders + sellOrders
                },
                onFailure = { errorMessage ->
                    throw IOException(errorMessage)
                }
            )
            Result.success(resultOrders)
        } catch (e: Exception) {
            Result.failure(e)
        } finally {
            // Call the actual API to fetch orders
            // After fetching orders:
            isCurrentlyUpdating.set(false)
            _isUpdating.postValue(false)
        }
    }

    private suspend fun waitForTor() = coroutineScope {
        // If there's already a Deferred, wait for it
        waitingForTor?.let {
            it.await()
            return@coroutineScope
        }

        // If this is the first call, create a new Deferred
        waitingForTor = async {
            while (!torManager.state.isBootstrapped || _isTorReady.value == false) {
                _isTorReady.postValue(false)

                Log.d(TAG, "Waiting for Tor to turn on...")
                if (!torManager.state.isStarting()) {
                    torManager.start()
                }

                var waitedSeconds = 0
                while (!torManager.state.isBootstrapped && waitedSeconds < 20) {
                    delay(1000) // Wait for 1 seconds before checking again
                    waitedSeconds += 1
                    torManager.addLine("Still waiting for Tor to turn on...")
                }

                // Make a call to getInfo to test the connection
                torManager.addLine("---------------------------------")
                torManager.addLine("Testing connection to RoboSats")
                val infoResult = getInfo(checkTorConnection = false)
                if (infoResult.isFailure) {
                    val errorMessage = "Failed to establish a connection via Tor. Restarting Tor..."
                    torManager.addLine("---------------------------------")
                    Log.e(TAG, errorMessage)
                    torManager.addLine(errorMessage)
                    torManager.restart()
                } else {
                    _isTorReady.postValue(true)
                }

                delay(3000) // Wait for 3 seconds before checking again
            }
            Log.d(TAG, "Tor is now on.")
        }

        // Await the newly created Deferred
        waitingForTor?.await()

        // Reset the Deferred to null after it's done
        waitingForTor = null
    }

    private fun isRetryableError(e: IOException): Boolean {
        // Define logic to determine if the error is retryable
        return e.message?.contains("timeout") == true || e is SocketTimeoutException
    }
}
