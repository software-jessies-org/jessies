package terminator;

import e.forms.*;
import e.gui.*;
import e.util.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.awt.image.*;
import java.io.*;
import java.util.*;
import java.util.List;
import java.util.regex.*;
import javax.swing.*;
import javax.swing.event.*;

/**
 * Reads in settings from the file system and makes them conveniently
 * available.
 * 
 * There's a grand tradition amongst Unix terminal emulators of pretending
 * to be XTerm, but that didn't work out well for us, because we're too
 * different.
 * 
 * All settings should have a default, so that users can use "--help" to see
 * every available option, and edit them in the preferences dialog.
 */
public class Options {
	private static final String ALPHA = "alpha";
	private static final String ANTI_ALIAS = "antiAlias";
	private static final String BLOCK_CURSOR = "blockCursor";
	private static final String CURSOR_BLINK = "cursorBlink";
	private static final String FANCY_BELL = "fancyBell";
	private static final String VISUAL_BELL = "visualBell";
	private static final String FONT_NAME = "fontName";
	private static final String FONT_SIZE = "fontSize";
	private static final String HIDE_MOUSE_WHEN_TYPING = "hideMouseWhenTyping";
	private static final String INITIAL_COLUMN_COUNT = "initialColumnCount";
	private static final String INITIAL_ROW_COUNT = "initialRowCount";
	private static final String SCROLL_KEY = "scrollKey";
	private static final String SCROLL_TTY_OUTPUT = "scrollTtyOutput";
	private static final String USE_ALT_AS_META = "useAltAsMeta";
	private static final String USE_MENU_BAR = "useMenuBar";
	
	private final Pattern resourcePattern = Pattern.compile("(?:Terminator(?:\\*|\\.))?(\\S+):\\s*(.+)");
	
	private static final Color LIGHT_BLUE = new Color(0xb3d4ff);
	private static final Color NEAR_BLACK = new Color(0x181818);
	private static final Color NEAR_GREEN = new Color(0x72ff00);
	private static final Color NEAR_WHITE = new Color(0xeeeeee);
	private static final Color SELECTION_BLUE = new Color(0x1c2bff);
	private static final Color VERY_DARK_BLUE = new Color(0x000045);
	
	private static final Options INSTANCE = new Options();
	
	// Mutable at any time.
	private HashMap<String, Object> options = new HashMap<String, Object>();
	// Immutable after initialization.
	private HashMap<String, Object> defaults = new HashMap<String, Object>();
	private HashMap<String, String> descriptions = new HashMap<String, String>();
	
	private HashMap<String, Color> rgbColors = null;
	
	// Non-null if the preferences dialog is currently showing.
	private FormBuilder form;
	
	public static Options getSharedInstance() {
		return INSTANCE;
	}
	
	public void writeOptions(Appendable out, boolean showEvenIfDefault) throws IOException {
		String[] keys = options.keySet().toArray(new String[options.size()]);
		Arrays.sort(keys);
		for (String key : keys) {
			Object value = options.get(key);
			
			if (value.equals(defaults.get(key)) && showEvenIfDefault == false) {
				continue;
			}
			
			String description = descriptions.get(key);
			if (description != null) {
				out.append("\n# " + description + "\n");
			}
			if (value instanceof Color) {
				value = colorToString((Color) value);
			} else if (value instanceof Font) {
				value = Font.class.cast(value).getFamily();
			}
			out.append("Terminator*" + key + ": " + value + "\n");
		}
	}
	
	private static String colorToString(Color color) {
		return String.format("#%06x", color.getRGB() & 0xffffff);
	}
	
	/**
	 * Whether or not pressing a key should cause the the scrollbar to go
	 * to the bottom of the scrolling region.
	 */
	public boolean isScrollKey() {
		return booleanResource(SCROLL_KEY);
	}
	
	/**
	 * Whether or not output to the terminal should cause the scrollbar to
	 * go to the bottom of the scrolling region.
	 */
	public boolean isScrollTtyOutput() {
		return booleanResource(SCROLL_TTY_OUTPUT);
	}
	
