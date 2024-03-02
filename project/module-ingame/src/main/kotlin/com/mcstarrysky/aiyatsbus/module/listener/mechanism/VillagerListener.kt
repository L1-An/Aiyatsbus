package com.mcstarrysky.aiyatsbus.module.listener.mechanism

import com.mcstarrysky.aiyatsbus.core.*
import com.mcstarrysky.aiyatsbus.core.data.CheckType
import com.mcstarrysky.aiyatsbus.core.data.Group
import org.bukkit.event.entity.VillagerAcquireTradeEvent
import org.bukkit.inventory.MerchantRecipe
import taboolib.common.platform.event.EventPriority
import taboolib.common.platform.event.SubscribeEvent
import taboolib.module.configuration.Config
import taboolib.module.configuration.ConfigNode
import taboolib.module.configuration.Configuration

/**
 * Aiyatsbus
 * com.mcstarrysky.aiyatsbus.module.listener.mechanism.VillagerListener
 *
 * @author mical
 * @since 2024/2/22 23:35
 */
object VillagerListener {

    @Config("mechanisms/villager.yml")
    lateinit var conf: Configuration
        private set

    @ConfigNode(bind = "mechanisms/villager.yml", value = "enableEnchantTrade")
    var enableEnchantTrade = true

    @ConfigNode(bind = "mechanisms/villager.yml", value = "tradeGroup")
    var tradeGroup = "可交易附魔"

    @ConfigNode(bind = "mechanisms/villager.yml", value = "amount")
    var amount = 2

    @SubscribeEvent(priority = EventPriority.LOWEST)
    fun acquireTrade(e: VillagerAcquireTradeEvent) {
        val origin = e.recipe
        val result = origin.result.clone()

        if (result.fixedEnchants.isEmpty()) return
        if (!enableEnchantTrade) {
            e.isCancelled = true
            return
        }

        result.clearEts()
        repeat(amount) {
            result.addEt((Group.groups[tradeGroup]?.enchantments ?: listOf<AiyatsbusEnchantment>()).filter {
                it.limitations.checkAvailable(CheckType.ATTAIN, result).first && it.alternativeData.isTradeable
            }.drawEt() ?: return@repeat)
        }
        if (result.fixedEnchants.isEmpty())
            e.isCancelled = true

        origin.run origin@{
            e.recipe = MerchantRecipe(result, uses, maxUses, hasExperienceReward(), villagerExperience, priceMultiplier, demand, specialPrice, shouldIgnoreDiscounts())
                .run new@{ this@new.ingredients = this@origin.ingredients; this }
        }
    }
}