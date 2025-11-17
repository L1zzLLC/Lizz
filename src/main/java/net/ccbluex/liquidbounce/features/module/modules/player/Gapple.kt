package net.ccbluex.liquidbounce.features.module.modules.player

import com.viaversion.viaversion.api.protocol.version.ProtocolVersion
import de.florianmichael.vialoadingbase.ViaLoadingBase
import net.ccbluex.liquidbounce.event.*
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.modules.combat.KillAura
import net.ccbluex.liquidbounce.ui.font.Fonts
import net.ccbluex.liquidbounce.utils.client.MinecraftInstance
import net.ccbluex.liquidbounce.utils.client.PacketUtils.sendPacket
import net.ccbluex.liquidbounce.utils.inventory.InventoryUtils
import net.ccbluex.liquidbounce.utils.extras.StuckUtils
import net.ccbluex.liquidbounce.utils.extras.BlinkUtils
import net.ccbluex.liquidbounce.utils.render.RenderUtils.drawRoundedGradientRectCorner
import net.ccbluex.liquidbounce.utils.render.RenderUtils.drawRoundedRect
import net.ccbluex.liquidbounce.utils.GlowUtils
import net.minecraft.client.gui.ScaledResolution
import net.minecraft.entity.EntityLivingBase
import net.minecraft.init.Items
import net.minecraft.network.play.client.*
import net.minecraft.util.BlockPos
import java.awt.Color

object Gapple : Module("Gapple",Category.PLAYER) {
    private val heal by int("health", 20, 0..40)
    private val sendDelay by int("SendDelay",3,1..10)
    private val sendOnceTicks = 1;
    private val stuck by boolean("Stuck",false)
    private val stopMove by boolean("StopMove",false)

    var noCancelC02 by boolean("NoCancelC02", false)
    var noC02 by boolean("NoC02", false) //这俩玩意本来是备着花雨庭更新grim搞得，结果就是他一直不更新，然后目前lastest Grim也绕不过。

    private val autoGapple by boolean("AutoGapple", false)

    private var slot = -1
    private var c03s = 0
    private var c02s = 0
    private var canStart = false

    var eating: Boolean = false //强制减速了。
    var pulsing: Boolean = false
    var target: EntityLivingBase? = null


    override fun onEnable() {

        c03s = 0

        slot = InventoryUtils.findItem(36, 45, Items.golden_apple)!!

        if (slot != -1) {
            slot = slot - 36
        }
    }


    override fun onDisable() {
        eating = false

        if (canStart) {
            pulsing = false
            eating = false
            BlinkUtils.stopBlink()
        }

        if (stuck) {
            StuckUtils.stopStuck()
        }
    }

