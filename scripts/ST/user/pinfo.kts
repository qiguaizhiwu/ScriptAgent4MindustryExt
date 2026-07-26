package wayzer.ST.cmds.pinfo

import mindustry.gen.Iconc
import cf.wayzer.placehold.PlaceHoldApi.with
import mindustry.net.Administration
import arc.util.Time  
import kotlinx.coroutines.*  
import mindustry.gen.Groups  
import mindustry.gen.Player   
import kotlin.random.Random  
import arc.util.Align
import java.time.Duration
import kotlinx.coroutines.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
import java.lang.Thread.sleep
import coreMindustry.*
import java.io.File

registerVar("admincheck", "空占位", DynamicVar { Groups.unit.size() })

command("pinfo", "个人信息") {
    aliases = listOf("查看个人信息")
    body {
        val player = player ?: returnReply("[red]该命令后台不可用".with())
        val (admincheck, _) = if (player.hasPermission("wayzer.admin")) {
            "是" to registerVar("admincheck.ext", "空占位", null)
        } else {
            "否" to null
        }
        
        val pinfo = """
[cyan]个人信息

[acid]玩家名: [white]{player.name}
[acid]玩家ip: [white]{player.ip}
[acid]代表3位短ID: [white]{player.shortID}

[acid]你的账号是否为管理员: ${admincheck}

[white]玩家队伍:{player.team}
玩家单位:{player.unit}

[acid]玩家主IP:{player.ip}

[acid]玩家UUID:
{player.uuid}

        """.trimIndent()
        
        reply("${pinfo}".with(), MsgType.InfoMessage)
    }
}
