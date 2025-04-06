package tetris;

import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;
import javax.microedition.lcdui.Alert;
import javax.microedition.lcdui.Canvas;
import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.CommandListener;
import javax.microedition.lcdui.Display;
import javax.microedition.lcdui.Displayable;
import javax.microedition.lcdui.Font;
import javax.microedition.lcdui.Graphics;
import javax.microedition.midlet.MIDlet;

/**
 * @author Mircea-Sebastian Turtureanu
 */
public class MainMIDlet extends MIDlet implements CommandListener {

	private boolean midletPaused = false;
	private Command exitCommand;
	private TetrisCanvas canvas;

	// Colors
	private static final int YELLOW = 0xFFFF00;
	private static final int BLUE = 0x1638FF;
	private static final int CYAN = 0x1BE5F7;
	private static final int RED = 0xF63736;
	private static final int GREEN = 0x2FF941;
	private static final int ORANGE = 0xF7B63D;
	private static final int PURPLE = 0xA629FB;

	private static final int WHITE = 0xFFFFFF;
	private static final int BLACK = 0x000000;


	// Tetrominos
	private static final int TETROMINOS[][][] = {
		//	I
		{
			{CYAN, CYAN, CYAN, CYAN},
			{0, 0, 0, 0}
		},
		// L
		{
			{0, 0, ORANGE},
			{ORANGE, ORANGE, ORANGE}
		},
		// J
		{
			{BLUE, 0, 0},
			{BLUE, BLUE, BLUE}
		},
		// S
		{
			{0, GREEN, GREEN},
			{GREEN, GREEN, 0}
		},
		// Z
		{
			{RED, RED, 0},
			{0, RED, RED}
		},
		// O
		{
			{YELLOW, YELLOW},
			{YELLOW, YELLOW}
		},
		// T
		{
			{0, PURPLE, 0},
			{PURPLE, PURPLE, PURPLE}
		}
	};

	//	Game variables
	// Constant variables
	private static final int WIDTH = 16;
	private static final int HEIGHT = 24;
	private static final Font TETRIS_FONT = Font.getFont(Font.FACE_MONOSPACE, Font.STYLE_BOLD, Font.SIZE_LARGE);

	private static int ARENA_PANEL_HEIGHT;
	private static int ARENA_PANEL_WIDTH;
	private static int STATUS_PANEL_WIDTH;
	private static int ARENA_TOP;
	private static int PADDING;
	private static int BLOCK_SIZE;


	// Timers
	// move the tetromino down every FALL_TIME miliseconds
	private Timer fallTimer;
	private static final int FALL_TIME = 1000;

	// used for dropping the tetromino faster when holding DOWN
	// FAST_FALL_TIME: how many miliseconds is DOWN held before dropping faster
	private Timer fastFallTimer;
	private static final int FAST_FALL_TIME = 100;

	//	 Dynamic variables
	private static int grid[][] = new int[HEIGHT][WIDTH];
	private int[][] currentTetromino = generateTetromino();
	private int currentTetrominoX = (WIDTH - currentTetromino[0].length) / 2;
	private static int currentTetrominoY = ARENA_TOP;
	private static int[][][] nextTetrominos = new int[4][][];
	private static int score = 0;

	private boolean canMoveDown(int[][] tetromino) {
		for (int i = 0; i < tetromino.length; i++) {
			for (int j = 0; j < tetromino[i].length; j++) {
				if (tetromino[i][j] != 0) {
					int newX = currentTetrominoX + j;
					int newY = currentTetrominoY + i + 1;
					if (newY >= HEIGHT || grid[newY][newX] != BLACK) {
						return false;
					}
				}
			}
		}
		return true;
	}

	// fix or "set" the tetromino in place
	private void fixTetromino() {
		for (int i = 0; i < currentTetromino.length; i++) {
			for (int j = 0; j < currentTetromino[i].length; j++) {
				if (currentTetromino[i][j] != 0) {
					grid[currentTetrominoY + i][currentTetrominoX + j] = currentTetromino[i][j];
				}
			}
		}
	}

