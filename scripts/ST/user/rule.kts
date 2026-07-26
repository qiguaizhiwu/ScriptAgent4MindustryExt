package ST.rule

import mindustry.gen.Call
import mindustry.gen.Player
import mindustry.mod.Plugin

command("rule", "查看服务器玩家规则") {
    aliases = listOf("玩家规则")
    body {
        val rule = """
        [#dafcffff]光[#ffd37fff]之[#f0d4ffff]域 [white]玩家规则:
        [yellow]
        不得刷屏，辱骂，挑衅玩家(包括起哄)
        不得在PVP时恶意组队，捣乱。
        不得做换炸服图，炸电池等造成服务器问题(卡顿)

        [red]禁止
        刷进出服信息  
        骂人,嘲讽,诋毁,诈骗他人,
        宣传禁止内容或网站
        如发现有违规请跟管理反映或私信Q
        
        举报qq: 3288163665
        
        """.trimIndent()
        reply("${rule}".with(),MsgType.InfoMessage)
    }
}
