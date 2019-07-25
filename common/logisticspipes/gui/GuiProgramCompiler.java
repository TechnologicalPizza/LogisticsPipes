package logisticspipes.gui;

import java.io.IOException;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import logisticspipes.LPItems;
import net.minecraft.client.gui.GuiButton;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTTagString;
import net.minecraft.util.ResourceLocation;

import logisticspipes.blocks.LogisticsProgramCompilerTileEntity;
import logisticspipes.interfaces.IGuiOpenControler;
import logisticspipes.items.ItemLogisticsPipe;
import logisticspipes.items.ItemLogisticsProgrammer;
import logisticspipes.items.ItemModule;
import logisticspipes.items.ItemUpgrade;
import logisticspipes.network.PacketHandler;
import logisticspipes.network.abstractpackets.ModernPacket;
import logisticspipes.network.packets.block.CompilerTriggerTaskPacket;
import logisticspipes.proxy.MainProxy;
import logisticspipes.utils.Color;
import logisticspipes.utils.gui.DummyContainer;
import logisticspipes.utils.gui.GuiGraphics;
import logisticspipes.utils.gui.GuiScrollBar;
import logisticspipes.utils.gui.LogisticsBaseGuiScreen;
import logisticspipes.utils.gui.GuiInputBar;
import logisticspipes.utils.gui.SmallGuiButton;
import logisticspipes.utils.gui.TextListDisplay;
import logisticspipes.utils.string.StringUtils;

//TODO: Config Option for disabling program compilation
public class GuiProgramCompiler extends LogisticsBaseGuiScreen {

	private final LogisticsProgramCompilerTileEntity compiler;
	private final TextListDisplay.List categoryTextList;
	private final TextListDisplay.List programTextList;
	private final TextListDisplay categoryList;
	private final TextListDisplay programList;
	private final TextListDisplay programListLarge;
	private SmallGuiButton programmerButton;
	private GuiInputBar searchBar;
	private GuiScrollBar scrollBar;

	public GuiProgramCompiler(EntityPlayer player, LogisticsProgramCompilerTileEntity compiler) {
		super(250, 220, 0, 0);
		this.compiler = compiler;

		categoryTextList = new TextListDisplay.List() {

			@Override
			public int getSize() {
				if (compiler.getInventory().getStackInSlot(0).isEmpty()) {
					return 0;
				}
				NBTTagList list = compiler.getNBTTagListForKey("compilerCategories");
				return (int) LogisticsProgramCompilerTileEntity.programByCategory.keySet().stream()
						.filter(it -> list.tagList.stream().noneMatch(nbtBase -> ((NBTTagString) nbtBase).getString().equals(it.toString()))).count();
			}

			@Override
			public String getTextAt(int index) {
				if (compiler.getInventory().getStackInSlot(0).isEmpty()) {
					return "";
				}
				NBTTagList list = compiler.getNBTTagListForKey("compilerCategories");
				return StringUtils.translate("gui.compiler." + LogisticsProgramCompilerTileEntity.programByCategory.keySet().stream()
						.filter(it -> list.tagList.stream().noneMatch(nbtBase -> ((NBTTagString) nbtBase).getString().equals(it.toString()))).collect(Collectors
								.toList()).get(index).toString().replace(':', '.'));
			}

			@Override
			public int getTextColor(int index) {
				return 0xFFFFFF;
			}
		};

		programTextList = new TextListDisplay.List() {

			@Override
			public int getSize() {
				if (compiler.getInventory().getStackInSlot(0).isEmpty())
					return 0;

				NBTTagList list = compiler.getNBTTagListForKey("compilerCategories");
				return getProgramListForSelectionIndex(list).size();
			}

			@Override
			public String getTextAt(int index) {
				if (compiler.getInventory().getStackInSlot(0).isEmpty())
					return "";

				NBTTagList list = compiler.getNBTTagListForKey("compilerCategories");
				ResourceLocation sel = getProgramListForSelectionIndex(list).get(index);
				Item selItem = Item.REGISTRY.getObject(sel);
				if (selItem != null)
					return StringUtils.translate(selItem.getUnlocalizedName() + ".name");

				return "UNDEFINED";
			}

			@Override
			public int getTextColor(int index) {
				if (compiler.getInventory().getStackInSlot(0).isEmpty())
					return 0xFFFFFF;

				NBTTagList list = compiler.getNBTTagListForKey("compilerCategories");
				ResourceLocation selected = getProgramListForSelectionIndex(list).get(index);
				NBTTagList listPrograms = compiler.getNBTTagListForKey("compilerPrograms");

				if (listPrograms.tagList.stream().anyMatch(it -> new ResourceLocation(((NBTTagString) it).getString()).equals(selected))) {
					ItemStack stack = compiler.getInventory().getStackInSlot(1);
					if (stackHasRecipeTarget(stack, selected))
						return 0xCCCCFF; // blue for flashed program
					return 0xAAFFAA; // green for available programs
				}
				return 0xFFAAAA;
			}
		};

		categoryList = new TextListDisplay(this, 8, 8, 175, 126, 8, categoryTextList);
		programList = new TextListDisplay(this, 80, 8, 19, 126, 8, programTextList);
		programListLarge = new TextListDisplay(this, 8, 8, 19, 126, 8, programTextList);

		IInventory containerInventory = compiler.getInventory();
		inventorySlots = createDummyContainer(player, containerInventory, containerInventory);
	}