	/**
	 * Whether or not to anti-alias text.
	 */
	public boolean isAntiAliased() {
		return booleanResource(ANTI_ALIAS);
	}
	
	/**
	 * Whether to use a block cursor instead of an underline cursor.
	 */
	public boolean isBlockCursor() {
		return booleanResource(BLOCK_CURSOR);
	}
	
	/**
	 * Whether or not to use a nicer-looking but more expensive visual
	 * bell rendition. If there were a way to detect a remote X11 display --
	 * the only place where we really need to disable this -- we could
	 * remove this option, but I don't know of one.
	 */
	public boolean isFancyBell() {
		return booleanResource(FANCY_BELL);
	}
	
	/**
	 * Whether to do nothing when asked to flash.
	 */
	public boolean isVisualBell() {
		return booleanResource(VISUAL_BELL);
	}
	
	/**
	 * Whether or not to blink the cursor.
	 */
	public boolean shouldCursorBlink() {
		return booleanResource(CURSOR_BLINK);
	}
	
	/**
	 * Whether or not to use a menu bar.
	 */
	public boolean shouldUseMenuBar() {
		return booleanResource(USE_MENU_BAR);
	}
	
	/**
	 * Whether or not the alt key should be meta.
	 * If true, you can't use alt as part of your system's input method.
	 * If false, you can't comfortably use emacs(1).
	 */
	public boolean shouldUseAltKeyAsMeta() {
		return booleanResource(USE_ALT_AS_META);
	}
	
	public boolean shouldHideMouseWhenTyping() {
		return booleanResource(HIDE_MOUSE_WHEN_TYPING);
	}
	
	/**
	 * How many rows a new window should have.
	 */
	public int getInitialRowCount() {
		return integerResource(INITIAL_ROW_COUNT);
	}
	
	/**
	 * How many columns a new window should have.
	 */
	public int getInitialColumnCount() {
		return integerResource(INITIAL_COLUMN_COUNT);
	}
	
	public double getAlpha() {
		return doubleResource(ALPHA);
	}
	
	private double doubleResource(String name) {
		return (Double) options.get(name);
	}
	
	private int integerResource(String name) {
		return (Integer) options.get(name);
	}
	
	private boolean booleanResource(String name) {
		return (Boolean) options.get(name);
	}
	
	/**
	 * Returns a color, if explicitly configured by the user.
	 * We understand colors specified in the #rrggbb form,
	 * or those parsed from rgb.txt.
	 * 
	 * Color names supported by xterm (defaults in parentheses) include:
	 * 
	 *  background (white)
	 *  foreground (black)
	 *  cursorColor (black; we use green)
	 *  highlightColor (reverse video)
	 *  pointerColor
	 *  pointerBackgroundColor
	 * 
	 * xterm also offers complete control over all the ECMA colors.
	 */
	public Color getColor(String name) {
		return (Color) options.get(name);
	}
	
	private Color colorFromString(String description) {
		if (description.startsWith("#")) {
			return Color.decode("0x" + description.substring(1));
		} else {
			return getRgbColor(description);
		}
	}
	
	/**
	 * Returns a suitable fixed font (ignoring all X11 configuration,
	 * because we're unlikely to understand those font specifications).
	 * So we don't get into trouble with Xterm's font resource, and
	 * to work around Font.decode's weaknesses, we use two resources:
	 * "fontName" and "fontSize".
	 */
	public Font getFont() {
		return new Font(Font.class.cast(options.get(FONT_NAME)).getFamily(), Font.PLAIN, integerResource(FONT_SIZE));
	}
	
	private Options() {
		initDefaults();
		initDefaultColors();
		readStoredOptions();
		updateColorBD();
	}
	
	/**
	 * Parses "-xrm <resource-string>" options from an array of
	 * command-line arguments, returning the remaining arguments as
	 * a List.
	 */
	public List<String> parseCommandLine(String[] arguments) {
		ArrayList<String> otherArguments = new ArrayList<String>();
		for (int i = 0; i < arguments.length; ++i) {
			if (arguments[i].equals("-xrm")) {
				String resourceString = arguments[++i];
				processResourceString(resourceString);
			} else {
				otherArguments.add(arguments[i]);
			}
		}
		return otherArguments;
	}
	
