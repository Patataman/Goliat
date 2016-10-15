package bot;

import jnibwapi.BWAPIEventListener;
import jnibwapi.Position;

public class Goliat implements BWAPIEventListener {

	public static void main(String[] args) {
	}

	public void connected() {}

	public void matchStart() {}

	public void matchFrame() {}

	public void matchEnd(boolean winner) {}

	public void keyPressed(int keyCode) {}

	public void sendText(String text) {}

	public void receiveText(String text) {}

	public void playerLeft(int playerID) {}

	public void nukeDetect(Position p) {}

	public void nukeDetect() {}

	public void unitDiscover(int unitID) {}
	
	public void unitEvade(int unitID) {}

	public void unitShow(int unitID) {}

	public void unitHide(int unitID) {}

	public void unitCreate(int unitID) {}

	public void unitDestroy(int unitID) {}

	public void unitMorph(int unitID) {}

	public void unitRenegade(int unitID) {}

	public void saveGame(String gameName) {}

	public void unitComplete(int unitID) {}

	public void playerDropped(int playerID) {}

}
