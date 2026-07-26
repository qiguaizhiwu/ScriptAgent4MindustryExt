@file:Depends("coreLibrary/db/h2db", "数据库存储")
@file:Depends("wayzer/maps", "地图管理")

package wayzer.maps.ban

import coreLibrary.*
import coreLibrary.lib.*
import coreMindustry.lib.*
import kotlinx.coroutines.*
import mindustry.Vars
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.javatime.CurrentTimestamp
import org.jetbrains.exposed.sql.javatime.timestamp
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.SchemaUtils
import wayzer.MapManager
import wayzer.MapRegistry
import wayzer.MapChangeEvent
import coreLib.db.DBApi

// 数据库表定义
object BannedMaps : IntIdTable("BannedMaps") {
    val mapId = integer("map_id").uniqueIndex()
    val mapName = text("map_name")
    val bannedAt = timestamp("banned_at").defaultExpression(CurrentTimestamp)
}
DBApi.registerTable(BannedMaps)

// 地图ban服务
class MapBanService {
    private val bannedMapIds = mutableSetOf<Int>()

    /** 初始化：自动创建数据表，再加载数据 */
    suspend fun loadBannedMaps() {
        transaction {
            // 自动创建不存在的表与缺失列
            SchemaUtils.createMissingTablesAndColumns(BannedMaps)
            BannedMaps.selectAll().forEach {
                bannedMapIds.add(it[BannedMaps.mapId])
            }
        }
        logger.info("Loaded ${bannedMapIds.size} banned maps")
    }

    fun isBanned(mapId: Int): Boolean = mapId in bannedMapIds

    suspend fun banMap(mapId: Int, mapName: String): Boolean {
        if (isBanned(mapId)) return false

        transaction {
            SchemaUtils.createMissingTablesAndColumns(BannedMaps)
            BannedMaps.insert {
                it[this.mapId] = mapId
                it[this.mapName] = mapName
            }
        }
        bannedMapIds.add(mapId)

        // 检查并换掉被ban的当前地图
        checkAndReplaceBannedMap()
        return true
    }

    suspend fun unbanMap(mapId: Int): Boolean {
        if (!isBanned(mapId)) return false

        transaction {
            SchemaUtils.createMissingTablesAndColumns(BannedMaps)
            BannedMaps.deleteWhere { BannedMaps.mapId eq mapId }
        }
        bannedMapIds.remove(mapId)
        return true
    }

    // 新增：检查并替换被ban的地图
    suspend fun checkAndReplaceBannedMap() {
        try {
            // 确保在游戏线程获取当前地图ID
            withContext(Dispatchers.game) {
                val currentMapId = MapManager.current.id
                if (isBanned(currentMapId)) {
                    broadcast("[red]当前地图已被ban，正在更换地图...".with())
                    MapManager.loadMap()
                }
            }
        } catch (e: Exception) {
            logger.warning("检查被ban地图时出错: ${e.message}")
        }
    }
}

// 创建服务实例
val mapBanService = MapBanService()

// 在数据库连接准备好后加载被ban地图列表
onEnable {
    launch {
        mapBanService.loadBannedMaps()

        // 使用循环和延迟实现周期性检查
        while (true) {
            try {
                mapBanService.checkAndReplaceBannedMap()
            } catch (e: Exception) {
                logger.warning("周期性检查被ban地图时出错: ${e.message}")
            }
            delay(5000) // 每5秒检查一次
        }
    }
}

// 监听地图加载事件 - 确保正确获取地图ID
listen<MapChangeEvent> { event ->
    val mapId = event.info.id
    if (mapBanService.isBanned(mapId)) {
        event.cancelled = true
        broadcast("[red]地图 {map.name} 已被ban，正在更换...".with("map" to event.info))

        // 确保在游戏线程执行换图操作
        launch(Dispatchers.game) {
            delay(100)
            MapManager.loadMap()
        }
    }
}

// 整合的ban命令 - 支持ban当前地图或指定地图
command("banmap-banMap", "ban地图") {
    aliases = listOf("banmap")
    usage = "[地图ID]"
    requirePermission("wayzer.maps.ban")
    body {
        val mapId: Int
        val mapName: String

        if (arg.isEmpty()) {
            // 无参数：ban当前地图
            if (Vars.state.map == null) {
                reply("[red]当前没有加载地图".with())
                return@body
            }
            mapId = MapManager.current.id
            mapName = Vars.state.map.name()
        } else {
            // 有参数：ban指定地图
            mapId = arg[0].toIntOrNull() ?: returnReply("[red]无效的地图ID".with())
            val map = MapRegistry.findById(mapId, reply) ?: returnReply("[red]找不到地图ID: {id}".with("id" to mapId))
            mapName = map.name
        }

        if (!mapBanService.banMap(mapId, mapName)) {
            reply("[yellow]地图 [accent]{mapName}[] 已被ban过".with("mapName" to mapName))
            return@body
        }

        broadcast("[red]地图 [yellow]{mapName}[] 已被ban".with("mapName" to mapName))
    }
}

command("banmap-unbanMap", "解禁地图") {
    aliases = listOf("unbanmap")
    usage = "<地图ID>"
    requirePermission("wayzer.maps.ban")
    body {
        if (arg.isEmpty()) returnReply("[red]请输入地图ID".with())

        val mapId = arg[0].toIntOrNull() ?: returnReply("[red]无效的地图ID".with())
        if (!mapBanService.unbanMap(mapId)) {
            reply("[yellow]该地图未被ban".with())
            return@body
        }

        broadcast("[green]地图ID: [yellow]{id}[] 已解禁".with("id" to mapId))
    }
}

command("banmap-banedMaps", "查看被ban的地图") {
    aliases = listOf("banedmaps")
    requirePermission("wayzer.maps.ban")
    body {
        val bannedMaps = transaction {
            SchemaUtils.createMissingTablesAndColumns(BannedMaps)
            BannedMaps.selectAll().map {
                it[BannedMaps.mapId] to it[BannedMaps.mapName]
            }
        }

        if (bannedMaps.isEmpty()) {
            reply("[green]没有被ban的地图".with())
            return@body
        }

        val message = StringBuilder()
        message.append("[yellow]被ban的地图列表:[]\n")
        bannedMaps.forEach { (id, name) ->
            message.append("[lightgray]- ID: $id, 名称: $name\n")
        }
        reply(message.toString().with())
    }
}

// 注册权限
PermissionApi.registerDefault("wayzer.maps.ban", group = "@admin")
