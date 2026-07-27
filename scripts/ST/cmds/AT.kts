@file:Depends("coreMindustry/menu", "菜单选人")
@file:Depends("coreMindustry/utilTextInput", "输入消息")
@file:Depends("wayzer/cmds/share")

package wayzer.cmds

import arc.util.Log
import mindustry.gen.Groups
import mindustry.gen.Player
import mindustry.gen.Call
import cf.wayzer.placehold.VarString
import coreMindustry.PagedMenuBuilder
import coreMindustry.UtilTextInput

command("At", "选择玩家发送@消息".with()) {
    aliases = listOf("@")
    usage = ""
    body {
        val sender = player ?: returnReply("[red]控制台不能使用@功能".with())
        
        //获取在线玩家列表
        val onlinePlayers = Groups.player.toList()
        if (onlinePlayers.isEmpty()) {
            returnReply("[red]当前没有其他在线玩家".with())
        }

        //处理参数逻辑
        var targetPlayer: Player? = null

        //情况1：有参数时先尝试匹配玩家名
        if (arg.isNotEmpty()) {
            val searchName = arg[0]
            //模糊匹配玩家名
            val matches = onlinePlayers.filter { 
                it.name().contains(searchName, ignoreCase = true) 
            }
            when (matches.size) {
                0 -> {
                    //无匹配时显示菜单
                    reply("[yellow]未找到玩家 '$searchName'，显示在线玩家列表".with())
                }
                1 -> {
                    //唯一匹配时直接选中
                    targetPlayer = matches[0]
                }
                else -> {
                    //多个匹配时显示筛选后的菜单
                    reply("[yellow]找到多个匹配玩家，请选择".with())
                    PagedMenuBuilder(matches) {
                        option(it.name()) { targetPlayer = it }
                    }.apply {
                        title = "选择@目标玩家"
                        sendTo(sender, 60_000)
                    }
                }
            }
        } else {
            //情况2：无参数时直接显示完整菜单
            PagedMenuBuilder(onlinePlayers) {
                option(it.name()) { targetPlayer = it }
            }.apply {
                title = "选择@目标玩家"
                sendTo(sender, 60_000)
            }
        }

        //等待玩家选择（如果还没选择）
        if (targetPlayer == null) {
            //检查是否在菜单选择后仍未确定目标
            targetPlayer ?: returnReply("[yellow]已取消选择".with())
        }

        //获取消息内容
        var content = getInput("输入消息", "[yellow]请输入@消息内容".with())
        

        //发送@消息
        //发送广播消息
    val broadcastMsg = "[white]${sender.name()}: [red]@ [white]${targetPlayer.name()} [white]: $content".with().toString()
    Call.sendMessage(broadcastMsg)
    //发送@提醒消息
    val targetMsg = "[white]${sender.name()} [red] @[white]你[white]：\n$content".with().toString()
    targetPlayer.sendMessage(targetMsg,MsgType.InfoMessage)
    }
}

// 注册@all指令：触发弹窗输入并发送全体消息
command("ATall", "发送@全体成员消息".with()) {
    aliases = listOf("@all")
    usage = ""
    body {
        val sender = player ?: returnReply("[red]控制台不能使用@all功能".with())
        
        // 获取在线玩家（排除发送者自己）
        val onlinePlayers = Groups.player.toList().filter { it != sender }
        if (onlinePlayers.isEmpty()) {
            returnReply("[red]当前没有其他在线玩家".with())
        }

        // 步骤1：弹出消息输入弹窗
        val content = getInput("输入消息", "[yellow]请输入@消息内容".with())
        

        // 步骤2：发送全体消息
        // 1. 向全体玩家广播消息
        val broadcastMsg = "[gray]${sender.name()}[red] @全体成员 [white]$content".with()
        broadcast(broadcastMsg.with())
        
        // 2. 向每个在线玩家发送单独提醒
        broadcast("[yellow]${sender.name()}[red] @全体成员[white]：\n$content".with(),type = MsgType.InfoMessage)

        // 反馈发送结果
        reply("[green]已向所有 ${onlinePlayers.size} 名在线玩家发送消息".with())
    }
}
