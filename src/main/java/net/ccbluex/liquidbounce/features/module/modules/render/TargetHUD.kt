package net.ccbluex.liquidbounce.features.module.modules.render

import net.ccbluex.liquidbounce.event.Render2DEvent
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.modules.combat.KillAura
import net.ccbluex.liquidbounce.features.module.modules.misc.AntiBot
import net.ccbluex.liquidbounce.ui.client.hud.element.elements.Target
import net.ccbluex.liquidbounce.ui.font.Fonts
import net.ccbluex.liquidbounce.utils.render.RenderUtils
import net.ccbluex.liquidbounce.utils.render.RenderUtils.deltaTime
import net.ccbluex.liquidbounce.utils.extras.ColorUtils
import net.minecraft.client.gui.GuiChat
import net.minecraft.client.gui.ScaledResolution
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.client.renderer.RenderHelper
import net.minecraft.entity.EntityLivingBase
import net.minecraft.entity.player.EntityPlayer
import org.lwjgl.opengl.GL11.*
import net.ccbluex.liquidbounce.event.*
import net.ccbluex.liquidbounce.utils.GlowUtils
import net.ccbluex.liquidbounce.utils.render.RenderUtils.drawRoundedRect
import net.ccbluex.liquidbounce.utils.render.RenderUtils.drawScaledCustomSizeModalRect
import net.ccbluex.liquidbounce.utils.render.RenderUtils.withClipping
import java.awt.Color
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.*
import kotlin.math.*
import net.minecraft.util.EnumChatFormatting.BOLD
import net.ccbluex.liquidbounce.utils.render.Stencil
import net.ccbluex.liquidbounce.utils.render.animation.AnimationUtil
import net.minecraft.util.ResourceLocation
import net.minecraft.client.renderer.RenderHelper.disableStandardItemLighting
import net.minecraft.client.renderer.RenderHelper.enableGUIStandardItemLighting
import net.minecraft.entity.Entity


object TargetHUD : Module("TargetHUD", Category.RENDER) {

    // General Settings
    private val hudStyle by choices(
        "Style",
        arrayOf(
            "Lizz",
            "Novoline",
            "Compact",
            "Rise",
            "Southside",
            "Styles",
            "戶籍",
            "Myau",
            "Naven",
            "Exhibition",
            "Opai"  // 添加Opai模式
        ),
        "Novoline"
    )
    private val posX by int("PosX", 0, -400..400)
    private val posY by int("PosY", 0, -400..400)
    private val animSpeed by float("AnimationSpeed", 0.1F, 0.01F..0.5F)

    // Novoline Settings (原Lizz设置)
    private val novolineColorMode by choices("Novoline-Color", arrayOf("Custom", "Health", "Rainbow"), "Health") { hudStyle == "Novoline" }
    private val novolineColorRed by int("Novoline-Red", 0, 0..255) { hudStyle == "Novoline" && novolineColorMode == "Custom" }
    private val novolineColorGreen by int("Novoline-Green", 120, 0..255) { hudStyle == "Novoline" && novolineColorMode == "Custom" }
    private val novolineColorBlue by int("Novoline-Blue", 255, 0..255) { hudStyle == "Novoline" && novolineColorMode == "Custom" }
    private val novolineColorSpec by boolean("Novoline-Gradient",true) {hudStyle == "Novoline"}
    private val novolineLeftColor by color("Novoline-left-Color", Color(0, 255, 150)) { novolineColorSpec }
    private val novolineRightColor by color("Novoline-right-Color", Color(10, 80, 120)) { novolineColorSpec }

    // Lizz模式颜色设置
    private val lizzColorMode by choices("Lizz-Color", arrayOf("Custom", "Health"), "Health") { hudStyle == "Lizz" }
    private val lizzColorRed by int("Lizz-Red", 76, 0..255) { hudStyle == "Lizz" && lizzColorMode == "Custom" }
    private val lizzColorGreen by int("Lizz-Green", 157, 0..255) { hudStyle == "Lizz" && lizzColorMode == "Custom" }
    private val lizzColorBlue by int("Lizz-Blue", 240, 0..255) { hudStyle == "Lizz" && lizzColorMode == "Custom" }

    // Styles Settings (原Novoline设置)
    private val stylesShadow by boolean("Styles-Shadow", true) { hudStyle == "Styles" }

    // Arc Settings
    private val arcRainbow by boolean("Arc-Rainbow", true) { hudStyle == "Arc" }
    private val arcColorRed by int("Arc-Red", 255, 0..255) { hudStyle == "Arc" && !arcRainbow }
    private val arcColorGreen by int("Arc-Green", 255, 0..255) { hudStyle == "Arc" && !arcRainbow }
    private val arcColorBlue by int("Arc-Blue", 255, 0..255) { hudStyle == "Arc" && !arcRainbow }

    // Myau Settings
    private val rainbow by boolean("Myau-Rainbow", true) { hudStyle == "Myau" }
    private val borderRed by int("Myau-Border-Red", 255, 0..255) { hudStyle == "Myau" }
    private val borderGreen by int("Myau-Border-Green", 255, 0..255) { hudStyle == "Myau" }
    private val borderBlue by int("Myau-Border-Blue", 255, 0..255) { hudStyle == "Myau" }
    private val showAvatar by boolean("Myau-Show-Avatar", true) { hudStyle == "Myau" }

    // RavenB4 Settings
    val barColorR by int("RavenB4-BarColorR", 255, 0..255) { hudStyle == "RavenB4" }
    private val barColorG by int("RavenB4-BarColorG", 255, 0..255) { hudStyle == "RavenB4" }
    private val barColorB by int("RavenB4-BarColorB", 255, 0..255) { hudStyle == "RavenB4" }
    private val animSpeedRB4 by int("RavenB4-AnimSpeed", 3, 1..10) { hudStyle == "RavenB4" }

    // Moon4 Settings
    private val riseBarColorR by int("Rise-BarR", 70, 0..255) { hudStyle == "Rise" }
    private val riseBarColorG by int("Rise-BarG", 130, 0..255) { hudStyle == "Rise" }
    private val riseBarColorB by int("Rise-BarB", 255, 0..255) { hudStyle == "Rise" }
    private val riseBGColorR by int("Rise-BGR", 30, 0..255) { hudStyle == "Rise" }
    private val riseBGColorG by int("Rise-BGG", 30, 0..255) { hudStyle == "Rise" }
    private val riseBGColorB by int("Rise-BGB", 30, 0..255) { hudStyle == "Rise" }
    private val riseBGColorA by int("Rise-BGA", 180, 0..255) { hudStyle == "Rise" }
    private val riseAnimSpeed by int("Rise-AnimSpeed", 4, 1..10) { hudStyle == "Rise" }
    private val riseShadowCheck by boolean("Shadow", true) { hudStyle == "Rise" }
    private val riseGradient by boolean("Gradient", true) { hudStyle == "Rise" }
    private val riseBGColor2 by color("Rise-Gradient-Color2", Color(70, 130, 255)) { hudStyle == "Rise" && riseGradient }

    // Opai模式设置
    private val opaiThemeColor by color("Opai-themeColor", Color(242,172,244)) { hudStyle == "Opai" }
    private val opaiBackGroundAlpha by int("Opai-BackGroundAlpha",120,0..255) { hudStyle == "Opai" }
    private val opaiShadowCheck by boolean("Opai-ShadowCheck",false) { hudStyle == "Opai" }
    private val opaiShadowStrengh by float("Opai-shadowStrengh",0.5f,0.0f..1.0f) { hudStyle == "Opai" && opaiShadowCheck }
    private val opaiVanishDelay by int("Opai-VanishDelay", 300, 0..500) { hudStyle == "Opai" }

