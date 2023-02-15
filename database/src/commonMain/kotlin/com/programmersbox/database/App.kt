package com.programmersbox.database

import io.realm.kotlin.MutableRealm
import io.realm.kotlin.Realm
import io.realm.kotlin.RealmConfiguration
import io.realm.kotlin.ext.asFlow
import io.realm.kotlin.ext.realmListOf
import io.realm.kotlin.migration.AutomaticSchemaMigration
import io.realm.kotlin.types.RealmList
import io.realm.kotlin.types.RealmObject
import io.realm.kotlin.types.annotations.PrimaryKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull

public class PillWeightsDB : RealmObject {
    @PrimaryKey
    public var name: String = ""
    public var bottleWeight: Double = 0.0
    public var pillWeight: Double = 0.0
}

internal class PillWeightConfig : RealmObject {
    var pillWeightList: RealmList<PillWeightsDB> = realmListOf()
}

public class PillWeightDatabase {
    private val realm by lazy {
        Realm.open(
            RealmConfiguration.Builder(setOf(PillWeightConfig::class, PillWeightsDB::class))
                .schemaVersion(1)
                .migration(AutomaticSchemaMigration { })
                .deleteRealmIfMigrationNeeded()
                .build()
        )
    }

    private suspend fun initialDb(): PillWeightConfig {
        val f = realm.query(PillWeightConfig::class).first().find()
        return f ?: realm.write { copyToRealm(PillWeightConfig()) }
    }

    public suspend fun getItems(): Flow<List<PillWeightsDB>> = initialDb().asFlow()
        .mapNotNull { it.obj }
        .distinctUntilChanged()
        .map { it.pillWeightList.toList() }

    public suspend fun saveInfo(name: String, pillWeight: Double, bottleWeight: Double) {
        realm.updateInfo<PillWeightConfig> {
            it?.pillWeightList?.add(
                PillWeightsDB().apply {
                    this.name = name
                    this.pillWeight = pillWeight
                    this.bottleWeight = bottleWeight
                }
            )
        }
    }

    public suspend fun removeInfo(name: String, pillWeight: Double, bottleWeight: Double) {
        realm.updateInfo<PillWeightConfig> {
            it?.pillWeightList?.remove(
                PillWeightsDB().apply {
                    this.name = name
                    this.pillWeight = pillWeight
                    this.bottleWeight = bottleWeight
                }
            )
        }
    }
}

private suspend inline fun <reified T : RealmObject> Realm.updateInfo(crossinline block: MutableRealm.(T?) -> Unit) {
    query(T::class).first().find()?.also { info ->
        write { block(findLatest(info)) }
    }
}