	private void addDefault(String name, Object value, String description) {
		defaults.put(name, value);
		options.put(name, value);
		descriptions.put(name, description);
	}
	
	private Font makePrototypeFont(String familyName) {
		return new Font(familyName, Font.PLAIN, 1);
	}
	
	/**
	 * Sets the defaults for non-color options.
	 */
	private void initDefaults() {
		addDefault(ALPHA, Double.valueOf(1.0), "Terminal opacity");
		addDefault(ANTI_ALIAS, Boolean.FALSE, "Anti-alias text?");
		addDefault(BLOCK_CURSOR, Boolean.FALSE, "Use block cursor?");
		addDefault(CURSOR_BLINK, Boolean.TRUE, "Blink cursor?");
		addDefault(FANCY_BELL, Boolean.TRUE, "High-quality rendering of the visual bell?");
		addDefault(VISUAL_BELL, Boolean.TRUE, "Visual bell (as opposed to no bell)?");
		addDefault(FONT_NAME, makePrototypeFont(GuiUtilities.getMonospacedFontName()), "Font family");
		addDefault(FONT_SIZE, Integer.valueOf(12), "Font size (points)");
		addDefault(HIDE_MOUSE_WHEN_TYPING, Boolean.TRUE, "Hide mouse when typing");
		addDefault(INITIAL_COLUMN_COUNT, Integer.valueOf(80), "New terminal width");
		addDefault(INITIAL_ROW_COUNT, Integer.valueOf(24), "New terminal height");
		addDefault(SCROLL_KEY, Boolean.TRUE, "Scroll to bottom on key press?");
		addDefault(SCROLL_TTY_OUTPUT, Boolean.FALSE, "Scroll to bottom on output?");
		addDefault(USE_ALT_AS_META, Boolean.FALSE, "Use alt key as meta key (for Emacs)?");
		
		if (GuiUtilities.isMacOs() || GuiUtilities.isWindows() || GuiUtilities.isGtk()) {
			// GNOME, Mac, and Win32 users are accustomed to every window having a menu bar.
			options.put(USE_MENU_BAR, Boolean.TRUE);
			defaults.put(USE_MENU_BAR, Boolean.TRUE);
		} else {
			// FIXME: I'm still psyching myself up for the inevitable battle of removing this option.
			addDefault(USE_MENU_BAR, Boolean.TRUE, "Use a menu bar?");
		}
	}
	
