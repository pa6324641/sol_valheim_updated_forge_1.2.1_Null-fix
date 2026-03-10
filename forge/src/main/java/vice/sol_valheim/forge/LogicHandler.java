package vice.sol_valheim.forge;

import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import vice.sol_valheim.accessors.PlayerEntityMixinDataAccessor; // Import 放在這裡沒關係
import vice.sol_valheim.ValheimFoodData;

public class LogicHandler {
    public static void handleRightClick(PlayerInteractEvent.RightClickItem event) {
        var player = event.getEntity();
        var stack = event.getItemStack();

        // 只有在執行到這行時，才會去加載 Mixin 介面，避開了啟動時的掃描衝突
        if (player instanceof PlayerEntityMixinDataAccessor accessor) {
            ValheimFoodData data = accessor.sol_valheim$getFoodData();
            if (data != null) {
                boolean alreadyEatenAndCooling = data.ItemEntries.stream()
                    .anyMatch(entry -> entry.item.getItem() == stack.getItem() && !entry.canEatEarly());

                if (alreadyEatenAndCooling) {
                    event.setCanceled(true);
                }
            }
        }
    }
}