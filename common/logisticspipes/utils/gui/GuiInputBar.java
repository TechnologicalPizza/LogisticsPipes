package logisticspipes.utils.gui;

import java.awt.Toolkit;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.util.Locale;

import logisticspipes.utils.Color;

import net.minecraft.client.gui.FontRenderer;

import org.lwjgl.input.Keyboard;

public class GuiInputBar {

	public enum Align {
		LEFT,
		CENTER,
		RIGHT
	}

	public int minNumber = 0;

	public String input1 = "";
	public String input2 = "";
	public String placeholder;

	private boolean isFocused;
	private boolean displayCursor = true;
	private long cursorOldSystemTime = 0;
	private int searchWidth;
	private boolean numberOnly;
	private Align align;

	private final FontRenderer fontRenderer;
	private final LogisticsBaseGuiScreen screen;
	private int left, top, height, width;

	public GuiInputBar(FontRenderer fontRenderer, LogisticsBaseGuiScreen screen, int left, int top, int width, int height) {
		this(fontRenderer, screen, left, top, width, height, true);
	}

	public GuiInputBar(FontRenderer fontRenderer, LogisticsBaseGuiScreen screen, int left, int top, int width, int height, boolean isFocused) {
		this(fontRenderer, screen, left, top, width, height, isFocused, false);
	}

	public GuiInputBar(FontRenderer fontRenderer, LogisticsBaseGuiScreen screen, int left, int top, int width, int height, boolean isFocused, boolean numberOnly) {
		this(fontRenderer, screen, left, top, width, height, isFocused, numberOnly, Align.LEFT);
	}

	public GuiInputBar(FontRenderer fontRenderer, LogisticsBaseGuiScreen screen, int left, int top, int width, int height, boolean isFocused, boolean numberOnly, Align align) {
		this.fontRenderer = fontRenderer;
		this.screen = screen;
		this.left = left;
		this.top = top;
		this.width = width;
		this.height = height;
		searchWidth = width - (int) (4.5f + (this.height - 9) / 2f);
		this.isFocused = isFocused;
		this.numberOnly = numberOnly;
		this.align = align;
	}

	public void reposition(int left, int top, int width, int heigth) {
		this.left = left;
		this.top = top;
		this.width = width;
		this.height = heigth;
		searchWidth = width - (int) (4.5f + (this.height - 9) / 2f);
	}

	public void renderGui() {
		if (isFocused()) {
			screen.drawRect(left + 0, top - 2, left + width - 0, top + height - 0, Color.BLACK);
			screen.drawRect(left + 1, top - 1, left + width - 1, top + height - 1, Color.WHITE);
		} else {
			screen.drawRect(left + 1, top - 1, left + width - 1, top + height - 1, Color.BLACK);
		}
		screen.drawRect(left + 2, top - 0, left + width - 2, top + height - 2, Color.DARKER_GREY);

		if (align == Align.RIGHT)
			fontRenderer.drawString(input1 + input2, left + 2 + (this.height - 9) / 2f + searchWidth - fontRenderer.getStringWidth(input1 + input2), top + (this.height - 9) / 2f, 0xFFFFFF, false);
		else if (align == Align.CENTER)
			fontRenderer.drawString(input1 + input2, left + 2 + (this.height - 9) / 2f + (searchWidth - fontRenderer.getStringWidth(input1 + input2)) / 2f, top + (this.height - 9) / 2f, 0xFFFFFF, false);
		else
			fontRenderer.drawString(input1 + input2, left + 2 + (this.height - 9) / 2f, top + (this.height - 9) / 2f, 0xFFFFFF, false);

		if(getContentLength() == 0) {
			if (align == Align.RIGHT)
				fontRenderer.drawString(placeholder, left + 2 + (this.height - 9) / 2f + searchWidth - fontRenderer.getStringWidth(placeholder), top + (this.height - 9) / 2f, 0x778888, true);
			else if (align == Align.CENTER)
				fontRenderer.drawString(placeholder, left + 2 + (this.height - 9) / 2f + (searchWidth - fontRenderer.getStringWidth(placeholder)) / 2f, top + (this.height - 9) / 2f, 0x778888, true);
			else
				fontRenderer.drawString(placeholder, left + 2 + (this.height - 9) / 2f, top + (this.height - 9) / 2f, 0x778888, true);
		}

		if (isFocused()) {
			float lineX;
			if (align == Align.RIGHT)
				lineX = left + 2 + (this.height - 9) / 2f + searchWidth - fontRenderer.getStringWidth(input2);
			else if (align == Align.CENTER)
				lineX = left + 2 + (this.height - 9) / 2f + (searchWidth - fontRenderer.getStringWidth(input2)) / 2f + (fontRenderer.getStringWidth(input1)) / 2f;
			else
				lineX = left + 2 + (this.height - 9) / 2f + fontRenderer.getStringWidth(input1);

			if (System.currentTimeMillis() - cursorOldSystemTime > 500) {
				displayCursor = !displayCursor;
				cursorOldSystemTime = System.currentTimeMillis();
			}

			if (displayCursor)
				screen.drawRect((int) (lineX), top + 1, (int) (lineX + 1), top + height - 3, Color.WHITE);
		}
	}

