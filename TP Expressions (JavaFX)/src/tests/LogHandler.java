package tests;

import java.util.Deque;
import java.util.LinkedList;
import java.util.logging.Handler;
import java.util.logging.LogRecord;

/**
 * Log Handler to record a few Logger records.
 * this handler can be used to assert log messages have been issued
 * @author davidroussel
 */
public class LogHandler extends Handler
{

	/**
	 * Maximum number of records to keep in this handler
	 */
	int maxSize;

	/**
	 * Stack of records kept by this handler
	 */
	private Deque<LogRecord> records = null;

	/**
	 * Constructor
	 * @param size the number of records to keep
	 */
	public LogHandler(int size)
	{
		maxSize = size;
		records = new LinkedList<>();
	}

	/**
	 * Publish a {@code LogRecord}.
	 * <p>
	 * The logging request was made initially to a {@code Logger} object,
	 * which initialized the {@code LogRecord} and forwarded it here.
	 * <p>
	 * The {@code Handler} is responsible for formatting the message, when and
	 * if necessary. The formatting should include localization.
	 * @param record description of the log event. A null record is
	 * silently ignored and is not published
	 */
	@Override
	public void publish(LogRecord record)
	{
		records.push(record);
		if (records.size() > maxSize)
		{
			records.removeLast();
		}
	}

    /**
     * Flush any buffered output.
     */
	@Override
	public void flush()
	{
		// TODO Auto-generated method stub
	}

	/**
	 * Close the {@code Handler} and free all associated resources.
	 * <p>
	 * The close method will perform a {@code flush} and then close the
	 * {@code Handler}. After close has been called this {@code Handler}
	 * should no longer be used. Method calls may either be silently
	 * ignored or may throw runtime exceptions.
	 * @throws SecurityException if a security manager exists and if
	 * the caller does not have {@code LoggingPermission("control")}.
	 */
	@Override
	public void close() throws SecurityException
	{
		records.clear();
	}

	/**
	 * {@link #records} emptiness accessor
	 * @return true if {@link #records} is empty
	 * @see java.util.Collection#isEmpty()
	 */
	public boolean isEmpty()
	{
		return records.isEmpty();
	}

	/**
	 * Peek last added record without removing it
	 * @return the last added record
	 * @see java.util.Deque#peek()
	 */
	public LogRecord peek()
	{
		return records.peek();
	}

	/**
	 * Pop last added record
	 * @return the last added record
	 * @see java.util.Deque#pop()
	 */
	public LogRecord pop()
	{
		return records.pop();
	}

	/**
	 * Number of records in this handler
	 * @return the number of records in this handler
	 * @see java.util.Deque#size()
	 */
	public int size()
	{
		return records.size();
	}

	/**
	 * Clear all {@link #records}
	 * @see java.util.Collection#clear()
	 */
	public void clear()
	{
		records.clear();
	}
}
