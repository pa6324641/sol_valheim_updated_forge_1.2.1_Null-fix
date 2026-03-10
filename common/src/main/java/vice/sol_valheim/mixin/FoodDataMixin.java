package vice.sol_valheim.mixin;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodData;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import vice.sol_valheim.accessors.FoodDataPlayerAccessor;
import vice.sol_valheim.accessors.PlayerEntityMixinDataAccessor;
import vice.sol_valheim.ValheimFoodData;
import vice.sol_valheim.ModConfig; // <--- 必須加上這一行
import vice.sol_valheim.SOLValheim; // <--- 建議也加上，如果後面有用到

@Mixin(FoodData.class)
public class FoodDataMixin implements FoodDataPlayerAccessor
{
    @Unique
    private Player sol_valheim$player;

    @Override
    public Player sol_valheim$getPlayer() { return sol_valheim$player;}

    @Override
    public void sol_valheim$setPlayer(Player player) { sol_valheim$player = player; }

	@Inject(at = @At("HEAD"), method = "eat(Lnet/minecraft/world/item/Item;Lnet/minecraft/world/item/ItemStack;)V")
	public void onEatFood(Item item, ItemStack stack, CallbackInfo ci)
	{
		if (sol_valheim$player == null || sol_valheim$player.level().isClientSide) return;

		ItemStack realFoodStack;
		
		// 優先判斷傳入的 stack，但如果它已經空了(數量變0)，就用傳入的 item 重新建立一個
		if (stack != null && !stack.isEmpty()) {
			realFoodStack = stack.copy(); // 建議 copy 一份，避免後續邏輯影響原始 stack
		} else if (item != null) {
			realFoodStack = new ItemStack(item);
		} else {
			return;
		}

		// 強制確保數量為 1，這是防止「佔 HUD 沒效果」的保險
		if (realFoodStack.getCount() <= 0) {
			realFoodStack.setCount(1);
		}

		var accessor = (PlayerEntityMixinDataAccessor) sol_valheim$player;
		var foodData = accessor.sol_valheim$getFoodData();
		
		// 檢查 Config 是否存在
		if (ModConfig.getFoodConfig(realFoodStack) != null) {
			foodData.eatItem(realFoodStack);
			
			// 呼叫同步
			accessor.sol_valheim$refreshFoodData();
		}
	}

    @Unique
    private void sol_valheim$syncToClient() {
        // 透過介面轉型呼叫 PlayerEntityMixin 裡的 refreshFoodData
        if (sol_valheim$player instanceof PlayerEntityMixinDataAccessor accessor) {
            accessor.sol_valheim$refreshFoodData();
        }
    }
}