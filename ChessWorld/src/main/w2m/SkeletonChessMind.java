package w2m;

import org.w2mind.net.*;

public class SkeletonChessMind implements Mind {
	public void endrun() throws RunError {
	}

	public void newrun() throws RunError {
	}

	int a = 8;
	public Action getaction(State s) throws RunError {
		// always try to move the same pawn, causing an invalid move forfeit quickly
		String fen = s.toString();
		Action action = new Action(String.format("%d,%d", a, a+16));
		a++;
		return action;
	}
}
