package com.interactimuse.spatial.content.leonardo

internal data class Phase(val name: String, val period: String, val city: String, val x: Float, val y: Float)

internal val phases = listOf(
    Phase("佛罗伦萨学徒期", "约 1469–1482", "芬奇／佛罗伦萨", .58f, .59f),
    Phase("米兰第一期", "1482–1499", "米兰", .59f, .21f),
    Phase("漫游年", "1499–1503", "曼托瓦／威尼斯", .87f, .34f),
    Phase("佛罗伦萨第二期", "1503–1506", "佛罗伦萨", .73f, .61f),
    Phase("米兰第二期", "1506–1512", "米兰", .55f, .39f),
    Phase("罗马期", "1513–1516", "罗马", .86f, .83f),
    Phase("法国末期", "1516–1519", "昂布瓦斯", .24f, .22f),
)

internal data class LifeEvent(
    val phase: Int,
    val date: String,
    val title: String,
    val story: String,
    val art: String? = null,
)

internal val lifeEvents = listOf(
    LifeEvent(0, "1452", "出生于芬奇", "达芬奇出生于托斯卡纳的芬奇一带。安基亚诺是传统认定的出生地。"),
    LifeEvent(0, "约 1469", "赴佛罗伦萨", "进入韦罗基奥工坊，在绘画、雕塑和工艺共同发展的环境中学习。"),
    LifeEvent(0, "1472", "登记入画家行会", "达芬奇在佛罗伦萨画家行会登记。《受胎告知》可作为观察早期画法的入口。", "annunciation"),
    LifeEvent(0, "1478–81", "独立委托", "1478 年获得圣贝尔纳多小堂委托，1481 年受托创作《三王来拜》。《吉内薇拉·班琪》约作于 1474–78 年，作为这一阶段的肖像入口。", "ginevra"),
    LifeEvent(1, "约 1482", "赴米兰", "进入斯福尔扎宫廷环境，兼顾艺术、庆典设计、军事工程和城市研究。"),
    LifeEvent(1, "1495–98", "创作《最后的晚餐》", "为米兰圣玛丽亚感恩修道院食堂创作墙面画。所用混合技法有别于传统湿壁画，此节点作叙事展示。"),
    LifeEvent(1, "1483–99", "米兰成熟期", "1483 年开始《岩间圣母》祭坛画委托。两幅存世版本的手势和标识有明显差异。", "rocks"),
    LifeEvent(2, "1499–1503", "离开米兰", "斯福尔扎政权失势后离开米兰，经曼托瓦、威尼斯等地，回到佛罗伦萨。"),
    LifeEvent(3, "约 1503", "《蒙娜丽莎》起笔", "一般定为约 1503 年开始，达芬奇其后长期保留并修改作品。", "mona"),
    LifeEvent(4, "1506–12", "重返米兰", "在法国控制下的米兰工作，与夏尔·当布瓦兹有联系，继续解剖、水利及绘画研究。圣安妮构图跨越多个时期。", "anne"),
    LifeEvent(5, "1513–16", "赴罗马", "受朱利亚诺·德·美第奇庇护，在罗马继续光学、几何等研究。"),
    LifeEvent(6, "1516", "定居法国", "受弗朗索瓦一世邀请，居住于昂布瓦斯附近的克洛·吕斯。《施洗者圣约翰》归入晚期作品，起笔可能早于赴法。", "john"),
    LifeEvent(6, "1519.05.02", "逝世于昂布瓦斯", "达芬奇于法国逝世，手稿及部分作品的后续流传与学生弗朗切斯科·梅尔齐有关。"),
)

internal data class Hotspot(val name: String, val x: Float, val y: Float, val note: String)
internal data class Evidence(
    val id: String,
    val kind: String,
    val name: String,
    val prompt: String,
    val guide: List<Pair<Float, Float>>,
)
internal data class Artwork(
    val id: String,
    val name: String,
    val catalog: String,
    val hotspots: List<Hotspot> = emptyList(),
    val evidence: List<Evidence> = emptyList(),
    val guide: List<Pair<Float, Float>> = emptyList(),
)

