package wayzer.map

import mindustry.entities.type.Player
import mindustry.game.EventType
import mindustry.gen.Call
import mindustry.world.blocks.storage.CoreBlock


fun CoreBlock.CoreBuild.showInfo(p: Player) {
    if (p.con == null) return
    fun draw(dotY: Float, text: String) = Call.label(p.con, text, 60 * 5f/*5分钟*/, x, y + dotY)
    draw(0f, "[purple]====[white]${world.map.name()}[purple]====")
    draw(-10f, "[purple]By: [scarlet]${world.map.author()}")
    world.map.description().apply {
        var lastEnd = -1
        var pos = -20f
        while (lastEnd < length) {
            val newEnd = indexOfAny(charArrayOf(' ', '，', ',', '.', '。'), lastEnd + 30).takeUnless { it == -1 }
                    ?: length
            draw(pos, substring(lastEnd + 1, newEnd))
            lastEnd = newEnd
            pos += -10f
        }
    }
//    draw(-20f, world.map.description())
}

listen<EventType.WorldLoadEvent>{
    Core.app.post{
        playerGroup.forEach {
            it.team().cores().firstOrNull()?.showInfo(it)
        }
    }
}

listen<EventType.PlayerJoin>{e->
    Core.app.post {
        e.player.team().cores().firstOrNull()?.showInfo(e.player)
    }
}