package ST.dice

import kotlin.random.Random
import arc.util.Timer
import mindustry.gen.Player
import mindustry.gen.Building
import java.util.concurrent.ConcurrentHashMap
import mindustry.content.Items
import mindustry.Vars
import coreLibrary.lib.CommandContext
import coreLibrary.lib.CommandHandler
import coreLibrary.lib.CommandInfo
import coreLibrary.lib.Commands

command("dice-dicelist", "骰子概率公示") {
    aliases = listOf("dl")
    body {
           
            val text = """
    骰子dice各奖项公示(物品和单位)
    
    [red]运气没了 你...完蛋了! [cyan](仅在itemdice中出现)
    [red]运气几乎没了 你...   
    [red]运气糟透了 你是*多了吗？!  
    [red]运气不佳! [pink]哎,你这小南梁
    [yellow]运气还可以! [pink]杂鱼~杂鱼~  
    [yellow]运气较好!  
    [green]运气超好!   
    [gold]欧皇转世! 小心你的阳寿! 
        """.trimIndent()
        reply("${text}".with(),MsgType.InfoMessage)
    }
}

// itemdice

// 存储玩家冷却时间的映射表，使用玩家ID作为键，冷却结束时间作为值
private val cooldowns = ConcurrentHashMap<String, Long>()

command("dice-itemdice", "物品运气骰子，300秒冷却") {
    aliases = listOf("di")
    type = CommandType.Client
    attr(dicecheck)
    body {
        val player = player!!
        val playerId = player.uuid() // 使用玩家唯一ID跟踪冷却
        
        if (player!!.dead())
            returnReply("[red]你已死亡".with())
            
        if (state.rules.pvp)
            returnReply("[red]PVP禁用dice".with())
        
        val currentTime = System.currentTimeMillis()
        val cooldownTime = 300 * 1000 // 300秒冷却，单位为毫秒
        
        // 检查冷却时间
        val lastUseTime = cooldowns[playerId] ?: 0
        if (currentTime - lastUseTime < cooldownTime) {
            val remaining = (cooldownTime - (currentTime - lastUseTime)) / 1000
            player.sendMessage("[red]请等待${remaining}秒后再使用骰子命令!".with())
            return@body
        }
        
        // 更新冷却时间
        cooldowns[playerId] = currentTime
        
        val pn = player.name
        
        // 生成0-100的随机数
        val randomIntInRange = Random.nextInt(101)
        
        // 判定运气等级
        val luckMessage = when {
            randomIntInRange < 1 -> "[red]运气没了 你...完蛋了!(坏笑)"
            randomIntInRange < 5 -> "[red]运气几乎没了 你..."
            randomIntInRange < 10 -> "[red]运气糟透了 你是*多了吗？!"
            randomIntInRange < 30 -> "[red]运气不佳! [pink]哎,你这小南娘"
            randomIntInRange < 60 -> "[yellow]运气还可以! [pink]杂鱼~杂鱼~"
            randomIntInRange < 80 -> "[yellow]运气较好!"
            randomIntInRange < 99 -> "[green]运气超好!"
            else -> "[gold]欧皇转世! 小心你的阳寿!"
        }
        val de = when {
            randomIntInRange < 1 -> -1
            randomIntInRange < 5 -> 0
            randomIntInRange < 10 -> 1
            randomIntInRange < 30 -> 2
            randomIntInRange < 60 -> 3
            randomIntInRange < 80 -> 4
            randomIntInRange < 95 -> 5
            else -> 6
        }
        val dm = when {
            de == -1 -> "核心爆炸!"
            de == 0 -> "核心扣光物品"
            de == 1 -> "核心扣10000物品"
            de == 2 -> "核心扣5000物品"
            de == 3 -> "核心加1500物品"
            de == 4 -> "核心加5000物品"
            de == 5 -> "核心加10000物品"
            else -> "核心加many many物品("
        }
        
        
        
        // 发送第一条消息
        broadcast("${pn}[yellow]丢出了一个物品运气骰子".with())
        
        // 使用Timer延迟1秒发送结果
        Timer.schedule({
            broadcast("[yellow]骰子结果是：${randomIntInRange} !  ${luckMessage}".with())
            broadcast("${dm}".with())
            val Items = Vars.content.items()
            val core = player!!.team().core()
            
            if(core == null)
            returnReply("[red]你的队伍核心已炸".with())
        
        if (de == -1){
        val team = player!!.team()
        team.data().cores.toArray().forEach {
                    if (it.team == team) it.kill()
                }
        }    
        if (de == 0){
        Items.each { item ->
        core.items.remove(item, 999999999)
        }
        }
        if (de == 1){
        Items.each { item ->
        core.items.remove(item, 10000)
        }
        }
        if (de == 2){
        Items.each { item ->
        core.items.remove(item, 5000)
        }
        }
        if (de == 3){
        Items.each { item ->
        core.items.add(item, 1500)
        }
        }
        if (de == 4){
        Items.each { item ->
        core.items.add(item, 5000)
        }
        }
        if (de == 5){
        Items.each { item ->
        core.items.add(item, 10000)
        }
        }
        if (de == 6){
        Items.each { item ->
        core.items.add(item, 999999999)
        }
        }
        }, 1f)
    }
}


// unitdice

