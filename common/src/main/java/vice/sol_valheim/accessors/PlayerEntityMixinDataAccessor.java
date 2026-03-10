package vice.sol_valheim.accessors;

import com.mojang.authlib.minecraft.client.MinecraftClient;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import vice.sol_valheim.ValheimFoodData;

public interface PlayerEntityMixinDataAccessor {
    // 獲取食物數據
    ValheimFoodData sol_valheim$getFoodData();

    // 新增：定義重新整理（同步）數據的方法，這將解決編譯錯誤
    void sol_valheim$refreshFoodData();
}