//WayZer 版权所有(请勿删除版权注解)
import arc.util.Align
import java.time.Duration
import mindustry.gen.Player
import mindustry.gen.Groups
import cf.wayzer.placehold.PlaceHoldApi.with
import java.io.File
import mindustry.game.EventType

val defaultTemplate = """
           [#dafcffff]光[#ffd37fff]之[#f0d4ffff]域
[#d2b48cff] <8-Server-MDTX>
|[#7FFFD4] < 当前地图/计时 RAM>[]
|[#87CEEB][{map.id}][white]{map.name}[cyan][{map.mode}][]
|[#AFEEEE]{state.gameTime 分钟}/{state.gameTime 秒}[]
|[#f0d4ffff]{heapUse}/16384MB[]

[#FFB6C1]       < 常用指令 >[]
|Q群/join  服规/rule
|赞助/ctl  [royal]关闭/broad[]
""".trimIndent()

val template by config.key(
    defaultTemplate, "积分榜模板",
    "其中{cK}{cV}{cA}为颜色变量，{listPrefix xx}行供其他插件动态扩展。",
    "开头{magic}会被替换特殊颜色，供MDTX客户端识别",
)
//Color变量 cK - KEY, cV - VALUE, cA - ACTION
val msg
    get() = template.with(
        "magic" to "[#FEBBEF][]",//供MDTX识别
        "cK" to "[gray]", "cV" to "[lightgray]", "cA" to "[slate]",
    )

val disabled = mutableSetOf<String>()

command("board", "开关积分板显示") {
    aliases = listOf("broad", "scoreboard")
    attr(ClientOnly)
    body {
        if (!disabled.remove(player!!.uuid()))
            disabled.add(player!!.uuid())
        reply("[green]切换成功".with())
    }
}

//避免找不到 scoreboard.ext.* 变量
registerVar("scoreboard.ext.null", "空占位", null)
registerVar("scoreBroad.ext.null", "空占位(兼容旧插件)", null)

registerVar("scoreboard.ext.patches-count", "Patcher状态显示", DynamicVar {
    if (state.data.patches.isEmpty) return@DynamicVar null
    "{cK}属性修改已加载: {cV}{count}".with("count" to state.data.patches.size)
})

onEnable {
    loop(Dispatchers.game) {
        delay(Duration.ofSeconds(1).toMillis())
        Groups.player.forEach {
            if (disabled.contains(it.uuid())) return@forEach
            val mobile = it.con?.mobile == true
            Call.infoPopup(
                it.con, msg.with().toPlayer(it), 1.013f,
                Align.topLeft, if (mobile) 210 else 190, 0, 0, 0
            )
        }
    }
}
