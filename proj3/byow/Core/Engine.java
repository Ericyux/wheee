package byow.Core;

import byow.TileEngine.TERenderer;
import byow.TileEngine.TETile;
import byow.TileEngine.Tileset;
import java.util.Queue;
import java.util.LinkedList;
import java.util.Collections;

import java.awt.*;
import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;

public class Engine {
    private Graph kruskalsTree;
    public static final File CWD = new File(System.getProperty("user.dir"));
    public static final File SAVE_DATA = Utils.join(CWD, "save_data.txt");
    public static final File SEED = Utils.join(CWD, "seed.txt");
    TERenderer ter = new TERenderer();
    /* Feel free to change the width and height. */
    public static final int WIDTH = 80;
    public static final int HEIGHT = 50;
    public static final double PROBABILITY = 0.07;
    public static final int LOWER_BOUND = 10;
    public static final int HIGHER_BOUND = 15;
    public static final int ROOM_LOWER_BOUND = 8;
    public static final int ROOM_UPPER_BOUND = 11;
    public static final int FONT_SIZE = 30;
    private Random rand;
    private Graph graph;
    private HashMap<Integer, int[]> rooms;
    private String seed;

    private Coordinate redSquare;  // Line ~38

    public void setRand(Random rand) {
        this.rand = rand;
    }

    public Engine() {
        ter.initialize(WIDTH, HEIGHT + 2);

        graph = new Graph();
        rooms = new HashMap<>();
    }

    /**
     * Method used for exploring a fresh world. This method should handle all inputs,
     * including inputs from the main menu.
     */
    public void interactWithKeyboard() {
        StdDraw.setPenColor(Color.white);
        Font f = StdDraw.getFont();
        StdDraw.setFont(new Font("Monaco", Font.BOLD, FONT_SIZE));
        StdDraw.text(WIDTH / 2, HEIGHT * 7 / 8, "CSA Project 5");
        StdDraw.text(WIDTH / 2, HEIGHT * 1 / 2, "New World (N)");
        StdDraw.text(WIDTH / 2, HEIGHT * 2 / 8, "Quit (Q)");
        StdDraw.show();
        while (true) {
            while (!StdDraw.hasNextKeyTyped()) {
                int asdf = 0;
            }
            char c = StdDraw.nextKeyTyped();
            if (c == 'r' || c == 'R') {
                StdDraw.clear();
                StdDraw.setFont(f);
                run(interactWithInputString("r"));
                break;
            }
            if (c == 'n' || c == 'N') {
                StdDraw.clear(Color.BLACK);
                StdDraw.show();
                String stringSeed = "";
                while (true) {
                    while (!StdDraw.hasNextKeyTyped()) {
                        int asdf = 0;
                    }
                    char curr = StdDraw.nextKeyTyped();
                    if (curr == 's' || curr == 'S') {
                        break;
                    }
                    stringSeed += curr;
                    StdDraw.clear(Color.BLACK);
                    StdDraw.text(WIDTH / 2, HEIGHT / 2, stringSeed);
                    StdDraw.show();
                }
                StdDraw.setFont(f);
                run(interactWithInputString("n" + stringSeed + "s"));
                break;
            } else if (c == 'l' || c == 'L') {
                StdDraw.clear();
                StdDraw.setFont(f);
                run(interactWithInputString("l"));
                break;
            } else if (c == 'Q' || c == 'q') {
                StdDraw.clear(Color.BLACK);
                StdDraw.show();
                break;
            }
        }
    }

