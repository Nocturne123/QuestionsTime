package fr.nocturne123.questionstime.question.component;

import org.spongepowered.api.item.inventory.ItemStack;

public class Prize {

	private int money;
	private boolean announce;
	private ItemStack[] items;

	public Prize(int money, boolean announce, ItemStack[] is) {
		this.money = money > 0 ? money : 0;
		this.announce = announce;
		if(is != null)
			this.items = is;
		else
			this.items = new ItemStack[0];
	}
	
	public int getMoney() {
		return money;
	}
	
	public ItemStack[] getItemStacks() {
		return this.items;
	}
	
	public boolean isAnnounce() {
		return announce;
	}

	
}
