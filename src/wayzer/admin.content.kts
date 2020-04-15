package wayzer

import arc.util.Time
import mindustry.entities.type.Player
import mindustry.game.EventType
import mindustry.net.Packets
import java.util.*

name = "基础: 管理员与禁封"

var admins by config.key<Set<String>>(hashSetOf<String>(), "管理员列表(UUID)")
val pluginLog by config.key(dataDirectory.child("logs").child("secureLog.log").file()!!, "安全日记文件")

@Suppress("PropertyName")
val Admin = Admin()

inner class Admin : SharedData.IAdmin {
    override fun isAdmin(player: Player): Boolean {
        return admins.contains(player.uuid)
    }

    override fun ban(player: Player, uuid: String) {
        if (!isAdmin(player)) return
        val target = playerGroup.find { it.uuid == uuid } ?: return
        netServer.admins.banPlayerID(uuid)
        broadcast("[red] 管理员禁封了{target.name}".i18n("target" to target))
        secureLog("Ban", "${target.name} Ban ${target.name}(${target.uuid})")
    }

    override fun secureLog(tag: String, text: String) {
        ContentHelper.logToConsole("[red][$tag][yellow]$text")
        pluginLog.appendText("[$tag][${Date()}] $text\n")
    }
}
SharedData.admin = Admin

listen<EventType.PlayerBanEvent> {
    it.player?.info?.lastKicked = Time.millis()
    it.player?.con?.kick(Packets.KickReason.banned)
}

command("list", "列出当前玩家") { _, p ->
    val list = playerGroup.all().map {
        "{player.name}[white]([red]{player.shortID}[white]) ".i18n("player" to it)
    }
    p.sendMessage("{list}".i18n("list" to list))
}
command("ban", "管理指令: 列出已ban用户，ban或解ban", "[3位id]", CommandType.Client) { arg, p ->
    if (!Admin.isAdmin(p!!))
        return@command p.sendMessage("[red]你没有权限使用该命令".i18n())
    val uuid = arg.getOrNull(0)
    if (uuid == null) {//list
        val sorted = netServer.admins.banned.sortedByDescending { it.lastKicked }
        val list = sorted.map {
            "[white]{info.name}[white]([red]{info.shortID}[] [white]{info.lastBan:MM/dd}[]),"
                    .i18n("info" to it)
        }
        p.sendMessage("Bans: {list}".i18n("list" to list))
    } else {
        netServer.admins.banned.find { it.id.startsWith(uuid) }?.let {
            netServer.admins.unbanPlayerID(it.id)
            Admin.secureLog("UnBan", "${p.name} unBan ${it.lastName}(${it.id})")
            return@command p.sendMessage("[green]解Ban成功 {info.name}".i18n("info" to it))
        }
        playerGroup.find { it.uuid.startsWith(uuid) }?.let {
            Admin.ban(p, it.uuid)
            return@command p.sendMessage("[green]Ban成功 {player.name}".i18n("player" to it))
        }
        p.sendMessage("[red]找不到改用户,请确定三位字母id输入正确! /list 或 /ban 查看".i18n())
    }
}
command("madmin", "列出或添加删除管理", "[uuid]", CommandType.Server) { arg, p ->
    val uuid = arg.getOrNull(0)
    if (uuid == null) {
        val list = admins.map {
            "{info.name}({info.uuid},{info.lastJoin:MM/dd hh:mm}),".i18n("info" to netServer.admins.getInfo(it))
        }
        p.sendMessage("Admins: {list}".i18n("list" to list))
    } else {
        if (admins.contains(uuid)) {
            admins = admins - uuid
            return@command p.sendMessage("[red]$uuid [green] has been removed from Admins[]")
        }
        val info = netServer.admins.getInfoOptional(uuid)
                ?: return@command p.sendMessage("[red]Can't found player")
        admins = admins + uuid
        p.sendMessage("[red] ${info.lastName}($uuid) [green] has been added to Admins")
    }
}