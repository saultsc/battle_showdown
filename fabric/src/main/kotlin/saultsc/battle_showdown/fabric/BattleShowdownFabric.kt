package saultsc.battle_showdown.fabric

import net.fabricmc.api.ModInitializer
import saultsc.battle_showdown.BattleShowdown
import saultsc.battle_showdown.fabric.network.FabricNetworkManager

object BattleShowdownFabric : ModInitializer {
    override fun onInitialize() {
        BattleShowdown.init(FabricNetworkManager)
        FabricNetworkManager.registerPayloads()
    }
}