    // State Variables
    private val decimalFormat = DecimalFormat("0.0", DecimalFormatSymbols(Locale.ENGLISH))
    private var target: EntityLivingBase? = null
    private var lastTarget: EntityLivingBase? = null
    override var hue = 0.0f
    // Wave样式设置项
    private val waveColor by choices("Wave-Color", arrayOf("Health", "Rainbow", "Custom"), "Health") { hudStyle == "Wave" }
    private val waveCustomColor = Color(0, 150, 255)

    // Pulse样式设置项
    private val pulseSpeed by float("Pulse-Speed", 0.05F, 0.01F..0.2F) { hudStyle == "Pulse" }
    private val pulseThickness by float("Pulse-Thickness", 4F, 1F..10F) { hudStyle == "Pulse" }

    // Neon样式设置项
    private val neonGlow by boolean("Neon-Glow", true) { hudStyle == "Neon" }
    private val neonColor = Color(76, 140, 240)
    // Animation States
    private var easingHealth = 0F
    private var moon4EasingHealth = 0F
    private var southsideEasingHealth = 0F
    private var slideIn = 0F
    private var damageHealth = 0F

    // Opai模式变量
    private var opaiDelayCounter = 0
    private var opaiAnimX = 135F

    override fun onEnable() {
        easingHealth = 0F
        moon4EasingHealth = 0F
        southsideEasingHealth = 0f
        slideIn = 0F
        damageHealth = 0f
        hue = 0.0f
        target = null
        lastTarget = null
        opaiDelayCounter = 0
        opaiAnimX = 135F
    }

    private fun updateSouthsideEasingHealth(targetHealth: Float, maxHealth: Float) {
        val changeAmount = abs(southsideEasingHealth - targetHealth)
        var speed = 0.02f * deltaTime

        if (changeAmount > 5) {
            speed *= 2.0f
        } else if (changeAmount > 2) {
            speed *= 1.5f
        }

        if (abs(southsideEasingHealth - targetHealth) < 0.1f) {
            southsideEasingHealth = targetHealth
        } else if (southsideEasingHealth > targetHealth) {
            southsideEasingHealth -= min(speed * 1.2f, southsideEasingHealth - targetHealth)
        } else {
            southsideEasingHealth += min(speed, targetHealth - southsideEasingHealth)
        }
        southsideEasingHealth = southsideEasingHealth.coerceAtMost(maxHealth)
    }

    private fun renderSouthsideHUD(x: Float, y: Float) {
        val entity = target ?: lastTarget ?: return

        val health = entity.health
        val maxHealth = entity.maxHealth
        val healthPercent = (health / maxHealth).coerceIn(0f, 1f)

        // Update easing health
        updateSouthsideEasingHealth(health, maxHealth)
        val easingHealthPercent = (southsideEasingHealth / maxHealth).coerceIn(0f, 1f)

        val name = entity.name
        val width = Fonts.fontSemibold40.getStringWidth(name) + 75f
        val presentWidth = easingHealthPercent * width

        GlStateManager.pushMatrix()

        // Animation
        val animOutput = slideIn
        GlStateManager.translate((x + width / 2) * (1 - animOutput).toDouble(), (y + 20) * (1 - animOutput).toDouble(), 0.0)
        GlStateManager.scale(animOutput, animOutput, animOutput)

        // Background
        RenderUtils.drawRect(x, y, x + width, y + 40, Color(0, 0, 0, 100).rgb)
        RenderUtils.drawRect(x, y, x + presentWidth, y + 40, Color(230, 230, 230, 100).rgb)

        // Vertical health indicator
        val healthColor = when {
            healthPercent > 0.5 -> Color(63, 157, 4, 150)
            healthPercent > 0.25 -> Color(255, 144, 2, 150)
            else -> Color(168, 1, 1, 150)
        }
        RenderUtils.drawRect(x, y + 12.5f, x + 3, y + 27.5f, healthColor.rgb)

        // Head
        mc.netHandler.getPlayerInfo(entity.uniqueID)?.let {
            Target().drawHead(it.locationSkin, x.toInt() + 7, y.toInt() + 7, 26, 26, Color.WHITE)
        } ?: RenderUtils.drawRect(x + 6, y + 6, x + 34, y + 34, Color.BLACK.rgb)


        // Text
        Fonts.fontSemibold40.drawString(name, x + 40, y + 7, Color(200, 200, 200, 255).rgb)
        Fonts.fontSemibold40.drawString("${health.toInt()} HP", x + 40, y + 22, Color(200, 200, 200, 255).rgb)

        // Held Item
        val itemStack = entity.heldItem
        val itemX = x + Fonts.fontSemibold40.getStringWidth(name) + 50
        if (itemStack != null) {
            GlStateManager.pushMatrix()
            GlStateManager.translate(itemX, y + 12, 0f)
            GlStateManager.scale(1.5f, 1.5f, 1.5f) // Make item bigger
            RenderHelper.enableGUIStandardItemLighting()
            mc.renderItem.renderItemAndEffectIntoGUI(itemStack, 0, 0)
            RenderHelper.disableStandardItemLighting()
            GlStateManager.popMatrix()
        } else {
            Fonts.fontSemibold40.drawString("?", x + Fonts.fontSemibold40.getStringWidth(name) + 55, y + 11, Color(200, 200, 200, 255).rgb)
        }

        GlStateManager.popMatrix()
    }

    val onRender2D = handler<Render2DEvent> { event ->
        val kaTarget = KillAura.target

        // Update target logic
        if (kaTarget != null && kaTarget is EntityPlayer && !AntiBot.isBot(kaTarget)) {
            target = kaTarget
        } else if (mc.currentScreen is GuiChat) {
            target = mc.thePlayer
        } else if (target != null && (KillAura.target == null || !target!!.isEntityAlive || AntiBot.isBot(target!!))) {
            target = null
        }

        // Handle target change for animations
        if (target != lastTarget) {
            if (lastTarget != null) { // Smooth out previous target
                easingHealth = lastTarget!!.health
                damageHealth = lastTarget!!.health
            } else if (target != null) { // Instantly set for new target
                easingHealth = target!!.health
                damageHealth = target!!.health
            }
            // Instantly set health for animated styles to prevent animating from old target
            if (target != null) {
                moon4EasingHealth = target!!.health
                southsideEasingHealth = target!!.health
            }
        }

        lastTarget = target

        // Update global animations
        hue += 0.05f * deltaTime * 0.1f
        if (hue > 1F) hue = 0F

        slideIn = lerp(slideIn, if (target != null) 1F else 0F, animSpeed)

        if (slideIn < 0.01F && target == null) return@handler

        val sr = ScaledResolution(mc)

        // Centralized positioning
        val x = sr.scaledWidth / 2F + posX
        val y = sr.scaledHeight / 2F + posY

        when (hudStyle.lowercase(Locale.getDefault())) {
            // New Styles
            "lizz" -> renderLizzHUD(x, y)
            "novoline" -> renderNovolineHUD(x, y)
            "arc" -> renderArcHUD(x, y)
            "compact" -> renderCompactHUD(x, y)
            "rise" -> renderMoon4HUD(x, y)
            "southside" -> renderSouthsideHUD(x,y)
            "styles" -> renderStylesHUD(sr)
            "戶籍" -> render0x01a4HUD(sr)
            "myau" -> renderMyauHUD(sr)
            "ravenb4" -> renderRavenB4HUD(sr)
            "naven" -> renderNavenHUD(sr)
            "neon" -> renderNeonHUD(x, y)
            "exhibition" -> renderExhibitionHUD(x, y)
            "opai" -> renderOpaiHUD(sr)  // 添加Opai模式
        }
    }

