package wayzer

import arc.util.Time
import mindustry.entities.type.Player
import mindustry.game.EventType
import mindustry.net.Packets
import java.util.*

name = "基础: 禁封管理"

val pluginLog by config.key(dataDirectory.child("logs").child("secureLog.log").file()!!, "安全日记文件")

fun secureLog(tag: String, text: String) {
    ContentHelper.logToConsole("[red][$tag][yellow]$text")
    pluginLog.appendText("[$tag][${Date()}] $text\n")
}

fun ban(player: Player, uuid: String) {
    val target = netServer.admins.getInfoOptional(uuid) ?: return
    netServer.admins.banPlayerID(uuid)
    broadcast("[red] 管理员禁封了{target.name}".with("target" to target))
    secureLog("Ban", "${player.name} Ban ${target.lastName}(${uuid})")
}
export(::secureLog, ::ban)

listen<EventType.PlayerBanEvent> {
    it.player?.info?.lastKicked = Time.millis()
    it.player?.con?.kick(Packets.KickReason.banned)
}

command("list", "列出当前玩家", {}) {
    val list = playerGroup.map {
        "{player.name}[white]([red]{player.shortID}[white]) ".with("player" to it)
    }
    reply("{list}".with("list" to list))
}
command("ban", "管理指令: 列出已ban用户，ban或解ban", {
    this.usage = "[3位id]"
    permission = "wayzer.admin.ban"
}) {
    val uuid = arg.getOrNull(0)
    if (uuid == null || uuid.length < 3) {//list
        val sorted = netServer.admins.banned.sortedByDescending { it.lastKicked }
        val list = sorted.map {
            "[white]{info.name}[white]([red]{info.shortID}[] [white]{info.lastBan:MM/dd}[]),"
                    .with("info" to it)
        }
        reply("Bans: {list}".with("list" to list))
    } else {
        netServer.admins.banned.find { it.id.startsWith(uuid) }?.let {
            netServer.admins.unbanPlayerID(it.id)
            secureLog("UnBan", "${player!!.name} unBan ${it.lastName}(${it.id})")
            return@command reply("[green]解Ban成功 {info.name}".with("info" to it))
        }
        (netServer.admins.getInfoOptional(uuid))?.let {
            netServer.admins.banPlayerID(uuid)
            returnReply("[green]Ban成功 {player.name}".with("player" to it))
        }
        if (player != null) playerGroup.find { it.uuid.startsWith(uuid) }?.let {
            ban(player!!, it.uuid)
            returnReply("[green]Ban成功 {player.name}".with("player" to it))
        }
        reply("[red]找不到该用户,请确定三位字母id输入正确! /list 或 /ban 查看".with())
    }
}