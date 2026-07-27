@file:Depends("coreMindustry/menu", "调用菜单")
package ST.cmds

import arc.graphics.Color
import mindustry.core.Version.type
import kotlin.random.Random
import coreMindustry.MenuBuilder
import java.*

val menu = coreMindustry.Menu::class.java.getContextScript()

suspend fun Player.choose(p: Player) {
    MenuBuilder<Unit>(true){
    	title = "烟花界面"
        msg = """请选择你的眼花类型
        
    [red]请勿连续放烟花！！！"""
        option("[yellow]小型烟花\n[white]最基础的小烟花"){
                firework(p, 1, 0)
        }
        newRow()
        option("[yellow]中型烟花\n[white]5次连续烟花"){
                firework(p, 3, 1)
        }
        newRow()
        option("[yellow]大型烟花\n[white]15次烟花与公屏宣告"){
                firework(p, 1, 2)
        }
    }.sendTo(p, 20_000)
}

suspend fun firework(p: Player, coin: Int, type: Int){
    val unit = p.unit()

        if(type == 0){
            Call.effect(Fx.impactReactorExplosion, unit.x, unit.y, 0f, Color.red)
        }else if(type == 1){
            launch(Dispatchers.game) {
                repeat(5) {
                    delay(500L)
                    val x = unit.x + (-200..200).random()
                    val y = unit.y + (-200..200).random()
                    Call.effect(Fx.reactorExplosion, x, y, 0f, Color.red)
                }
            }
        }else if(type == 2){
            launch(Dispatchers.game) {
                broadcast("[yellow]看！{name}正在({x},{y})放烟花！".with("name" to p.name, "x" to (unit.x / 8f).toInt(), "y" to (unit.y / 8f).toInt()))
                repeat(15) {
                    delay(500L)
                    val x = unit.x + (-250..250).random()
                    val y = unit.y + (-250..250).random()
                    if(Random.nextFloat() > 0.5f) {
                        Call.effect(Fx.reactorExplosion, x, y, 0f, Color.red)
                    }else{
                        Call.effect(Fx.impactReactorExplosion, x, y, 0f, Color.red)
                    }
                }
            }
        }else{

        }
}

command("firework", "燃放烟花"){
    type = CommandType.Client
    body {
        player!!.choose(player!!)

    }
}
