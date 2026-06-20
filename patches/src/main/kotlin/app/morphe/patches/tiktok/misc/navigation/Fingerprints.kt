package app.morphe.patches.tiktok.misc.navigation

import app.morphe.patcher.Fingerprint

internal object HomeTabAbilityListFingerprint : Fingerprint(
    definingClass = "/TabAbilityAssem;",
    name = "d22",
    returnType = "Ljava/util/List;",
    parameters = listOf("Z"),
)

internal object BottomTabBuildListFingerprint : Fingerprint(
    definingClass = "/10oS;",
    name = "LJJL",
    returnType = "V",
    parameters = listOf("Ljava/util/List;"),
)
