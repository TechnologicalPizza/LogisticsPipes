package logisticspipes.utils.gui.extension;

import java.util.ArrayList;
import java.util.List;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;

public abstract class GuiExtension {

	@Getter
	@Setter(value = AccessLevel.PACKAGE)
	private boolean extending;
	private int currentW = getMinimumWidth();
	private int currentH = getMinimumHeight();
	@Getter
	private int currentXPos = 0;
	@Getter
	private int currentYPos = 0;
	private int targetYPos = 0;
	private boolean init = true;
	private List<Integer> slotList = new ArrayList<>();
	private List<Integer> buttonList = new ArrayList<>();

	public abstract int getFinalWidth();

	public abstract int getFinalHeight();

	public abstract void renderForeground(int left, int top);

	public final void update(int xPos, int yPos) {
		currentXPos = xPos;
		if (yPos > currentYPos + 1 && !init) {
			currentYPos += 2;
		} else if (yPos < currentYPos - 1 && !init) {
			currentYPos -= 2;
		} else {
			currentYPos = yPos;
		}
		targetYPos = yPos;
		init = false;
		if (extending) {
			if (currentH < getFinalHeight()) {
				currentH += 4;
			} else {
				currentH = getFinalHeight();
			}
			if (currentW < getFinalWidth()) {
				currentW += 2;
			} else {
				currentW = getFinalWidth();
			}
		} else {
			if (currentH > getMinimumHeight()) {
				currentH -= 4;
			} else {
				currentH = getMinimumHeight();
			}
			if (currentW > getMinimumWidth()) {
				currentW -= 2;
			} else {
				currentW = getMinimumWidth();
			}
		}
	}

	public int getMinimumWidth() {
		return 23;
	}

	public int getMinimumHeight() {
		return 26;
	}

	public int getCurrentWidth() {
		return currentW;
	}

	public int getCurrentHeight() {
		return currentH;
	}

	public boolean isFullyExtended() {
		return currentW == getFinalWidth() && currentH == getFinalHeight() && targetYPos == currentYPos;
	}

	public boolean isFullyRetracted() {
		return currentW == getMinimumWidth() && currentH == getMinimumHeight() && targetYPos == currentYPos;
	}

	public void handleMouseOverAt(int xPos, int yPos) {}

	public void registerSlot(int id) {
		slotList.add(id);
	}

	public boolean renderSlot(int id) {
		return slotList.contains(id);
	}

	public boolean renderSelectSlot(int id) {
		return slotList.contains(id);
	}

	public void registerButton(int id) {
		buttonList.add(id);
	}

	public boolean renderButton(int id) {
		return buttonList.contains(id);
	}
}
