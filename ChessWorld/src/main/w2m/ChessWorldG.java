package w2m;

import guibase.ChessController;
import guibase.GUIInterface;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import javax.imageio.ImageIO;

import org.w2mind.net.AbstractWorld;
import org.w2mind.net.Action;
import org.w2mind.net.RunError;
import org.w2mind.net.Score;
import org.w2mind.net.State;

import chess.ComputerPlayer;
import chess.Position;

/**
 * ChessWorld - a basic wrapper around the free CuckooChess engine, which models
 * the board, moves and the opponent player. Not complete...
 * 
 * @author Omf
 * 
 */
public class ChessWorldG extends AbstractWorld {
	int MAX_STEPS = 50;
	List<String> scoreCols;
	int timestep;

	String SUPPORT_DIR = "rsc"; // support files

	String IMG_BOARD = SUPPORT_DIR + "/chessboard.png";

	// transient - don't serialise these:
	private transient ArrayList<BufferedImage> buf;
	private transient InputStream chessboardStream;
	private transient Image chessboard;
	private transient Image[] pieceImg;

	int imgwidth, imgheight;
	private ChessController controller;
	private boolean wasValidMove;
	private int gameNum;
	private int invalidMoves;
	protected int promotedPiece;
	private GUIInterface gameDelegate;
	private boolean gameOver;
	protected String status;
	private int won;
	private int drew;
	private int totalMoves;
	protected Position position;
	private long lastActionTime;
	private boolean restart;
	private int winningGameMoves;
	private int survived;

