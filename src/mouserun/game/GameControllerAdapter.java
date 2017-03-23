/** 
 * MouseRun. A programming game to practice building intelligent things.
 * Copyright (C) 2013  Muhammad Mustaqim
 * 
 * This file is part of MouseRun.
 *
 * MouseRun is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MouseRun is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MouseRun.  If not, see <http://www.gnu.org/licenses/>.
 **/
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
	
	/** 
	 * This method will be invoked and adapter has to cause all represents to halt.
	 */
	public void stop();
	
	/**
	* This method is to be invoked when showing the number of seconds left to the game end.
	*/
	public void displayCountDown(int seconds);

}