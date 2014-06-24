package logisticspipes.proxy.cc.objects;

import logisticspipes.proxy.cc.interfaces.CCCommand;
import logisticspipes.proxy.cc.interfaces.CCQueued;
import logisticspipes.proxy.cc.interfaces.CCType;
import logisticspipes.utils.item.ItemIdentifier;
import logisticspipes.utils.item.ItemIdentifierInventory;

@CCType(name="FilterInventory")
public class CCFilterInventory {
	
	private final ItemIdentifierInventory inv;
	
	public CCFilterInventory(ItemIdentifierInventory inv) {
		this.inv = inv;
	}
	
	@CCCommand(description="Returns the size of this FilterInventory")
	public int getSizeInventory() {
		return inv.getSizeInventory();
	}
	
	@CCCommand(description="Returns the ItemIdentifier in the givven slot")
	@CCQueued
	public ItemIdentifier getItemIdentifier(Double slot) {
		int s = slot.intValue();
		if(s <= 0 || s > this.getSizeInventory()) throw new UnsupportedOperationException("Slot out of Inventory");
		if(s != (double) slot) throw new UnsupportedOperationException("Slot not an Integer");
		s--;
		if(inv.getIDStackInSlot(s) == null) return null;
		return inv.getIDStackInSlot(s).getItem();
	}

	@CCCommand(description="Sets the ItemIdentifier at the givven slot")
	@CCQueued
	public void setItemIdentifier(Double slot, ItemIdentifier ident) {
		int s = slot.intValue();
		if(s <= 0 || s > this.getSizeInventory()) throw new UnsupportedOperationException("Slot out of Inventory");
		if(s != (double) slot) throw new UnsupportedOperationException("Slot not an Integer");
		s--;
		inv.setInventorySlotContents(s, ident.makeStack(1));
	}
}