	// clear full rows
	private void clearRows() {
		int fullCount = 0;
		for (int i = HEIGHT - 1; i >= 0; i--) {
			boolean full = true;

			// check if the current row is full
			for (int j = 0; j < WIDTH; j++) {
				if (grid[i][j] == BLACK) {
					full = false;
					break;
				}
			}

			if (full) {
				fullCount++;
				// shift all rows above down by one
				for (int k = i; k > 0; k--) {
					// (didn't know Netbeans had a suggestion for this)
					System.arraycopy(grid[k - 1], 0, grid[k], 0, WIDTH);
				}

				i++; // recheck row after shifting the above rows
			}
		}

		// calculate score
		switch (fullCount) {
			case 1:
				score += 40;
				break;
			case 2:
				score += 100;
				break;
			case 3:
				score += 300;
				break;
			case 4:
				score += 1200;
				break;
			default:
				score += fullCount * fullCount * 100;
		}
	}

	// reset score and reinitialize variables
	private void restartGame() {
		score = 0;
		initialize();
	}

	private void checkGameOver() {
		int tetrominoWidth = currentTetromino[0].length;

		for (int j = 0; j < tetrominoWidth; j++) {
			int gridX = currentTetrominoX + j;
			// if the tetromino is placed in such a way that it can lead to a game over
			if (currentTetromino[0][j] != 0 && grid[0][gridX] != BLACK) {
				switchDisplayable(null, new GameOverCanvas());
			}
		}
	}

	// print game over screen
	private class GameOverCanvas extends Canvas implements CommandListener {

		// buttons (OK & EXIT)
		private Command restartCommand;
		private Command exitCommand;

		public GameOverCanvas() {
			restartCommand = new Command("Restart", Command.OK, 1);
			addCommand(restartCommand);

			exitCommand = new Command("Exit", Command.EXIT, 1);
			addCommand(exitCommand);

			setCommandListener(this);
		}

		protected void paint(Graphics g) {
			g.setColor(BLACK);
			g.fillRect(0, 0, getWidth(), getHeight());
			g.setColor(WHITE);
			g.setFont(TETRIS_FONT);
			g.drawString("GAME OVER", getWidth() / 2, getHeight() / 4, Graphics.TOP | Graphics.HCENTER);
			g.drawString("SCORE", getWidth() / 2, getHeight() / 2, Graphics.TOP | Graphics.HCENTER);
			g.drawString(score + "", getWidth() / 2, getHeight() / 2 + PADDING * 4, Graphics.TOP | Graphics.LEFT);
		}

		public void commandAction(Command command, Displayable displayable) {
			if (command == restartCommand) {
				restartGame();
			} else if (command == exitCommand) {
				exitMIDlet();
			}
		}
	}


	private void moveTetrominoDown() {
		// move down
		if (canMoveDown(currentTetromino)) {
			currentTetrominoY++;
		} else { // if already at the bottom
			fixTetromino();
			clearRows();

			// check if the player lost
			checkGameOver();

			// use the next tetromino
			currentTetromino = nextTetrominos[0];
			currentTetrominoX = (WIDTH - currentTetromino[0].length) / 2;
			currentTetrominoY = 0;

			// shift the next tetromino array
			for (int i = 0; i < nextTetrominos.length - 1; i++) {
				nextTetrominos[i] = nextTetrominos[i + 1];
			}

			// generate new last next tetromino
			nextTetrominos[nextTetrominos.length - 1] = generateTetromino();
		}

		canvas.repaint();
	}

	private void initialize() {
		// if we are restarting, clear the timers
		if (fallTimer != null) {
			fallTimer.cancel();
		}

		if (fastFallTimer != null) {
			fastFallTimer.cancel();
		}

		// initialize grid
		for (int i = 0; i < HEIGHT; i++) {
			for (int j = 0; j < WIDTH; j++) {
				grid[i][j] = BLACK;
			}
		}

		// generetae tetrominos
		currentTetromino = generateTetromino();

		for (int i = 0; i < nextTetrominos.length; i++) {
			nextTetrominos[i] = generateTetromino();
		}

		// start fall timer
		fallTimer = new Timer();
		fallTimer.scheduleAtFixedRate(new TimerTask() {
			public void run() {
				moveTetrominoDown();
			}
		}, FALL_TIME, FALL_TIME);

		canvas = new TetrisCanvas();
		switchDisplayable(null, canvas);
	}

	// MIDlet functions
	public void startMIDlet() {
	}

	public void resumeMIDlet() {
	}

	public void switchDisplayable(Alert alert, Displayable nextDisplayable) {
		Display display = getDisplay();
		if (alert == null) {
			display.setCurrent(nextDisplayable);
		} else {
			display.setCurrent(alert, nextDisplayable);
		}
	}

	public void commandAction(Command command, Displayable displayable) {
		if (command == getExitCommand()) {
			exitMIDlet();
		}
	}

