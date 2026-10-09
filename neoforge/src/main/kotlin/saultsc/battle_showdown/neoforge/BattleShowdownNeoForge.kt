package saultsc.battle_showdown.neoforge

import net.neoforged.fml.ModLoadingContext
import net.neoforged.fml.common.Mod
import saultsc.battle_showdown.BattleShowdown
import saultsc.battle_showdown.neoforge.network.NeoForgeNetworkManager

@Mod(BattleShowdown.MOD_ID)
object BattleShowdownNeoForge {
    init {
        BattleShowdown.init(NeoForgeNetworkManager)

        val modBus = requireNotNull(ModLoadingContext.get().activeContainer.eventBus) { "Missing mod event bus" }
        modBus.addListener(NeoForgeNetworkManager::registerPayloads)
    }
}
