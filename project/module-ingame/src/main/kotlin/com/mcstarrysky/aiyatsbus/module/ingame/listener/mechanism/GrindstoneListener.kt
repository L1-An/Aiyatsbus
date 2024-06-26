@file:Suppress("deprecation")

package com.mcstarrysky.aiyatsbus.module.ingame.listener.mechanism

import com.destroystokyo.paper.event.inventory.PrepareGrindstoneEvent
import com.destroystokyo.paper.event.player.PlayerPickupExperienceEvent
import com.mcstarrysky.aiyatsbus.core.addEt
import com.mcstarrysky.aiyatsbus.core.clearEts
import com.mcstarrysky.aiyatsbus.core.fixedEnchants
import com.mcstarrysky.aiyatsbus.core.isInGroup
import com.mcstarrysky.aiyatsbus.core.util.calcToDouble
import com.mcstarrysky.aiyatsbus.core.util.calcToInt
import com.mcstarrysky.aiyatsbus.core.util.remove
import org.bukkit.entity.ExperienceOrb
import org.bukkit.entity.Player
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.inventory.ItemStack
import taboolib.common.platform.event.EventPriority
import taboolib.common.platform.event.SubscribeEvent
import taboolib.library.configuration.ConfigurationSection
import taboolib.module.configuration.Config
import taboolib.module.configuration.ConfigNode
import taboolib.module.configuration.Configuration
import taboolib.module.configuration.conversion
import java.util.*
import kotlin.math.roundToInt

@ConfigNode(bind = "core/mechanisms/grindstone.yml")
object GrindstoneListener {

    @Config("core/mechanisms/grindstone.yml", autoReload = true)
    lateinit var conf: Configuration
        private set

    @ConfigNode("grindstone.vanilla")
    var enableVanilla = true
    @ConfigNode("exp_per_enchant")
    var expPerEnchant = "30*{level}/{max_level}*{rarity_bonus}"
    @ConfigNode("accumulation")
    var accumulation = true
    @ConfigNode("default_bonus")
    var defaultBonus = 1.0
    @ConfigNode("blacklist_group")
    var blacklist = "不可磨砂类附魔"

    @delegate:ConfigNode("rarity_bonus")
    val rarityBonus by conversion<ConfigurationSection, Map<String, Double>> {
        mapOf(*getKeys(false).map { it to getDouble(it) }.toTypedArray())
    }

    @delegate:ConfigNode("privilege")
    val privilege by conversion<List<String>, Map<String, String>> {
        mapOf(*map { it.split(":")[0] to it.split(":")[1] }.toTypedArray())
    }

    private val grindstoning = mutableMapOf<UUID, Int>()

    @SubscribeEvent(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    fun grindstone(event: PrepareGrindstoneEvent) {
        if (!enableVanilla) return

        val player = event.viewers.getOrNull(0) as? Player ?: return
        val result = event.result?.clone() ?: return
        val inv = event.inventory
        val upper = inv.upperItem
        val lower = inv.lowerItem
        var exp = 0

        result.clearEts()
        grind(player, upper)?.let { (item, refund) ->
            item.fixedEnchants.forEach { (enchant, level) -> result.addEt(enchant, level) }
            exp += refund
        }
        grind(player, lower)?.let { (item, refund) ->
            item.fixedEnchants.forEach { (enchant, level) -> result.addEt(enchant, level) }
            exp += refund
        }

        grindstoning[player.uniqueId] = exp
        event.result = result
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    fun exp(event: PlayerPickupExperienceEvent) {
        if (!enableVanilla) return

        val orb = event.experienceOrb
        val uuid = orb.triggerEntityId ?: return
        if (orb.spawnReason != ExperienceOrb.SpawnReason.GRINDSTONE) return

        grindstoning.remove(uuid)?.let {
            orb.experience = it
        } ?: run {
            orb.remove()
            event.isCancelled = true
            return
        }
    }

    @SubscribeEvent
    fun quit(e: PlayerQuitEvent) {
        if (!enableVanilla) return

        grindstoning.remove(e.player.uniqueId)
    }

    private fun grind(player: Player, item: ItemStack?): Pair<ItemStack, Int>? {
        var total = 0.0
        val result = item?.clone() ?: return null
        result.clearEts()
        item.fixedEnchants.forEach { (enchant, level) ->
            val maxLevel = enchant.basicData.maxLevel
            if (enchant.enchantment.isInGroup(blacklist)) result.addEt(enchant, level)
            else {
                val bonus = rarityBonus[enchant.rarity.id] ?: rarityBonus[enchant.rarity.name] ?: defaultBonus
                val refund = expPerEnchant.calcToDouble("level" to level, "max_level" to maxLevel, "bonus" to bonus)
                if (accumulation) total += refund
                else total = maxOf(total, refund)
            }
        }

        return result to finalRefund(total, player)
    }

    private fun finalRefund(origin: Double, player: Player) = privilege.maxOf { (perm, expression) ->
        if (player.hasPermission(perm)) expression.calcToInt("refund" to origin)
        else origin.roundToInt()
    }.coerceAtLeast(0)
}