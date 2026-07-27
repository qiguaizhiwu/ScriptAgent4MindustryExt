package ST.cmds.marks

import arc.math.geom.Vec2
import mindustry.core.World
import mindustry.entities.Units
import mindustry.gen.Unit
import mindustry.gen.Player
import java.time.Duration
import java.time.Instant


command("mark-mark", "标记当前鼠标坐标并广播(mark)") {
    aliases = listOf("mark")
    body {
        val player = player ?: returnReply("该命令后台不可用".with())
        val pn = player!!.name
        val mouseX = player.mouseX
        val mouseY = player.mouseY
        val tileX = (mouseX / 8).toInt()
        val tileY = (mouseY / 8).toInt()
        
        broadcast("${pn} [cyan]标记了一个坐标".with())
        broadcast("<MDTX Server>[#eab678ff]<Mark>[]($tileX,$tileY)".with())
        player.sendMessage("[green]已标记坐标: ($tileX,$tileY)",type = MsgType.Announce)
    }
}


command("mark-what", "标记当前鼠标坐标并广播(what)") {
    aliases = listOf("what")
    body {
        val player = player ?: returnReply("该命令后台不可用".with())
        val pn = player!!.name
        val mouseX = player.mouseX
        val mouseY = player.mouseY
        val tileX = (mouseX / 8).toInt()
        val tileY = (mouseY / 8).toInt()
        
        broadcast("${pn} [cyan]标记了一个坐标".with())
        broadcast("<MDTX Server>[#ff69b4ff]<What>[]($tileX,$tileY)".with())
        player.sendMessage("[green]已标记坐标: ($tileX,$tileY)",type = MsgType.Announce)
    }
}


command("mark-attack", "标记当前鼠标坐标并广播(attack)") {
    aliases = listOf("attack")
    body {
        val player = player ?: returnReply("该命令后台不可用".with())
        val pn = player!!.name
        val mouseX = player.mouseX
        val mouseY = player.mouseY
        val tileX = (mouseX / 8).toInt()
        val tileY = (mouseY / 8).toInt()
        
        broadcast("${pn} [cyan]标记了一个坐标".with())
        broadcast("<MDTX Server>[#dc143cff]<Attack>[]($tileX,$tileY)".with())
        player.sendMessage("[green]已标记坐标: ($tileX,$tileY)",type = MsgType.Announce)
    }
}


command("mark-defend", "标记当前鼠标坐标并广播(defend)") {
    aliases = listOf("defend")
    body {
        val player = player ?: returnReply("该命令后台不可用".with())
        val pn = player!!.name
        val mouseX = player.mouseX
        val mouseY = player.mouseY
        val tileX = (mouseX / 8).toInt()
        val tileY = (mouseY / 8).toInt()
        
        broadcast("${pn} [cyan]标记了一个坐标".with())
        broadcast("<MDTX Server>[#7fff00ff]<Defend>[]($tileX,$tileY)".with())
        player.sendMessage("[green]已标记坐标: ($tileX,$tileY)",type = MsgType.Announce)
    }
}
