@file:Depends("coreMindustry/menu", "菜单选人")
@file:Depends("coreMindustry/utilTextInput", "输入消息")
@file:Depends("wayzer/cmds/share")

package wayzer.cmds

import coreMindustry.PagedMenuBuilder
import coreMindustry.UtilTextInput
import coreLibrary.lib.Commands
import coreLibrary.lib.command
import coreMindustry.lib.broadcast

// 菜单命令集合对象（类似 SkillCommands）
object MenuCommands : Commands()

command("pm", "私聊玩家".with()){
    aliases = listOf("私聊")
    usage = "<玩家名> <消息>"
    body {
        // 确保命令由玩家执行
        val sender = player ?: returnReply("[red]控制台不能使用私聊功能".with())

        // 获取目标玩家
        val target = if (arg.isNotEmpty()) {
            Groups.player.find { it.name.equals(arg[0], true) }
                ?: returnReply("[red]找不到玩家: {arg}".with("arg" to arg[0]))
        } else {
            // 菜单选择玩家
            var result: Player? = null
            PagedMenuBuilder(Groups.player.toList()) {
                option(it.name) { result = it }
            }.apply {
                title = "选择私聊对象"
                sendTo(sender, 60_000)
            }
            result ?: returnReply("[yellow]已取消".with())
        }

        // 获取消息内容
        val message = getInput("私聊内容", "[yellow]请输入你要私聊的内容".with())
     

        // 发送私聊消息
        target.sendMessage("[gray][[私聊] [cyan]${sender.name}[] -> 你:[white] $message".with())
        reply("[gray][[私聊] 你 -> [cyan]${target.name}[]: $message".with())
    }
}