    var AnimX = 100F

    private fun renderMoon4HUD(x: Float, y: Float) {
        val entity = target ?: lastTarget ?: return

        // Animate towards the current target's health, or towards 0 if no target.
        val currentHealth = target?.health ?: 0f
        moon4EasingHealth += ((currentHealth - moon4EasingHealth) / 2.0F.pow(10.0F - riseAnimSpeed)) * deltaTime

        val mainColor = Color(riseBarColorR, riseBarColorG, riseBarColorB)
        val bgColor = Color(riseBGColorR, riseBGColorG, riseBGColorB, riseBGColorA)
        val smallBGColor : Color

        var boldName = "$BOLD${entity.name}"
        val healthInt = entity.health.toInt()
        val percentText = "${healthInt}.0"
        val healthWidth = Fonts.fontGoogleSans35.getStringWidth(percentText)

        if (mc.thePlayer.health < entity.health){
            boldName = "Losing: "+boldName
        }else if(mc.thePlayer.health > entity.health){
            boldName = "Winning: "+boldName
        }else{
            boldName = "Saming: "+boldName
        }

        val nameLength = (Fonts.fontGoogleSans40.getStringWidth(boldName)).coerceAtLeast(
            Fonts.fontSemibold35.getStringWidth(percentText)
        ).toFloat() + 20F

        val healthPercent = (entity.health / entity.maxHealth).coerceIn(0F, 1F)
        val barWidth = healthPercent * (nameLength - 2F)

        GlStateManager.pushMatrix()
        GlStateManager.translate(x, y, 0F)
        GlStateManager.scale(slideIn, slideIn, slideIn)

        // Backgrounds
        if (riseShadowCheck) ShowShadow(-1F,-1F,2F + nameLength + 45F + healthWidth,1F + 36F,1F)
        RenderUtils.drawRoundedRect(-1F, -1F, 2F + nameLength + 45F + healthWidth, 1F + 36F, bgColor.rgb, 10f)

        // Head with Stencil
        mc.netHandler.getPlayerInfo(entity.uniqueID)?.let { playerInfo ->
            Stencil.write(false)
            glDisable(GL_TEXTURE_2D)
            glEnable(GL_BLEND)
            glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA)
            RenderUtils.drawRoundedRect(1f, 0.5f, 1f + 35f, 0.5f + 35f, Color.WHITE.rgb, 7F)
            glDisable(GL_BLEND)
            glEnable(GL_TEXTURE_2D)
            Stencil.erase(true)
            drawRoundedHead(playerInfo.locationSkin, 3, 2, 31, 31, Color.WHITE)
            Stencil.dispose()
        }

        // Text
        Fonts.fontGoogleSans40.drawString(boldName, 2F + 36F, 7F, -1)

        AnimX = AnimationUtil.base(AnimX.toDouble(),barWidth.toDouble(),0.2).toFloat()

        // Health Bar
        RenderUtils.drawRoundedRect(38F, 24F, 38F + nameLength, 28f, Color(0, 0, 0, 100).rgb, 4f)
        if (!riseGradient){
            RenderUtils.drawRoundedRect(38F, 24F, 38F + barWidth, 28f, mainColor.rgb, 4f)
            smallBGColor = mainColor
        }else{
            RenderUtils.drawRoundedGradientRectCorner(38F, 24F, 38F + barWidth, 28f,4f,mainColor.rgb, Color(riseBGColor2.red, riseBGColor2.green, riseBGColor2.blue).rgb)
            smallBGColor = Color(riseBGColor2.red, riseBGColor2.green, riseBGColor2.blue)
        }
        drawRoundedRect(38F, 24F, 38F + AnimX, 28f, Color(smallBGColor.red, smallBGColor.green, smallBGColor.blue,50).rgb, 4f)

        Fonts.fontGoogleSans35.drawString(percentText, 2F + nameLength + 40F, 23F, -1)

