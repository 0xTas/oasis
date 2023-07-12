package dev.oasis.mixin;

import dev.oasis.Oasis;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import org.spongepowered.asm.mixin.Mutable;
import net.minecraft.client.gui.GuiScreenBook;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import com.lambda.client.util.text.MessageSendHelper;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;


/**
 * @author Tas [@0xTas] <root@0xTas.dev>
 *
 * This module adds a button to GuiScreenBook which, when pressed, deobfuscates text from written books with obfuscation formatting.
 */
@Mixin(GuiScreenBook.class)
public abstract class MixinGuiScreenBook extends GuiScreen {
    private boolean deobfuscate;
    @Shadow private int cachedPage;
    private GuiButton buttonDeobfuscate;
    @Shadow @Final private boolean bookIsUnsigned;
    @Mutable @Final @Shadow private final ItemStack book;

    @Shadow private NBTTagList bookPages;

    @Shadow private int currPage;

    public MixinGuiScreenBook(ItemStack book) {
        this.book = book;
        this.deobfuscate = false;
    }


    @Inject(method = "initGui", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiScreenBook;updateButtons()V"))
    public void mixinInitGui(CallbackInfo ci) {
        boolean containsObfuscation = this.book.getTagCompound() != null && this.book.getTagCompound().toString().contains("§k");
        if (!this.bookIsUnsigned && containsObfuscation) {
            this.buttonDeobfuscate = this.addButton(new GuiButton(6, this.width / 2 + 2, 217, 98, 20, "Deobfuscate"));
        }
    }

    @Inject(method = "updateButtons", at = @At("TAIL"))
    public void mixinUpdateButtons(CallbackInfo ci) {
        boolean bookContainsObfuscation = this.book.getTagCompound() != null && this.book.getTagCompound().toString().contains("§k");
        boolean pageContainsObfuscation = this.book.getTagCompound() != null && this.bookPages.getStringTagAt(this.currPage).contains("§k");
        if (!this.bookIsUnsigned && bookContainsObfuscation) {
            this.buttonDeobfuscate.visible = pageContainsObfuscation;
        }
    }

    @Inject(method = "actionPerformed", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiScreenBook;updateButtons()V"))
    protected void mixinActionPerformed(GuiButton button, CallbackInfo ci) {
        if (button.id == 6) {
            NBTTagCompound meta = this.book.getTagCompound();
            if (meta != null) {
                String bookTitle = meta.getString("title");
                if (bookTitle.contains("§k")) {
                    String deobfuscatedTitle = bookTitle.replace("§k", "");
                    MessageSendHelper.INSTANCE.sendChatMessage("§8["+Oasis.INSTANCE.rCC()+"☯§8] §7Deobfuscated Title: §6"+deobfuscatedTitle);
                }
            }
            this.deobfuscate = true;
            this.cachedPage = -1;
        }
    }

    @ModifyVariable(method = "drawScreen", at = @At(value = "INVOKE_ASSIGN", target = "Lnet/minecraft/nbt/NBTTagList;getStringTagAt(I)Ljava/lang/String;"), ordinal = 1)
    private String mixinDrawScreen(String s5) {
        if (this.deobfuscate) {
            return s5.replace("§k", "");
        } else {
            return s5;
        }
    }
}
