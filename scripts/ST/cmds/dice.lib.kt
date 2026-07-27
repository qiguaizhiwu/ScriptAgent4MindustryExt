package ST.dice

import cf.wayzer.placehold.PlaceHoldApi.with
import coreLibrary.lib.*
import coreMindustry.lib.ClientOnly
import coreMindustry.lib.broadcast
import coreMindustry.lib.player
import mindustry.Vars
import mindustry.gen.Player
import java.time.Duration

object dicecheck : CommandHandler {
    private val mapDisabled get() = Vars.state.rules.tags.getBool("@noSkills")
    
        override suspend fun CommandContext.handle() {
        ClientOnly.handle()
        if (mapDisabled) returnReply("[red]当前地图禁用dice".with())
        if (player!!.dead()) returnReply("[red]死亡状态无法使用dice".with())
    }
}
