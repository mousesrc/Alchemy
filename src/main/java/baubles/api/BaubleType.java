package baubles.api;

import org.apache.commons.lang3.ArrayUtils;

import index.alchemy.api.annotation.Lang;

@Lang
public enum BaubleType {
	AMULET(0),
	RING(1, 2),
	BELT(3),
	HEAD(4),
	BODY(5),
	CHARM(6),
	TRINKET(0, 1, 2, 3, 4, 5, 6);
	
	int[] validSlots;
	
	private BaubleType(int... validSlots) {
		this.validSlots = validSlots;
	}
	
	public static final String PREFIX = "bauble.type.";
	
	public boolean hasSlot(int slot) {
		return ArrayUtils.contains(validSlots, slot); 
	}

	public int[] getValidSlots() {
		return validSlots;
	}
	
}