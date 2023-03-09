import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateList
import com.programmersbox.common.*
import com.programmersbox.database.PillWeightDatabase
import io.ktor.http.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import moe.tlaster.precompose.navigation.Navigator

class MultiPillViewModel(
    private val navigator: Navigator,
    private val viewModelScope: CoroutineScope
) {
    internal val db = Database(viewModelScope)

    internal val network = mutableMapOf<String, NetworkInfo>()

    var currentNetwork by mutableStateOf<NetworkInfo?>(null)

    val pillCount: PillCount by derivedStateOf {
        currentNetwork?.pillCount ?: PillCount(0.0, PillWeights())
    }

    val pillWeightList: SnapshotStateList<PillCount> = mutableStateListOf()

    internal val pillAlreadySaved by derivedStateOf {
        pillWeightList.any { it.pillWeights.uuid == pillCount.pillWeights.uuid }
    }

    internal var connectedState by mutableStateOf(ConnectionState.Idle)

    internal val connectionError by derivedStateOf { connectedState == ConnectionState.Error }

    internal var showErrorBanner by mutableStateOf(false)

    val urlHistory: SnapshotStateList<String> = mutableStateListOf()
    var url: String by mutableStateOf("")

    init {
        snapshotFlow { connectedState }
            .filter { connectedState == ConnectionState.Connected }
            .onEach {
                delay(2500)
                connectedState = ConnectionState.Idle
            }
            .launchIn(viewModelScope)

        snapshotFlow { connectedState }
            .onEach { showErrorBanner = connectionError }
            .launchIn(viewModelScope)

        viewModelScope.launch {
            db.list()
                .onEach {
                    pillWeightList.clear()
                    pillWeightList.addAll(it)
                }
                .launchIn(this)
        }

        /*viewModelScope.launch {
            db.currentPill()
                .onEach { pillCount = it }
                .launchIn(this)
        }*/

        viewModelScope.launch {
            db.urlHistory()
                .filter { it.isNotEmpty() }
                .onEach { it.forEach { connectToNetwork(it) } }
                .launchIn(this)
        }

        viewModelScope.launch {
            db.urlHistory()
                .onEach {
                    urlHistory.clear()
                    urlHistory.addAll(it)
                }
                .launchIn(this)
        }
    }

    internal fun removeUrl(url: String) {
        viewModelScope.launch { db.removeUrl(url) }
    }

    fun changeNetwork(url: String) {
        viewModelScope.launch { db.saveUrl(url) }
    }

    internal fun reconnect(url: String) {
        viewModelScope.launch { connectToNetwork(url) }
    }

    private suspend fun connectToNetwork(url: String) {
        network[url]?.network?.close()
        val info = network.getOrPut(url) { NetworkInfo(url) }
        info.newNetwork(url, viewModelScope) { pill ->
            db.updateCurrentPill(pill)
            if (pillWeightList.any { it.pillWeights.uuid == pill.pillWeights.uuid }) {
                db.updateCurrentCountInfo(pill)
            }
        }
    }

    internal fun onDrawerItemMainScreenClick(pillWeights: PillWeights, url: String) {
        sendNewConfig(pillWeights, url)
    }

    fun sendNewConfig(pillWeights: PillWeights, url: String) {
        viewModelScope.launch {
            network[url]?.network?.updateConfig(pillWeights)
                ?.onSuccess { println(it) }
                ?.onFailure { it.printStackTrace() }
        }
    }

    fun setCurrentPillCount(networkInfo: NetworkInfo) {
        currentNetwork = networkInfo
    }

    internal fun updateConfig(pillCount: PillCount) {
        viewModelScope.launch { db.updateInfo(pillCount) }
    }

    internal fun saveNewConfig(pillWeights: PillWeights) {
        viewModelScope.launch { db.savePillWeightInfo(pillWeights) }
    }

    internal fun removeConfig(pillWeights: PillWeights) {
        viewModelScope.launch { db.removePillWeightInfo(pillWeights) }
    }

    internal fun showNewPill() {
        navigator.navigateToNewPill()
    }

    fun showMainScreen() {
        while (navigator.canGoBack) {
            navigator.goBack()
        }
    }

    internal fun showDiscovery() {
        navigator.navigateToDiscovery()
    }
}

class Database(scope: CoroutineScope) {
    private val db = PillWeightDatabase("multipill")
    suspend fun list(): Flow<List<PillCount>> = db.getItems()
        .mapNotNull { l ->
            l.map {
                PillCount(
                    count = it.currentCount,
                    PillWeights(
                        name = it.name,
                        pillWeight = it.pillWeight,
                        bottleWeight = it.bottleWeight,
                        uuid = it.uuid
                    )
                )
            }
        }

    suspend fun savePillWeightInfo(pillWeights: PillWeights) {
        db.saveInfo(
            name = pillWeights.name,
            pillWeight = pillWeights.pillWeight,
            bottleWeight = pillWeights.bottleWeight,
            uuid = pillWeights.uuid
        )
    }

    suspend fun removePillWeightInfo(pillWeights: PillWeights) {
        db.removeInfo(pillWeights.uuid)
    }

    suspend fun url(): Flow<String> = db.getUrl()
    suspend fun saveUrl(url: String) = db.saveUrl(url)
    suspend fun updateInfo(pillCount: PillCount) {
        db.updateInfo(pillCount.pillWeights.uuid) {
            currentCount = pillCount.count
            pillWeight = pillCount.pillWeights.pillWeight
            bottleWeight = pillCount.pillWeights.bottleWeight
            name = pillCount.pillWeights.name
        }
    }

    suspend fun updateCurrentCountInfo(pillCount: PillCount) {
        db.updateInfo(pillCount.pillWeights.uuid) {
            currentCount = pillCount.count
        }
    }

    suspend fun currentPill(): Flow<PillCount> = db.getLatest()
        .map {
            PillCount(
                count = it.currentCount,
                PillWeights(
                    name = it.name,
                    pillWeight = it.pillWeight,
                    bottleWeight = it.bottleWeight,
                    uuid = it.uuid
                )
            )
        }

    suspend fun updateCurrentPill(pillCount: PillCount) {
        db.updateLatest(
            currentCount = pillCount.count,
            name = pillCount.pillWeights.name,
            bottleWeight = pillCount.pillWeights.bottleWeight,
            pillWeight = pillCount.pillWeights.pillWeight,
            uuid = pillCount.pillWeights.uuid,
        )
    }

    suspend fun urlHistory() = db.getUrlHistory()
    suspend fun removeUrl(url: String) = db.removeUrl(url)
}

class NetworkInfo(
    val ip: String,
    var network: Network? = null
) {
    var pillCount: PillCount by mutableStateOf(PillCount(0.0, PillWeights()))
        private set

    private var firstTime = true

    var connectionState by mutableStateOf(ConnectionState.Idle)

    fun newNetwork(url: String, viewModelScope: CoroutineScope, updatePill: suspend (PillCount) -> Unit) {
        network = Network(Url("http://$url:8080"))

        network!!.socketConnection()
            .catch {
                connectionState = ConnectionState.Error
                emit(Result.failure(it))
            }
            .onEach { result ->
                result
                    .onSuccess { pill ->
                        if (connectionState != ConnectionState.Idle || firstTime) {
                            firstTime = false
                            connectionState = ConnectionState.Connected
                        }
                        pillCount = pill
                        updatePill(pill)
                    }
                    .onFailure { connectionState = ConnectionState.Error }
            }
            .launchIn(viewModelScope)
    }
}