        GlStateManager.popMatrix()
    }

    private fun renderExhibitionHUD(x: Float, y: Float) {
        val entity = target ?: lastTarget ?: return

        val width = 120F  // 长120f
        val height = 45F  // 高45f
        val modelSize = 40F  // 模型尺寸40x40

        GlStateManager.pushMatrix()
        GlStateManager.translate(x, y, 0F)
        GlStateManager.scale(slideIn, slideIn, slideIn)

        // 绘制背景 - 颜色(10,10,10), alpha=255
        RenderUtils.drawRect(0F, 0F, width, height, Color(10, 10, 10, 255).rgb)

        // 绘制1f粗的内边框 - 颜色(40,40,40), alpha=255
        // 上边框
        RenderUtils.drawRect(0F, 0F, width, 1F, Color(40, 40, 40, 255).rgb)
        // 下边框
        RenderUtils.drawRect(0F, height - 1F, width, height, Color(40, 40, 40, 255).rgb)
        // 左边框
        RenderUtils.drawRect(0F, 0F, 1F, height, Color(40, 40, 40, 255).rgb)
        // 右边框
        RenderUtils.drawRect(width - 1F, 0F, width, height, Color(40, 40, 40, 255).rgb)

        // 在最左侧渲染40x40的玩家模型
        val modelX = 2.5F  // 稍微偏移，避免紧贴边框
        val modelY = (height - modelSize) / 2  // 垂直居中

        // 渲染玩家实体模型 - 这会自动显示皮肤、盔甲和手持物品
        renderPlayerModel(entity, modelX, modelY, modelSize)

        // 右侧信息区域 - 向左调整位置
        val infoX = modelX + modelSize + 2F  // 模型右侧只留2像素间距，更靠左
        val infoWidth = width - infoX - 5F   // 右侧信息区域宽度

        // 在顶部渲染目标名字，使用Minecraft原版字体
        val name = entity.name
        val nameY = 5F  // 顶部位置

        // 使用Minecraft原版字体渲染名字
        mc.fontRendererObj.drawStringWithShadow(
            name,
            infoX,
            nameY,
            Color.WHITE.rgb
        )

        // 在名字下方渲染HP和距离信息 - 使用更小的字体
        val healthText = "HP: ${decimalFormat.format(entity.health)}"
        val distance = mc.thePlayer.getDistanceToEntity(entity)
        val distanceText = "Dist: ${decimalFormat.format(distance)}"
        val infoText = "$healthText | $distanceText"

        // 计算信息文本的位置 - 在名字下方，使用更小的字体
        val infoY = nameY + mc.fontRendererObj.FONT_HEIGHT + 2F

        // 使用更小的字体渲染信息文本 - 通过缩放实现
        GlStateManager.pushMatrix()
        GlStateManager.translate(infoX, infoY, 0F)
        GlStateManager.scale(0.8F, 0.8F, 0.8F) // 缩小到80%
        mc.fontRendererObj.drawStringWithShadow(
            infoText,
            0F,
            0F,
            Color.WHITE.rgb
        )
        GlStateManager.popMatrix()

        // 在信息文本下方绘制血条
        val barY = infoY + 8F  // 信息文本下方
        val barHeight = 5F
        val barWidth = infoWidth

        // 计算血量百分比
        val healthPercent = entity.health / entity.maxHealth

        // 根据血量百分比确定血条颜色 - 使用更深的绿色
        val barColor = when {
            healthPercent > 2.0/3.0 -> Color(0, 180, 0)  // 更深的绿色 - 大于2/3
            healthPercent > 1.0/3.0 -> Color(255, 255, 0)  // 黄色 - 1/3到2/3之间
            else -> Color(255, 0, 0)  // 红色 - 小于1/3
        }

        // 绘制血条背景
        RenderUtils.drawRect(infoX, barY, infoX + barWidth, barY + barHeight, Color(40, 40, 40).rgb)

        // 绘制血条前景
        val barFillWidth = barWidth * healthPercent
        RenderUtils.drawRect(infoX, barY, infoX + barFillWidth, barY + barHeight, barColor.rgb)

        // 添加分割线 - 每4f一个竖条
        val segmentWidth = 4F
        val dividerWidth = 1F
        var currentX = infoX + segmentWidth

        while (currentX < infoX + barWidth) {
            // 绘制分割线
            RenderUtils.drawRect(
                currentX, barY,
                currentX + dividerWidth, barY + barHeight,
                Color(10, 10, 10).rgb  // 使用背景颜色
            )
            currentX += segmentWidth + dividerWidth
        }

        GlStateManager.popMatrix()
    }

    // 渲染玩家实体模型函数 - 这会自动显示皮肤、盔甲和手持物品
    private fun renderPlayerModel(entity: EntityLivingBase, x: Float, y: Float, size: Float) {
        GlStateManager.pushMatrix()

        // 设置渲染位置和缩放
        GlStateManager.translate(x + size / 2, y + size, 0F)
        GlStateManager.scale(size / 2, size / 2, size / 2) // 适当缩放模型

        // 设置模型旋转角度（让模型面向屏幕）
        GlStateManager.rotate(180F, 0F, 0F, 1F) // 翻转模型（原本是倒着的）
        GlStateManager.rotate(135F, 0F, 1F, 0F) // Y轴旋转，让模型有角度

        // 启用光照和渲染设置
        RenderHelper.enableStandardItemLighting()
        GlStateManager.enableDepth()
        GlStateManager.disableCull()

        // 设置渲染管理器
        val renderManager = mc.renderManager
        renderManager.setRenderShadow(false) // 禁用阴影

        try {
            // 渲染玩家实体 - 这会自动渲染皮肤、盔甲和手持物品
            renderManager.renderEntityWithPosYaw(
                entity,
                0.0, 0.0, 0.0,
                0F,  // Yaw
                1.0F // Partial ticks
                // Don't use bounding box
            )
        } catch (e: Exception) {
            // 如果渲染失败，绘制一个占位方块
            RenderUtils.drawRect(x, y, x + size, y + size, Color.RED.rgb)
        }

        // 恢复渲染状态
        renderManager.setRenderShadow(true)
        GlStateManager.enableCull()
        GlStateManager.disableDepth()
        RenderHelper.disableStandardItemLighting()

        GlStateManager.popMatrix()
    }

    private fun renderLizzHUD(x: Float, y: Float) {
        val entity = target ?: lastTarget ?: return

        // 更新动画血量
        easingHealth = lerp(easingHealth, entity.health, animSpeed)

        val width = 135F
        val height = 35F
        val avatarSize = 30F
        val padding = 1F

        GlStateManager.pushMatrix()
        GlStateManager.translate(x, y, 0F)
        GlStateManager.scale(slideIn, slideIn, slideIn)

        // 添加阴影效果 - 与Rise模式相同原理
        ShowShadow(0F, 0F, width, height, 0.3F)

        // 绘制背景 - 带1f空白边框
        RenderUtils.drawRect(
            padding, padding,
            width - padding, height - padding,
            Color(30, 30, 30, 180).rgb
        )

        // 绘制头像 (左侧)
        val avatarX = padding + 2F
        val avatarY = padding + 2F
        mc.netHandler.getPlayerInfo(entity.uniqueID)?.let {
            Target().drawHead(
                it.locationSkin,
                avatarX.toInt(),
                avatarY.toInt(),
                avatarSize.toInt(),
                avatarSize.toInt(),
                Color.WHITE
            )
        }

        // 计算右侧区域
        val rightAreaX = avatarX + avatarSize + 5F
        val rightAreaWidth = width - rightAreaX - padding - 2F

        // 绘制名字 (与头像顶部对齐)
        val name = entity.name
        Fonts.fontRegular35.drawString(name, rightAreaX, avatarY + 1, Color.WHITE.rgb)

        // 绘制WL指示器 (固定在右上角)
        val playerHealth = mc.thePlayer.health
        val targetHealth = entity.health
        val winOrLose = if (playerHealth > targetHealth) "W" else "L"
        val wlColor = if (winOrLose == "W") Color(0, 255, 0) else Color(255, 0, 0)

        // 固定在右上角，不随名字长度变化
        val wlX = width - 15F  // 距离右侧15像素
        val wlY = avatarY + 1  // 与名字同一行
        Fonts.fontRegular35.drawString(winOrLose, wlX, wlY, wlColor.rgb)

        // 绘制血条 (名字下方，占满剩余宽度)
        val barY = avatarY + 5 + Fonts.fontRegular35.fontHeight + 3F
        val barHeight = 8F
        val barWidth = rightAreaWidth

        // 血条背景
        RenderUtils.drawRect(rightAreaX, barY, rightAreaX + barWidth, barY + barHeight, Color(50, 50, 50).rgb)

        // 计算血量百分比和血条颜色
        val healthPercent = (easingHealth / entity.maxHealth).coerceIn(0F, 1F)
        val barFillWidth = barWidth * healthPercent

        // 根据血量百分比调整血条颜色
        val barColor = when {
            lizzColorMode == "Health" -> {
                when {
                    healthPercent > 2F/3F -> Color(76, 157, 240)      // 大于2/3
                    healthPercent > 1F/3F -> Color(56, 137, 220)      // 1/3到2/3
                    else -> Color(36, 117, 200)                       // 小于1/3
                }
            }
            else -> {
                when {
                    healthPercent > 2F/3F -> Color(lizzColorRed, lizzColorGreen, lizzColorBlue)
                    healthPercent > 1F/3F -> Color(
                        max(lizzColorRed - 20, 0),
                        max(lizzColorGreen - 20, 0),
                        max(lizzColorBlue - 20, 0)
                    )
                    else -> Color(
                        max(lizzColorRed - 40, 0),
                        max(lizzColorGreen - 40, 0),
                        max(lizzColorBlue - 40, 0)
                    )
                }
            }
        }

        // 绘制血条前景
        RenderUtils.drawRect(rightAreaX, barY, rightAreaX + barFillWidth, barY + barHeight, barColor.rgb)

        // 在血条末端显示血量数字
        val healthText = decimalFormat.format(easingHealth)
        val healthTextWidth = Fonts.fontRegular35.getStringWidth(healthText)
        val healthTextX = rightAreaX + barWidth - healthTextWidth - 2F
        val healthTextY = barY + 2 + (barHeight - Fonts.fontRegular35.fontHeight) / 2F

        Fonts.fontRegular35.drawString(healthText, healthTextX, healthTextY, Color.WHITE.rgb)

        GlStateManager.popMatrix()
    }

    private fun renderNovolineHUD(x: Float, y: Float) {  // 原renderFluxHUD
        val entity = target ?: lastTarget ?: return
        val width = 120F
        val height = 46F

        // Update animations
        easingHealth = lerp(easingHealth, entity.health, animSpeed)

        GlStateManager.pushMatrix()
        GlStateManager.translate(x, y, 0F)
        GlStateManager.scale(slideIn, slideIn, slideIn)
        GlStateManager.translate(-x, -y, 0F)

        // Background (1 is the offset)
        RenderUtils.drawRoundedBorderRect(x+1, y+1, x + width - 1, y + height - 1, 1f, Color(40, 40, 40,200).rgb, Color.BLACK.rgb, 0F)

        // Head
        mc.netHandler.getPlayerInfo(entity.uniqueID)?.let {
            Target().drawHead(it.locationSkin, (x + 6).toInt(), (y + 6).toInt(), 34, 34, Color.WHITE)
        }

        // Name
        Fonts.minecraftFont.drawString(entity.name, (x + 46).toInt(), (y + 8).toInt(), Color.WHITE.rgb)

        // Health Bar
        val healthPercent = (easingHealth / entity.maxHealth).coerceIn(0F, 1F)
        val healthBarWidth = (width - 52) * healthPercent
        val barColor = when(novolineColorMode) {
            "Custom" -> Color(novolineColorRed, novolineColorGreen, novolineColorBlue)
            "Rainbow" -> Color.getHSBColor(hue, 0.7f, 0.9f)
            else -> ColorUtils.getHealthColor(easingHealth, entity.maxHealth)
        }

        RenderUtils.drawRect(x + 46, y + 22, x + width - 6, y + 32, Color(45, 45, 45).rgb)
        if (!novolineColorSpec){
            RenderUtils.drawRect(x + 46, y + 22, x + 46 + healthBarWidth, y + 32, barColor.rgb)
        }else{
            RenderUtils.drawGradientRect(x+46,y+22, x+46+healthBarWidth,y+32, novolineLeftColor.rgb, novolineRightColor.rgb,0f)
        }

        // Health Text - 百分比显示
        val healthPercentage = (easingHealth / entity.maxHealth * 100).coerceIn(0F, 100F)
        val healthText = "${decimalFormat.format(healthPercentage)}%"
        Fonts.minecraftFont.drawString(healthText, (x + 66).toInt(), (y + 24).toInt(), Color.WHITE.rgb)

        GlStateManager.popMatrix()
    }

    private fun renderStylesHUD(sr: ScaledResolution) {  // 原renderNovolineHUD，添加阴影
        val entity = target ?: return
        val x = sr.scaledWidth / 2 + this.posX
        val y = sr.scaledHeight / 2 + this.posY
        val width = (38 + Fonts.fontRegular35.getStringWidth(entity.name)).coerceAtLeast(118).toFloat()

        // 添加阴影效果
        if (stylesShadow) {
            ShowShadow(x.toFloat(), y.toFloat(), width + 14f, 44f, 0.3F)
        }

        RenderUtils.drawRect(x.toFloat(), y.toFloat(), x + width + 14f, y + 44f, Color(0, 0, 0, 120).rgb)

        mc.netHandler.getPlayerInfo(entity.uniqueID)?.let {
            Target().drawHead(it.locationSkin, x + 3, y + 3, 30, 30, Color.WHITE)
        }

        Fonts.fontRegular35.drawString(entity.name, x + 34.5f, y + 4f, Color.WHITE.rgb)
        Fonts.fontRegular35.drawString("Health: ${"%.1f".format(entity.health)}", x + 34.5f, y + 14f, Color.WHITE.rgb)
        Fonts.fontRegular35.drawString("Distance: ${"%.1f".format(mc.thePlayer.getDistanceToEntity(entity))}m", x + 34.5f, y + 24f, Color.WHITE.rgb)

        RenderUtils.drawRect(x + 2.5f, y + 35.5f, x + width + 11.5f, y + 37.5f, Color(0, 0, 0, 200).rgb)
        RenderUtils.drawRect(x + 3f, y + 36f, x + 3f + (entity.health / entity.maxHealth) * (width + 8f), y + 37f, Color(0,255,150))

        RenderUtils.drawRect(x + 2.5f, y + 39.5f, x + width + 11.5f, y + 41.5f, Color(0, 0, 0, 200).rgb)
        RenderUtils.drawRect(x + 3f, y + 40f, x + 3f + (entity.totalArmorValue / 20f) * (width + 8f), y + 41f, Color(77, 128, 255).rgb)
    }

    private fun renderArcHUD(x: Float, y: Float) {
        val entity = target ?: lastTarget ?: return
        val size = 50F

        // Update animations
        easingHealth = lerp(easingHealth, entity.health, animSpeed)

        GlStateManager.pushMatrix()
        val scale = slideIn.pow(0.5f)
        GlStateManager.translate(x + size / 2, y + size / 2, 0F)
        GlStateManager.scale(scale, scale, scale)
        GlStateManager.translate(-(x + size / 2), -(y + size / 2), 0F)

        // Draw Head clipped in a circle
        RenderUtils.withClipping(
            { drawCircle(x + size / 2, y + size / 2, size / 2 - 3, Color.WHITE.rgb) },
            { mc.netHandler.getPlayerInfo(entity.uniqueID)?.let {
                Target().drawHead(it.locationSkin, x.toInt() + 3, y.toInt() + 3, (size - 6).toInt(), (size - 6).toInt(), Color.WHITE)
            }}
        )

        // Health Arc
        val healthPercent = (easingHealth / entity.maxHealth).coerceIn(0F, 1F)
        val arcColor = if(arcRainbow) Color.getHSBColor(hue, 0.6f, 1f) else Color(arcColorRed, arcColorGreen, arcColorBlue)

        // Background Arc
        drawCircleArc(x + size / 2, y + size / 2, size / 2 - 1.5F, 3F, 0F, 360F, Color(40, 40, 40, (200 * slideIn).toInt()))
        // Foreground Arc
        if (healthPercent > 0) {
            drawCircleArc(x + size / 2, y + size / 2, size / 2 - 1.5F, 3F, -90F, 360F * healthPercent, arcColor)
        }

        // Text Info
        val textX = x + size + 5
        val nameColor = Color(255, 255, 255, (255 * slideIn).toInt()).rgb
        val healthColor = Color(200, 200, 200, (255 * slideIn).toInt()).rgb
        Fonts.fontSemibold40.drawString(entity.name, textX, y + 8, nameColor)
        Fonts.fontSemibold35.drawString("HP: ${decimalFormat.format(easingHealth)}", textX, y + 24, healthColor)

        GlStateManager.popMatrix()
    }

    private fun renderCompactHUD(x: Float, y: Float) {
        val entity = target ?: lastTarget ?: return
        val width = 120F
        val height = 18F

        // Update animations
        if (target != null) {
            easingHealth = lerp(easingHealth, entity.health, animSpeed * 1.5f)
            if (abs(entity.health - damageHealth) > 0.1f && easingHealth < damageHealth) {
                // Damage flash effect, damageHealth catches up to easingHealth
                damageHealth = lerp(damageHealth, easingHealth, animSpeed * 0.5f)
            } else {
                damageHealth = easingHealth
            }
        } else {
            // When target is lost, both bars fade out
            easingHealth = lerp(easingHealth, 0f, animSpeed)
            damageHealth = lerp(damageHealth, 0f, animSpeed)
        }

        if (target != null && target != lastTarget) {
            damageHealth = entity.maxHealth // Reset damage bar on new target
        }

        GlStateManager.pushMatrix()
        val scale = slideIn.pow(2f)
        GlStateManager.translate(x + width / 2, y + height / 2, 0F)
        GlStateManager.scale(1f, scale, 1f)
        GlStateManager.translate(-(x + width / 2), -(y + height / 2), 0F)

        if (scale < 0.05f) {
            GlStateManager.popMatrix()
            return
        }

        // Background
        RenderUtils.drawRect(x, y, x + width, y + height, Color(100, 100, 100, (180 * scale).toInt()).rgb)

        // Health Bars
        val healthPercent = (easingHealth / entity.maxHealth).coerceIn(0F, 1F)
        val damagePercent = (damageHealth / entity.maxHealth).coerceIn(0F, 1F)
        val barColor = Color(100, 100, 100, (150 * scale).toInt())

        // Damage bar (the trail)
        RenderUtils.drawRect(x + 0, y + 0, x + 0 + (width - 4) * damagePercent, y + height - 0, barColor)
        // Health bar
        RenderUtils.drawRect(x + 0, y + 0, x + 0 + (width - 4) * healthPercent, y + height - 0, barColor)

        // Text on bar
        val text = "${entity.name} - ${decimalFormat.format(easingHealth)} HP"
        Fonts.fontSemibold40.drawCenteredString(text, x + width / 2, y + height / 2 - Fonts.fontSemibold35.fontHeight / 2 + 1, Color.WHITE.rgb, false)

        GlStateManager.popMatrix()
    }

    private fun renderNeonHUD(x: Float, y: Float) {
        val entity = target ?: return
        val width = 161F
        val height = 47F

        easingHealth = lerp(easingHealth, entity.health, animSpeed * 10)

        GlStateManager.pushMatrix()
        GlStateManager.translate(x, y, 0F)
        GlStateManager.scale(slideIn, slideIn, 1F)

        // 添加边缘阴影效果
        ShowShadow(0F, 0F, width, height, 0.3F)

        // 设置背景颜色为(210,210,210) alpha=5
        RenderUtils.drawRoundedRect(0F, 0F, width, height, Color(210, 210, 210, 5).rgb, 8F)

        // 霓虹发光效果
        if(neonGlow) {
            for(i in 1..3) {
                val glowSize = i * 2F
                RenderUtils.drawRoundedRect(-glowSize, -glowSize, width+glowSize, height+glowSize,
                    Color(neonColor.red, neonColor.green, neonColor.blue, 30/i).rgb, 8F + glowSize)
            }
        }

        // 头像绘制
        mc.netHandler.getPlayerInfo(entity.uniqueID)?.let {
            Target().drawHead(it.locationSkin, 8, 8, 33, 33, Color.WHITE)
        }

        // 名称文本和W/L指示器
        val name = entity.name
        Fonts.fontRegular40.drawString(name, 51F, 12F, Color.WHITE.rgb)

        // 添加W/L指示器
        val playerHealth = mc.thePlayer.health
        val targetHealth = entity.health
        val winOrLose = if (playerHealth >= targetHealth) "W" else "L"
        val wlColor = if (winOrLose == "W") Color(0, 255, 0) else Color(255, 0, 0)

        val nameWidth = Fonts.fontRegular40.getStringWidth(name)
        Fonts.fontRegular40.drawString(winOrLose, 51F + nameWidth + 3, 12F, wlColor.rgb)

        // 霓虹健康条
        val healthPercent = easingHealth / entity.maxHealth
        val barHeight = 10F
        val barX = 51F
        val barY = 24F

        // 背景条
        RenderUtils.drawRoundedRect(barX, barY, width - 20F, barY + barHeight, Color(100, 100, 100).rgb, 2F)

        // 前景条（霓虹效果）
        val gradientWidth = (width - 80F) * healthPercent
        RenderUtils.drawRoundedRect(barX, barY, barX + gradientWidth, barY + barHeight,
            neonColor.brighter().rgb, 2F)

        // 健康数值
        val healthText = "${decimalFormat.format(easingHealth)} / ${entity.maxHealth}"
        Fonts.fontRegular40.drawString(healthText, width - Fonts.fontSemibold35.getStringWidth(healthText) - 50F, barY + 0F, Color(255, 255, 255).rgb)

        // 距离显示
        val distance = mc.thePlayer.getDistanceToEntity(entity)
        val distanceText = "${decimalFormat.format(distance)}m"
        Fonts.fontRegular35.drawString(distanceText, barX, barY + barHeight + 4F, Color(200,200, 200, 180).rgb)

        GlStateManager.popMatrix()
    }

    // Opai模式渲染函数
    private fun renderOpaiHUD(sr: ScaledResolution) {
        val killAuraTarget = KillAura.target.takeIf { it is EntityPlayer }
        val shouldRender = KillAura.handleEvents() && killAuraTarget != null || mc.currentScreen is GuiChat
        val target = killAuraTarget ?: if (opaiDelayCounter >= opaiVanishDelay) {
            mc.thePlayer
        } else {
            lastTarget ?: mc.thePlayer
        }

        if (shouldRender) {
            opaiDelayCounter = 0
        } else {
            opaiDelayCounter++
        }

        if (!shouldRender && opaiDelayCounter >= opaiVanishDelay) return

        val x = sr.scaledWidth / 2F + posX
        val y = sr.scaledHeight / 2F + posY

        val targetName = target.name + "  "
        val targetNameWidth = Fonts.fontSemibold35.getStringWidth(targetName)
        val targetHealth = target.health.toInt()
        val targetHealthWidth = Fonts.fontSemibold35.getStringWidth(targetHealth.toString())
        val textsDrawBegin = 3.5f + 30f + 3.5f
        val allTextLen = targetNameWidth + targetHealthWidth
        val resultProgressWidth = max(135f, textsDrawBegin + allTextLen + 8f)
        val publicXY: Pair<Float, Float> = Pair(3.5f * 2 + resultProgressWidth, 3.5f + 30f + 3.5f + 5f + 3.5f)

        GlStateManager.pushMatrix()
        GlStateManager.translate(x, y, 0F)
        GlStateManager.scale(slideIn, slideIn, slideIn)

        // 阴影
        if (opaiShadowCheck) {
            ShowShadow(0f, 0f, publicXY.first, publicXY.second, opaiShadowStrengh)
        }

        // 背景
        RenderUtils.drawRoundedBorderRect(0f, 0f, publicXY.first - 3.5f, publicXY.second, 0.2f,
            Color(0, 0, 0, opaiBackGroundAlpha).rgb, Color(0, 0, 0, opaiBackGroundAlpha).rgb, 5f)

        if (target is EntityLivingBase) {
            // 绘制头像
            drawOpaiHead(target, 3.5f, 3.5f)
        }

        val progressBarLength = resultProgressWidth / target.maxHealth * target.health
        opaiAnimX = AnimationUtil.base(opaiAnimX.toDouble(), progressBarLength.toDouble(), 0.2).toFloat()

        // 血条背景
        RenderUtils.drawRoundedBorderRect(3.5f, 3.5f + 30f + 3.5f, resultProgressWidth, 3.5f + 30f + 3.5f + 5f, 0.3f,
            Color(0, 0, 0, 200).rgb, Color(0, 0, 0, 200).rgb, 5f)

        // 血条动画效果
        RenderUtils.drawRoundedBorderRect(3.5f, 3.5f + 30f + 3.5f, opaiAnimX, 3.5f + 30f + 3.5f + 5f, 0.3f,
            Color(opaiThemeColor.red, opaiThemeColor.green, opaiThemeColor.blue, 150).rgb,
            Color(opaiThemeColor.red, opaiThemeColor.green, opaiThemeColor.blue, 150).rgb, 4F)

        // 血条实际值
        RenderUtils.drawRoundedBorderRect(3.5f, 3.5f + 30f + 3.5f, progressBarLength, 3.5f + 30f + 3.5f + 5f, 0.3f,
            opaiThemeColor.rgb, opaiThemeColor.rgb, 4F)

        // 文本
        Fonts.fontSemibold35.drawString(targetName, textsDrawBegin + 3.5f, 3.5f * 2, Color.WHITE.rgb)
        Fonts.fontSemibold35.drawString(targetHealth.toString(), textsDrawBegin + targetNameWidth + 3.5f, 3.5f * 2 - 1F, opaiThemeColor.rgb)

        // 绘制盔甲
        val armorX = textsDrawBegin
        val armorY = 3.5f + 30f - 18
        drawOpaiArmor(armorX, armorY, target)

        GlStateManager.popMatrix()
    }

    // Opai模式绘制头像
    private fun drawOpaiHead(target: EntityLivingBase, x: Float, y: Float) {
        val texture = mc.renderManager.getEntityRenderObject<Entity>(target)
            ?.getEntityTexture(target) ?: return

        withClipping(main = {
            drawRoundedRect(x, y, x + 30f, y + 30f, 0, 5f)
        }, toClip = {
            RenderUtils.drawHead(
                texture, x.toInt(), y.toInt(),
                8f, 8f, 8, 8, 30, 30, 64f, 64f,
                Color.WHITE
            )
        })
    }

    // Opai模式绘制盔甲
    private fun drawOpaiArmor(x: Float, y: Float, target: EntityLivingBase) {
        if (target !is EntityPlayer) return
        GlStateManager.pushMatrix()
        glEnable(GL_BLEND)
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA)
        enableGUIStandardItemLighting()
        var offsetX = x
        val renderItem = mc.renderItem
        for (index in 3 downTo 0) {
            val stack = target.inventory.armorInventory[index] ?: continue
            renderItem.renderItemIntoGUI(stack, offsetX.toInt(), y.toInt())
            renderItem.renderItemOverlays(mc.fontRendererObj, stack, offsetX.toInt(), y.toInt())
            offsetX += 18f
        }
        disableStandardItemLighting()
        glDisable(GL_BLEND)
        GlStateManager.popMatrix()
    }

    // Helper function to draw an arc, can be moved to RenderUtils later
    private fun drawCircleArc(x: Float, y: Float, radius: Float, lineWidth: Float, startAngle: Float, endAngle: Float, color: Color) {
        glPushMatrix()
        glEnable(GL_BLEND)
        glDisable(GL_TEXTURE_2D)
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA)
        glEnable(GL_LINE_SMOOTH)
        glLineWidth(lineWidth)

        glColor4f(color.red / 255F, color.green / 255F, color.blue / 255F, color.alpha / 255F)

        glBegin(GL_LINE_STRIP)
        for (i in (startAngle / 360 * 100).toInt()..(endAngle / 360 * 100).toInt()) {
            val angle = (i / 100.0 * 360.0 * (PI / 180)).toFloat()
            glVertex2f(x + sin(angle) * radius, y + cos(angle) * radius)
        }
        glEnd()

        glDisable(GL_LINE_SMOOTH)
        glEnable(GL_TEXTURE_2D)
        glDisable(GL_BLEND)
        glPopMatrix()
        glColor4f(1f, 1f, 1f, 1f)
    }

    private fun drawCircle(x: Float, y: Float, radius: Float, color: Int) {
        val side = (radius * 2).toInt()
        glEnable(GL_BLEND)
        glDisable(GL_TEXTURE_2D)
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA)
        glEnable(GL_POLYGON_SMOOTH)
        glBegin(GL_TRIANGLE_FAN)
        RenderUtils.glColor(Color(color))
        for (i in 0..side) {
            val angle = i * (Math.PI * 2) / side
            glVertex2d(x + sin(angle) * radius, y + cos(angle) * radius)
        }
        glEnd()
        glDisable(GL_POLYGON_SMOOTH)
        glEnable(GL_TEXTURE_2D)
        glDisable(GL_BLEND)
    }

    override fun onDisable() {
        GlStateManager.color(1f, 1f, 1f, 1f)
        GlStateManager.disableBlend()
    }

    override val tag get() = hudStyle

    private fun lerp(start: Float, end: Float, speed: Float): Float = start + (end - start) * speed * (deltaTime / (1000F / 60F))

    private fun getRainbowColor(): Color = Color.getHSBColor(hue, 1f, 1f)

    // --- Original HUD Render Functions ---

    private fun updateRavenB4Anim(targetHealth: Float) {
        easingHealth += ((targetHealth - easingHealth) / 2.0F.pow(10.0F - animSpeedRB4)) * deltaTime
    }

    private fun renderRavenB4HUD(sr: ScaledResolution) {
        val entity = target ?: return
        val x = sr.scaledWidth / 2F + posX
        val y = sr.scaledHeight / 2F + posY
        val font = Fonts.minecraftFont
        val hp = decimalFormat.format(entity.health)
        val hplength = font.getStringWidth(hp)
        val length = font.getStringWidth(entity.displayName.formattedText)
        val barColor = Color(barColorR, barColorG, barColorB)
        val totalWidth = x + length + hplength + 23F
        val totalHeight = y + 35F

        GlStateManager.pushMatrix()
        updateRavenB4Anim(entity.health)
        RenderUtils.drawRoundedGradientOutlineCorner(x, y, totalWidth, totalHeight, 2F, 8F, barColor.rgb, barColor.rgb)
        RenderUtils.drawRoundedRect(x, y, totalWidth, totalHeight, Color(0, 0, 0, 100).rgb, 4F)
        GlStateManager.enableBlend()
        font.drawStringWithShadow(entity.displayName.formattedText, x + 6F, y + 8F, Color.WHITE.rgb)

        val winOrLose = if (entity.health < mc.thePlayer.health) "W" else "L"
        val wlColor = if (winOrLose == "W") Color(0, 255, 0).rgb else Color(139, 0, 0).rgb
        font.drawStringWithShadow(winOrLose, x + length + hplength + 11.6F, y + 8F, wlColor)

        font.drawStringWithShadow(hp, x + length + 8F, y + 8F, ColorUtils.reAlpha(ColorUtils.getHealthColor(entity.health, entity.maxHealth).rgb, 255F).rgb)

        GlStateManager.disableAlpha()
        GlStateManager.disableBlend()
        RenderUtils.drawRoundedRect(x + 5.0F, y + 29.55F, x + length + hplength + 18F, y + 25F, Color(0, 0, 0, 110).rgb, 2F)
        RenderUtils.drawRoundedGradientRectCorner(
            x + 5F, y + 25F,
            x + 5F + (easingHealth / entity.maxHealth) * (length + hplength + 13F),
            y + 29.5F, 4F, barColor.rgb, barColor.rgb
        )
        GlStateManager.popMatrix()
    }

    private fun renderNavenHUD(sr: ScaledResolution) {
        val entity = target ?: return
        val x = sr.scaledWidth / 2f + posX
        val y = sr.scaledHeight / 2f + posY
        val width = 130f
        val height = 50f

        GlStateManager.pushMatrix()
        GlStateManager.translate(x, y, 0F)
        GlStateManager.scale(slideIn, slideIn, slideIn)

        // 添加阴影效果
        ShowShadow(0F, 0F, width, height, 0.3F)

        // 背景 - alpha改为180
        RenderUtils.drawRoundedRect(0F, 0F, width, height, Color(30, 30, 30, 180).rgb, 5f)

        mc.netHandler.getPlayerInfo(entity.uniqueID)?.locationSkin?.let {
            Target().drawHead(it, 7, 7, 30, 30, Color.WHITE)
        }

        val barX1 = 5f
        val barY1 = height - 10f
        val barX2 = width - 5f
        val barY2 = barY1 + 3f
        RenderUtils.drawRoundedRect(barX1, barY1, barX2, barY2, Color(0, 0, 0, 200).rgb, 2f)

        val healthPercent = entity.health / entity.maxHealth
        val fillX2 = barX1 + (barX2 - barX1) * healthPercent
        RenderUtils.drawRoundedRect(barX1, barY1, fillX2, barY2, Color(160, 42, 42).rgb, 2f)

        Fonts.fontRegular35.drawString(entity.name, 40f, 10f, Color.WHITE.rgb)
        Fonts.fontRegular35.drawString("Health: ${"%.2f".format(entity.health)}", 40f, 22f, Color.WHITE.rgb)
        Fonts.fontRegular35.drawString("Distance: ${"%.2f".format(entity.getDistanceToEntity(mc.thePlayer))}", 40f, 30f, Color.WHITE.rgb)

        GlStateManager.popMatrix()
    }

    private fun renderMyauHUD(sr: ScaledResolution) {
        val entity = target ?: return
        val x = sr.scaledWidth / 2F + posX
        val y = sr.scaledHeight / 2F + posY
        val nameWidth = Fonts.fontRegular35.getStringWidth(entity.name)
        val hudWidth = maxOf(80f, nameWidth + 20f)
        val hudHeight = 25f
        val avatarSize = hudHeight

        if (rainbow) {
            hue += 0.0005f
            if (hue > 1f) hue = 0f
        }
        val borderColor = if (rainbow) getRainbowColor() else Color(borderRed, borderGreen, borderBlue)
        val healthBarColor = if (rainbow) getRainbowColor() else Color(maxOf(borderRed - 50, 0), maxOf(borderGreen - 50, 0), maxOf(borderBlue - 50, 0))

        val totalWidth = if (showAvatar) hudWidth + avatarSize else hudWidth

        RenderUtils.drawRect(x - 1, y - 1, x + totalWidth + 1, y, borderColor.rgb)
        RenderUtils.drawRect(x - 1, y + hudHeight, x + totalWidth + 1, y + hudHeight + 1, borderColor.rgb)
        RenderUtils.drawRect(x - 1, y, x, y + hudHeight, borderColor.rgb)
        RenderUtils.drawRect(x + totalWidth, y, x + totalWidth + 1, y + hudHeight, borderColor.rgb)
        RenderUtils.drawRect(x, y, x + totalWidth, y + hudHeight, Color(0, 0, 0, 100).rgb) // Background

        if (showAvatar) {
            mc.netHandler.getPlayerInfo(entity.uniqueID)?.locationSkin?.let {
                Target().drawHead(it, x.toInt(), y.toInt(), avatarSize.toInt(), avatarSize.toInt(), Color.WHITE)
            }
        }

        val textX = if (showAvatar) x + avatarSize + 3 else x + 3
        Fonts.fontSemibold35.drawString(entity.name, textX, y + 1, Color.WHITE.rgb)
        val healthText = String.format("%.1f", entity.health)
        Fonts.fontSemibold35.drawString(healthText, textX, y + 11, healthBarColor.rgb)
        Fonts.fontSemibold35.drawString("\u2764", textX + Fonts.fontSemibold35.getStringWidth(healthText) + 2, y + 11, healthBarColor.rgb)

        val barY = y + 21
        val barWidth = hudWidth - 5f
        RenderUtils.drawRect(textX, barY, textX + barWidth, barY + 3, Color(64, 64, 64).rgb)
        val targetFill = (entity.health / entity.maxHealth) * barWidth
        easingHealth = lerp(easingHealth, targetFill, 0.1f)
        RenderUtils.drawRect(textX, barY, textX + easingHealth, barY + 3, healthBarColor.rgb)

        val playerHealth = mc.thePlayer.health
        val (winLoss, wlColor) = when {
            playerHealth > entity.health -> "W" to Color(0, 255, 0)
            playerHealth < entity.health -> "L" to Color(255, 0, 0)
            else -> "D" to Color(255, 255, 0)
        }
        Fonts.fontSemibold35.drawString(winLoss, x + totalWidth - Fonts.fontSemibold35.getStringWidth(winLoss) - 1, y + 1, wlColor.rgb)

        val diff = playerHealth - entity.health
        val diffText = if (diff > 0) "+${"%.1f".format(diff)}" else String.format("%.1f", diff)
        val diffColor = when {
            diff > 0 -> Color(0, 255, 0)
            diff < 0 -> Color(255, 0, 0)
            else -> Color(255, 255, 0)
        }
        Fonts.fontSemibold35.drawString(diffText, maxOf(x + totalWidth - Fonts.fontSemibold35.getStringWidth(diffText) - 1, textX), y + 11, diffColor.rgb)
    }

    private fun render0x01a4HUD(sr: ScaledResolution) {
        val entity = target ?: return
        val x = sr.scaledWidth / 2 + this.posX
        val y = sr.scaledHeight / 2 + this.posY

        RenderUtils.drawRect(x + 11F, y - 15F, x + 150F, y + 90F, Color(30, 30, 30, 200).rgb)
        Fonts.fontSemibold35.drawString("PLC 全国人口档案查询系统", x + 15f, y - 5f, Color.WHITE.rgb)
        Fonts.fontSemibold35.drawString("姓名: ${entity.name}", x + 15f, y + 5f, Color.WHITE.rgb)
        Fonts.fontSemibold35.drawString("健康: ${entity.health.toInt()}/${entity.maxHealth.toInt()}", x + 15f, y + 25f, Color.WHITE.rgb)
        Fonts.fontSemibold35.drawString("资产: ${entity.totalArmorValue}", x + 15f, y + 45f, Color.WHITE.rgb)
        Fonts.fontSemibold35.drawString("身份证: ${entity.entityId}", x + 15f, y + 65f, Color.WHITE.rgb)
    }

    private fun drawRoundedHead(skinLocation: ResourceLocation, x: Int, y: Int, width: Int, height: Int, color: Color, radius: Float = 6f) {
        Stencil.write(false)
        glDisable(GL_TEXTURE_2D)
        glEnable(GL_BLEND)
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA)
        RenderUtils.drawRoundedRect(x.toFloat(), y.toFloat(), (x + width).toFloat(), (y + height).toFloat(), color.rgb, radius)
        glDisable(GL_BLEND)
        glEnable(GL_TEXTURE_2D)
        Stencil.erase(true)
        Target().drawHead(skinLocation, x, y, width, height, color)
        Stencil.dispose()
    }

    private fun ShowShadow(startX: Float,startY: Float,width: Float,height:Float,shadowStrengh:Float){
        GlowUtils.drawGlow(
            startX, startY,
            width, height,
            (shadowStrengh * 13F).toInt(),
            Color(0, 0, 0, 120)
        )
    }
}