	public synchronized void showPreferencesDialog(Frame parent) {
		// We can't keep reusing a form that we create just once, because you can't change the owner of an existing JDialog.
		// But we don't want to pop up another dialog if one's already up, so defer to the last one if it's still up.
		if (form != null) {
			for (FormPanel panel : form.getFormPanels()) {
				if (panel.isShowing()) {
					((JDialog) SwingUtilities.getAncestorOfClass(JDialog.class, panel)).toFront();
					return;
				}
			}
		}
		
		this.form = new FormBuilder(parent, "Preferences", new String[] { "General", "Colors" });
		List<FormPanel> formPanels = form.getFormPanels();
		FormPanel generalPanel = formPanels.get(0);
		FormPanel colorsPanel = formPanels.get(1);
		Map<String, ColorPreference> colorPreferences = new HashMap<String, ColorPreference>();
		
		// Offer various preset color combinations.
		// Note that these get fossilized into the user's preferences; updating values here doesn't affect users who've already clicked the button.
		colorsPanel.addRow("Presets:", makePresetButton(colorPreferences, "  Terminator  ", VERY_DARK_BLUE, NEAR_WHITE, Color.GREEN, SELECTION_BLUE));
		colorsPanel.addRow("", makePresetButton(colorPreferences, "Black on White", Color.WHITE, NEAR_BLACK, Color.BLUE, LIGHT_BLUE));
		colorsPanel.addRow("", makePresetButton(colorPreferences, "Green on Black", Color.BLACK, NEAR_GREEN, Color.GREEN, SELECTION_BLUE));
		colorsPanel.addRow("", makePresetButton(colorPreferences, "White on Black", Color.BLACK, NEAR_WHITE, Color.GREEN, Color.DARK_GRAY));
		
		String[] keys = options.keySet().toArray(new String[options.size()]);
		Arrays.sort(keys);
		for (String key : keys) {
			String description = descriptions.get(key);
			if (description != null) {
				Object value = options.get(key);
				if (value instanceof Boolean) {
					generalPanel.addRow("", new BooleanPreference(key).makeUi());
				} else if (value instanceof Integer) {
					generalPanel.addRow(description + ":", new IntegerPreference(key).makeUi());
				} else if (value instanceof Font) {
					generalPanel.addRow(description + ":", new FontPreference(key).makeUi());
				} else if (value instanceof Color) {
					ColorPreference colorPreference = new ColorPreference(key);
					colorPreferences.put(key, colorPreference);
					colorsPanel.addRow(description + ":", colorPreference.makeUi());
				} else if (value instanceof Double) {
					colorsPanel.addRow(description + ":", new DoublePreference(key).makeUi());
				} else {
					// FIXME: we should probably handle String.
					// FIXME: the Final Solution should use a HashMap<Class, XPreference>.
					// FIXME: the preference classes should be based on a base class extracted from ColorPreference, rather than the older Action scheme.
					continue;
				}
			}
		}
		
		// Save the preferences if the user hits "Save".
		form.getFormDialog().setAcceptCallable(new java.util.concurrent.Callable<Boolean>() {
			public Boolean call() {
				File optionsFile = getOptionsFile();
				boolean saved = writeOptionsTo(optionsFile);
				if (saved == false) {
					SimpleDialog.showAlert(null, "Couldn't save preferences.", "There was a problem writing preferences to \"" + optionsFile + "\".");
				} else {
					form = null;
				}
				return saved;
			}
		});
		
		// Restore the preferences if the user hits "Cancel".
		final HashMap<String, Object> initialOptions = new HashMap<String, Object>(options);
		form.getFormDialog().setCancelRunnable(new Runnable() {
			public void run() {
				options = initialOptions;
				Terminator.getSharedInstance().optionsDidChange();
			}
		});
		
		form.getFormDialog().setRememberBounds(false);
		form.show("Save");
	}
	