private fun guide(vararg points: Pair<Float, Float>) = points.toList()

internal val coreWorks = listOf(
    Artwork("annunciation", "受胎告知", "约 1472–1475 · 乌菲齐美术馆",
        listOf(
            Hotspot("天使衣褶", .23f, .58f, "沿明暗交界往下看。折线组织出弯曲的膝部，亮部与阴影共同提示布的厚薄。"),
            Hotspot("远处山脉", .47f, .36f, "比较前景植物与远山的边界。远景对比减弱，颜色趋冷，形成空气透视。"),
        ),
        listOf(Evidence("drapery-study", "素描", "衣褶习作", "对比衣褶如何落在膝部。图示线只提示观察位置，不能据此认定习作服务于这幅画。", guide(.12f to .78f, .32f to .37f, .56f to .27f, .81f to .74f))),
        guide(.12f to .73f, .21f to .50f, .29f to .54f, .39f to .80f)),
    Artwork("ginevra", "吉内薇拉·班琪", "约 1474–1478 · 美国国家美术馆",
        listOf(
            Hotspot("脸的转向", .50f, .33f, "鼻梁偏向画面左侧，两眼仍可见。试着用轮廓线判断脸的转向，再看肩线如何平衡它。"),
            Hotspot("杜松", .75f, .40f, "深色枝叶把面部轮廓托出来。杜松的意大利语 ginepro 也让人联想到人物姓名。"),
        ),
        listOf(
            Evidence("ginevra-back", "物证", "同一木板的背面", "背面的枝条、花环和铭文提供身份线索。正背面是同一物件，可并置观察两面的视觉语言。", guide(.26f to .69f, .48f to .29f, .74f to .66f)),
            Evidence("hand-study", "素描", "手与手臂习作", "比较手势和肖像底部的构图空间。这一搭配是讨论线索，不能重建裁切前原貌。", guide(.18f to .66f, .44f to .35f, .73f to .52f)),
        ),
        guide(.29f to .72f, .37f to .30f, .52f to .22f, .67f to .37f, .70f to .68f)),
    Artwork("rocks", "岩间圣母", "约 1483–1486 · 卢浮宫",
        listOf(
            Hotspot("天使指向", .73f, .64f, "观察天使手指指向画面左下的人物。切换伦敦版，看这个指示动作如何消失。"),
            Hotspot("洞窟与光", .44f, .20f, "轮廓并非到处同样清楚。对比洞口亮处和近处暗区里岩石的边缘。"),
        ),
        listOf(Evidence("rocks-london", "物证", "《岩间圣母》伦敦版", "两版在指向手势、光环、十字杖等处不同，轮廓叠加用来比较构图，不等于判定制作先后或作者分工。", guide(.22f to .75f, .49f to .32f, .77f to .70f))),
        guide(.24f to .76f, .47f to .32f, .77f to .67f)),
    Artwork("mona", "蒙娜丽莎", "约 1503 年起 · 卢浮宫",
        listOf(
            Hotspot("嘴角的渐隐法", .44f, .309f, "放大嘴角，沿嘴唇与脸颊的暗部观察。轮廓由逐渐过渡的明暗形成，注意别把数字锐化或裂纹当作笔触。"),
            Hotspot("交叠双手", .50f, .75f, "上方的手覆盖下方的手，两组明暗使静止的姿势产生深度。试着沿指节追踪方向。"),
        ),
        listOf(
            Evidence("portrait-study", "素描", "都灵红粉笔男性头像（常被认为是自画像）", "先对齐眼线，再比较鼻根、嘴角和下颌。相似可能来自构图、比例或操作变换，叠加不能证明同一人物，也不能证实作者身份。", guide(.515f to .299f, .724f to .306f, .665f to .381f, .682f to .444f)),
            Evidence("optics", "科学手稿", "巴黎手稿 D · 视觉研究", "把手稿中的光线示意与画中明暗渐变并读。理论与画法的关系是观察问题，来源与用途需分别判断。", guide(.22f to .34f, .54f to .50f, .78f to .33f)),
        ),
        guide(.398f to .233f, .501f to .235f, .436f to .273f, .445f to .310f)),
    Artwork("anne", "圣母子与圣安妮", "约 1501–1519 · 卢浮宫",
        listOf(
            Hotspot("交错的身体", .48f, .41f, "安妮、圣母和圣婴组织出相互借力的形体。沿头部和双臂画出一条观看路线。"),
            Hotspot("羔羊", .73f, .74f, "圣婴抓住羔羊，把视线拉向画面右下。可与纸本习作里伸向圣婴的小约翰比较。"),
        ),
        listOf(Evidence("anne-cartoon", "构图手稿", "伯灵顿大宅素描稿", "构图稿以小约翰替代油画中的羔羊。试着对齐头部，再找肢体与视线的变动。", guide(.26f to .33f, .46f to .27f, .64f to .59f, .80f to .61f))),
        guide(.32f to .28f, .44f to .35f, .67f to .56f, .78f to .75f)),
    Artwork("john", "施洗者圣约翰", "约 1508–1519 · 卢浮宫",
        listOf(
            Hotspot("指向光源之外", .30f, .25f, "抬起的手在明暗之间形成清晰的方向。试着追踪指尖、肩线到微笑的连续关系。"),
            Hotspot("从暗中浮现的面孔", .61f, .37f, "脸和肩的边缘逐步融进背景。比较亮部细节与暗部仍可分辨的轮廓。"),
        ),
        listOf(Evidence("optics", "科学手稿", "巴黎手稿 D · 视觉研究", "从光线传播与视觉研究出发，回到画面检查轮廓渐隐。手稿提供问题，没有给这幅画作直接释义。", guide(.20f to .31f, .48f to .53f, .81f to .32f))),
        guide(.27f to .19f, .43f to .39f, .65f to .34f, .74f to .62f)),
)