	private static boolean stackHasRecipeTarget(ItemStack stack, ResourceLocation target){
		if (!stack.hasTagCompound())
			return false;
		return new ResourceLocation(stack.getTagCompound().getString(ItemLogisticsProgrammer.RECIPE_TARGET)).equals(target);
	}

	public static DummyContainer createDummyContainer(EntityPlayer player, IInventory dummyInventory, IInventory inventory, IGuiOpenControler... controller) {
		DummyContainer dummy;
		if (controller != null) {
			dummy = new DummyContainer(player, dummyInventory, controller);
		} else {
			dummy = new DummyContainer(player.inventory, dummyInventory);
		}

		dummy.addRestrictedSlot(0, inventory, 10, 115, LPItems.disk);
		dummy.addRestrictedSlot(1, inventory, 224, 115, LPItems.logisticsProgrammer);
		dummy.addNormalSlotsForPlayerInventory(45, 135);

		return dummy;
	}

	@Override
	public void initGui() {
		super.initGui();

		buttonList.clear();
		buttonList.add(new SmallGuiButton(0, guiLeft + 8, guiTop + 100, 15, 10, "/\\"));
		buttonList.add(new SmallGuiButton(1, guiLeft + 24, guiTop + 100, 15, 10, "\\/"));
		buttonList.add(new SmallGuiButton(2, guiLeft + 40, guiTop + 98, 40, 12, "Unlock"));
		buttonList.add(programmerButton = new SmallGuiButton(5, guiLeft + 130, guiTop + 98, 44, 12, "Compile"));

		if (searchBar == null) {
			searchBar = new GuiInputBar(fontRenderer, this, 0, 0, 0, 0);
			searchBar.placeholder = StringUtils.translate("gui.compiler.searchplaceholder");
		}
		searchBar.reposition(guiLeft + 30, guiTop + 115, 190, 16);

		if (scrollBar == null) {
			scrollBar = new GuiScrollBar(this, 0, 0, 0, 0);
			scrollBar.scrollIfNotFocused = true;
		}
		scrollBar.reposition(getRight() - 16, guiTop + 8, 9, 86);
	}

