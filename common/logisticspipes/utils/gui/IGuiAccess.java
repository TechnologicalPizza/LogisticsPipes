package logisticspipes.utils.gui;

import net.minecraft.client.Minecraft;

public interface IGuiAccess extends IGuiArea {

	int getXSize();

	int getYSize();

	Minecraft getMC();
}