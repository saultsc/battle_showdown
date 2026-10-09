package saultsc.battle_showdown

import net.minecraft.resources.ResourceLocation
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import saultsc.battle_showdown.battle.BattlePreviewManager
import saultsc.battle_showdown.network.NetworkManager

object BattleShowdown {
    const val MOD_ID = BattleShowdownBuildDetails.MOD_ID
    const val VERSION = BattleShowdownBuildDetails.VERSION

    @JvmField
    val LOGGER: Logger = LogManager.getLogger(MOD_ID)

    lateinit var networkManager: NetworkManager
        private set

    fun init(networkManager: NetworkManager) {
        LOGGER.info("Launching $MOD_ID $VERSION")
        this.networkManager = networkManager
        BattlePreviewManager.register()
    }

    fun id(path: String): ResourceLocation = ResourceLocation.fromNamespaceAndPath(MOD_ID, path)
}