	// sets up new buffer to hold images
	private void initImages() {
		if (imagesDesired) {
			// per-timestep image list
			buf = new ArrayList<BufferedImage>(); // buffer is cleared for each
													// timestep, multiple images
													// per timestep

			// lazy load images...
			if (chessboardStream == null) {
				try {
					ImageIO.setUseCache(false); // use memory, not disk, for
												// temporary images

					chessboardStream = getClass().getResourceAsStream(
							"/" + IMG_BOARD);
					if (chessboardStream != null)
						chessboard = javax.imageio.ImageIO
								.read(chessboardStream);
					else
						chessboard = ImageIO.read(new File(IMG_BOARD));

					// init and load all piece images (first slot is unused)
					pieceImg = new Image[13];
					for (int i = 1; i <= 12; i++) {
						String imgPath = "/" + SUPPORT_DIR + "/p" + i + ".png";
						InputStream stream = getClass().getResourceAsStream(
								imgPath);
						if (stream != null)
							pieceImg[i] = javax.imageio.ImageIO.read(stream);
						else
							pieceImg[i] = ImageIO.read(new File(imgPath));

					}
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}

	// adds image to buffer
	private void addImage() {
		// System.out.println("Asked to add image for state: "
		// + controller.getFEN());
		if (imagesDesired) {
			// do something with status var
			BufferedImage img = new BufferedImage(640, 688,
					BufferedImage.TYPE_INT_RGB);

			Graphics2D graphics = img.createGraphics();
			graphics.setColor(Color.RED);
			graphics.setFont(new Font("Courier New", Font.PLAIN, 24));
			graphics.drawImage(chessboard, 0, 0, null);
			// draw location of all remaining pieces
			for (int i = 0; i < 64; i++) {
				// System.out.print(position.getPiece(i) + " ");
				int square = 63 - i;
				int piece = position.getPiece(square);
				if (piece > 0)
					graphics.drawImage(pieceImg[piece], 80 * (square % 8),
							80 * (7 - (square / 8)), null);
			}
			graphics.drawString("Status: " + status, 4, 666);
			buf.add(img);
			// try {
			// ImageIO.write(img, "png", new File("tmp-board.png"));
			// } catch (IOException e) {
			// e.printStackTrace();
			// }
		}
	}

	public void newrun() throws RunError {
		scoreCols = new LinkedList<String>();
		scoreCols.add("Won");
		scoreCols.add("Drew");

		scoreCols.add("Avg. moves to win");
		scoreCols.add("Survival moves");
		scoreCols.add("Invalid moves");
		scoreCols.add("Captured Queen");
		scoreCols.add("Captured Bishop");
		scoreCols.add("Captured Knight");
		scoreCols.add("Captured Rook");
		scoreCols.add("Captured Pawn");

		scoreCols.add("Captured with Queen");
		scoreCols.add("Captured with Bishop");
		scoreCols.add("Captured with Knight");
		scoreCols.add("Captured with Rook");
		scoreCols.add("Captured with Pawn");

		scoreCols.add("Promoted Pawn");
		scoreCols.add("Castled");
		scoreCols.add("Enemy in Check");
		scoreCols.add("Player in Check"); // negative...
		// need to track these events in Game.java? -> whiteCapturedQueen++ ...

		gameNum = 0;
		gameDelegate = createGameDelegate();
		controller = new ChessController(gameDelegate);
		gameDelegate.setChessController(controller); // recursive dependency,
														// meh.
		controller.newGame(true, 0, false);
		controller.startGame();
	}

	private GUIInterface createGameDelegate() {
		return new GUIInterface() {
			private ChessController chessController;

			public int timeLimit() {
				return 500;
			}

			public boolean showThinking() {
				return false;
			}

			public void setThinkingString(String str) {
			}

			public void setStatusString(String str) {
				status = str;
			}

			public void setSelection(int sq) {
			}

			public void setPosition(chess.Position pos) {
				position = pos;
				addImage();
				// need to account for check/etc
			}

			public void setMoveListString(String str) {
			}

			public void runOnUIThread(Runnable runnable) {
			}

			public void requestPromotePiece() {
				// always promote to queen...
				promotedPiece++;
				chessController.reportPromotePiece(4);
			}

			public void reportInvalidMove(chess.Move m) {
				wasValidMove = false;
			}

			public boolean randomMode() {
				return false;
			}

			@Override
			public void setChessController(ChessController c) {
				chessController = c;
			}
		};
	}

	public void endrun() throws RunError {
	}

	public State getstate() throws RunError {
		return new State(controller.getFEN());
	}

	// Add any number of images to a list of images for this step.
	// The first image on the list for this step should be the image before we
	// take the action.
	public State takeaction(Action action) throws RunError {
		totalMoves++;
		initImages(); // If run with images off, imagesDesired = false and this
						// does nothing.
		if (restart) {
			// can't do this at end of previous takeaction() call
			controller.startGame();
			restart = false;
		} else {
			addImage(); // image before white's move
		}

		wasValidMove = true;
		String[] move = action.toString().split(",");
		int fromSqi = Integer.parseInt(move[0]);
		int toSqi = Integer.parseInt(move[1]);
		// check for timeout (move time more than 700 ms)
		long now = System.currentTimeMillis();
		if (lastActionTime > 0 && now - lastActionTime > 700) {
			startNextGame("Ran out of time. Game forfeit!");
		} else {
			controller.humanMove(new chess.Move(fromSqi, toSqi, 0));
		}
		// intermediate state image added by game delegate
		System.out.println("gamenum: " + gameNum + ", totalMoves: " + totalMoves + ", game status: " + status);
		if (!wasValidMove) {
			// System.out.println("Invalid move (status: " + status + ")");
			invalidMoves++;
			survived += totalMoves;
			startNextGame("Invalid move. Game forfeit!");
		} else if (status.contains("Game over")) {
			if (status.contains("white mates")) {
				winningGameMoves += totalMoves;
				won++;
			} else if (status.contains("draw")) {
				survived += totalMoves;
				drew++;
			} else
				survived += totalMoves;
			startNextGame(status);
		}

		if (gameOver) {
			addImage();
			gameOver = false;
		}
		// The last timestep of the run shows the final state, and no action can
		// be taken in this state.
		// Whatever is the last image built on the run will be treated as the
		// image for this final state.

		// save time of last action
		lastActionTime = System.currentTimeMillis();
		return getstate();
	}

	private void startNextGame(String endStatus) {
		gameOver = true;
		gameNum++;
		totalMoves = 0;
		// addImage();
		controller.newGame(true, 0, false);
		status = endStatus;
		restart = true;
	}

	// have this evaluate to true when all games have completed?
	private boolean runFinished() {
		return gameNum > 4;
	}

	public Score getscore() throws RunError {
		List<Comparable> values = new LinkedList<Comparable>();
		values.add(won);
		values.add(drew);
		values.add(-winningGameMoves / Math.max(1, won)); // want to reward
															// achieving wins in
															// least amount of
															// moves
		values.add(survived); // want to reward
															// surviving as long
															// as possible (in
															// non-win games)
		values.add(-invalidMoves); // Invalid moves, reward minimisation of this
		values.add(0); // Captured Queen
		values.add(0); // Captured Bishop
		values.add(0); // Captured Knight
		values.add(0); // Captured Rook
		values.add(0); // Captured Pawn
		values.add(0); // Captured with Queen
		values.add(0); // Captured with Bishop
		values.add(0); // Captured with Knight
		values.add(0); // Captured with Rook
		values.add(0); // Captured with Pawn
		values.add(0); // Promoted Pawn
		values.add(0); // Castled
		values.add(0); // Enemy in Check
		values.add(-0); // Player in Check // negative...
		String s = "score: " + Arrays.toString(values.toArray()); // shouldn't
																	// have to
																	// do this

		Score score = new Score(s, runFinished(), scoreCols, values);
		System.out.println("score: " + score);
		return score;
	}

	// Return image(s) of World.
	// Image may show more information than State (what the Mind sees).
	// This method returns a list of images for this step - we allow multiple
	// images per step.
	// e.g. You move, get one image, the opponent moves, next image, your turn
	// again (this is 2 images per step).
	// This list of images should normally be built in takeaction method.
	// The first image on the list for this step should be the image before we
	// take the action on this step.
	public ArrayList<BufferedImage> getimage() throws RunError {
		return buf;
	}
}
