package logisticspipes.pipes;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;

import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidUtil;

import logisticspipes.LogisticsPipes;
import logisticspipes.interfaces.ITankUtil;
import logisticspipes.interfaces.routing.IAdditionalTargetInformation;
import logisticspipes.interfaces.routing.IRequestItems;
import logisticspipes.interfaces.routing.IRequireReliableTransport;
import logisticspipes.modules.abstractmodules.LogisticsModule;
import logisticspipes.network.GuiIDs;
import logisticspipes.pipes.basic.CoreRoutedPipe;
import logisticspipes.proxy.MainProxy;
import logisticspipes.proxy.SimpleServiceLocator;
import logisticspipes.request.RequestTree;
import logisticspipes.textures.Textures;
import logisticspipes.textures.Textures.TextureType;
import logisticspipes.transport.LPTravelingItem.LPTravelingItemServer;
import logisticspipes.transport.PipeTransportLogistics;
import logisticspipes.utils.CacheHolder.CacheTypes;
import logisticspipes.utils.FluidIdentifier;
import logisticspipes.utils.FluidIdentifierStack;
import logisticspipes.utils.item.ItemIdentifier;
import logisticspipes.utils.item.ItemIdentifierInventory;
import logisticspipes.utils.item.ItemIdentifierStack;
import network.rs485.logisticspipes.world.WorldCoordinatesWrapper;

public class PipeItemsFluidSupplier extends CoreRoutedPipe implements IRequestItems, IRequireReliableTransport {

	private boolean _lastRequestFailed = false;

	public PipeItemsFluidSupplier(Item item) {
		super(new PipeTransportLogistics(true) {

			@Override
			public boolean canPipeConnect(TileEntity tile, EnumFacing dir) {
				if (super.canPipeConnect(tile, dir)) {
					return true;
				}
				if (SimpleServiceLocator.pipeInformationManager.isItemPipe(tile)) {
					return false;
				}
				//TODO: FIXME
				/*
				if (tile instanceof IFluidHandler) {
					IFluidHandler liq = (IFluidHandler) tile;
					if (liq.getTankInfo(dir.getOpposite()) != null && liq.getTankInfo(dir.getOpposite()).length > 0) {
						return true;
					}
				}
				*/
				return false;
			}
		}, item);

		throttleTime = 100;
	}

	@Override
	public TextureType getCenterTexture() {
		return Textures.LOGISTICSPIPE_LIQUIDSUPPLIER_TEXTURE;
	}

	/* TRIGGER INTERFACE */
	public boolean isRequestFailed() {
		return _lastRequestFailed;
	}

	public void setRequestFailed(boolean value) {
		_lastRequestFailed = value;
	}

	@Override
	public LogisticsModule getLogisticsModule() {
		return null;
	}

	@Override
	public ItemSendMode getItemSendMode() {
		return ItemSendMode.Fast;
	}

	public void endReached(LPTravelingItemServer data, TileEntity tile) {
		getCacheHolder().trigger(CacheTypes.Inventory);
		transport.markChunkModified(tile);
		notifyOfItemArrival(data.getInfo());
		EnumFacing orientation = data.output.getOpposite();
		if (getOriginalUpgradeManager().hasSneakyUpgrade()) {
			orientation = getOriginalUpgradeManager().getSneakyOrientation();
		}
		ITankUtil util = SimpleServiceLocator.tankUtilFactory.getTankUtilForTE(tile, orientation);
		if (util == null) {
			return;
		}
		if (SimpleServiceLocator.pipeInformationManager.isItemPipe(tile)) {
			return;
		}
		if (data.getItemIdentifierStack() == null) {
			return;
		}
		FluidIdentifierStack liquidId = FluidIdentifierStack.getFromStack(data.getItemIdentifierStack());
		if (liquidId == null) {
			return;
		}
		while (data.getItemIdentifierStack().getStackSize() > 0 && util.fill(liquidId, false) == liquidId.getAmount() && this.useEnergy(5)) {
			util.fill(liquidId, true);
			data.getItemIdentifierStack().lowerStackSize(1);
			Item item = data.getItemIdentifierStack().getItem().item;
			if (item.hasContainerItem(data.getItemIdentifierStack().makeNormalStack())) {
				Item containerItem = item.getContainerItem();
				transport.sendItem(new ItemStack(containerItem, 1));
			}
		}
	}

	@Override
	public boolean hasGenericInterests() {
		return true;
	}

	// from PipeItemsFluidSupplier
	private ItemIdentifierInventory dummyInventory = new ItemIdentifierInventory(9, "Fluids to keep stocked", 127);

	private final HashMap<ItemIdentifier, Integer> _requestedItems = new HashMap<>();

	private boolean _requestPartials = false;

