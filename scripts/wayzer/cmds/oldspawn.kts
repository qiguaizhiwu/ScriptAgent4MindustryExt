package wayzer.cmds
//WayZer 版权所有(请勿删除版权注解)
import mindustry.ctype.ContentType
import mindustry.game.Team
import mindustry.type.UnitType

name = "扩展功能: 召唤单位"

command("spawn", "召唤单位(旧版,新版/spawns)") {
    usage = "[类型ID=列出] [队伍ID,默认为sharded] [数量=1] [armor=?]"
    permission = "wayzer.admin"
    aliases = listOf("召唤")
    body {
        val list = content.getBy<UnitType>(ContentType.unit).filterNot { it.internal }
        val type = arg.getOrNull(0)?.toIntOrNull()?.let { list.getOrNull(it) } ?: returnReply(
            "[red]请输入类型ID: {list}"
                .with("list" to list.mapIndexed { i, type -> "[yellow]$i[green]($type)" }.joinToString())
        )
        val team = arg.getOrNull(1)?.let { s ->
            s.toIntOrNull()?.let { Team.all.getOrNull(it) } ?: returnReply(
                "[red]请输入队伍ID: {list}"
                    .with("list" to Team.baseTeams.mapIndexed { i, type -> "[yellow]$i[green]($type)" }.joinToString())
            )
        } ?: Team.sharded
        val unit = player?.unit()
        val num = arg.getOrNull(2)?.toIntOrNull() ?: 1
        val armor = arg.getOrNull(3)?.toFloatOrNull()
        repeat(num) {
            type.create(team).apply {
                this.armor = armor ?: this.armor
                if (unit != null) set(unit.x, unit.y)
                else team.data().core()?.let {
                    set(it.x, it.y)
                }
                add()
            }
        }
        reply("[green]成功为 {team} 生成 {num} 只 {type}".with("team" to team, "num" to num, "type" to type.name))
        reply("[yellow]警告:目前已有新版本spawn:/spawns".with())
    }
}