    val onTick = handler<PreTickEvent> {
        if(mc.thePlayer.health < heal) {
            if (!eating) {
                target = KillAura.target

                c03s = 0

                slot = InventoryUtils.findItem(36, 45, Items.golden_apple)!!

                if (slot != -1) {
                    slot = slot - 36
                }
            }

            if (MinecraftInstance.mc.thePlayer == null || MinecraftInstance.mc.thePlayer.isDead) {
                BlinkUtils.stopBlink()
                state = false
                return@handler
            }
            if (slot == -1) {
                state = false
                return@handler
            }
            if (eating) {
                if (stuck) {
                    StuckUtils.stuck()
                }
                if (!BlinkUtils.blinking) {
                    BlinkUtils.blink(
                        C09PacketHeldItemChange::class.java,
                        C0EPacketClickWindow::class.java,
                        C0DPacketCloseWindow::class.java
                    )
                    BlinkUtils.setCancelReturnPredicate(C07PacketPlayerDigging::class.java) { it -> (it as C07PacketPlayerDigging).status == C07PacketPlayerDigging.Action.RELEASE_USE_ITEM }
                    BlinkUtils.setCancelReturnPredicate(C08PacketPlayerBlockPlacement::class.java) { it ->
                        ((it as C08PacketPlayerBlockPlacement).position.y == -1)
                    }
                    BlinkUtils.setCancelReturnPredicate(C02PacketUseEntity::class.java) { it -> noCancelC02 }
                    BlinkUtils.setCancelReturnPredicate(C0APacketAnimation::class.java) { it -> noCancelC02 }
                    BlinkUtils.setCancelAction(C03PacketPlayer::class.java) { packet -> c03s++ }
                    BlinkUtils.setReleaseAction(C03PacketPlayer::class.java) { packet -> c03s-- }
                    BlinkUtils.setReleaseReturnPredicateMap(C02PacketUseEntity::class.java) { packet -> !eating && noC02 }
                    BlinkUtils.setCancelAction(C02PacketUseEntity::class.java) { packet -> c02s++ }
                    BlinkUtils.setReleaseAction(C02PacketUseEntity::class.java) { packet -> c02s-- }
                    canStart = true
                }
            } else {
                eating = true
//                chat("开始吃")
            }
            if (c03s >= 32) {
                eating = false
                pulsing = true
                BlinkUtils.resetBlackList()
                sendPacket(C09PacketHeldItemChange(slot), false)
                println("Start!")
                sendPacket(
                    C08PacketPlayerBlockPlacement(
                        MinecraftInstance.mc.thePlayer.inventoryContainer.getSlot(
                            slot + 36
                        ).stack
                    ), false
                )
                if (ViaLoadingBase.getInstance().targetVersion.newerThanOrEqualTo(ProtocolVersion.v1_12_2)) {
                    sendPacket(
                        C08PacketPlayerBlockPlacement(BlockPos(-1, -2, -1), 255, null, 0.0f, 0.0f, 0.0f), false
                    )
                }
                BlinkUtils.stopBlink()
                println("Stop!")
//                chat("吃完了")
                sendPacket(C09PacketHeldItemChange(MinecraftInstance.mc.thePlayer.inventory.currentItem), false)
                pulsing = false
                if (autoGapple) {
                    c03s = 0

                    slot = InventoryUtils.findItem(36, 45, Items.golden_apple) ?: -1;

                    if (slot != -1) {
                        slot = slot - 36
                    }
                } else {
                    state = false
                }
//                if (autoBlock != "Off" && (!blinked || !net.ccbluex.liquidbounce.utils.client.BlinkUtils.isBlinking) && target != null) {
//                    if (autoBlock == "QuickMarco") {
//                        sendOffHandUseItem()
//                    } else if (autoBlock == "Packet") {
//                        sendPacket(C08PacketPlayerBlockPlacement(mc.thePlayer.heldItem))
//                    }
////                    chat("发送防砍包")
//                }
                return@handler
            }

            if ((MinecraftInstance.mc.thePlayer.ticksExisted % sendDelay) == 0) {
                for (i in 0..<sendOnceTicks) {
                    BlinkUtils.releasePacket(true)
                }
            }
        }else{
            eating = false

            if (canStart) {
                pulsing = false
                eating = false
                BlinkUtils.stopBlink()
            }

            if (stuck) {
                StuckUtils.stopStuck()
            }
        }
    }

    val onMovementInput = handler<MovementInputEvent> { event ->
        if(eating && stopMove){
            event.originalInput.moveStrafe = 0F
            event.originalInput.moveForward = 0F
        }
    }
    val onRender2D = handler<Render2DEvent> {
        val scaledScreen = ScaledResolution(mc)
        drawProgressBar(scaledScreen.scaledWidth.toFloat(), scaledScreen.scaledHeight.toFloat())
    }

    private fun drawProgressBar(width: Float, height: Float) {
        val progressLength = 140F
        val startY = height / 4 * 3
        val startX = width / 2 - progressLength / 2

        val progressRatio = (c03s.toFloat() / 32F).coerceIn(0f, 1f)
        val currentProgress = progressLength * progressRatio
        val progressPercent = (progressRatio * 100).toInt()

        // 添加阴影效果
        ShowShadow(startX - 2, startY - 2, progressLength + 4, 11F, 0.3F)

        // 绘制进度条背景
        drawRoundedRect(startX, startY, startX + progressLength, startY + 7F, Color(0, 0, 0, 128).rgb, 2F)

        // 绘制进度条前景
        if (currentProgress != 0f) {
            drawRoundedGradientRectCorner(
                startX, startY,
                startX + currentProgress, startY + 7F,
                3f,
                Color(76, 157, 240, 255).rgb,
                Color(53, 200, 167, 255).rgb
            )
        }

        // 在进度条右侧显示百分比
        val percentText = "$progressPercent%"
        val percentTextWidth = Fonts.fontGoogleSans35.getStringWidth(percentText)
        Fonts.fontGoogleSans35.drawString(
            percentText,
            startX + progressLength + 5,
            startY + 0,
            Color(255, 255, 255, 255).rgb,
            true
        )
    }

    private fun ShowShadow(startX: Float, startY: Float, width: Float, height: Float, shadowStrength: Float) {
        GlowUtils.drawGlow(
            startX, startY,
            width, height,
            (shadowStrength * 13F).toInt(),
            Color(0, 0, 0, 120)
        )
    }
}