	@Override
	public void throttledUpdateEntity() {
		if (!isEnabled()) {
			return;
		}

		if (MainProxy.isClient(getWorld())) {
			return;
		}
		super.throttledUpdateEntity();

		Iterator<ITankUtil> iterator = new WorldCoordinatesWrapper(container).connectedTileEntities()
				.filter(adjacent -> !SimpleServiceLocator.pipeInformationManager.isItemPipe(adjacent.getTileEntity()))
				.map(adjacent -> SimpleServiceLocator.tankUtilFactory.getTankUtilForTE(adjacent.getTileEntity(), adjacent.getDirection()))
				.filter(Objects::nonNull)
				.iterator();

		while (iterator.hasNext()) {
			ITankUtil next = iterator.next();

			if (!next.containsTanks()) {
				continue;
			}

			//How much do I want?
			Map<ItemIdentifier, Integer> wantContainers = dummyInventory.getItemsAndCount();
			HashMap<FluidIdentifier, Integer> wantFluids = new HashMap<>();
			for (Entry<ItemIdentifier, Integer> item : wantContainers.entrySet()) {
				ItemStack wantItem = item.getKey().unsafeMakeNormalStack(1);
				FluidStack liquidstack = FluidUtil.getFluidContained(wantItem);
				if (liquidstack == null) {
					continue;
				}
				wantFluids.put(FluidIdentifier.get(liquidstack), item.getValue() * liquidstack.amount);
			}

			//How much do I have?
			HashMap<FluidIdentifier, Integer> haveFluids = new HashMap<>();

			next.forEachFluid(fluid -> {
				if (wantFluids.containsKey(fluid.getFluid())) {
					haveFluids.merge(fluid.getFluid(), fluid.getAmount(), (a, b) -> a + b);
				}
			});

			//HashMap<Integer, Integer> needFluids = new HashMap<Integer, Integer>();
			//Reduce what I have and what have been requested already
			for (Entry<FluidIdentifier, Integer> liquidId : wantFluids.entrySet()) {
				Integer haveCount = haveFluids.get(liquidId.getKey());
				if (haveCount != null) {
					liquidId.setValue(liquidId.getValue() - haveCount);
				}
			}
			for (Entry<ItemIdentifier, Integer> requestedItem : _requestedItems.entrySet()) {
				ItemStack wantItem = requestedItem.getKey().unsafeMakeNormalStack(1);
				FluidStack requestedFluidId = FluidUtil.getFluidContained(wantItem);
				if (requestedFluidId == null) {
					continue;
				}
				FluidIdentifier requestedFluid = FluidIdentifier.get(requestedFluidId);
				Integer want = wantFluids.get(requestedFluid);
				if (want != null) {
					wantFluids.put(requestedFluid, want - requestedItem.getValue() * requestedFluidId.amount);
				}
			}

			((PipeItemsFluidSupplier) container.pipe).setRequestFailed(false);

			//Make request

			for (ItemIdentifier need : wantContainers.keySet()) {
				FluidStack requestedFluidId = FluidUtil.getFluidContained(need.unsafeMakeNormalStack(1));
				if (requestedFluidId == null) {
					continue;
				}
				if (!wantFluids.containsKey(FluidIdentifier.get(requestedFluidId))) {
					continue;
				}
				int countToRequest = wantFluids.get(FluidIdentifier.get(requestedFluidId)) / requestedFluidId.amount;
				if (countToRequest < 1) {
					continue;
				}

				if (!useEnergy(11)) {
					break;
				}

				boolean success = false;

				if (_requestPartials) {
					countToRequest = RequestTree.requestPartial(need.makeStack(countToRequest), (IRequestItems) container.pipe, null);
					if (countToRequest > 0) {
						success = true;
					}
				} else {
					success = RequestTree.request(need.makeStack(countToRequest), (IRequestItems) container.pipe, null, null);
				}

				if (success) {
					Integer currentRequest = _requestedItems.get(need);
					if (currentRequest == null) {
						_requestedItems.put(need, countToRequest);
					} else {
						_requestedItems.put(need, currentRequest + countToRequest);
					}
				} else {
					((PipeItemsFluidSupplier) container.pipe).setRequestFailed(true);
				}
			}
		}
	}

	@Override
	public void readFromNBT(NBTTagCompound nbttagcompound) {
		super.readFromNBT(nbttagcompound);
		dummyInventory.readFromNBT(nbttagcompound, "");
		_requestPartials = nbttagcompound.getBoolean("requestpartials");
	}

	@Override
	public void writeToNBT(NBTTagCompound nbttagcompound) {
		super.writeToNBT(nbttagcompound);
		dummyInventory.writeToNBT(nbttagcompound, "");
		nbttagcompound.setBoolean("requestpartials", _requestPartials);
	}

	private void decreaseRequested(ItemIdentifierStack item) {
		int remaining = item.getStackSize();
		//see if we can get an exact match
		Integer count = _requestedItems.get(item.getItem());
		if (count != null) {
			_requestedItems.put(item.getItem(), Math.max(0, count - remaining));
			remaining -= count;
		}
		if (remaining <= 0) {
			return;
		}
		//still remaining... was from fuzzyMatch on a crafter
		for (Entry<ItemIdentifier, Integer> e : _requestedItems.entrySet()) {
			if (e.getKey().item == item.getItem().item && e.getKey().itemDamage == item.getItem().itemDamage) {
				int expected = e.getValue();
				e.setValue(Math.max(0, expected - remaining));
				remaining -= expected;
			}
			if (remaining <= 0) {
				return;
			}
		}
		//we have no idea what this is, log it.
		debug.log("liquid supplier got unexpected item " + item.toString());
	}

	@Override
	public void itemLost(ItemIdentifierStack item, IAdditionalTargetInformation info) {
		decreaseRequested(item);
	}

	@Override
	public void itemArrived(ItemIdentifierStack item, IAdditionalTargetInformation info) {
		decreaseRequested(item);
		delayThrottle();
	}

	public boolean isRequestingPartials() {
		return _requestPartials;
	}

	public void setRequestingPartials(boolean value) {
		_requestPartials = value;
	}

	@Override
	public void onWrenchClicked(EntityPlayer entityplayer) {
		entityplayer.openGui(LogisticsPipes.instance, GuiIDs.GUI_FluidSupplier_ID, getWorld(), getX(), getY(), getZ());
	}

	/*** GUI ***/
	public ItemIdentifierInventory getDummyInventory() {
		return dummyInventory;
	}
}