    /**
     * Method used for autograding and testing your code. The input string will be a series
     * of characters (for example, "n123sswwdasdassadwas", "n123sss:q", "lwww". The engine should
     * behave exactly as if the user typed these characters into the engine using
     * interactWithKeyboard.
     *
     * Recall that strings ending in ":q" should cause the game to quite save. For example,
     * if we do interactWithInputString("n123sss:q"), we expect the game to run the first
     * 7 commands (n123sss) and then quit and save. If we then do
     * interactWithInputString("l"), we should be back in the exact same state.
     *
     * In other words, running both of these:
     *   - interactWithInputString("n123sss:q")
     *   - interactWithInputString("lww")
     *
     * should yield the exact same world state as:
     *   - interactWithInputString("n123sssww")
     *
     * @param input the input string to feed to your program
     * @return the 2D TETile[][] representing the state of the world
     */
    public TETile[][] interactWithInputString(String input) {
        String stringSeed;
        boolean replay = false;
        if (input.charAt(0) == 'N' || input.charAt(0) == 'n') {
            stringSeed = "";
            for (int i = 1; i < input.length(); i++) {
                if (input.charAt(i) == 's' || input.charAt(i) == 'S') {
                    if (i + 1 != input.length()) {
                        input = input.substring(i + 1);
                    } else {
                        input = "";
                    }
                    break;
                }
                stringSeed += input.charAt(i);
                Utils.writeContents(SAVE_DATA, "");
            }
        } else if (input.charAt(0) == 'l' || input.charAt(0) == 'L') {
            stringSeed = Utils.readContentsAsString(SEED);
            input = Utils.readContentsAsString(SAVE_DATA) + input.substring(1);
        } else if (input.charAt(0) == 'r')  {
            stringSeed = Utils.readContentsAsString(SEED);
            input = Utils.readContentsAsString(SAVE_DATA);
            replay = true;
        } else {
            throw new IllegalArgumentException();
        }
        long longSeed = Long.parseLong(stringSeed);
        Random r = new Random(longSeed);
        setRand(r);
        this.seed = stringSeed;
        TETile[][] finalWorldFrame = new TETile[WIDTH][HEIGHT];
        for (int i = 0; i < WIDTH; i++) {
            for (int j = 0; j < HEIGHT; j++) {
                finalWorldFrame[i][j] = Tileset.NOTHING;
            }
        }
        generateRooms(RandomUtils.uniform(rand, ROOM_LOWER_BOUND, ROOM_UPPER_BOUND), finalWorldFrame);
        for (int i : graph.getAllVertices()) {
            for (int j : graph.getAllVertices()) {
                if (i != j) {
                    graph.addEdge(i, j, rand.nextInt());
                }
            }
        }
        graph = graph.kruskals();
        kruskalsTree = graph;  // store the tree
        for (int i : graph.getAllVertices()) {
            for (int j : graph.getAllVertices()) {
                if (RandomUtils.bernoulli(rand, PROBABILITY)) {
                    boolean no = false;
                    for (byow.Core.Edge e : graph.getAllEdges()) {
                        if (e.getSource() == i && e.getDest() == j || e.getSource() == j && e.getDest() == i) {
                            no = true;
                            break;
                        }
                    }
                    if (!no) {
                        graph.addEdge(i, j);
                    }
                }
            }
        }
        for (byow.Core.Edge e : graph.getAllEdges()) {
            generateHallway(finalWorldFrame, e.getSource(), e.getDest());
        }

        redSquare = placeRedSquare(finalWorldFrame);  // Line ~160

        fixWalls(finalWorldFrame);
        addDoors(finalWorldFrame);
        Coordinate avatar = getFloorTile(finalWorldFrame);
        finalWorldFrame[avatar.x][avatar.y] = Tileset.AVATAR;
        ter.renderFrame(finalWorldFrame);
        ArrayList<String> allCommands = new ArrayList<>();
        allCommands.add("");
        for (int i = 0; i < input.length(); i++) {
            if (interact(finalWorldFrame, i, input, avatar, allCommands)) {
                break;
            }
            if (replay) {
                ter.renderFrame(finalWorldFrame);
                StdDraw.pause(100);
            }
        }
        ter.renderFrame(finalWorldFrame);
        return finalWorldFrame;
    }
    public void addDoors(TETile[][] world) {
        for (int i = 0; i < WIDTH; i++) {
            for (int j = 0; j < HEIGHT; j++) {
                if (world[i][j].equals(Tileset.FLOOR)) {
                    boolean upOpening = world[i][j + 1].equals(Tileset.FLOOR) && world[i + 1][j + 1].equals(Tileset.FLOOR) && world[i - 1][j + 1].equals(Tileset.FLOOR) && world[i + 1][j].equals(Tileset.WALL) && world[i - 1][j].equals(Tileset.WALL);
                    boolean downOpening = world[i][j - 1].equals(Tileset.FLOOR) && world[i + 1][j - 1].equals(Tileset.FLOOR) && world[i - 1][j - 1].equals(Tileset.FLOOR) && world[i + 1][j].equals(Tileset.WALL) && world[i - 1][j].equals(Tileset.WALL);
                    boolean rightOpening = world[i + 1][j].equals(Tileset.FLOOR) && world[i + 1][j + 1].equals(Tileset.FLOOR) && world[i + 1][j - 1].equals(Tileset.FLOOR) && world[i][j + 1].equals(Tileset.WALL) && world[i][j - 1].equals(Tileset.WALL);
                    boolean leftOpening = world[i - 1][j].equals(Tileset.FLOOR) && world[i - 1][j + 1].equals(Tileset.FLOOR) && world[i - 1][j - 1].equals(Tileset.FLOOR) && world[i][j + 1].equals(Tileset.WALL) && world[i][j - 1].equals(Tileset.WALL);
                    if (upOpening || downOpening || rightOpening || leftOpening && RandomUtils.bernoulli(rand, 0.1)) {
                        world[i][j] = Tileset.UNLOCKED_DOOR;
                    }
                }
            }
        }
    }
    public boolean interact(TETile[][] world, int i, String input, Coordinate avatar, ArrayList<String> allCommands) {
        char c = input.charAt(i);
        if (i != 0 && input.charAt(i - 1) == ':') {
            return false;
        }
        if (c == ':') {
            if (input.length() > i + 1) {
                char next = input.charAt(i + 1);
                if (next == 'q' || next == 'Q') {
                    Utils.writeContents(SAVE_DATA, allCommands.get(0));
                    Utils.writeContents(SEED, this.seed);
                    StdDraw.clear(Color.BLACK);
                    StdDraw.show();
                    return true;
                }
            }
        }
        allCommands.add(allCommands.get(0) + c);
        allCommands.remove(0);
        if (c == 'e' || c == 'E') {
            if (world[avatar.x + 1][avatar.y].equals(Tileset.UNLOCKED_DOOR)) {
                world[avatar.x + 1][avatar.y] = Tileset.FLOOR;
            }
            if (world[avatar.x - 1][avatar.y].equals(Tileset.UNLOCKED_DOOR)) {
                world[avatar.x - 1][avatar.y] = Tileset.FLOOR;
            }
            if (world[avatar.x][avatar.y + 1].equals(Tileset.UNLOCKED_DOOR)) {
                world[avatar.x][avatar.y + 1] = Tileset.FLOOR;
            }
            if (world[avatar.x][avatar.y - 1].equals(Tileset.UNLOCKED_DOOR)) {
                world[avatar.x][avatar.y - 1] = Tileset.FLOOR;
            }
        }
        if (c == 'w' || c == 'W') {
            TETile nextTile = world[avatar.x][avatar.y + 1];
            if (nextTile.equals(Tileset.FLOOR) || "red square".equals(nextTile.description())) {
                world[avatar.x][avatar.y] = Tileset.FLOOR;
                world[avatar.x][avatar.y + 1] = Tileset.AVATAR;
                avatar.y++;
            }
        }

        if (c == 'a' || c == 'A') {
            TETile nextTile = world[avatar.x - 1][avatar.y];
            if (nextTile.equals(Tileset.FLOOR) || "red square".equals(nextTile.description())) {
                world[avatar.x][avatar.y] = Tileset.FLOOR;
                world[avatar.x - 1][avatar.y] = Tileset.AVATAR;
                avatar.x--;
            }
        }

        if (c == 's' || c == 'S') {
            TETile nextTile = world[avatar.x][avatar.y - 1];
            if (nextTile.equals(Tileset.FLOOR) || "red square".equals(nextTile.description())) {
                world[avatar.x][avatar.y] = Tileset.FLOOR;
                world[avatar.x][avatar.y - 1] = Tileset.AVATAR;
                avatar.y--;
            }
        }

        if (c == 'd' || c == 'D') {
            TETile nextTile = world[avatar.x + 1][avatar.y];
            if (nextTile.equals(Tileset.FLOOR) || "red square".equals(nextTile.description())) {
                world[avatar.x][avatar.y] = Tileset.FLOOR;
                world[avatar.x + 1][avatar.y] = Tileset.AVATAR;
                avatar.x++;
            }
        }

        if (c == 'b' || c == 'B') {
            ArrayList<Coordinate> path = bfsShortestPath(avatar, redSquare);
            for (Coordinate step : path) {
                // Update world to move the avatar to the new step
                moveAvatarToCoordinate(world, avatar, step);
                ter.renderFrame(world);
            }
        }

        // Check if avatar has reached the red square
        if (avatar.x == redSquare.x && avatar.y == redSquare.y) {
            System.exit(0);  // Exit the program
        }

        return false;
    }
    public void hud(TETile[][] world, TETile[][] displayWorld) {
        StdDraw.setPenColor(Color.white);
        if (StdDraw.mouseX() < 0 || StdDraw.mouseX() >= WIDTH || StdDraw.mouseY() < 0 || StdDraw.mouseY() >= HEIGHT) {
            return;
        }
        TETile t = world[(int) StdDraw.mouseX()][(int) StdDraw.mouseY()];
        if (t.equals(Tileset.FLOOR)) {
            StdDraw.text(WIDTH - 5, HEIGHT + 1, "Floor");
        }
        if (t.equals(Tileset.WALL)) {
            StdDraw.text(WIDTH - 5, HEIGHT + 1, "Wall");
        }
        if (t.equals(Tileset.NOTHING)) {
            StdDraw.text(WIDTH - 5, HEIGHT + 1, "Void");
        }
        if (t.equals(Tileset.UNLOCKED_DOOR)) {
            StdDraw.text(WIDTH - 5, HEIGHT + 1, "Door - Open with 'E'");
        }
        StdDraw.show();
        ter.renderFrame(displayWorld);
    }
    public void run(TETile[][] world) {
        boolean fogOfWar = false;
        TETile[][] foggyWorld = new TETile[WIDTH][HEIGHT];
        Coordinate avatar = new Coordinate(0, 0);
        ArrayList<String> allCommands = new ArrayList<>();
        allCommands.add(Utils.readContentsAsString(SAVE_DATA));
        for (int i = 0; i < WIDTH; i++) {
            for (int j = 0; j < HEIGHT; j++) {
                if (world[i][j].equals(Tileset.AVATAR)) {
                    avatar = new Coordinate(i, j);
                    break;
                }
            }
        }
        while (true) {
            while (!StdDraw.hasNextKeyTyped()) {
                if (fogOfWar) {
                    hud(world, foggyWorld);
                } else {
                    hud(world, world);
                }
            }
            char curr = StdDraw.nextKeyTyped();
            if (curr == ':') {
                while (!StdDraw.hasNextKeyTyped()) {
                    if (fogOfWar) {
                        hud(world, foggyWorld);
                    } else {
                        hud(world, world);
                    }
                }
                char next = StdDraw.nextKeyTyped();
                if (next == 'q' || next == 'Q') {
                    interact(world, 0, ":q", avatar, allCommands);
                    break;
                }
            }
            if (curr == 'f' || curr == 'F') {
                fogOfWar = !fogOfWar;
            }
            interact(world, 0, "" + curr, avatar, allCommands);
            if (fogOfWar) {
                for (int i = 0; i < WIDTH; i++) {
                    for (int j = 0; j < HEIGHT; j++) {
                        foggyWorld[i][j] = Tileset.NOTHING;
                        int totalOffset = Math.abs(i - avatar.x) + Math.abs(j - avatar.y);
                        if (totalOffset < 5) {
                            foggyWorld[i][j] = world[i][j];
                        }
                    }
                }
                ter.renderFrame(foggyWorld);
            } else {
                ter.renderFrame(world);
            }
        }
    }