// 存储玩家冷却时间的映射表，使用玩家ID作为键，冷却结束时间作为值
private val ducooldowns = ConcurrentHashMap<String, Long>()

command("dice-unitdice", "单位运气骰子，120秒冷却") {
    aliases = listOf("du")
    type = CommandType.Client
    attr(dicecheck)
    body {
        val player = player!!
        val playerId = player.uuid() // 使用玩家唯一ID跟踪冷却
        
        if (player!!.dead())
            returnReply("[red]你已死亡".with())
            
        if (state.rules.pvp)
            returnReply("[red]PVP禁用dice".with())
        
        val currentTime = System.currentTimeMillis()
        val cooldownTime = 120 * 1000 // 120秒冷却，单位为毫秒
        
        // 检查冷却时间
        val lastUseTime = ducooldowns[playerId] ?: 0
        if (currentTime - lastUseTime < cooldownTime) {
            val remaining = (cooldownTime - (currentTime - lastUseTime)) / 1000
            player.sendMessage("[red]请等待${remaining}秒后再使用骰子命令!".with())
            return@body
        }
        
        // 更新冷却时间
        ducooldowns[playerId] = currentTime
        
        val pn = player.name
        
        // 生成0-100的随机数
        val randomIntInRange = Random.nextInt(101)
        
        // 判定运气等级
        val luckMessage = when {
            randomIntInRange < 5 -> "[red]运气几乎没了了 你..."
            randomIntInRange < 10 -> "[red]运气糟透了 你是*多了吗？!"
            randomIntInRange < 30 -> "[red]运气不佳! [pink]哎,你这小南娘"
            randomIntInRange < 60 -> "[yellow]运气还可以! [pink]杂鱼~杂鱼~"
            randomIntInRange < 80 -> "[yellow]运气较好!"
            randomIntInRange < 95 -> "[green]运气超好!"
            else -> "[gold]欧皇转世! 小心你的阳寿!"
        }
        val de = when {
            randomIntInRange < 5 -> 0
            randomIntInRange < 10 -> 1
            randomIntInRange < 30 -> 2
            randomIntInRange < 60 -> 3
            randomIntInRange < 80 -> 4
            randomIntInRange < 95 -> 5
            else -> 6
        }
        val dm = when {
            de == 0 -> "单位死亡"
            de == 1 -> "单位濒死"
            de == 2 -> "单位扣一半血"
            de == 3 -> "单位获得自身血量200%的护盾"
            de == 4 -> "单位获得自身血量500%的护盾,添加全正面buff"
            de == 5 -> "单位获得自身血量1000%的护盾,添加全正面buff"
            else -> "单位获得自身血量2000%的护盾,添加全正面buff"
        }
        
        
        
        // 发送第一条消息
        broadcast("${pn}[yellow]丢出了一个单位运气骰子".with())
        
        // 使用Timer延迟1秒发送结果
        Timer.schedule({
            broadcast("[yellow]骰子结果是：${randomIntInRange} !  ${luckMessage}".with())
            broadcast("${dm}".with())
           
            
            val h = player.unit().health()
        val s = player.unit().shield()
        
        if(h == null)
        returnReply("[red]你没有附身到单位中".with())
        
        if (de == 0){

        player!!.unit().kill()
        }
        if (de == 1){
        val dh = h * 0.2f
        player!!.unit().health = dh
        }
        if (de == 2){
        val dh = h * 0.5f
        player!!.unit().health = dh
        }
        if (de == 3){
        val ds = s + h * 2f
        player!!.unit().shield = ds
        }
        if (de == 4){
        val ds = s + h * 5f
        player!!.unit().shield = ds
        val unit = player!!.unit()
    val buff2 = StatusEffects.shielded
    val buff3 = StatusEffects.boss
    val buff4 = StatusEffects.fast
    val buff5 = StatusEffects.overclock
    val buff6 = StatusEffects.overdrive
    val duration = 360000000f * 300000f 

    
    unit.apply(buff2, duration)
    unit.apply(buff3, duration)
    unit.apply(buff4, duration)
    unit.apply(buff5, duration)
    unit.apply(buff6, duration)
        }
        if (de == 5){
        val ds = s + h * 10f
        player!!.unit().shield = ds
        val unit = player!!.unit()
    val buff2 = StatusEffects.shielded
    val buff3 = StatusEffects.boss
    val buff4 = StatusEffects.fast
    val buff5 = StatusEffects.overclock
    val buff6 = StatusEffects.overdrive
    val duration = 360000000f * 300000f 

    
    unit.apply(buff2, duration)
    unit.apply(buff3, duration)
    unit.apply(buff4, duration)
    unit.apply(buff5, duration)
    unit.apply(buff6, duration)
        }
        if (de == 6){
        val ds = s + h * 20f
        player!!.unit().shield = ds
    val unit = player!!.unit()
    val buff2 = StatusEffects.shielded
    val buff3 = StatusEffects.boss
    val buff4 = StatusEffects.fast
    val buff5 = StatusEffects.overclock
    val buff6 = StatusEffects.overdrive
    val duration = 360000000f * 300000f 

    
    unit.apply(buff2, duration)
    unit.apply(buff3, duration)
    unit.apply(buff4, duration)
    unit.apply(buff5, duration)
    unit.apply(buff6, duration)
        }
        }, 1f)
    }
}
