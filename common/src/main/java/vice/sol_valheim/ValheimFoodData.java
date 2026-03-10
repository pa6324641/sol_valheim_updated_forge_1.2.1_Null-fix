package vice.sol_valheim;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.syncher.EntityDataSerializer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.UseAnim;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class ValheimFoodData
{
    public static final EntityDataSerializer<ValheimFoodData> FOOD_DATA_SERIALIZER = new EntityDataSerializer<>(){
        @Override
        public void write(FriendlyByteBuf buffer, ValheimFoodData value)
        {
            buffer.writeNbt(value.save(new CompoundTag()));
        }

        @Override
        public ValheimFoodData read(FriendlyByteBuf buffer) {

            return ValheimFoodData.read(buffer.readNbt());
        }

        @Override
        public ValheimFoodData copy(ValheimFoodData value)
        {
            var ret = new ValheimFoodData();
            ret.MaxItemSlots = value.MaxItemSlots;
            ret.ItemEntries = value.ItemEntries.stream().map(EatenFoodItem::new).collect(Collectors.toCollection(ArrayList::new));
            if (value.DrinkSlot != null)
                ret.DrinkSlot = new EatenFoodItem(value.DrinkSlot);
            return ret;
        }

    };

    public List<EatenFoodItem> ItemEntries = new ArrayList<>();
    public EatenFoodItem DrinkSlot;
    public int MaxItemSlots = SOLValheim.Config.common.maxSlots;

	public void eatItem(ItemStack food)	{
		// 增加：防禦性檢查，如果物品真的空了，就不執行任何邏輯
		if (food == null || food.isEmpty()) return;

		if (food.is(Items.ROTTEN_FLESH))
			return;

		var config = ModConfig.getFoodConfig(food);
		if (config == null)
			return;

		var isDrink = food.getUseAnimation() == UseAnim.DRINK;
		if (isDrink) {
			if (DrinkSlot != null && !DrinkSlot.canEatEarly())
				return;

			// 確保建立一個新的副本，避免引用原始已歸零的 stack
			DrinkSlot = new EatenFoodItem(food.copy(), config.getTime());
			return;
		}

		var existing = getEatenFood(food);
		if (existing != null) {
			if (!existing.canEatEarly()) return;
			existing.ticksLeft = config.getTime();
			existing.item = food.copy(); // 使用副本
			return;
		}

		if (ItemEntries.size() < MaxItemSlots)
		{
			ItemEntries.add(new EatenFoodItem(food.copy(), config.getTime()));
			return;
		}

		for (var item : ItemEntries)
		{
			if (item.canEatEarly())
			{
				item.ticksLeft = config.getTime();
				item.item = food.copy(); // 使用副本
				return;
			}
		}
	}

    public boolean canEat(ItemStack food)
    {
        if (food.is(Items.ROTTEN_FLESH))
            return true;

        if (food.getUseAnimation() == UseAnim.DRINK)
            return DrinkSlot == null || DrinkSlot.canEatEarly();

        var existing = getEatenFood(food);
        if (existing != null)
            return existing.canEatEarly();

        if (ItemEntries.size() < MaxItemSlots)
            return true;

        return ItemEntries.stream().anyMatch(EatenFoodItem::canEatEarly);
    }

	public EatenFoodItem getEatenFood(ItemStack food) {
		return ItemEntries.stream()
				.filter(item -> ItemStack.isSameItemSameTags(item.item, food))
				.findFirst()
				.orElse(null);
	}

    public void clear()
    {
        ItemEntries.clear();
        DrinkSlot = null;
    }


	public void tick() {
		// 確保 ticksLeft 不會減到負數，且移除 <= 0 的物件
		ItemEntries.removeIf(item -> {
			item.ticksLeft--;
			return item.ticksLeft <= 0;
		});

		if (DrinkSlot != null) {
			DrinkSlot.ticksLeft--;
			if (DrinkSlot.ticksLeft <= 0) DrinkSlot = null;
		}
	}


    public float getTotalFoodNutrition()
    {
        float nutrition = 0f;
        for (var item : ItemEntries)
        {
            ModConfig.Common.FoodConfig food = ModConfig.getFoodConfig(item.item);
            if (food == null)
                continue;

            nutrition += food.getHearts();
        }

        if (DrinkSlot != null)
        {
            ModConfig.Common.FoodConfig food = ModConfig.getFoodConfig(DrinkSlot.item);
            if (food != null)
            {
                nutrition += food.getHearts();
            }

            nutrition = nutrition * (1.0f + SOLValheim.Config.common.drinkSlotFoodEffectivenessBonus);
        }

        return nutrition;
    }


    public float getRegenSpeed()
    {
        float regen = 0.25f;
        for (var item : ItemEntries)
        {
            ModConfig.Common.FoodConfig food = ModConfig.getFoodConfig(item.item);
            if (food == null)
                continue;

            regen += food.getHealthRegen();
        }

        if (DrinkSlot != null)
        {
            ModConfig.Common.FoodConfig food = ModConfig.getFoodConfig(DrinkSlot.item);
            if (food != null)
            {
                regen += food.getHealthRegen();
            }

            regen = regen * (1.0f + SOLValheim.Config.common.drinkSlotFoodEffectivenessBonus);
        }

        return regen;
    }


    public CompoundTag save(CompoundTag tag) {
        int count = 0;
        tag.putInt("max_slots", MaxItemSlots);
        tag.putInt("count", ItemEntries.size());
        for (var item : ItemEntries)
        {
            tag.putString("id" + count, item.item.getItem().arch$registryName().toString());
            CompoundTag stackData = item.item.getTag();
            if (stackData != null) {
                tag.put("data" + count, stackData);
            }
            tag.putInt("ticks" + count, item.ticksLeft);
            count++;
        }

		if (DrinkSlot != null)
		{
			tag.putString("drink", DrinkSlot.item.getItem().arch$registryName().toString());
			CompoundTag stackData = DrinkSlot.item.getTag();
			if (stackData != null) {
				tag.put("drinkData", stackData); // 移除 + count，使用固定標籤
			}
			tag.putInt("drinkticks", DrinkSlot.ticksLeft);
		}

        return tag;
    }

    public static ValheimFoodData read(CompoundTag tag) {
        var instance = new ValheimFoodData();
        instance.MaxItemSlots = tag.getInt("max_slots");

        var size = tag.getInt("count");
        for (int count = 0; count < size; count++)
        {
            var str = tag.getString("id" + count);
            var ticks = tag.getInt("ticks" + count);
            var item = SOLValheim.ITEMS.getRegistrar().get(new ResourceLocation(str));
            var stack = new ItemStack(item, 1);
            if (tag.contains("data" + count)) {
                var data = tag.getCompound("data" + count);
                stack.setTag(data);
            }

            instance.ItemEntries.add(new EatenFoodItem(stack, ticks));
        }

        var drinkTicks = tag.getInt("drinkticks");
		var drink = tag.getString("drink");
		if (!drink.isBlank())
		{
			var item = SOLValheim.ITEMS.getRegistrar().get(new ResourceLocation(drink));
			var stack = new ItemStack(item, 1);
			if (tag.contains("drinkData")) { // 名稱需與 save 一致
				stack.setTag(tag.getCompound("drinkData"));
			}
			instance.DrinkSlot = new EatenFoodItem(stack, tag.getInt("drinkticks"));
		}

        return instance;
    }


    public static class EatenFoodItem {
        public ItemStack item;
        public int ticksLeft;

        public boolean canEatEarly() {
            if (ticksLeft < 1200)
                return true;

            var config = ModConfig.getFoodConfig(item);
            if (config == null)
                return false;

            return ((float) this.ticksLeft / config.getTime()) < SOLValheim.Config.common.eatAgainPercentage;
        }

		public EatenFoodItem(ItemStack item, int ticksLeft)
		{
			this.item = item.copy();
			this.item.setCount(1); // 強制數量為 1，防止變為空物品
			this.ticksLeft = ticksLeft;
		}

		public EatenFoodItem(EatenFoodItem eaten)
		{
			// 必須也使用 copy 並確保數量
			this.item = eaten.item.copy();
			this.item.setCount(1);
			this.ticksLeft = eaten.ticksLeft;
		}
    }
}