	@Override
	protected void actionPerformed(GuiButton button) throws IOException {
		super.actionPerformed(button);
		switch (button.id) {
			case 0:
				categoryList.scrollDown();
				break;
			case 1:
				categoryList.scrollUp();
				break;

			case 2:
				if (categoryList.getSelected() != -1) {
					NBTTagList list = compiler.getNBTTagListForKey("compilerCategories");
					ResourceLocation cat = LogisticsProgramCompilerTileEntity.programByCategory.keySet().stream()
							.filter(it -> list.tagList.stream().noneMatch(nbtBase -> ((NBTTagString) nbtBase).getString().equals(it.toString())))
							.collect(Collectors.toList()).get(categoryList.getSelected());
					MainProxy.sendPacketToServer(PacketHandler.getPacket(CompilerTriggerTaskPacket.class).setCategory(cat).setType("category").setTilePos(compiler));
				}
				break;

			case 5:
				int selectedIndex = isShowingLargeProgramList()
						? programListLarge.getSelected() : programList.getSelected();

				if (selectedIndex != -1) {
					NBTTagList list = compiler.getNBTTagListForKey("compilerCategories");
					ResourceLocation selected = getProgramListForSelectionIndex(list).get(selectedIndex);
					CompilerTriggerTaskPacket packet = PacketHandler.getPacket(CompilerTriggerTaskPacket.class).setCategory(selected);

					NBTTagList listPrograms = compiler.getNBTTagListForKey("compilerPrograms");
					if (listPrograms.tagList.stream().anyMatch(it -> new ResourceLocation(((NBTTagString) it).getString()).equals(selected))) {
						MainProxy.sendPacketToServer(packet.setType("flash").setTilePos(compiler));
					} else {
						MainProxy.sendPacketToServer(packet.setType("program").setTilePos(compiler));
					}
				}
				break;
		}
	}

	@Override
	protected void drawGuiContainerBackgroundLayer(float var1, int mouseX, int mouseY) {
		GuiGraphics.drawGuiBackGround(mc, guiLeft, guiTop, right, bottom, zLevel, true);
		GuiGraphics.drawPlayerInventoryBackground(mc, guiLeft + 45, guiTop + 135);
		GuiGraphics.drawSlotDiskBackground(mc, guiLeft + 9, guiTop + 114);
		GuiGraphics.drawSlotProgrammerBackground(mc, guiLeft + 223, guiTop + 114);

		if (compiler.getCurrentTask() != null) {
			buttonList.forEach(b -> b.visible = false);

			fontRenderer.drawString(StringUtils.translate("gui.compiler.processing"), guiLeft + 10, guiTop + 39, 0x000000);
			String name;
			Item item = Item.REGISTRY.getObject(compiler.getCurrentTask());
			if (item != null) {
				name = item.getUnlocalizedName() + ".name";
			} else {
				name = "gui.compiler." + compiler.getCurrentTask().toString().replace(':', '.');
			}
			String text = StringUtils.getCuttedString(StringUtils.translate(name), 160, fontRenderer);
			fontRenderer.drawString(text, guiLeft + 10, guiTop + 70, 0x000000);
			drawRect(guiLeft + 9, guiTop + 50, guiLeft + 241, guiTop + 66, Color.BLACK);
			drawRect(guiLeft + 10, guiTop + 51, guiLeft + 240, guiTop + 65, Color.WHITE);
			drawRect(guiLeft + 11, guiTop + 52, guiLeft + 11 + (int) (228 * compiler.getTaskProgress()), guiTop + 64, Color.GREEN);

			if (!compiler.isWasAbleToConsumePower()) {
				fontRenderer.drawString(StringUtils.translate("gui.compiler.nopower.1"), guiLeft + 68, guiTop + 10, 0x000000);
				fontRenderer.drawString(StringUtils.translate("gui.compiler.nopower.2"), guiLeft + 35, guiTop + 20, 0x000000);
			}

		} else {
			buttonList.forEach(b -> b.visible = true);

			if (isShowingLargeProgramList()) {
				buttonList.stream().limit(3).forEach(b -> b.visible = false);
				programListLarge.renderGuiBackground(mouseX, mouseY);
				scrollBar.scrollable = programListLarge;
			} else {
				buttonList.stream().limit(3).forEach(b -> b.visible = true);
				categoryList.renderGuiBackground(mouseX, mouseY);
				programList.renderGuiBackground(mouseX, mouseY);
				scrollBar.scrollable = programList;
			}

			scrollBar.renderGui(mouseX, mouseY);

			searchBar.renderGui();

			int selectedIndex = isShowingLargeProgramList()
					? programListLarge.getSelected() : programList.getSelected();

			if (selectedIndex != -1) {
				NBTTagList list = compiler.getNBTTagListForKey("compilerCategories");
				ResourceLocation selected = getProgramListForSelectionIndex(list).get(selectedIndex);
				NBTTagList listPrograms = compiler.getNBTTagListForKey("compilerPrograms");
				if (listPrograms.tagList.stream().anyMatch(it -> new ResourceLocation(((NBTTagString) it).getString()).equals(selected))) {
					ItemStack stack = compiler.getInventory().getStackInSlot(1);
					boolean hasTarget = stackHasRecipeTarget(stack, selected);
					programmerButton.enabled = !stack.isEmpty() && !hasTarget;

					if (hasTarget)
						programmerButton.displayString = "Flashed";
					else if (!stack.isEmpty())
						programmerButton.displayString = "Flash";
					else
						programmerButton.displayString = "No Item";
				} else {
					programmerButton.displayString = "Compile";
					programmerButton.enabled = true;
				}
			}
		}
	}

