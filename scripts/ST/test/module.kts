@file:Depends("coreMindustry")
@file:Import("com.google.guava:guava:30.1-jre", mavenDepends = true)
@file:Import("Dim.lib.*", defaultImport = true)
@file:Depends("coreLibrary")
@file:Import("arc.Core", libraryByClass = true)
@file:Import("mindustry.Vars", libraryByClass = true)
@file:Import("arc.Core", defaultImport = true)
@file:Import("mindustry.Vars.*", defaultImport = true)
@file:Import("mindustry.content.*", defaultImport = true)
@file:Import("mindustry.gen.Player", defaultImport = true)
@file:Import("mindustry.gen.Call", defaultImport = true)
@file:Import("mindustry.gen.Groups", defaultImport = true)
@file:Import("mindustry.game.EventType", defaultImport = true)

package ST.test

import mindustry.net.Packets
import mindustry.net.Packets.ConnectPacket

name = "ST test Mindustry Plugin"

Listener//ensure init
onEnable {
    RootCommands.hookGameHandler()
}
