package vice.sol_valheim.forge;

import dev.architectury.platform.forge.EventBuses;
import vice.sol_valheim.SOLValheim;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.ItemStack;

@Mod(SOLValheim.MOD_ID)
public class ForgeInitializer
{
    public ForgeInitializer() {
        EventBuses.registerModEventBus(
                SOLValheim.MOD_ID,
                FMLJavaModLoadingContext.get().getModEventBus()
        );

        // 修改這裡：移除 Minecraft.getInstance()
        SOLValheim.init((stack) -> {
            if (stack == null || stack.isEmpty())
                return null;

            if (!stack.isEdible())
                return null;

            // 伺服器端安全的做法：直接從物品獲取屬性，不傳入玩家物件
            // 注意：某些模組食物可能需要玩家物件才能獲取動態屬性，
            // 但在伺服器初始化階段通常只能傳 null 或不傳
            return stack.getFoodProperties(null); 
        });
    }
}