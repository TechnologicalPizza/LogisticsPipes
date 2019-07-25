package logisticspipes.utils.gui;

import java.util.ArrayList;

import net.minecraft.util.math.MathHelper;

import lombok.Getter;
import org.lwjgl.input.Mouse;

import logisticspipes.utils.Color;

public class GuiScrollBar implements IGuiScrollable {

	private final LogisticsBaseGuiScreen screen;
	@Getter
	private int height, width;
	private int left, top;

	private boolean isFocused;
	private int pointerClickOffset;
	private ArrayList<IGuiScrollArea> extraScrollAreas;

	public IGuiScrollable scrollable;
	public boolean scrollIfNotFocused;

	private int maxScrollValue;
	private float scrollValue; // use float for smooth dragging

	@Getter
	private int minPointerHeight;

	@Getter
	private boolean isDragging;

	public GuiScrollBar(LogisticsBaseGuiScreen screen, int left, int top, int width, int height, boolean isFocused) {
		this.screen = screen;
		reposition(left, top, width, height);
		this.isFocused = isFocused;

		this.extraScrollAreas = new ArrayList<>();

		this.scrollIfNotFocused = false;
		this.maxScrollValue = 1;
		this.minPointerHeight = this.height < 50 ? 5 : this.height / 10;
	}

	public GuiScrollBar(LogisticsBaseGuiScreen screen, int left, int top, int width, int height) {
		this(screen, left, top, width, height, true);
	}

	public void reposition(int left, int top, int width, int height) {
		this.left = left;
		this.top = top;
		this.width = width;
		this.height = height;
	}

	public void renderGui(int mouseX, int mouseY) {
		update(mouseX, mouseY);

		screen.drawRect(left - 1, top - 1, left + width + 1, top + height + 1, isFocused() ? Color.WHITE : Color.DARKER_GREY);
		screen.drawRect(left, top, left + width, top + height, Color.GREY);

		int pointerY = getPointerTop();
		int pointerH = getPointerHeight();
		screen.drawRect(left, top + pointerY, left + width, top + pointerH + pointerY, Color.LIGHTER_GREY);
	}

	private void update(int mouseX, int mouseY) {
		if (scrollable != null) {
			setMaxScrollValue(scrollable.getMaxScrollValue());
			scrollable.setScrollValue(getScrollValue());
		}

		boolean leftDown = Mouse.isButtonDown(0);
		if (leftDown && isFocused) {
			int barY = mouseY - top;

			if(isDragging) {
				int heldY = barY - pointerClickOffset;
				scrollValue = heldY / (float) (height - getPointerHeight()) * maxScrollValue;
			}

			if (!isDragging) {
				isDragging = true;
				int pointerY = getPointerTop();
				int pointerH = getPointerHeight();

				if (mouseX >= left && mouseX < left + width && barY >= pointerY && barY < pointerY + pointerH)
					pointerClickOffset = barY - pointerY;
				else
					pointerClickOffset = pointerH / 2;
			}
		} else {
			isDragging = false;
		}

		clampScrollValue();
	}

	public void handleMouseInput(float mouseWheel) {
		if (mouseWheel != 0) {
			int mx = screen.getGuiX(Mouse.getX());
			int my = screen.getGuiX(Mouse.getY());
			if (mx >= screen.getGuiLeft() && mx < screen.getRight() && my >= screen.getGuiTop() && my < screen.getBottom()) {
				if (isFocused || scrollIfNotFocused || isPointOverExtendedArea(mx, my - top)) {
					scrollValue = Math.round(scrollValue) - (int) mouseWheel;
					clampScrollValue();
				}
			}
		}
	}

	private void clampScrollValue() {
		scrollValue = MathHelper.clamp(scrollValue, 0, maxScrollValue);
	}

	public boolean isPointOverExtendedArea(int x, int y) {
		if (isPointOverArea(x, y, this) || (scrollable != null && isPointOverArea(x, y, scrollable))) {
			return true;
		} else {
			for (IGuiScrollArea area : extraScrollAreas)
				if (area.isScrollAreaActive() && isPointOverArea(x, y, area))
					return true;
		}
		return false;
	}

	public boolean isPointOverArea(int x, int y, IGuiScrollArea area) {
		return area.isScrollAreaActive() &&
				x >= area.getGuiLeft() && x < area.getRight() && y >= area.getGuiTop() && y < area.getBottom();
	}

	public boolean handleClick(int x, int y, int button) {
		if (x >= left && x < left + width && y >= top && y < top + height) {
			focus();
			return true;
		} else if (isFocused()) {
			unfocus();
			return true;
		}
		return false;
	}

	public boolean addScrollArea(IGuiScrollArea area) {
		if (area == null || extraScrollAreas.contains(area))
			return false;
		return extraScrollAreas.add(area);
	}

	public boolean removeScrollArea(IGuiScrollArea area) {
		if (area == null)
			return false;
		return extraScrollAreas.remove(area);
	}

	public boolean isScrollAreaActive() {
		return true;
	}

	public int getGuiLeft() {
		return left;
	}

	public int getGuiTop() {
		return top;
	}

	public int getRight() {
		return left + width;
	}

	public int getBottom() {
		return top + height;
	}

	public int getPointerHeight() {
		return MathHelper.clamp((height / maxScrollValue), minPointerHeight, height);
	}

	public int getPointerTop() {
		return (int) Math.floor((scrollValue / (float) maxScrollValue) * (height - getPointerHeight()));
	}

	public int getMaxScrollValue() {
		return maxScrollValue;
	}

	public int getScrollValue() {
		return Math.round(scrollValue + 1f / maxScrollValue / 2f);
	}

	public void scrollUp() {
		scrollValue++;
	}

	public void scrollDown() {
		if (scrollValue > 0)
			scrollValue--;
	}

	public void setMinPointerHeight(int value) {
		minPointerHeight = MathHelper.clamp(value, 1, height);
	}

	public void setMaxScrollValue(int value) {
		maxScrollValue = MathHelper.clamp(value, 1, Integer.MAX_VALUE);
	}

	public void setScrollValue(int value) {
		this.scrollValue = value;
		clampScrollValue();
	}

	private void focus() {
		isFocused = true;
	}

	private void unfocus() {
		isFocused = false;
	}

	public boolean isFocused() {
		return isFocused;
	}
}