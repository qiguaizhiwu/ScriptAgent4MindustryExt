@file:Depends("coreMindustry/menu", "菜单系统")
@file:Depends("wayzer/maps", "地图管理")
@file:Depends("coreMindustry/utilTextInput", "输入文本")
@file:Depends("wayzer/cmds/share")

package cmds

import arc.util.Log
import coreMindustry.*
import mindustry.ctype.ContentType
import mindustry.game.Team
import mindustry.gen.Player
import mindustry.gen.*
import mindustry.type.UnitType
import kotlinx.coroutines.launch
import mindustry.gen.Iconc
import mindustry.gen.Icon
import mindustry.content.UnitTypes
import wayzer.cmds.*

name = "菜单召唤单位"

val PAGE_SIZE = 80

// 可变容器，用来存选中单位+队伍，替代var传参
class SpawnCache {
    var unit: UnitType? = null
    var team: Team? = null
}

fun getUnitTypes(): List<UnitType> {
    return content.getBy<UnitType>(ContentType.unit).filterNot { it.internal }
}

fun getAvailableTeams(): List<Team> {
    return Team.all.filter {
        it != Team.derelict && it.id != 255 && it.id >= 0 && it.id < 256
    }
}

suspend fun showUnitTypeMenu(player: Player, page: Int = 1, cache: SpawnCache) {
    val allUnits = getUnitTypes()
    val totalPages = maxOf(1, (allUnits.size + PAGE_SIZE - 1) / PAGE_SIZE)
    val currentPage = page.coerceIn(1, totalPages)

    val startIndex = (currentPage - 1) * PAGE_SIZE
    val endIndex = minOf(startIndex + PAGE_SIZE, allUnits.size)
    val pageUnits = allUnits.subList(startIndex, endIndex)

    MenuV2(player) {
        columnPreRow = 5
        title = "选择单位类型 - 第${currentPage}/${totalPages}页"
        msg = """
            [yellow]请选择要召唤的单位类型
            [gray]点击单位类型继续
            =========================
        """.trimIndent()

        pageUnits.forEach { unit ->
            option("${unit.emoji()}") {
                showTeamMenu(player, unit, cache)
            }
        }

        if (currentPage > 1) {
            option("[yellow]<< 上一页") {
                showUnitTypeMenu(player, currentPage - 1, cache)
            }
        }

        if (currentPage < totalPages) {
            option("[yellow]下一页 >>") {
                showUnitTypeMenu(player, currentPage + 1, cache)
            }
        }
        columnPreRow = 1
        option("[red]关闭菜单") {}
    }.send().await()
}

suspend fun showTeamMenu(player: Player, unit: UnitType, cache: SpawnCache) {
    val teams = getAvailableTeams()

    MenuV2(player) {
        columnPreRow = 3
        title = "选择队伍"
        msg = """
            [yellow]请选择召唤单位的队伍
            [gray]当前单位: [yellow]${unit.name}
            =========================
        """.trimIndent()

        teams.forEach { team ->
            val color = when(team.id) {
                Team.sharded.id -> "gray"
                Team.blue.id -> "blue"
                Team.crux.id -> "purple"
                else -> "white"
            }
            option("[$color]${team.coloredName()}") {
                showAmountMenu(player, unit, team, cache)
            }
        }
        columnPreRow = 1
        option("[blue]返回单位选择") {
            showUnitTypeMenu(player, 1, cache)
        }
    }.send().await()
}

suspend fun showAmountMenu(player: Player, unit: UnitType, team: Team, cache: SpawnCache) {
    val amounts = listOf(1, 5, 10, 20)

    MenuV2(player) {
        columnPreRow = 2
        title = "选择数量"
        msg = """
            [yellow]请选择召唤数量
            [gray]单位: [yellow]${unit.name}
            [gray]队伍: [${if(team == Team.sharded) "gray" else if(team == Team.blue) "blue" else "purple"}]${team.coloredName()}
            =========================
        """.trimIndent()

        amounts.forEach { amount ->
            option("[green]${amount}个") {
                spawnUnits(player, unit, team, amount)
            }
        }

        option("[yellow]输入自定义数量") {
            // 写入缓存
            cache.unit = unit
            cache.team = team
        }

        columnPreRow = 1
        option("[blue]返回队伍选择") {
            showTeamMenu(player, unit, cache)
        }
    }.send().await()
}

fun spawnUnits(player: Player, unit: UnitType, team: Team, amount: Int) {
    val playerUnit = player.unit()
    val spawnX = playerUnit?.x ?: player.x
    val spawnY = playerUnit?.y ?: player.y

    repeat(amount) {
        unit.create(team).apply {
            set(spawnX, spawnY)
            add()
        }
    }

    val teamColor = when(team.id) {
        Team.sharded.id -> "gray"
        Team.blue.id -> "blue"
        Team.crux.id -> "purple"
        else -> "white"
    }

    player.sendMessage(
        "[green]成功召唤 [yellow]${amount}个${unit.name} " +
                "到[$teamColor]${team.coloredName()}"
    )
    Log.info("${player.name} 召唤了 $amount 个 ${unit.name} (队伍: ${team.id})")
}

command("spawns", "打开新召唤单位菜单") {
    requirePermission("wayzer.admin")
    body {
        val p = player ?: returnReply("[red]需要玩家执行".with())
        launch(Dispatchers.game) {
            // 创建缓存容器
            val cache = SpawnCache()
            // 传入缓存对象
            showUnitTypeMenu(p, 1, cache)

            // 菜单走完，缓存有数据则执行getInput
            val selUnit = cache.unit
            val selTeam = cache.team
            if (selUnit != null && selTeam != null) {
                val input = getInput("输入消息", "[yellow]请输入召唤数量(1-100)".with())
                if (input = toIntOrNull()?.coerceIn(1, 100)) {
                    spawnUnits(p, selUnit, selTeam, input)
                } else {
                    p.sendMessage("[red]无效数字，召唤取消".with())
                }
            }
        }
    }
}
