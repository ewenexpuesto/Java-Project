package utils;

import java.util.logging.Level;
import java.util.logging.Logger;

import javafx.scene.image.Image;
import logger.LoggerFactory;

/**
 * Factory providing {@link Image} icons in order to reuse frequently used images
 * @author davidroussel
 */
public class IconFactory
{
	/**
	 * Symbolic constants for icon size
	 */
	public enum IconSize
	{
		/**
		 * Symbol for small icons: 16x16
		 */
		SMALL,
		/**
		 * Symbol for small icons: 32x32
		 */
		MEDIUM,
		/**
		 * Symbol for small icons: 64x64
		 */
		LARGE;

		/**
		 * Actual numeric size
		 * @return the actual width and height corresponding to this symbolic size
		 * @throws AssertionError for unexpected enum value
		 */
		public int size() throws AssertionError
		{
			switch (this)
			{
				case SMALL:
					return 16;
				case MEDIUM:
					return 32;
				case LARGE:
					return 48;
				default:
					throw new AssertionError("Unexpected value: " + this);
			}
		}
		/*
		 * Enums have a default final hashCode method which can not be overridden
		 * but can be used
		 */
	}
	/**
	 * Path beginning for all searched images
	 */
	private final static String ImagePrefix = "icons/";

	/**
	 * Path end for all searched images
	 */
	private final static String ImagePostfix = ".png";

	/**
	 * La factory stockant et fournissant les icônes
	 */
	static private FlyweightFactory<Image> iconFactory =
		new FlyweightFactory<Image>();

	/**
	 * Logger from {@link #iconFactory}
	 */
	static private Logger logger = LoggerFactory
	    .getParentLogger(IconFactory.class,
	                     iconFactory.getLogger(),
	                     (iconFactory .getLogger() == null ?
	                      Level.INFO : null)); // null level to inherit parent logger's level

	/**
	 * Factory method retrieving an small Image icon (16x16) based on a provided
	 * icon name
	 * @param name The name of the icon to search for (e.g. "Circle" will trigger
	 * a search for "Circle-16.png" file)
	 * @return The image corresponding to this name or null if there is no such
	 * image.
	 */
	public static Image getSmallIcon(String name)
	{
		return getIcon(name, IconSize.SMALL);
	}

	/**
	 * Factory method retrieving an medium Image icon (32x32) based on a provided
	 * icon name
	 * @param name The name of the icon to search for (e.g. "Circle" will trigger
	 * a search for "Circle-32.png" file)
	 * @return The image corresponding to this name or null if there is no such
	 * image.
	 */
	public static Image getMediumIcon(String name)
	{
		return getIcon(name, IconSize.MEDIUM);
	}

	/**
	 * Factory method retrieving an large Image icon (48x48) based on a provided
	 * icon name
	 * @param name The name of the icon to search for (e.g. "Circle" will trigger
	 * a search for "Circle-48.png" file)
	 * @return The image corresponding to this name or null if there is no such
	 * image.
	 */
	public static Image getLargeIcon(String name)
	{
		return getIcon(name, IconSize.LARGE);
	}

	/**
	 * Factory method retrieving an Image icon based on a provided icon name
	 * @param name The name of the icon to search for (e.g. "Circle" will trigger
	 * a search for "Circle-{16|32|48}.png" file)
	 * @param size The required image size
	 * @return The image corresponding to this name or null if there is no such
	 * image.
	 */
	public static Image getIcon(String name, IconSize size)
	{
		if ((name == null) || name.isEmpty())
		{
			logger.severe("<EMPTY or NULL NAME>");
			return null;
		}

		int hash = name.hashCode() + (31 * size.hashCode());
		Image icon = iconFactory.get(hash);

		if (icon == null)
		{
			StringBuilder sb = new StringBuilder();
			sb.append(ImagePrefix);
			sb.append(name);
			sb.append('-');
			sb.append(size.size());
			sb.append(ImagePostfix);
			String fileName = new String(sb.toString());
			try
			{
				icon = new Image(fileName);
			}
			catch (IllegalArgumentException iae)
			{
				logger.severe(name + ": couldn't load file " + fileName);
			}

			if ((icon != null) && !icon.isError())
			{
				iconFactory.put(hash, icon);
			}
			icon = iconFactory.get(hash);
		}
		return icon;
	}

	/**
	 * Logger accessor
	 * @return The current logger of this factory
	 */
	public static Logger getLogger()
	{
		return logger;
	}
}