    public Coordinate getFloorTile(TETile[][] world) {
        ArrayList<Coordinate> tiles = new ArrayList<>();
        for (int i = 0; i < WIDTH; i++) {
            for (int j = 0; j < HEIGHT; j++) {
                if (world[i][j].equals(Tileset.FLOOR)) {
                    tiles.add(new Coordinate(i, j));
                }
            }
        }
        return tiles.get(RandomUtils.uniform(rand, tiles.size()));
    }

    private Coordinate placeRedSquare(TETile[][] world) {  // Line ~380
        Coordinate location = getFloorTile(world);
        world[location.x][location.y] = new TETile('\u2588', Color.RED, Color.RED, "red square");
        return location;
    }


    private void fixWalls(TETile[][] world) {
        for (int i = 1; i < WIDTH - 1; i++) {
            for (int j = 1; j < HEIGHT - 1; j++) {
                if (world[i][j].equals(Tileset.FLOOR)) {
                    if (world[i - 1][j - 1].equals(Tileset.NOTHING)) {
                        world[i - 1][j - 1] = Tileset.WALL;
                    }
                    if (world[i - 1][j].equals(Tileset.NOTHING)) {
                        world[i - 1][j] = Tileset.WALL;
                    }
                    if (world[i - 1][j + 1].equals(Tileset.NOTHING)) {
                        world[i - 1][j + 1] = Tileset.WALL;
                    }
                    if (world[i][j + 1].equals(Tileset.NOTHING)) {
                        world[i][j + 1] = Tileset.WALL;
                    }
                    if (world[i + 1][j + 1].equals(Tileset.NOTHING)) {
                        world[i + 1][j + 1] = Tileset.WALL;
                    }
                    if (world[i + 1][j].equals(Tileset.NOTHING)) {
                        world[i + 1][j] = Tileset.WALL;
                    }
                    if (world[i + 1][j - 1].equals(Tileset.NOTHING)) {
                        world[i + 1][j - 1] = Tileset.WALL;
                    }
                    if (world[i][j - 1].equals(Tileset.NOTHING)) {
                        world[i][j - 1] = Tileset.WALL;
                    }
                }
            }
            ter.renderFrame(world);
        }
    }

