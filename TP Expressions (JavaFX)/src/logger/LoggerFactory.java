package logger;

import java.io.IOException;
import java.util.logging.FileHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

/**
 * Logger Factory
 * @author davidroussel
 */
public class LoggerFactory
{
	/**
	 * Factory method for a console logger
	 * @param <E> the type of client to get logger
	 * @param client The logger's client class, used to provide name to logger
	 * @param level Min log level (e.g. FINE, INFO, WARNING, SEVERE)
	 * @return A simple console logger
	 * @throws IOException If the getLogger operation fails
	 */
	public static <E> Logger getConsoleLogger(Class<E> client, Level level)
	    throws IOException
	{
		Logger logger = null;
		try
		{
			logger = getLogger(client, true, null, false, null, level);
		}
		catch (IOException e)
		{
			System.err.println("getConsoleLogger: impossible file IO error");
			throw e;
		}

		return logger;
	}

	/**
	 * Factory method for a child logger
	 * @param <E> The type of client to the logger
	 * @param client The logger's client class, used to provide name to logger
	 * @param parentLogger The parent logger (if any)
	 * @param level Min log level (e.g. FINE, INFO, WARNING, SEVERE)
	 * @return A child logger to the parent logger
	 */
	public static <E> Logger getParentLogger(Class<E> client,
	                                         Logger parentLogger,
	                                         Level level)
	{
		Logger logger = null;
		Logger parent;
		if (parentLogger == null)
		{
			parent  = Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);
		}
		else
		{
			parent = parentLogger;
		}
		try
		{
			logger = getLogger(client, true, null, false, parent, level);
		}
		catch (IOException e)
		{
			System.err.println("getParentLogger: impossible file IO error");
			e.printStackTrace();
			System.exit(e.hashCode());
		}

		return logger;
	}

	/**
	 * Factory method for a file logger
	 * @param <E> The type of client to the logger
	 * @param client The logger's client class, used to provide name to logger
	 * @param fileName File name to log in
	 * @param xmlFormat Flag to format output with XML
	 * @param level Min log level (e.g. FINE, INFO, WARNING, SEVERE)
	 * @return A file logger
	 * @throws IOException If the file could not be opened
	 */
	public static <E> Logger getFileLogger(Class<E> client,
	                                       String fileName,
	                                       boolean xmlFormat,
	                                       Level level)
	    throws IOException
	{
		return getLogger(client, false, fileName, xmlFormat, null, level);
	}

	/**
	 * Factory method for a general logger
	 * @param <E> The type of client to the logger
	 * @param client The logger's client class, used to provide name to logger
	 * @param verbose True to display messages in console
	 * @param logFileName File name to log in (or null)
	 * @param xmlFormat Flag to format output with XML
	 * @param parentLogger The parent logger (if any)
	 * @param level Min log level (e.g. FINE, INFO, WARNING, SEVERE)
	 * @return A general logger
	 * @throws IOException If the file could not be opened
	 */
	public static <E> Logger getLogger(Class<E> client,
	                                   boolean verbose,
	                                   String logFileName,
	                                   boolean xmlFormat,
	                                   Logger parentLogger,
	                                   Level level)
	    throws IOException
	{
		Logger logger = null;

		if (verbose || (logFileName != null) || (parentLogger != null))
		{
			if (client != null)
			{
				String canonicalName = client.getCanonicalName();
				logger = Logger.getLogger(canonicalName);

				if (parentLogger != null)
				{
					logger.setParent(parentLogger);
				}
				else
				{
					if (!verbose)
					{
						/*
						 * We don't want messages to be sent to console
						 */
						logger.setUseParentHandlers(false);
					}
				}

				if (logFileName != null)
				{
					String filename = logFileName;
					if (xmlFormat)
					{
						if (!logFileName.contains(new String("xml")))
						{
							filename = logFileName + ".xml";
						}
					}

					// Add file handler to logger
					try
					{
						Handler handler = new FileHandler(filename);
						if (!xmlFormat)
						{
							/*
							 * Default file formatting will be XML,
							 * so we need to setup a simple formatter
							 */
							handler.setFormatter(new SimpleFormatter());
						}

						// Adds file handler to logger
						logger.addHandler(handler);
						logger.info("log file created");
					}
					catch (IllegalArgumentException e)
					{
						String message = "Empty log file name";
						logger.severe(message);
						logger.severe(e.getLocalizedMessage());
						throw e;
					}
					catch (SecurityException e)
					{
						String message =
						    "Do not have privileges to open log file "
						        + logFileName;
						logger.warning(message);
						logger.warning(e.getLocalizedMessage());
					}
					catch (IOException e)
					{
						String message = "Error opening file " + logFileName;
						logger.severe(message);
						logger.severe(e.getLocalizedMessage());
						throw e;
					}
				}
			}
			else
			{
				if (parentLogger != null)
				{
					logger = parentLogger;
				}
			}
		}

		if (logger != null)
		{
//			logger.info("Logger ready");
			logger.setLevel(level);
		}

		return logger;
	}
}
