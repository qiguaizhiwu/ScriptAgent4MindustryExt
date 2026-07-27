@file:Depends("coreMindustry/menu", "菜单支持")
@file:Depends("wayzer/cmds/share")

package wayzer.ext

import arc.Events
import arc.util.Interval
import coreLibrary.lib.command
import coreMindustry.MenuBuilder
import kotlinx.coroutines.*
import kotlin.random.Random
import kotlin.math.sqrt
import mindustry.gen.Player

val MENU_TIMEOUT_MS = 1000_000L

// 坐标类
data class Point(val x: Float, val y: Float) {
    fun distanceTo(other: Point): Float {
        val dx = other.x - x
        val dy = other.y - y
        return sqrt(dx * dx + dy * dy)
    }
    fun move(dx: Float, dy: Float): Point = Point(x + dx, y + dy)
    fun toGrid(): GridPoint = GridPoint(x.toInt(), y.toInt())
}
data class GridPoint(val x: Int, val y: Int)

// 单玩家独立游戏数据
class RouterGame {
    var routerPos = Point(5f, 5f)
    val bombs = mutableListOf<Point>()
    var score = 0
    var gameOver = false
    val moveSpeed = 1f
    val bombSpeed = 1f
    var turn = 0
    val bombAddInterval = 1
    val minSafeDist = 5f
    val mapSize = 10

    companion object {
        const val ROUTER_CHAR = ""
        const val BOMB_CHAR = ""
        const val FLOOR_CHAR = ""
    }

    private fun Point.clampMap(): Point {
        return Point(x.coerceIn(0f, mapSize - 1f), y.coerceIn(0f, mapSize - 1f))
    }

    fun spawnBomb(): Boolean {
        val router = routerPos
        val routerGrid = router.toGrid()
        repeat(50) {
            val x = Random.nextFloat() * (mapSize - 2) + 0.5f
            val y = Random.nextFloat() * (mapSize - 2) + 0.5f
            val bomb = Point(x, y)
            if (bomb.distanceTo(router) >= minSafeDist && bomb.toGrid() != routerGrid) {
                bombs.add(bomb)
                return true
            }
        }
        val safeCorner = if (routerGrid == GridPoint(0, 0))
            Point(mapSize - 1.5f, mapSize - 1.5f)
        else Point(0.5f, 0.5f)
        bombs.add(safeCorner)
        return true
    }

    fun moveAllBombs() {
        val router = routerPos
        val newBombs = mutableListOf<Point>()
        for (bomb in bombs) {
            val dist = bomb.distanceTo(router)
            if (dist < 0.5f) {
                gameOver = true
                return
            }
            val dx = (router.x - bomb.x) / dist * bombSpeed
            val dy = (router.y - bomb.y) / dist * bombSpeed
            val newPos = bomb.move(dx, dy).clampMap()
            if (newPos.distanceTo(router) < 0.7f) {
                gameOver = true
                return
            }
            newBombs.add(newPos)
        }
        bombs.clear()
        bombs.addAll(newBombs)
    }

    fun moveRouter(dx: Int, dy: Int) {
        if (gameOver) return
        val newPos = routerPos.move(dx * moveSpeed, -dy * moveSpeed).clampMap()
        if (newPos == routerPos) return
        routerPos = newPos
        turn++
        score++
        if (turn % bombAddInterval == 1) spawnBomb()
        checkCollision()
    }

    fun checkCollision() {
        val router = routerPos
        bombs.forEach { bomb ->
            if (bomb.distanceTo(router) < 0.7f) {
                gameOver = true
                return
            }
        }
    }

    fun buildMapText(): String {
        val map = Array(mapSize) { Array(mapSize) { FLOOR_CHAR } }
        val routerGrid = routerPos.toGrid()
        map[routerGrid.y][routerGrid.x] = "[green]$ROUTER_CHAR[]"
        bombs.forEach { bomb ->
            val bg = bomb.toGrid()
            if (bg != routerGrid) map[bg.y][bg.x] = "[red]$BOMB_CHAR[]"
        }
        val sb = StringBuilder("回合:${turn} 得分:${score}\n")
        map.forEach { row -> sb.append(row.joinToString("") + "\n") }
        // 修复：分开文本，不要把中文写到常量名里
        sb.append("\n[green]$ROUTER_CHAR 路由器 [red]$BOMB_CHAR 炸弹 [white]$FLOOR_CHAR 地板")
        return sb.toString()
    }
}

// 玩家游戏缓存
val playerGameMap = mutableMapOf<Player, RouterGame>()
val gameTickInterval = Interval()

// 全局帧循环
fun initGameTick() {
    Events.run(Interval(1)) {
        if (playerGameMap.isNotEmpty()) {
            playerGameMap.values.forEach { game ->
                if (!game.gameOver) game.moveAllBombs()
            }
        }
    }
}

// ===================== MenuV2 扩展函数 =====================
suspend fun Player.showRouterGameMenu() {
    val player = this
    val game = playerGameMap[player] ?: return
    while (true) {
        try {
            // 修复Long转Int：MENU_TIMEOUT_MS.toInt()
            val result = MenuBuilder<Unit>(true) {
                title = "[yellow]路由器逃生小游戏 | 当前得分:${game.score}"
                msg = game.buildMapText()

                option("[green]↑ 向上移动") {
                    game.moveRouter(0, 1)
                }
                newRow()

                option("[green]← 向左") {
                    game.moveRouter(-1, 0)
                }
                option("[green]→ 向右") {
                    game.moveRouter(1, 0)
                }
                newRow()

                option("[green]↓ 向下移动") {
                    game.moveRouter(0, -1)
                }
                newRow()

                option("[red]退出本局游戏") {
                    game.gameOver = true
                    player.sendMessage("[yellow]已退出当前游戏".with())
                    playerGameMap.remove(player)
                }
            }.sendTo(player, MENU_TIMEOUT_MS.toInt())

            // 超时result=null，继续循环重开菜单
            if (result == null) continue
            break
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    // 游戏结束后打开结算菜单
    if (playerGameMap.containsKey(player)) {
        player.showGameOverMenu()
    }
}

suspend fun Player.showGameOverMenu() {
    val player = this
    val game = playerGameMap[player] ?: return
    while (true) {
        try {
            // 修复Long转Int
            val result = MenuBuilder<Unit>(true) {
                title = "[red]游戏结束"
                msg = """
                [white]最终得分: ${game.score}
                [white]总回合数: ${game.turn}
                """.trimIndent()

                option("[green]再来一局") {
                    startNewRouterGame(player)
                }
                newRow()
                option("[red]彻底退出游戏") {
                    playerGameMap.remove(player)
                    player.sendMessage("[gray]已关闭路由器小游戏".with())
                }
            }.sendTo(player, MENU_TIMEOUT_MS.toInt())

            if (result == null) continue
            break
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}

fun startNewRouterGame(player: Player) {
    playerGameMap.remove(player)
    val newGame = RouterGame()
    newGame.spawnBomb()
    playerGameMap[player] = newGame
    CoroutineScope(Dispatchers.game).launch {
        player.showRouterGameMenu()
    }
    player.sendMessage("[green]路由器游戏已开启，使用菜单方向躲避炸弹！".with())
}

// 指令
command("router", "开启路由器逃生小游戏") {
    aliases = listOf("r")
    permission = "wayzer.admin"
    body {
        val p = player ?: returnReply("[red]仅玩家可执行".with())
        if (playerGameMap.containsKey(p)) {
            p.sendMessage("[yellow]已存在对局，将重新开始！".with())
        }
        startNewRouterGame(p)
    }
}

// 初始化帧循环
initGameTick()