    private void generateHallway(TETile[][] world, int from, int to) {
        int fromX = rooms.get(from)[0] + 1 + RandomUtils.uniform(rand, rooms.get(from)[2] - 2);
        int fromY = rooms.get(from)[1] + 1 + RandomUtils.uniform(rand, rooms.get(from)[3] - 2);
        int toX = rooms.get(to)[0] + 1 + RandomUtils.uniform(rand, rooms.get(to)[2] - 2);
        int toY = rooms.get(to)[1] + 1 + RandomUtils.uniform(rand, rooms.get(to)[3] - 2);
        // horizontal, then vertical
        if (RandomUtils.bernoulli(rand)) {
            if (fromX < toX) {
                for (int i = fromX; i < toX + 1; i++) {
                    world[i][fromY] = Tileset.FLOOR;
                }
                ter.renderFrame(world);
            } else {
                for (int i = fromX; i > toX - 1; i--) {
                    world[i][fromY] = Tileset.FLOOR;
                }
                ter.renderFrame(world);
            }
            if (fromY < toY) {
                for (int i = fromY; i < toY + 1; i++) {
                    world[toX][i] = Tileset.FLOOR;
                }
                ter.renderFrame(world);
            } else {
                for (int i = fromY; i > toY - 1; i--) {
                    world[toX][i] = Tileset.FLOOR;
                }
                ter.renderFrame(world);
            }
        } else { // vertical, then horizontal
            if (fromY < toY) {
                for (int i = fromY; i < toY + 1; i++) {
                    world[fromX][i] = Tileset.FLOOR;
                }
                ter.renderFrame(world);
            } else {
                for (int i = fromY; i > toY - 1; i--) {
                    world[fromX][i] = Tileset.FLOOR;
                }
                ter.renderFrame(world);
            }
            if (fromX < toX) {
                for (int i = fromX; i < toX + 1; i++) {
                    world[i][toY] = Tileset.FLOOR;
                }
                ter.renderFrame(world);
            } else {
                for (int i = fromX; i > toX - 1; i--) {
                    world[i][toY] = Tileset.FLOOR;
                }
                ter.renderFrame(world);
            }
        }
    }

