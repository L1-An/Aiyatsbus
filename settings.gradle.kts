rootProject.name = "Aiyatsbus"

include("plugin")
include("project:common")
// 实现
include("project:common-impl")
// 1.20.4 自定义附魔注册器
include("project:common-impl-v12004-paper")
include("project:common-impl-v12004-vanilla")
// nms 实现
include("project:common-impl-nms")
// 运行平台
include("project:module-bukkit")
// 自定义附魔触发器
include("project:module-custom-expansion")
include("project:module-custom-kotlin-script")
include("project:module-custom-splendid")
// 玩家交互代码
include("project:module-ingame")