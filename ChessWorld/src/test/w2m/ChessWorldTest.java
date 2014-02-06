package w2m;

import static org.junit.Assert.*;

import org.junit.Test;
import org.w2mind.net.Action;
import org.w2mind.net.RunError;
import org.w2mind.net.State;

public class ChessWorldTest {
	@Test
	public void testSimpleRun() {
		ChessWorldG world = new ChessWorldG();
		try {
			world.newrun();
			world.imagesDesired = true;
			State state = world.getstate();
			System.out.println("state: " + state);
			State state2 = world.takeaction(new Action("12,28"));
			System.out.println("state2: " + state2);
			// should end 1st game
			State state3 = world.takeaction(new Action("12,28"));
		} catch (RunError e) {
			e.printStackTrace();
			fail();
		}
	}
}
