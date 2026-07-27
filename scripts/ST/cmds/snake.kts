@file:Depends("coreMindustry/menu", "菜单支持")

package wayzer.ext

import arc.util.Time
import kotlinx.coroutines.*
import mindustry.gen.Player
import mindustry.gen.Call
import coreMindustry.MenuV2
import kotlin.random.Random
import coreLibrary.lib.Commands
import coreLibrary.lib.command
import coreMindustry.lib.broadcast

// 菜单命令集合对象（类似 SkillCommands）
object MenuCommands : Commands()

// 方向枚举
enum class Direction(val dx: Int, val dy: Int) {
    UP(0, -1), DOWN(0, 1), LEFT(-1, 0), RIGHT(1, 0)
}

// 贪吃蛇游戏状态类
class SnakeGame(private val width: Int = 10, private val height: Int = 10) {
    // 游戏元素字符
    companion object {
        const val EMPTY = ""
        const val HEAD = ""
        const val BODY = "c"
        const val APPLE = ""
    }

    // 蛇身体（第一个是蛇头）
    private var snake = mutableListOf(
        Point(width / 2, height / 2),
        Point(width / 2 - 1, height / 2),
        Point(width / 2 - 2, height / 2)
    )
    private var apple = generateApple()
    private var direction = Direction.RIGHT
    private var nextDirection: Direction? = null
    var score = 0
    var gameOver = false
    var win = false

    // 生成苹果位置
    private fun generateApple(): Point {
        while (true) {
            val x = Random.nextInt(width)
            val y = Random.nextInt(height)
            val point = Point(x, y)
            if (!snake.contains(point)) return point
        }
    }

    // 移动蛇
    fun move() {
        // 更新方向
        nextDirection?.let { newDir ->
            if (!isOpposite(newDir, direction)) {
                direction = newDir
            }
            nextDirection = null
        }

        // 计算新蛇头位置（穿越边界）
        val head = snake.first()
        val newHead = Point(
            (head.x + direction.dx + width) % width,
            (head.y + direction.dy + height) % height
        )

        // 检查碰撞
        if (snake.drop(1).any { it == newHead }) {
            gameOver = true
            return
        }

        // 检查是否吃到苹果
        if (newHead == apple) {
            snake.add(0, newHead)
            apple = generateApple()
            score = snake.size - 3

            // 检查是否获胜（占满屏幕）
            if (snake.size == width * height) {
                win = true
                gameOver = true
                return
            }
        } else {
            // 移动蛇
            snake.add(0, newHead)
            snake.removeLast()
        }
    }

    // 检查两个方向是否相反
    private fun isOpposite(dir1: Direction, dir2: Direction): Boolean {
        return dir1.dx == -dir2.dx && dir1.dy == -dir2.dy
    }

    // 设置方向（防止180度转弯）
    fun setDirection(newDir: Direction) {
        if (!isOpposite(newDir, direction)) {
            nextDirection = newDir
        }
    }

    // 渲染游戏画面
    fun render(): String {
        val grid = Array(height) { Array(width) { EMPTY } }

        // 放置苹果
        grid[apple.y][apple.x] = APPLE

        // 放置蛇
        snake.forEachIndexed { i, point ->
            grid[point.y][point.x] = if (i == 0) HEAD else BODY
        }

        // 构建字符串
        val sb = StringBuilder()
        sb.append("分数: $score | 长度: ${snake.size}\n")
        grid.forEach { row ->
            row.forEach { cell ->
                when (cell) {
                    HEAD -> sb.append("[green][]")
                    BODY -> sb.append("[yellow][]")
                    APPLE -> sb.append("[red][]")
                    else -> sb.append("")
                }
                sb.append("")
            }
            sb.appendLine()
        }
        return sb.toString()
    }

    // 重置游戏
    fun reset() {
        snake = mutableListOf(
            Point(width / 2, height / 2),
            Point(width / 2 - 1, height / 2),
            Point(width / 2 - 2, height / 2)
        )
        apple = generateApple()
        direction = Direction.RIGHT
        nextDirection = null
        score = 0
        gameOver = false
        win = false
    }
}

// 坐标点数据类
data class Point(val x: Int, val y: Int)

// 玩家游戏状态映射
private val snakeGames = mutableMapOf<Player, SnakeGame>()

// 贪吃蛇指令
command("snake", "启动贪吃蛇小游戏") {
    body {
        val player = player ?: return@body
        // 如果玩家已有游戏，先移除
        snakeGames.remove(player)?.let {
            player.sendMessage("[yellow]已重置游戏")
        }

        val game = SnakeGame()
        snakeGames[player] = game

        launch {
            showSnakeMenu(player, game)
        }
    }
}

command("snake", "启动贪吃蛇小游戏".with(), commands = MenuCommands ) {
    body {
        val player = player ?: return@body
        // 如果玩家已有游戏，先移除
        snakeGames.remove(player)?.let {
            player.sendMessage("[yellow]已重置游戏")
        }

        val game = SnakeGame()
        snakeGames[player] = game

        launch {
            showSnakeMenu(player, game)
        }
    }
}

// 显示贪吃蛇游戏菜单
private suspend fun showSnakeMenu(player: Player, game: SnakeGame) {
    MenuV2(player) {
        title = "贪吃蛇小游戏"
        msg = game.render()

        // 游戏结束处理
        if (game.gameOver) {
            msg = if (game.win) {
                "[green]恭喜获胜![]\n${game.render()}\n分数: ${game.score}"
            } else {
                "[red]游戏结束![]\n${game.render()}\n分数: ${game.score}"
            }

            // 修复 column 函数调用问题
            newRow()
            option("[green]再来一局") {
                game.reset()
                refresh()
            }
            option("[red]退出游戏") {
                snakeGames.remove(player)
            }
            return@MenuV2
        }

        // 游戏控制按钮 - 修复 column 函数调用问题
        newRow()
        option("[green]↑ 上") {
            game.setDirection(Direction.UP)
            game.move()
            refresh()
        }

        newRow()
        columnPreRow = 2
        option("[green]← 左") {
            game.setDirection(Direction.LEFT)
            game.move()
            refresh()
        }
        
        option("[green]→ 右") {
            game.setDirection(Direction.RIGHT)
            game.move()
            refresh()
        }
        
        columnPreRow = 1
        option("[green]↓ 下") {
            game.setDirection(Direction.DOWN)
            game.move()
            refresh()
        }

        newRow()
        option("[red]退出游戏") {
            snakeGames.remove(player)
        }
    }.send().awaitWithTimeout()
}

// 玩家离开时清理游戏
listen<EventType.PlayerLeave> { e ->
    snakeGames.remove(e.player)
}
