package logisticspipes.network.guis.block;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Container;

import logisticspipes.blocks.LogisticsProgramCompilerTileEntity;
import logisticspipes.gui.GuiProgramCompiler;
import logisticspipes.network.abstractguis.CoordinatesGuiProvider;
import logisticspipes.network.abstractguis.GuiProvider;

import logisticspipes.utils.StaticResolve;

@StaticResolve
public class ProgramCompilerGui extends CoordinatesGuiProvider {

	public ProgramCompilerGui(int id) {
		super(id);
	}

	@Override
	public Object getClientGui(EntityPlayer player) {
		LogisticsProgramCompilerTileEntity tile = this.getTile(player.getEntityWorld(), LogisticsProgramCompilerTileEntity.class);
		if (tile == null) {
			return null;
		}
		return new GuiProgramCompiler(player, tile);
	}

	@Override
	public Container getContainer(EntityPlayer player) {
		LogisticsProgramCompilerTileEntity tile = this.getTile(player.getEntityWorld(), LogisticsProgramCompilerTileEntity.class);
		if (tile == null) {
			return null;
		}
		return GuiProgramCompiler.createDummyContainer(player, null, tile.getInventory(), tile);
	}

	@Override
	public GuiProvider template() {
		return new ProgramCompilerGui(getId());
	}
}