	public Command getExitCommand() {
		if (exitCommand == null) {

			exitCommand = new Command("Exit", Command.EXIT, 0);
		}
		return exitCommand;
	}

	public Display getDisplay() {
		return Display.getDisplay(this);
	}

	public void exitMIDlet() {
		// let's not forget about our timers
		if (fallTimer != null) {
			fallTimer.cancel();
		}

		fastFallTimer.cancel();

		switchDisplayable(null, null);
		destroyApp(true);
		notifyDestroyed();
	}

	public void startApp() {
		if (midletPaused) {
			resumeMIDlet();
		} else {
			initialize();
			startMIDlet();
		}
		midletPaused = false;
	}

	public void pauseApp() {
		midletPaused = true;
	}

	public void destroyApp(boolean unconditional) {
	}


	public MainMIDlet() {
	}

	private int[][] generateTetromino() {
		// Random.nextInt() is kind of broken
		// so try and get a different value
		int random1 = new Random().nextInt(TETROMINOS.length);
		int random2 = new Random().nextInt(TETROMINOS.length);

		for (int i = 0; i < 200 || random1 == random2; i++) {
			random2 = new Random().nextInt(TETROMINOS.length);
		}
		return TETROMINOS[random2];
	}

	// rotate the tetromino by +90 degrees
	private int[][] rotateTetromino(int[][] tetromino) {
		int rows = tetromino.length;
		int cols = tetromino[0].length;
		int[][] rotatedTetromino = new int[cols][rows];

		// transpose the array and reverse rows (quite neatly)
		for (int i = 0; i < rows; i++) {
			for (int j = 0; j < cols; j++) {
				rotatedTetromino[j][rows - 1 - i] = tetromino[i][j];
			}
		}

		return rotatedTetromino;
	}

	private class TetrisCanvas extends Canvas {
		protected void paint(Graphics g) {
			// Arena Panel
			PADDING = (getWidth() / WIDTH) / 2;
			STATUS_PANEL_WIDTH = getWidth() / 3;
			ARENA_PANEL_WIDTH = getWidth() - STATUS_PANEL_WIDTH;
			BLOCK_SIZE = ARENA_PANEL_WIDTH / WIDTH;
			ARENA_PANEL_HEIGHT = HEIGHT * BLOCK_SIZE;
			ARENA_TOP = (getHeight() - ARENA_PANEL_HEIGHT) / 2;
			g.setColor(BLACK);
			g.fillRect(0, 0, getWidth(), getHeight());
			g.setColor(WHITE);
			g.drawRect(PADDING - 1, ARENA_TOP - 1, ARENA_PANEL_WIDTH + 1, ARENA_PANEL_HEIGHT + 1);

			for (int i = 0; i < HEIGHT; i++) {
				for (int j = 0; j < WIDTH; j++) {
					g.setColor(grid[i][j]);
					g.fillRect(PADDING + j * BLOCK_SIZE, i * BLOCK_SIZE + (getHeight() - ARENA_PANEL_HEIGHT) / 2, BLOCK_SIZE, BLOCK_SIZE);
				}
			}

			for (int i = 0; i < currentTetromino.length; i++) {
				for (int j = 0; j < currentTetromino[i].length; j++) {
					if (currentTetromino[i][j] != 0) {
						g.setColor(currentTetromino[i][j]);
						g.fillRect(PADDING + (currentTetrominoX + j) * BLOCK_SIZE,
						(currentTetrominoY + i) * BLOCK_SIZE + (getHeight() - ARENA_PANEL_HEIGHT) / 2,
						BLOCK_SIZE, BLOCK_SIZE);
					}
				}
			}

			// Status Panel
			int scorePadding = ((BLOCK_SIZE * 3 * 3 + PADDING / 2)) / 2 + PADDING * 2;

			g.setFont(TETRIS_FONT);
			g.setColor(WHITE);


			int scoreStringX = TETRIS_FONT.stringWidth("SCO") / 2 + ARENA_PANEL_WIDTH + PADDING * 2;
			g.drawString("SCORE", scoreStringX, ARENA_TOP, Graphics.TOP | Graphics.LEFT);

			int scoreNumberStringX = (STATUS_PANEL_WIDTH - TETRIS_FONT.stringWidth(score + "")) / 2 + ARENA_PANEL_WIDTH + PADDING;
			g.drawString(score + "", scoreNumberStringX, ARENA_TOP + PADDING * 4, Graphics.TOP | Graphics.LEFT);

			int nextStringX = TETRIS_FONT.stringWidth("NEXT") / 2 + ARENA_PANEL_WIDTH + PADDING * 2;
			g.drawString("NEXT", nextStringX, ARENA_TOP + scorePadding, Graphics.TOP | Graphics.LEFT);

			for (int i = 0; i < nextTetrominos.length; i++) {
				for (int j = 0; j < nextTetrominos[i].length; j++) {
					for (int k = 0; k < nextTetrominos[i][j].length; k++) {
						if (nextTetrominos[i][j][k] == 0) {
							g.setColor(BLACK);
						} else {
							g.setColor(nextTetrominos[i][j][k]);
						}

						int nextTetrominoBlockX
						= (PADDING + ARENA_PANEL_WIDTH + PADDING) // ARENA LEFT
						+ (STATUS_PANEL_WIDTH - BLOCK_SIZE * nextTetrominos[i][j].length) / 2 // padding between arena and status panel + center tetromino
						+ BLOCK_SIZE * k // next tetromino block offset
						- BLOCK_SIZE + BLOCK_SIZE / 4; // magic number

						int nextTetrominoBlockY
						= ARENA_TOP // align to arena top
						+ scorePadding // move under score
						+ PADDING * 2 // move under text
						+ PADDING * 2 // add padding under text
						+ j * BLOCK_SIZE // tetromino block Y offset
						+ i * BLOCK_SIZE * 3; // add spacing between tetrominos

						g.fillRect(nextTetrominoBlockX, nextTetrominoBlockY, BLOCK_SIZE, BLOCK_SIZE);
					}
				}
			}
		}