    private void generateRooms(int numRooms, TETile[][] world) {
        int width, height, lowerLeftCornerX, lowerLeftCornerY;
        for (int i = 0; i < numRooms; i++) {
            width = RandomUtils.uniform(rand, LOWER_BOUND, HIGHER_BOUND);
            height = RandomUtils.uniform(rand, LOWER_BOUND, HIGHER_BOUND);
            lowerLeftCornerX = RandomUtils.uniform(rand, WIDTH - width);
            lowerLeftCornerY = RandomUtils.uniform(rand, HEIGHT - height);
            while (roomOverlap(world, lowerLeftCornerX, lowerLeftCornerY, width, height)) {
                width = RandomUtils.uniform(rand, LOWER_BOUND, HIGHER_BOUND);
                height = RandomUtils.uniform(rand, LOWER_BOUND, HIGHER_BOUND);
                lowerLeftCornerX = RandomUtils.uniform(rand, WIDTH - width - 1);
                lowerLeftCornerY = RandomUtils.uniform(rand, HEIGHT - height - 1);
            }
            for (int j = lowerLeftCornerX; j < lowerLeftCornerX + width; j++) {
                for (int k = lowerLeftCornerY; k < lowerLeftCornerY + height; k++) {
                    boolean inBetweenX = j > lowerLeftCornerX && j < lowerLeftCornerX + width - 1;
                    boolean inBetweenY = k > lowerLeftCornerY && k < lowerLeftCornerY + height - 1;
                    if (inBetweenX && inBetweenY) {
                        world[j][k] = Tileset.FLOOR;
                    } else {
                        world[j][k] = Tileset.WALL;
                    }
                }
                ter.renderFrame(world);
            }
            int[] roomData = {lowerLeftCornerX, lowerLeftCornerY, width, height};
            rooms.put(i, roomData);
            graph.addVertex(i);
        }
    }

