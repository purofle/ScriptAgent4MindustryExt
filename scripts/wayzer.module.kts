@file:DependsModule("coreMindustry")

import cf.wayzer.placehold.DynamicVar
import cf.wayzer.placehold.TemplateHandler
import cf.wayzer.placehold.TemplateHandlerKey
import mindustry.entities.type.Player
import org.jetbrains.exposed.sql.transactions.transaction
import wayzer.lib.LangApi
import wayzer.lib.dao.Achievement
import wayzer.lib.dao.CacheEntity
import wayzer.lib.dao.PlayerData
import wayzer.lib.dao.PlayerProfile

name = "WayZer Mindustry Plugin"
/**
 * 移植自 https://github.com/way-zer/MyMindustryPlugin
 * 功能:
 * (maps) better Maps,GameOver,ChangeMap | 更好的地图管理系统
 * (admin) independent Admin System | 独立的管理员系统
 * (playerInfo) extend variables for PlayerInfo | 扩展info相关变量
 * (user/profileBind) user token generate,check and user bind | 账号令牌生成,检测及用户绑定
 * (user/level) user exp/level system | 用户经验等级系统
 * (user/achievement) user achievement system | 用户成就系统
 * (user/infoCommand) get profile info /info /mInfo(server) | /info指令(查看个人信息)
 * (ext/vote) Vote System includes: changeMap gameOver skipWave kick rollBack | 投票系统(换图,投降,跳波,踢人,回滚)
 * (ext/autoHost) autoHost after startup | 启动后自动开服
 * (ext/autoSave) autoSave every 10 minutes | 自动保存(10分钟)
 * (ext/welcomeMsg) join Welcome | 进服欢迎信息
 * (ext/alert) alert per interval | 定时轮播公告
 * (ext/betterTeam) | 更好的PVP队伍管理，管理员强制换队以及观察者支持
 * (ext/mapInfo) | 在游戏内显示地图信息
 * (ext/pvpProtect) pvp protect time | 开局pvp保护功能
 * (ext/serverStatus) /status | 获取服务器状态
 * (ext/mapSnap) /mapSnap | 保存地图快照到data/mapSnap下
 * TODO: (ext/lang)
 * (ext/reGrief/history) get tile action history | 获取某个的操作记录
 * (ext/reGrief/unitLimit) limit units pre team | 限制每个队伍的单位总数
 * TODO: (ext/reGrief/reactor)
 * TODO: (ext/special/builderRobot)
 */

addDefaultImport("wayzer.lib.*")
addDefaultImport("wayzer.lib.dao.*")
generateHelper()
registerTable(PlayerProfile.T, PlayerData.T,Achievement.T)

registerVar(TemplateHandlerKey,"多语言处理", TemplateHandler{
    getVar("player.lang")?.let {lang->
        LangApi.getLang(lang.toString()).trans(it)
    }?: it
})

registerVarForType<Player>().apply {
    registerChild("lang","玩家选定语言(占位)", DynamicVar { _, _ ->
        LangApi.DEFAULT
    })
}

onEnable{
    @OptIn(CacheEntity.NeedTransaction::class)
    transaction {
        playerGroup.forEach {
            PlayerData.findOrCreate(it)
        }
    }
}

onDisable{
    @OptIn(CacheEntity.NeedTransaction::class)
    transaction {
        PlayerData.allCached.forEach{it.save()}
    }
}