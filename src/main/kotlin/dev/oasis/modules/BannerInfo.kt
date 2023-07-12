package dev.oasis.modules

import dev.oasis.Oasis
import net.minecraft.block.BlockBanner
import com.lambda.client.module.Category
import net.minecraft.tileentity.TileEntityBanner
import com.lambda.client.plugin.api.PluginModule
import com.lambda.client.util.threads.safeListener
import com.lambda.client.util.text.MessageSendHelper
import net.minecraftforge.event.entity.player.PlayerInteractEvent


/**
 * @author Tas [0xTas] <root@0xTas.dev>
 */
internal object BannerInfo : PluginModule(
    name = "BannerInfo",
    description = "Right click banners to display their pattern and color data",
    category = Category.MISC,
    pluginMain = Oasis
) {
    // Event fires twice-per-click
    private var whyFireTwice = false

    init {
        safeListener<PlayerInteractEvent.RightClickBlock> {
            val pos = it.pos

            if (!whyFireTwice && world.getBlockState(pos).block is BlockBanner) {
                whyFireTwice = true
                val banner = world.getTileEntity(pos) as? TileEntityBanner
                val patterns = banner?.patternList ?: return@safeListener
                val colors = banner.colorList ?: return@safeListener

                val bannerData = StringBuilder()
                if (banner.hasCustomName()) {
                    bannerData.append("§8[${Oasis.rCC()}☯§8] §f${banner.name}\n")
                }
                for (i in 0 until patterns.size) {
                    if (i >= colors.size) break
                    bannerData.append("§8[${Oasis.rCC()}☯§8] §f${colors[i]} ${patterns[i].toString().lowercase()}")

                    if (i != patterns.size - 1) bannerData.append("\n")
                }

                MessageSendHelper.sendChatMessage(bannerData.toString())
            } else if (whyFireTwice) whyFireTwice = false
        }
    }
}