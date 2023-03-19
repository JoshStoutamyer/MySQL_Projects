package projects.exception;

/*
 * An exception class to be used to handle errors thrown, as unchecked exceptions, in the projects app.
 */

@SuppressWarnings("serial")
public class DbException extends RuntimeException {

	/*
	 * Gives an exception with a message
	 */
	public DbException(String message) {
		super(message);
	}
	
	/*
	 * Gives an exception with a cause.
	 */
	public DbException(Throwable cause) {
		super(cause);
	}

	/*
	 * Gives an exception with both a message and a cause.
	 */
	public DbException(String message, Throwable cause) {
		super(message, cause);
	}


}