		private boolean canMoveLeft() {
			for (int i = 0; i < currentTetromino.length; i++) {
				for (int j = 0; j < currentTetromino[i].length; j++) {
					if (currentTetromino[i][j] != 0) {
						int newX = currentTetrominoX + j - 1;
						int newY = currentTetrominoY + i;
						if (newX < 0 || grid[newY][newX] != BLACK) {
							return false;
						}
					}
				}
			}
			return true;
		}

		private boolean canMoveRight() {
			for (int i = 0; i < currentTetromino.length; i++) {
				for (int j = 0; j < currentTetromino[i].length; j++) {
					if (currentTetromino[i][j] != 0) {
						int newX = currentTetrominoX + j + 1;
						int newY = currentTetrominoY + i;
						if (newX >= WIDTH || grid[newY][newX] != BLACK) {
							return false;
						}
					}
				}
			}
			return true;
		}

		private void startFastDrop() {
			fastFallTimer = new Timer();
			fastFallTimer.scheduleAtFixedRate(new TimerTask() {
				public void run() {
					moveTetrominoDown();
				}
			}, 0, FAST_FALL_TIME);
		}

		private void stopFastDrop() {
			if (fastFallTimer != null) {
				fastFallTimer.cancel();
				fastFallTimer = null;
			}
		}

		protected void keyReleased(int keyCode) {
			int gameAction = getGameAction(keyCode);
			if (gameAction == DOWN) {
				stopFastDrop();
			}
		}

		// checks if the rotated tetromino can be placed
		private boolean canPlaceRotatedTetromino(int[][] tetromino) {
			for (int i = 0; i < tetromino.length; i++) {
				for (int j = 0; j < tetromino[i].length; j++) {
					if (tetromino[i][j] != 0) {
						int newX = currentTetrominoX + j;
						int newY = currentTetrominoY + i;
						if (newX < 0 || newX >= WIDTH || newY >= HEIGHT || grid[newY][newX] != BLACK) {
							return false;
						}
					}
				}
			}
			return true;
		}

		// Input handling
		protected void keyPressed(int keyCode) {
			int gameAction = getGameAction(keyCode);
			switch (gameAction) {
				case LEFT:
					if (canMoveLeft()) {
						currentTetrominoX--;
					}
					break;
				case RIGHT:
					if (canMoveRight()) {
						currentTetrominoX++;
					}
					break;
				case DOWN:
					if (fastFallTimer == null) {
						startFastDrop();
					}
					break;
				case UP:
					int[][] rotatedTetromino = rotateTetromino(currentTetromino);
					if (canPlaceRotatedTetromino(rotatedTetromino)) {
						currentTetromino = rotatedTetromino;
					}
					break;
				default:
					break;
			}
			repaint();
		}
	}
}
