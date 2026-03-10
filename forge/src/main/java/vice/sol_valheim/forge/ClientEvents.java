package vice.sol_valheim.forge;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent; // 確保有這個
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import vice.sol_valheim.SOLValheim;

@OnlyIn(Dist.CLIENT)
@Mod.EventBusSubscriber(modid = "sol_valheim", value = {Dist.CLIENT}, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class ClientEvents {
    
    // 注意：刪除了 PlayerEntityMixinDataAccessor 的 import!

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onItemTooltip(ItemTooltipEvent event) {
        SOLValheim.addTooltip(event.getItemStack(), event.getFlags(), event.getToolTip());
    }

    @SubscribeEvent
    public static void onRenderGUI(RenderGuiOverlayEvent.Pre event) {
        if (event.getOverlay() == VanillaGuiOverlay.FOOD_LEVEL.type())
            event.setCanceled(true);
    }

    @SubscribeEvent
    public static void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        // 只判斷客戶端，邏輯交給 LogicHandler
        if (event.getLevel().isClientSide) {
            LogicHandler.handleRightClick(event);
        }
    }
}