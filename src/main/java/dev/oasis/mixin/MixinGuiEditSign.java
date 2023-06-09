package dev.oasis.mixin;

import java.time.Instant;
import java.time.Duration;
import org.lwjgl.input.Keyboard;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.Executors;
import dev.oasis.modules.SignatureSign;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import net.minecraft.client.gui.GuiScreen;
import org.spongepowered.asm.mixin.Mutable;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.tileentity.TileEntitySign;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import java.util.concurrent.ScheduledExecutorService;
import net.minecraft.client.gui.inventory.GuiEditSign;
import net.minecraft.client.network.NetHandlerPlayClient;
import net.minecraft.network.play.client.CPacketUpdateSign;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;


/**
 * @author Tas [@0xTas] <root@0xTas.dev>
 */
@Mixin(GuiEditSign.class)
public abstract class MixinGuiEditSign extends GuiScreen {
    private Instant timeSince;

    @Mutable @Final @Shadow
    private final TileEntitySign tileSign;

    @Shadow
    public abstract void updateScreen();

    public MixinGuiEditSign(TileEntitySign tileSign) {
        this.tileSign = tileSign;
        this.timeSince = Instant.now();
    }

    @Inject(method = "initGui", at = @At("HEAD"))
    public void mixinInitGui(CallbackInfo ci) {
        // See SignatureSign.kt
        if (SignatureSign.INSTANCE.isEnabled()) {
            this.timeSince = Instant.now();
            ITextComponent[] signature = SignatureSign.INSTANCE.getSignature();

            System.arraycopy(signature, 0, this.tileSign.signText, 0, 4);
            this.updateScreen();
        }
    }

    @Inject(method = "onGuiClosed", at = @At("HEAD"), cancellable = true)
    public void mixinOnGuiClosed(CallbackInfo ci) {
        if (SignatureSign.INSTANCE.isDisabled()) return;

        Instant now = Instant.now();
        long elapsed = Duration.between(timeSince, now).toMillis();
        if (elapsed < 2000) {
            ci.cancel();
            Keyboard.enableRepeatEvents(false);
            long delay = 2000 - elapsed;
            NetHandlerPlayClient nethandlerplayclient = this.mc.getConnection();
            if (nethandlerplayclient != null) {
                ScheduledExecutorService send = Executors.newSingleThreadScheduledExecutor();
                send.schedule(() -> nethandlerplayclient.sendPacket(new CPacketUpdateSign(this.tileSign.getPos(), this.tileSign.signText)), delay, TimeUnit.MILLISECONDS);
            }
            this.tileSign.setEditable(true);
        }
        if (SignatureSign.INSTANCE.needsDisabling()) SignatureSign.INSTANCE.disable();
    }
}
