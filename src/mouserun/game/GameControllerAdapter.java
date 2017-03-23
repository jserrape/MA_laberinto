package mouserun.game;

/**
 * Interface GameControllerAdapter provides the interface for the GameController to 
 * raise events that occurred during the game to be reflected in the Game Interface.
 */
public interface GameControllerAdapter
{

	/**
	 * This method will be invoked and adapter has to prepare all necessary preloading and 
	 * starts the game. 
	 */
	public void start();
	

}