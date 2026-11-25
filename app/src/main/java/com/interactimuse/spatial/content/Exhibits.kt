package com.interactimuse.spatial.content

internal data class Exhibit(
    val name: String,
    val category: String,
    val metadata: String,
    val description: String,
    val detail: String,
    val model: String,
)

internal val exhibits = listOf(
    Exhibit(
        "后母戊鼎", "青铜器", "商代晚期 · 青铜",
        "方形鼎腹、厚立耳与四柱足构成稳重的轮廓。这件礼器提供了观察商代青铜铸造与礼仪文化的线索。",
        "从鼎耳看到鼎身，留意凸起的扉棱与对称分布的兽面纹。",
        "models/ding.glb",
    ),
    Exhibit(
        "蒙娜丽莎", "绘画", "约 1503 年起 · 木板油画",
        "达·芬奇笔下的女子望向观众，背后的道路和水域延伸至远山。",
        "留意嘴角与脸颊的明暗过渡，观察柔和的边缘如何让表情显得微妙。",
        "models/framed-painting.glb",
    ),
    Exhibit(
        "青釉瓶（示意）", "陶瓷", "Tripo 生成示意 · 青釉",
        "这件青釉瓶是根据文字生成的器形示意。细颈与鼓腹形成对比，可以观察瓶口、瓶肩与圈足的比例。",
        "从瓶口沿轮廓看到圈足，留意瓶颈与瓶肩的转折。",
        "models/celadon-vase.glb",
    ),
    Exhibit(
        "玉璧（示意）", "玉器", "器形示意 · 玉石",
        "玉璧的基本器形是中央有孔的圆形薄片。这件示意模型以同心凸纹呈现正面的层次。",
        "观察孔缘和外缘的厚度，比较贯通的中心孔与表面的凸纹。",
        "models/jade-bi.glb",
    ),
)
