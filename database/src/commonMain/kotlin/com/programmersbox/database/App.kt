package com.programmersbox.database

import io.realm.kotlin.MutableRealm
import io.realm.kotlin.Realm
import io.realm.kotlin.RealmConfiguration
import io.realm.kotlin.ext.asFlow
import io.realm.kotlin.ext.realmListOf
import io.realm.kotlin.ext.realmSetOf
import io.realm.kotlin.migration.AutomaticSchemaMigration
import io.realm.kotlin.types.RealmList
import io.realm.kotlin.types.RealmObject
import io.realm.kotlin.types.RealmSet
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull

public class PillWeightsDB : RealmObject {
    public var uuid: String = ""
    public var name: String = ""
    public var bottleWeight: Double = 0.0
    public var pillWeight: Double = 0.0
    public var currentCount: Double = 0.0
}

internal class PillWeightConfig : RealmObject {
    var pillWeightList: RealmList<PillWeightsDB> = realmListOf()
}

internal class PillCounterServerConfig : RealmObject {
    var url: String = ""
    var urlHistory: RealmSet<String> = realmSetOf()
}

public class PillWeightDatabase {
    private val realm by lazy {
        Realm.open(
            RealmConfiguration.Builder(
                setOf(
                    PillWeightConfig::class,
                    PillWeightsDB::class,
                    PillCounterServerConfig::class
                )
            )
                .schemaVersion(22)
                .migration(AutomaticSchemaMigration { })
                .deleteRealmIfMigrationNeeded()
                .build()
        )
    }

    private suspend fun initialDb(): PillWeightConfig {
        val f = realm.query(PillWeightConfig::class).first().find()
        return f ?: realm.write { copyToRealm(PillWeightConfig()) }
    }

    private suspend fun currentPillDb(): PillWeightsDB {
        val f = realm.query(PillWeightsDB::class).first().find()
        return f ?: realm.write { copyToRealm(PillWeightsDB()) }
    }

    private suspend fun serverConfigDb(): PillCounterServerConfig {
        val f = realm.query(PillCounterServerConfig::class).first().find()
        return f ?: realm.write { copyToRealm(PillCounterServerConfig()) }
    }

    public suspend fun getItems(): Flow<List<PillWeightsDB>> = initialDb().asFlow()
        .mapNotNull { it.obj }
        .distinctUntilChangedBy { it.pillWeightList }
        .map { it.pillWeightList.toList() }

    public suspend fun getUrl(): Flow<String> = serverConfigDb().asFlow()
        .mapNotNull { it.obj }
        .distinctUntilChangedBy { it.url }
        .map { it.url }

    public suspend fun getUrlHistory(): Flow<List<String>> = serverConfigDb().asFlow()
        .mapNotNull { it.obj }
        .distinctUntilChangedBy { it.urlHistory }
        .map { it.urlHistory.toList() }

    public suspend fun getLatest(): Flow<PillWeightsDB> = currentPillDb().asFlow()
        .mapNotNull { it.obj }
        .distinctUntilChangedBy { it }
        .mapNotNull { it }

    public suspend fun updateInfo(
        uuid: String,
        block: PillWeightsDB.() -> Unit
    ) {
        realm.updateInfo<PillWeightConfig> { config ->
            val index = config?.pillWeightList?.indexOfFirst { uuid == it.uuid }
            index?.let { it1 ->
                val pDB = config.pillWeightList.removeAt(it1).apply(block)
                config.pillWeightList.add(it1, pDB)
            }
        }
    }

    public suspend fun updateLatest(
        currentCount: Double,
        name: String,
        bottleWeight: Double,
        pillWeight: Double,
        uuid: String
    ) {
        realm.updateInfo<PillWeightsDB> {
            it?.apply {
                this.name = name
                this.pillWeight = pillWeight
                this.bottleWeight = bottleWeight
                this.uuid = uuid
                this.currentCount = currentCount
            }
        }
    }

    public suspend fun saveInfo(
        name: String,
        pillWeight: Double,
        bottleWeight: Double,
        uuid: String
    ) {
        realm.updateInfo<PillWeightConfig> {
            it?.pillWeightList?.add(
                PillWeightsDB().apply {
                    this.name = name
                    this.pillWeight = pillWeight
                    this.bottleWeight = bottleWeight
                    this.uuid = uuid
                }
            )
        }
    }

    public suspend fun removeInfo(uuid: String) {
        realm.updateInfo<PillWeightConfig> { pills ->
            pills?.pillWeightList?.removeAll { it.uuid == uuid }
        }
    }

    public suspend fun saveUrl(url: String) {
        realm.updateInfo<PillCounterServerConfig> {
            it?.url = url
            it?.urlHistory?.add(url)
        }
    }

    public suspend fun removeUrl(url: String) {
        realm.updateInfo<PillCounterServerConfig> {
            it?.urlHistory?.remove(url)
        }
    }
}

private suspend inline fun <reified T : RealmObject> Realm.updateInfo(crossinline block: MutableRealm.(T?) -> Unit) {
    query(T::class).first().find()?.also { info ->
        write { block(findLatest(info)) }
    }
}