    private boolean roomOverlap(TETile[][] world, int lowerLeftCornerX, int lowerLeftCornerY, int width, int height) {
        for (int i = lowerLeftCornerX; i < lowerLeftCornerX + width; i++) {
            for (int j = lowerLeftCornerY; j < lowerLeftCornerY + height; j++) {
                if (world[i][j] == Tileset.FLOOR || world[i][j] == Tileset.WALL) {
                    return true;
                }
            }
        }
        return false;
    }
    private ArrayList<Coordinate> bfsShortestPath(Coordinate start, Coordinate goal) {
        // 1. Check if start and goal are valid
        if (kruskalsTree.containsVertex(coordinateToVertex(start)) == false ||
                kruskalsTree.containsVertex(coordinateToVertex(goal)) == false) {
            System.out.println("Start or Goal is not valid.");
            return null;
        }

        Queue<Coordinate> queue = new LinkedList<>();
        HashMap<Coordinate, Coordinate> cameFrom = new HashMap<>();
        ArrayList<Coordinate> path = new ArrayList<>();

        queue.add(start);
        cameFrom.put(start, null);

        while (!queue.isEmpty()) {
            Coordinate current = queue.poll();

            // 2. Print the size of the queue
            System.out.println("Queue size: " + queue.size());

            if (current.equals(goal)) {
                // 3. Print when we find the goal
                System.out.println("Found the goal!");

                while (current != null) {
                    path.add(current);
                    current = cameFrom.get(current);
                }
                Collections.reverse(path);
                return path;
            }

            ArrayList<Coordinate> neighbors = getValidNeighbors(current);

            for (Coordinate neighbor : neighbors) {
                if (!cameFrom.containsKey(neighbor)) {
                    queue.add(neighbor);
                    cameFrom.put(neighbor, current);
                }
            }
        }

        System.out.println("Couldn't find a path to the goal.");
        return null;
    }


    private ArrayList<Coordinate> getValidNeighbors(Coordinate coord) {
        ArrayList<Coordinate> neighbors = new ArrayList<>();

        int[][] directions = {
                {-1, 0},  // Left
                {1, 0},   // Right
                {0, -1},  // Down
                {0, 1}    // Up
        };

        for (int[] dir : directions) {
            int x = coord.x + dir[0];
            int y = coord.y + dir[1];

            // Check if the coordinates are valid and correspond to a floor tile
            if (isValidCoordinate(x, y) && kruskalsTree.containsVertex(coordinateToVertex(new Coordinate(x, y)))) {
                neighbors.add(new Coordinate(x, y));
            }

        }

        return neighbors;
    }

    private boolean isValidCoordinate(int x, int y) {
        return x >= 0 && x < WIDTH && y >= 0 && y < HEIGHT;
    }

    public int coordinateToVertex(Coordinate c) {
        int maxWidth = WIDTH;
        return c.y * maxWidth + c.x;
    }

    private void moveAvatarToCoordinate(TETile[][] world, Coordinate avatar, Coordinate step) {
        // Update world to move the avatar from its current position to step
        world[avatar.x][avatar.y] = Tileset.FLOOR;
        avatar.x = step.x;
        avatar.y = step.y;
        world[avatar.x][avatar.y] = Tileset.AVATAR;
    }

    public class Coordinate implements Serializable {
        private int x;
        private int y;
        public Coordinate(int x, int y) {
            this.x = x;
            this.y = y;
        }

        public int getX() {
            return x;
        }

        public int getY() {
            return y;
        }

        public void setX(int x) {
            this.x = x;
        }

        public void setY(int y) {
            this.y = y;
        }
    }
}