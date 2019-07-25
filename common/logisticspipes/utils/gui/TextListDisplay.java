package logisticspipes.utils.gui;

import java.util.Collections;

import net.minecraft.client.gui.Gui;
import net.minecraft.util.text.TextFormatting;

import lombok.Getter;
import lombok.Setter;

import logisticspipes.utils.Color;
import logisticspipes.utils.string.StringUtils;

public class TextListDisplay implements IGuiScrollable {

	public interface List {
		int getSize();
		String getTextAt(int index);
		int getTextColor(int index);
	}

	private final List list;
	private final IGuiAccess gui;

	private final int borderTop;
	private final int borderRight;
	private final int borderBottom;
	private final int borderLeft;
	private final int elementsPerPage;

	private int mouseClickX = 0;
	private int mouseClickY = 0;
	private int mousePosX = 0;
	private int mousePosY = 0;
	private int scrollValue = 0;

	public boolean actAsScrollArea;

	@Getter
	private int selected = -1;
	private int hover = -1;

	public TextListDisplay(IGuiAccess gui, int borderLeft, int borderTop, int borderRight, int borderBottom, int elementPerPage, List list) {
		this.list = list;
		this.gui = gui;
		this.borderTop = borderTop;
		this.borderRight = borderRight;
		this.borderBottom = borderBottom;
		this.borderLeft = borderLeft;
		this.elementsPerPage = elementPerPage;

		this.actAsScrollArea = true;
	}

	public void mouseClicked(int x, int y, int button) {
		mouseClickX = x;
		mouseClickY = y;
	}

	public void renderGuiBackground(int mouseX, int mouseY) {
		mousePosX = mouseX;
		mousePosY = mouseY;

		Gui.drawRect(getGuiLeft(), getGuiTop(), getRight(), getBottom(), Color.getValue(Color.GREY));

		if (scrollValue + elementsPerPage > list.getSize())
			scrollValue = getMaxScrollValue();

		if (scrollValue < 0)
			scrollValue = 0;

		hover = -1;
		if (getGuiLeft() + 2 < this.mousePosX
				&& this.mousePosX < getRight() - 2 && getGuiTop() + 2 < this.mousePosY
				&& this.mousePosY < getGuiTop() + 3 + (elementsPerPage * 10)) {
			hover = scrollValue + (this.mousePosY - gui.getGuiTop() - borderTop - 3) / 10;
		}
		if(list.getSize() == 0 || hover >= list.getSize())
			hover = -1;

		if (getGuiLeft() + 2 < this.mouseClickX
				&& this.mouseClickX < getRight() - 2 && getGuiTop() + 2 < this.mouseClickY
				&& this.mouseClickY < getGuiTop() + 3 + (elementsPerPage * 10)) {
			selected = scrollValue + (this.mouseClickY - gui.getGuiTop() - borderTop - 3) / 10;

			mouseClickX = -1;
			mouseClickY = -1;
		}

		boolean selectedVisisble = false;
		for (int i = scrollValue; i < list.getSize() && (i - scrollValue) < elementsPerPage; i++) {
			if (i == selected) {
				Gui.drawRect(getGuiLeft() + 2, getGuiTop() + 2 + ((i - scrollValue) * 10), getRight() - 2, getGuiTop() + 13 + ((i - scrollValue) * 10), Color.getValue(Color.DARKER_GREY));
				selectedVisisble = true;
			}

			String name = list.getTextAt(i);
			name = StringUtils.getCuttedString(name, gui.getXSize() - borderRight - borderLeft - 6, gui.getMC().fontRenderer);
			gui.getMC().fontRenderer.drawString(name, getGuiLeft() + 4, getGuiTop() + 4 + ((i - scrollValue) * 10), list.getTextColor(i));
		}
		if (!selectedVisisble)
			selected = -1;
	}

	public void renderGuiForeground() {
		if(hover != -1)
			GuiGraphics.drawToolTip(mousePosX - gui.getGuiLeft(), mousePosY - gui.getGuiTop(), Collections.singletonList(list.getTextAt(hover)), TextFormatting.WHITE);
	}

	private void clampSelectedValue() {
		if (selected < 0)
			selected = -1;
		else if (selected >= list.getSize())
			selected = -1;
	}

	public void setSelected(int value) {
		selected = value;
		clampSelectedValue();
	}

	public boolean isScrollAreaActive() {
		return actAsScrollArea;
	}

	public int getGuiLeft() {
		return gui.getGuiLeft() + borderLeft;
	}

	public int getGuiTop() {
		return gui.getGuiTop() + borderTop;
	}

	public int getRight() {
		return gui.getRight() - borderRight;
	}

	public int getBottom() {
		return gui.getBottom() - borderBottom;
	}

	public void scrollUp() {
		scrollValue++;
	}

	public void scrollDown() {
		if (scrollValue > 0)
			scrollValue--;
	}

	public int getMaxScrollValue() {
		return list.getSize() - elementsPerPage;
	}

	public int getScrollValue() {
		return scrollValue;
	}

	public void setScrollValue(int value) {
		scrollValue = value;
	}
}