	private BufferedImage makeEmptyPresetButtonImageToFit(String name) {
		// This seems ugly and awkward, but I can't think of a simpler way to get a suitably-sized BufferedImage to work with, without hard-coding dimensions.
		BufferedImage image = new BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB);
		Graphics2D g = image.createGraphics();
		g.setFont(getFont());
		FontMetrics metrics = g.getFontMetrics();
		final int height = (int) ((double) metrics.getHeight() * 1.4);
		final int width = (int) (metrics.getStringBounds(name, g).getWidth() * 1.2);
		g.dispose();
		return new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
	}
	
	private BufferedImage makePresetButtonImage(String name, Color background, Color foreground) {
		// Make a representative image for the button.
		BufferedImage image = makeEmptyPresetButtonImageToFit(name);
		Graphics2D g = image.createGraphics();
		g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, isAntiAliased() ? RenderingHints.VALUE_TEXT_ANTIALIAS_ON : RenderingHints.VALUE_TEXT_ANTIALIAS_OFF);
		g.setFont(getFont());
		g.setColor(background);
		g.fillRect(0, 0, image.getWidth(), image.getHeight());
		g.setColor(background.darker());
		g.drawRect(0, 0, image.getWidth() - 1, image.getHeight() - 1);
		g.setColor(foreground);
		Rectangle2D stringBounds = g.getFontMetrics().getStringBounds(name, g);
		final int x = (image.getWidth() - (int) stringBounds.getWidth()) / 2;
		int y = (image.getHeight() - (int) stringBounds.getHeight()) / 2 + g.getFontMetrics().getAscent();
		y = (int) ((double) y / 1.1);
		g.drawString(name, x, y);
		g.dispose();
		return image;
	}
	
	private JComponent makePresetButton(final Map<String, ColorPreference> colorPreferences, String name, final Color background, final Color foreground, final Color cursor, final Color selection) {
		// FIXME: we need to update the button image when the user changes the anti-aliasing preference.
		JButton button = new JButton(new ImageIcon(makePresetButtonImage(name, background, foreground)));
		button.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				colorPreferences.get("background").updatePreference(background);
				colorPreferences.get("foreground").updatePreference(foreground);
				colorPreferences.get("cursorColor").updatePreference(cursor);
				colorPreferences.get("selectionColor").updatePreference(selection);
				updateColorBD();
			}
		});
		return button;
	}
	
	private boolean writeOptionsTo(File file) {
		PrintWriter out = null;
		try {
			out = new PrintWriter(new OutputStreamWriter(new FileOutputStream(file), "UTF-8"));
			out.println("# This file is written automatically by Terminator's preferences dialog.");
			writeOptions(out, false);
			return true;
		} catch (IOException ex) {
			Log.warn("Problem writing options to \"" + file + "\"", ex);
			return false;
		} finally {
			FileUtilities.close(out);
		}
	}
	
	private class BooleanPreference {
		private String key;
		
		public BooleanPreference(String key) {
			this.key = key;
		}
		
		public JComponent makeUi() {
			final JCheckBox checkBox = new JCheckBox(descriptions.get(key), (Boolean) options.get(key));
			checkBox.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					options.put(key, checkBox.isSelected());
					Terminator.getSharedInstance().optionsDidChange();
				}
			});
			return checkBox;
		}
	}
	
	private class DoublePreference {
		private String key;
		
		public DoublePreference(String key) {
			this.key = key;
		}
		
		public JComponent makeUi() {
			// FIXME: this isn't suitable for all possible double preferences, but it's fine for opacity.
			final JSlider slider = new JSlider(0, 255);
			slider.setValue((int) (255 * (Double) options.get(key)));
			slider.addChangeListener(new ChangeListener() {
				public void stateChanged(ChangeEvent e) {
					options.put(key, ((double) slider.getValue())/256);
					Terminator.getSharedInstance().optionsDidChange();
				}
			});
			slider.setEnabled(GuiUtilities.isWindows() == false);
			return slider;
		}
	}
	
	private class FontPreference {
		private String key;
		
		public FontPreference(String key) {
			this.key = key;
		}
		
		public JComponent makeUi() {
			final JComboBox comboBox = new JComboBox();
			// FIXME: filter out unsuitable fonts. "Zapf Dingbats", for example.
			// FIXME: pull fixed fonts to the top of the list?
			for (String name : GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames()) {
				comboBox.addItem(name);
			}
			Font currentFont = (Font) options.get(key);
			comboBox.setSelectedItem(currentFont.getFamily());
			updateComboBoxFont(comboBox);
			comboBox.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					options.put(key, makePrototypeFont(comboBox.getSelectedItem().toString()));
					updateComboBoxFont(comboBox);
					Terminator.getSharedInstance().optionsDidChange();
				}
			});
			// updateComboBoxFont sets the combo box font so that when you choose a font you can see a preview.
			// The alternatives in the pop-up menu, though, should either use their own fonts (which has a habit of causing performance problems) or the default combo box pop-up font. This renderer ensures the latter.
			final ListCellRenderer defaultRenderer = comboBox.getRenderer();
			comboBox.setRenderer(new ListCellRenderer() {
				public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
					Component result = defaultRenderer.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
					if (index != -1) {
						result.setFont(UIManager.getFont("List.font"));
					}
					return result;
				}
			});
			return comboBox;
		}
		
		private void updateComboBoxFont(JComboBox comboBox) {
			Font prototypeFont = (Font) options.get(key);
			comboBox.setFont(prototypeFont.deriveFont(comboBox.getFont().getSize2D()));
		}
	}
	
	private class ColorPreference {
		private String key;
		private ColorSwatchIcon icon;
		private JButton button;
		
		public ColorPreference(String key) {
			this.key = key;
			this.icon = new ColorSwatchIcon(getColor(key), new Dimension(60, 20));
		}
		
		public JComponent makeUi() {
			this.button = new JButton(icon);
			button.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					Color newColor = JColorChooser.showDialog(button, "Colors", getColor(key));
					if (newColor != null) {
						updatePreference(newColor);
					}
				}
			});
			// Remove the over-wide horizontal margins most LAFs use. They're trying to give text some breathing room, but we have no text.
			Insets margin = button.getMargin();
			margin.left = margin.right = margin.top = margin.bottom;
			button.setMargin(margin);
			button.putClientProperty("JButton.buttonType", "toolbar");
			return button;
		}
		
		public void updatePreference(Color newColor) {
			options.put(key, newColor);
			icon.setColor(newColor);
			button.repaint();
			updateColorBD();
			Terminator.getSharedInstance().optionsDidChange();
		}
	}
	
	private class IntegerPreference {
		private String key;
		
		public IntegerPreference(String key) {
			this.key = key;
		}
		
		public JComponent makeUi() {
			final ETextField textField = new ETextField(options.get(key).toString()) {
				@Override
				public void textChanged() {
					boolean okay = false;
					try {
						int newValue = Integer.parseInt(getText());
						// FIXME: really, an integer preference should have an explicit range.
						if (newValue > 0) {
							options.put(key, newValue);
							Terminator.getSharedInstance().optionsDidChange();
							okay = true;
						}
					} catch (NumberFormatException ex) {
					}
					setForeground(okay ? UIManager.getColor("TextField.foreground") : Color.RED);
				}
			};
			return textField;
		}
	}
	
	/**
	 * color0 to color7 are the normal colors (black, red3, green3,
	 * yellow3, blue3, magenta3, cyan3, and gray90).
	 *
	 * color8 to color15 are the bold colors (gray30, red, green, yellow,
	 * blue, magenta, cyan, and white).
	 */
	private void initDefaultColors() {
		// Defaults reminiscent of SGI's xwsh(1).
		addDefault("background", VERY_DARK_BLUE, "Background");
		addDefault("cursorColor", Color.GREEN, "Cursor");
		addDefault("foreground", NEAR_WHITE, "Text foreground");
		addDefault("selectionColor", SELECTION_BLUE, "Selection background");
		
		// This will be overridden with an appropriate value.
		// It doesn't get a description, so it won't appear in the UI.
		defaults.put("colorBD", Color.BLACK);
		options.put("colorBD", Color.BLACK);
	}
	
	/**
	 * Tries to get a good bold foreground color.
	 */
	private void updateColorBD() {
		Color foreground = getColor("foreground");
		Color colorBD = null;
		
		// If the color is one of the "standard" colors, use the usual bold variant.
		for (int i = 0; i < 8; ++i) {
			Color color = AnsiColor.byIndex(i);
			if (foreground.equals(color)) {
				colorBD = AnsiColor.byIndex(i + 8);
				break;
			}
		}
		
		// If that didn't work, try to invent a suitable color.
		if (colorBD == null) {
			// The typical use of colorBD is to turn off-white into pure white or off-black.
			// One approach might be to use the NTSC or HDTV luminance formula, but it's not obvious that they generalize to other colors.
			// Adjusting each component individually if it's close to full-on or full-off is simple and seems like it might generalize.
			colorBD = new Color(adjustForBD(foreground.getRed()), adjustForBD(foreground.getGreen()), adjustForBD(foreground.getBlue()));
		}
		
		options.put("colorBD", colorBD);
	}
	
	private static int adjustForBD(int component) {
		// These limits are somewhat arbitrary "round" hex numbers.
		// 0x11 would be too close to the LCD "blacker than black".
		// The default XTerm normal-intensity and bold blacks differ by 0x30.
		if (component < 0x33) {
			return 0x00;
		} else if (component > 0xcc) {
			return 0xff;
		} else {
			return component;
		}
	}
	
	/**
	 * Returns the name of the first "rgb.txt" file it finds.
	 */
	private String findRgbDotTxt() {
		String[] possibleRgbDotTxtLocations = {
			"/etc/X11/rgb.txt", // Debian/Ubuntu.
			"/usr/share/X11/rgb.txt", // Xorg Debian.
			"/usr/lib/X11/rgb.txt", // Xorg Ubuntu.
			"/usr/X11R6/lib/X11/rgb.txt", // XFree86 Linux, Mac OS with X11 installed.
			"/usr/share/emacs/21.2/etc/rgb.txt", // Mac OS without X11 installed.
		};
		for (String possibleLocation : possibleRgbDotTxtLocations) {
			if (FileUtilities.exists(possibleLocation)) {
				return possibleLocation;
			}
		}
		return null;
	}
	
	private void readRgbFile(String rgbDotTxtFilename) {
		rgbColors = new HashMap<String, Color>();
		String[] lines = StringUtilities.readLinesFromFile(rgbDotTxtFilename);
		for (String line : lines) {
			if (line.length() == 0 || line.startsWith("!") || line.startsWith("#")) {
				// X11's "rgb.txt" uses !-commenting, but Emacs' copy uses #-commenting, and contains an empty line.
				continue;
			}
			int r = channelAt(line, 0);
			int g = channelAt(line, 4);
			int b = channelAt(line, 8);
			line = line.substring(12).trim();
			rgbColors.put(line.toLowerCase(), new Color(r, g, b));
		}
	}
	
	private Color getRgbColor(String description) {
		// FIXME: with Sun's JVM, com.sun.java.swing.plaf.gtk.XColors.lookupColor returns a Color for each color named in "rgb.txt". We should try that (via reflection) first, and only then resort to reading "rgb.txt".
		if (rgbColors == null) {
			final String filename = findRgbDotTxt();
			if (filename == null) {
				return null;
			}
			try {
				readRgbFile(filename);
			} catch (Exception ex) {
				Log.warn("Problem reading colors from \"" + filename + "\"", ex);
			}
		}
		return rgbColors.get(description.toLowerCase());
	}
	
	private int channelAt(String line, int offset) {
		return Integer.parseInt(line.substring(offset, offset + 3).trim());
	}
	
	private File getOptionsFile() {
		return FileUtilities.fileFromString(System.getProperty("org.jessies.terminator.optionsFile"));
	}
	
	private void readStoredOptions() {
		File optionsFile = getOptionsFile();
		
		// Cope with old-style stored options.
		// Added on 2007-01-14, so it had better be a long time from then before you remove this!
		File oldFile = FileUtilities.fileFromString("~/.terminator-settings");
		if (oldFile.exists()) {
			// Read the options from the old file...
			readOptionsFrom(oldFile);
			// ...write them out in the new style...
			boolean saved = writeOptionsTo(optionsFile);
			// ...and then remove the old file if that was successful.
			if (saved) {
				oldFile.delete();
			}
		}
		
		// Cope with new-style stored options.
		readOptionsFrom(optionsFile);
	}
	
	private void readOptionsFrom(File file) {
		if (file.exists() == false) {
			return;
		}
		try {
			String[] lines = StringUtilities.readLinesFromFile(file.toString());
			for (String line : lines) {
				line = line.trim();
				if (line.length() == 0 || line.startsWith("#")) {
					continue;
				}
				processResourceString(line);
			}
		} catch (Exception ex) {
			Log.warn("Problem reading options from \"" + file + "\"", ex);
		}
	}
	
	private void processResourceString(String resourceString) {
		Matcher matcher = resourcePattern.matcher(resourceString);
		if (matcher.find()) {
			String key = matcher.group(1);
			String value = matcher.group(2);
			Object currentValue = options.get(key);
			if (currentValue == null) {
				throw new RuntimeException("Attempt to set unknown resource \"" + key + "\" - terminator --help lists the supported resources");
			}
			Class currentClass = currentValue.getClass();
			if (currentClass == Boolean.class) {
				options.put(key, Boolean.valueOf(value));
			} else if (currentClass == Double.class) {
				options.put(key, Double.valueOf(value));
			} else if (currentClass == Font.class) {
				options.put(key, makePrototypeFont(value));
			} else if (currentClass == Integer.class) {
				options.put(key, Integer.valueOf(value));
			} else if (currentClass == Color.class) {
				options.put(key, colorFromString(value));
			} else {
				throw new RuntimeException("Resource \"" + key + "\" had default value " + currentValue + " of class " + currentClass);
			}
		}
	}
}