	/**
	 * @return Boolean, true if click was handled.
	 */
	public boolean handleClick(int x, int y, int button) {
		if (x >= left + 2 && x < left + width - 2 && y >= top && y < top + height) {
			focus();
			if (button == 1) {
				input1 = "";
				input2 = "";
			}
			return true;
		} else if (isFocused()) {
			unfocus();
			return true;
		}
		return false;
	}

	private void unfocus() {
		isFocused = false;
		if (numberOnly) {
			input1 += input2;
			input2 = "";
			try {
				int value = Integer.valueOf(input1);
				value = Math.max(value, minNumber);
				input1 = Integer.toString(value);
			} catch (Exception e) {
				input1 = "";
			}
			if (input1.isEmpty() && input2.isEmpty()) {
				input1 = Integer.toString(minNumber);
			}
		}
	}

	private void focus() {
		isFocused = true;
	}

	public boolean isFocused() {
		return isFocused;
	}

	/**
	 * @return Boolean, true if key was handled.
	 */
	public boolean handleKey(char c, int i) {
		if (!isFocused() || i == 1)
			return false;

		if (c == 13 || i == 28) { //Enter
			unfocus();
		} else if (c == 8 || (i == 14 && System.getProperty("os.name").toLowerCase(Locale.US).contains("mac"))) { //Backspace
			if (input1.length() > 0) {
				input1 = input1.substring(0, input1.length() - 1);
			}
		} else if (i == 203) { //Left
			if (input1.length() > 0) {
				input2 = input1.substring(input1.length() - 1) + input2;
				input1 = input1.substring(0, input1.length() - 1);
			}
		} else if (i == 205) { //Right
			if (input2.length() > 0) {
				input1 += input2.substring(0, 1);
				input2 = input2.substring(1);
			}
		} else if (i == 199) { //Home
			input2 = input1 + input2;
			input1 = "";
		} else if (i == 207) { //End
			input1 = input1 + input2;
			input2 = "";
		} else if (i == 211) { //Del
			if (input2.length() > 0) {
				input2 = input2.substring(1);
			}
		} else if (i == 47 && Keyboard.isKeyDown(Keyboard.KEY_LCONTROL)) { //Ctrl-v
			boolean isFine = true;
			if (numberOnly) {
				try {
					Integer.valueOf(GuiInputBar.getClipboardString());
				} catch (Exception e) {
					isFine = false;
				}
			}
			if (isFine) {
				String toAdd = GuiInputBar.getClipboardString();
				while (fontRenderer.getStringWidth(input1 + toAdd + input2) > searchWidth) {
					toAdd = toAdd.substring(0, toAdd.length() - 1);
				}
				input1 = input1 + toAdd;
			}
		} else if ((!numberOnly && !Character.isISOControl(c)) || (numberOnly && Character.isDigit(c))) {
			if (fontRenderer.getStringWidth(input1 + c + input2) <= searchWidth) {
				input1 += c;
			}
		} else {
			//ignore this key/character
			return false;
		}

		// reset cursor visibility while typing
		displayCursor = true;
		cursorOldSystemTime = System.currentTimeMillis();

		return true;
	}

	public String getContent() {
		return input1 + input2;
	}

	public int getContentLength() {
		return input1.length() + input2.length();
	}

	public boolean isEmpty() {
		return input1.isEmpty() && input2.isEmpty();
	}

	private static String getClipboardString() {
		try {
			Transferable transferable = Toolkit.getDefaultToolkit().getSystemClipboard().getContents(null);
			if (transferable != null && transferable.isDataFlavorSupported(DataFlavor.stringFlavor)) {
				return (String) transferable.getTransferData(DataFlavor.stringFlavor);
			}
		} catch (Exception ignore) {
		}
		return "";
	}
}