internal val allWorks = coreWorks + listOf(
    Artwork("baptism", "基督受洗", "约 1470–1475 · 韦罗基奥工坊合作"),
    Artwork("benois", "贝诺瓦圣母", "约 1478–1480 · 艾尔米塔什博物馆"),
    Artwork("carnation", "持康乃馨的圣母", "约 1470 年代 · 慕尼黑老绘画陈列馆"),
    Artwork("jerome", "圣杰罗姆", "约 1480 年起 · 梵蒂冈博物馆 · 未完成"),
    Artwork("magi", "三王来拜", "1481 年起 · 乌菲齐美术馆 · 未完成"),
    Artwork("ermine", "抱银貂的女子", "约 1489–1491 · 恰尔托雷斯基博物馆"),
    Artwork("litta", "利塔圣母", "约 1490 年 · 艾尔米塔什博物馆 · 归属有讨论"),
    Artwork("musician", "音乐家肖像", "约 1485–1490 · 安布罗西亚纳美术馆"),
    Artwork("supper", "最后的晚餐", "1495–1498 · 米兰圣玛丽亚感恩修道院 · 墙面绘画"),
    Artwork("rocks-london", "岩间圣母 · 伦敦版", "约 1491/92–1508 · 英国国家美术馆"),
    Artwork("scapigliata", "散发女子头像", "约 1500–1510 · 帕尔马国家美术馆"),
    Artwork("ferronniere", "美丽的费隆妮叶", "约 1490–1497 · 卢浮宫"),
    Artwork("salvator", "救世主", "约 1500 年 · 归属有讨论"),
    Artwork("madonna-yarnwinder", "纺锤圣母 · 布克卢版", "约 1501 年起 · 达芬奇与工作室，归属有讨论"),
)
