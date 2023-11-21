package com.bitcoinwukong.robosats_android.repository

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.bitcoinwukong.robosats_android.model.Currency
import com.bitcoinwukong.robosats_android.model.OrderData
import com.bitcoinwukong.robosats_android.model.OrderType
import com.bitcoinwukong.robosats_android.model.PaymentMethod
import com.bitcoinwukong.robosats_android.model.Robot
import com.bitcoinwukong.robosats_android.network.ITorManager
import com.bitcoinwukong.robosats_android.utils.ROBOSATS_MAINNET
import com.bitcoinwukong.robosats_android.utils.ROBOSATS_TESTNET
import com.bitcoinwukong.robosats_android.utils.TOR_SOCKS_PORT
import com.bitcoinwukong.robosats_android.utils.hashTokenAsBase91
import io.matthewnelson.kmp.tor.controller.common.events.TorEvent
import io.matthewnelson.kmp.tor.manager.common.event.TorManagerEvent
import io.matthewnelson.kmp.tor.manager.common.state.TorState
import io.matthewnelson.kmp.tor.manager.common.state.isOn
import io.matthewnelson.kmp.tor.manager.common.state.isStarting
import kotlinx.coroutines.Dispatchers
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

class TorRepository(val torManager: ITorManager) : TorManagerEvent.SealedListener {
    private val isCurrentlyUpdating = AtomicBoolean(false)
    private val _isUpdating = MutableLiveData(false)
    val isUpdating: LiveData<Boolean> get() = _isUpdating

    private val _torState = MutableLiveData<TorState>(TorState.Off)
    val torState: LiveData<TorState> get() = _torState

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

    init {
        torManager.addListener(this)
    }

    override fun onEvent(event: TorManagerEvent) {
        _torState.postValue(torManager.state)
    }

    override fun onEvent(event: TorEvent.Type.MultiLineEvent, output: List<String>) {
        _torState.postValue(torManager.state)
    }

    override fun onEvent(event: TorEvent.Type.SingleLineEvent, output: String) {
        _torState.postValue(torManager.state)
    }

    private suspend fun makeApiRequest(
        url: HttpUrl,
        headers: Map<String, String> = emptyMap(),
        requestBody: RequestBody? = null,
        method: String = "GET",
        onSuccess: (String) -> Unit,
        onFailure: (String) -> Unit,
        maxRetries: Int = 3,
        retryDelayMillis: Long = 5000 // 5 seconds
    ) {
        Log.d(TAG, "Preparing to make API request to $url with headers: $headers")
        waitForTor()

        val httpClient = createHttpClient()

        val requestBuilder = Request.Builder().url(url).apply {
            headers.forEach { addHeader(it.key, it.value) }
            if (method == "POST" && requestBody != null) {
                post(requestBody)
                Log.d(TAG, "Setting request method to POST with body: $requestBody")
            }
        }
        val request = requestBuilder.build()
        Log.d(TAG, "Request built: $request")

        repeat(maxRetries) {
            try {
                httpClient.newCall(request).execute().use { response ->
                    Log.d(TAG, "makeApiRequest response received: $response")
                    if (response.isSuccessful) {
                        val responseBody = response.body?.string().orEmpty()
                        onSuccess(responseBody)
                        return
                    } else {
                        val errorMessage = "Failed with response code: ${response.code}, ${response.message}"
                        val responseBody = response.body?.string().orEmpty()
                        Log.e(TAG, errorMessage)
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
                Log.d(TAG, "Retrying API request... Attempt: ${it+1}")
                delay(retryDelayMillis) // Wait before retrying
            }
        }
        onFailure("Failed after $maxRetries attempts")
    }

    suspend fun makeGeneralRequest(
        api: String,
        token: String? = null,
        queryParams: Map<String, String> = emptyMap(),
        formBodyParams: Map<String, String> = emptyMap(),
        method: String = "GET",
        testNet: Boolean = false
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
            token?.let { // Add Authorization header if token is not null
                val hashedToken = hashTokenAsBase91(it)
                headers["Authorization"] = "Token $hashedToken"
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
                }
            )
            Result.success(jsonObject ?: throw IllegalStateException("No response data"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getInfo(): Result<JSONObject> = withContext(Dispatchers.IO) {
        makeGeneralRequest(
            api = "info"
        )
    }

    suspend fun getRobotInfo(token: String): Result<Robot> = withContext(Dispatchers.IO) {
        makeGeneralRequest(
            api = "robot",
            token = token
        ).fold(
            onSuccess = { jsonObject ->
                Result.success(Robot.fromTokenAndJson(token, jsonObject))
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
        val formBodyParams = mutableMapOf<String, String>(
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
            formBodyParams["min_amount"] = minAmount ?: throw IllegalArgumentException("minAmount is required when hasRange is true")
            formBodyParams["max_amount"] = maxAmount ?: throw IllegalArgumentException("maxAmount is required when hasRange is true")
        } else {
            formBodyParams["amount"] = amount ?: throw IllegalArgumentException("amount is required when hasRange is false")
        }
        if (isExplicit) {
            formBodyParams["satoshis"] = satoshis?.toString() ?: throw IllegalArgumentException("satoshis is required when isExplicit is true")
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
        orderId: Int): Result<OrderData> = withContext(Dispatchers.IO) {
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
                        if (orderData.type == OrderType.BUY) buyOrders.add(orderData) else sellOrders.add(orderData)
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

    private suspend fun waitForTor() {
        while (!torManager.state.isOn()) {
            Log.d(TAG, "Waiting for Tor to turn on...")
            if (!torManager.state.isStarting()) {
                torManager.start()
            }
            delay(5000) // Wait for 5 seconds before checking again
        }
        Log.d(TAG, "Tor is now on.")
    }

    private fun isRetryableError(e: IOException): Boolean {
        // Define logic to determine if the error is retryable
        return e.message?.contains("timeout") == true || e is SocketTimeoutException
    }
}