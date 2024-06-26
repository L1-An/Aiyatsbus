package com.mcstarrysky.aiyatsbus.core.data

import com.mcstarrysky.aiyatsbus.core.*
import com.mcstarrysky.aiyatsbus.core.util.Reloadable
import taboolib.common.LifeCycle
import taboolib.common.platform.Awake
import taboolib.common.platform.function.info
import taboolib.common.platform.function.registerLifeCycleTask
import taboolib.library.configuration.ConfigurationSection
import taboolib.module.configuration.Config
import taboolib.module.configuration.Configuration
import java.util.concurrent.ConcurrentHashMap

/**
 * Aiyatsbus
 * com.mcstarrysky.aiyatsbus.core.data.Group
 *
 * @author mical
 * @since 2024/2/18 11:09
 */
data class Group(
    private val root: ConfigurationSection,
    val name: String = root.name,
    val enchantments: List<AiyatsbusEnchantment> = root.getStringList("enchants").mapNotNull(::aiyatsbusEt)
        .toMutableList().also {
            it += root.getStringList("rarities")
                .map { aiyatsbusRarity(it)?.let { r -> aiyatsbusEts(r) } ?: listOf() }.flatten()
        },
    val skull: String = root.getString(
        "skull",
        "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNzRiODlhZDA2ZDMxOGYwYWUxZWVhZjY2MGZlYTc4YzM0ZWI1NWQwNWYwMWUxY2Y5OTlmMzMxZmIzMmQzODk0MiJ9fX0="
    )!!,
    val maxCoexist: Int = root.getInt("max_coexist", 1)
)

object GroupLoader {

    @Config("enchants/group.yml", autoReload = true)
    lateinit var config: Configuration
        private set

    val groups = ConcurrentHashMap<String, Group>()

    @Reloadable
    @Awake(LifeCycle.CONST)
    fun init() {
        registerLifeCycleTask(LifeCycle.ENABLE, StandardPriorities.GROUP) {
            load()
        }
    }

    @Awake(LifeCycle.ENABLE)
    fun reload() {
        registerLifeCycleTask(LifeCycle.ENABLE) {
            config.onReload {
                load()
            }
        }
    }

    private fun load() {
        val time = System.currentTimeMillis()
        groups.clear()
        groups += config.getKeys(false).map { config.getConfigurationSection(it)!! }.map { it.name to Group(it) }
        info("Loaded ${groups.size} groups in ${System.currentTimeMillis() - time}ms")
    }
}