	private List<ResourceLocation> getProgramListForSelectionIndex(NBTTagList list) {
		return list.tagList.stream().flatMap(
				nbtBase -> LogisticsProgramCompilerTileEntity.programByCategory.get(new ResourceLocation(((NBTTagString) nbtBase).getString())).stream())
				.filter(it -> StringUtils.translate(Item.REGISTRY.getObject(it).getUnlocalizedName() + ".name").toLowerCase().contains(searchBar.getContent().toLowerCase()))
				.sorted(Comparator.comparing(o -> getSortingClass(Item.REGISTRY.getObject((ResourceLocation) o)))
						.thenComparing(o -> StringUtils.translate(Item.REGISTRY.getObject((ResourceLocation) o).getUnlocalizedName() + ".name").toLowerCase())
				)
				.collect(Collectors.toList());
	}

	private int getSortingClass(Item object) {
		if (object instanceof ItemLogisticsPipe)
			return 0;
		else if (object instanceof ItemModule)
			return 1;
		else if (object instanceof ItemUpgrade)
			return 2;
		else
			return 10;
	}

	@Override
	public void handleMouseInputSub() throws IOException {
		super.handleMouseInputSub();

		float wheel = org.lwjgl.input.Mouse.getDWheel() / 120f;
		scrollBar.handleMouseInput(wheel);
	}

	@Override
	protected void keyTyped(char typedChar, int keyCode) throws IOException {
		if (compiler.getCurrentTask() == null) {
			if (!searchBar.handleKey(typedChar, keyCode)) {
				super.keyTyped(typedChar, keyCode);
			}
		} else {
			super.keyTyped(typedChar, keyCode);
		}
	}

	@Override
	protected void mouseClicked(int x, int y, int button) throws IOException {
		super.mouseClicked(x, y, button);
		if (compiler.getCurrentTask() == null) {
			scrollBar.handleClick(x, y, button);
			searchBar.handleClick(x, y, button);
			if (isShowingLargeProgramList()) {
				programListLarge.mouseClicked(x, y, button);
			} else {
				categoryList.mouseClicked(x, y, button);
				programList.mouseClicked(x, y, button);
			}
		}
	}

	@Override
	protected void drawGuiContainerForegroundLayer(int par1, int par2) {
		super.drawGuiContainerForegroundLayer(par1, par2);
		if (compiler.getCurrentTask() == null) {
			if (isShowingLargeProgramList()) {
				programListLarge.renderGuiForeground();
			} else {
				categoryList.renderGuiForeground();
				programList.renderGuiForeground();
			}
		}
	}

	protected boolean isShowingLargeProgramList() {
		return categoryTextList.getSize() == 0 && !compiler.getInventory().getStackInSlot(0).isEmpty();
	}
}
