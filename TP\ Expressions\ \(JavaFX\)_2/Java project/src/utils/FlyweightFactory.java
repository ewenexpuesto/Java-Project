package utils;

import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import logger.LoggerFactory;

/**
 * Flyweight factory to manage frequently used elements such as
 * {@link javafx.scene.paint.Color}s
 * or {@link javafx.scene.image.Image}s.
 * Elements are stored in a {@link HashMap}&lt;Integer, T&gt; where the
 * {@link Integer} key is the hashCode of stored element.
 * When an element is requested from this factory, a search is performed within
 * {@link #map} to find such element. If not present, the element is first added
 * to {@link #map} then returned from {@link #map}.
 * @author davidroussel
 * @param <T> The type of elements stored in this factory
 */
public class FlyweightFactory<T>
{
	/**
	 * {@link HashMap} storing elements
	 */
	protected HashMap<Integer, T> map;

	/**
	 * Logger to display messages (might be null)
	 */
	Logger logger;

	/**
	 * Constructor with parentLogger.
	 * Allocates the {@link #map}
	 * @param parentLogger The parent logger
	 */
	public FlyweightFactory(Logger parentLogger)
	{
		map = new HashMap<Integer, T>();
		logger = LoggerFactory.getParentLogger(getClass(),
		                                       parentLogger,
		                                       (parentLogger == null ?
		                                    	Level.INFO : null)); // null level to inherit parent logger's level
	}

	/**
	 * Default Constructor
	 * Allocates the {@link #map}
	 */
	public FlyweightFactory()
	{
		this(null);
	}

	/**
	 * Element accessor from hash code rather than element itself
	 * @param hash The hash code of the requested element
	 * @return The element corresponding to the provided hash code or null if
	 * there is no such element in this factory.
	 * @apiNote This method is required for storing elements that do not provide
	 * a valid hash code. In such cases we should provide the missing hash code.
	 */
	protected T get(int hash)
	{
		Integer key = Integer.valueOf(hash);
		if (map.containsKey(key))
		{
			return map.get(key);
		}

		return null;
	}

	/**
	 * Adds a new element pair &lt;Hash, Value&gt; to {@link #map}
	 * @param hash The hash code of the element to add
	 * @param element The element to add
	 * @return True if such element was not already present in the {@link #map}
	 * and has been added to the {@link #map}, false otherwise.
	 */
	protected boolean put(int hash, T element)
	{
		Integer key = Integer.valueOf(hash);
		if (!map.containsKey(key))
		{
			if (element != null)
			{
				map.put(key, element);
				return true;
			}
			else
			{
				logger.severe("null element");
			}
		}
		return false;
	}

	/**
	 * Element accessor from element prototype
	 * @param element The searched element
	 * @return The required element from the {@link HashMap}, which might be
	 * a different instance from element but with the same content.
	 */
	public T get(T element)
	{
		if (element != null)
		{
			int hash = element.hashCode();
			T result = get(hash);
			if (result == null)
			{
				put(hash, element);
				result = get(hash);
			}
			return result;
		}
		return null;
	}

	/**
	 * Content cleanup
	 */
	public void clear()
	{
		map.clear();
	}

	/**
	 * Logger accessor
	 * @return The logger
	 */
	public Logger getLogger()
	{
		return logger;
	}

	/**
	 * Cleanup before destruction
	 */
	@Override
	protected void finalize()
	{
		clear();
	}
}
