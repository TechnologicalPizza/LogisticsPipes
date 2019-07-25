package logisticspipes.utils.gui;

public interface IGuiScrollable extends IGuiScrollArea {

	void scrollUp();

	void scrollDown();

	int getMaxScrollValue();

	int getScrollValue();

	void setScrollValue(int value);
}