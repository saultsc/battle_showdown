package saultsc.battle_showdown.fabric.client

import net.fabricmc.api.ClientModInitializer
import saultsc.battle_showdown.fabric.network.FabricNetworkManager

object BattleShowdownFabricClient : ClientModInitializer {
    override fun onInitializeClient() {
        FabricNetworkManager.registerClientHandlers()
    }
}
