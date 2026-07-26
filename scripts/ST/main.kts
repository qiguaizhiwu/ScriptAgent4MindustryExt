package ST.main

import java.time.Instant
import kotlin.math.ceil
import kotlin.random.Random

command("ST", "ST插件运行状态") {
    body {
         val STmain = """
[acid]ST脚本正常运行中...

[red]服务器版本:[#C1FFC1]{game.version}

[cyan]已运行(开机)时间:[orange]
{state.uptime}
[yellow]在本局游戏的运行时间:
[orange]{state.gameTime 分钟}/{state.gameTime 秒}


[white]Produced by  [#bdaaff]奇怪[#aae8ff]の物
        """.trimIndent()
        reply("${STmain}".with(),MsgType.InfoMessage)
    }
}
