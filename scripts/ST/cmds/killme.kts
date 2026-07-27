package wayzer.cmds

import cf.wayzer.placehold.PlaceHoldApi.with
import mindustry.net.Administration
import arc.Events
import arc.util.Time
import mindustry.game.EventType.GameOverEvent
import java.time.Instant
import kotlin.math.ceil
import kotlin.random.Random
import mindustry.ctype.ContentType
import mindustry.io.SaveVersion
import mindustry.world.Block
import arc.struct.ObjectMap
import arc.util.pooling.Pools
import mindustry.game.EventType.ResetEvent
import mindustry.world.blocks.storage.StorageBlock
import mindustry.world.blocks.storage.StorageBlock.StorageBuild
import mindustry.net.Administration.PlayerInfo
import arc.util.CommandHandler
import mindustry.gen.*
import mindustry.gen.Player

command("killme", "自杀") {
    type = CommandType.Client
    body {
    val pn = player!!.name()
    val unit = player!!.unit()
        broadcast("玩家${pn} \n[red]杀死了自己附身的单位 [white]\n${unit}".with())
        unit.kill()
    }
}
