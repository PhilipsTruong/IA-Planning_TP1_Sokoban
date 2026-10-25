package sokoban;

import fr.uga.pddl4j.heuristics.state.StateHeuristic;
import fr.uga.pddl4j.parser.DefaultParsedProblem;
import fr.uga.pddl4j.parser.Parser;
import fr.uga.pddl4j.plan.Plan;
import fr.uga.pddl4j.planners.LogLevel;
import fr.uga.pddl4j.planners.statespace.HSP;
import fr.uga.pddl4j.problem.DefaultProblem;
import fr.uga.pddl4j.problem.operator.Action;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;

public class SokobanPDDLSolver {

    private int width;
    private int height;
    private int boxCount;
    private char[][] map;      // '#' = wall, '*' = goal/target, '.' = floor
    private int pusherX, pusherY;
    private int[][] boxPositions;

    private boolean[][] reachable;
    private boolean[][] canReachGoal;

    // U=0 (dy=-1), R=1 (dx=+1), D=2 (dy=+1), L=3 (dx=-1)
    private static final String[] DIR_NAMES = {"dir-up", "dir-right", "dir-down", "dir-left"};
    private static final int[] DX = {0, 1, 0, -1};
    private static final int[] DY = {-1, 0, 1, 0};

    public void parseLevelInput(Scanner scanner) {
        String firstLine = scanner.nextLine().trim();
        String[] parts = firstLine.split("\\s+");
        width = Integer.parseInt(parts[0]);
        height = Integer.parseInt(parts[1]);
        boxCount = Integer.parseInt(parts[2]);

        map = new char[height][width];
        for (int y = 0; y < height; y++) {
            String line = scanner.nextLine();
            for (int x = 0; x < width; x++) {
                map[y][x] = (x < line.length()) ? line.charAt(x) : '.';
            }
        }

        String[] pusherParts = scanner.nextLine().trim().split("\\s+");
        pusherX = Integer.parseInt(pusherParts[0]);
        pusherY = Integer.parseInt(pusherParts[1]);

        boxPositions = new int[boxCount][2];
        for (int i = 0; i < boxCount; i++) {
            String[] boxParts = scanner.nextLine().trim().split("\\s+");
            boxPositions[i][0] = Integer.parseInt(boxParts[0]);
            boxPositions[i][1] = Integer.parseInt(boxParts[1]);
        }

        computeReachableCells();
        computeBoxDeadlocks();
    }

    private boolean isInsideAndNotWall(int x, int y) {
        return x >= 0 && x < width && y >= 0 && y < height && map[y][x] != '#';
    }

    // BFS 

    private void computeReachableCells() {
        reachable = new boolean[height][width];
        Queue<int[]> queue = new ArrayDeque<>();

        if (isInsideAndNotWall(pusherX, pusherY)) {
            reachable[pusherY][pusherX] = true;
            queue.add(new int[]{pusherX, pusherY});
        }

        while (!queue.isEmpty()) {
            int[] curr = queue.poll();
            int cx = curr[0], cy = curr[1];
            for (int d = 0; d < 4; d++) {
                int nx = cx + DX[d];
                int ny = cy + DY[d];
                if (isInsideAndNotWall(nx, ny) && !reachable[ny][nx]) {
                    reachable[ny][nx] = true;
                    queue.add(new int[]{nx, ny});
                }
            }
        }
    }

    private void computeBoxDeadlocks() {
        canReachGoal = new boolean[height][width];
        Queue<int[]> queue = new ArrayDeque<>();

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                if (reachable[y][x] && isGoal(x, y)) {
                    canReachGoal[y][x] = true;
                    queue.add(new int[]{x, y});
                }
            }
        }

        while (!queue.isEmpty()) {
            int[] curr = queue.poll();
            int tx = curr[0], ty = curr[1];

            for (int d = 0; d < 4; d++) {
                int bx = tx - DX[d];
                int by = ty - DY[d];
                int px = bx - DX[d];
                int py = by - DY[d];

                if (isWalkable(bx, by) && isWalkable(px, py) && !canReachGoal[by][bx]) {
                    canReachGoal[by][bx] = true;
                    queue.add(new int[]{bx, by});
                }
            }
        }
    }

    private boolean isWalkable(int x, int y) {
        if (x < 0 || x >= width || y < 0 || y >= height) return false;
        return reachable[y][x];
    }

    private boolean isGoal(int x, int y) {
        return map[y][x] == '*';
    }

    private String locName(int x, int y) {
        return "loc_" + x + "_" + y;
    }

    private List<int[]> getGoalLocations() {
        List<int[]> goals = new ArrayList<>();
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                if (isWalkable(x, y) && isGoal(x, y)) {
                    goals.add(new int[]{x, y});
                }
            }
        }
        return goals;
    }

    public String generateProblemPDDL() {
        List<int[]> goals = getGoalLocations();

        StringBuilder sb = new StringBuilder();
        sb.append("(define (problem sokoban-level)\n");
        sb.append("  (:domain sokoban)\n");

        sb.append("  (:objects\n    ");
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                if (isWalkable(x, y)) {
                    sb.append(locName(x, y)).append(" ");
                }
            }
        }
        sb.append("- location\n");
        sb.append("    dir-up dir-right dir-down dir-left - direction\n");
        sb.append("  )\n");

        // -- Init --
        sb.append("  (:init\n");
        sb.append("    (player-at ").append(locName(pusherX, pusherY)).append(")\n");

        Set<String> boxCells = new HashSet<>();
        for (int i = 0; i < boxCount; i++) {
            int bx = boxPositions[i][0];
            int by = boxPositions[i][1];
            sb.append("    (box-at ").append(locName(bx, by)).append(")\n");
            boxCells.add(locName(bx, by));
        }

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                if (isWalkable(x, y) && !boxCells.contains(locName(x, y))) {
                    sb.append("    (clear ").append(locName(x, y)).append(")\n");
                }
            }
        }

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                if (!isWalkable(x, y)) continue;
                for (int d = 0; d < 4; d++) {
                    int nx = x + DX[d];
                    int ny = y + DY[d];
                    if (isWalkable(nx, ny)) {
                        sb.append("    (adjacent ")
                          .append(locName(x, y)).append(" ")
                          .append(locName(nx, ny)).append(" ")
                          .append(DIR_NAMES[d]).append(")\n");

                        int tx = nx + DX[d];
                        int ty = ny + DY[d];
                        if (isWalkable(tx, ty) && canReachGoal[ty][tx]) {
                            sb.append("    (can-push ")
                              .append(locName(x, y)).append(" ")
                              .append(locName(nx, ny)).append(" ")
                              .append(locName(tx, ty)).append(" ")
                              .append(DIR_NAMES[d]).append(")\n");
                        }
                    }
                }
            }
        }
        sb.append("  )\n");

        // -- Goal --
        sb.append("  (:goal (and\n");
        for (int[] g : goals) {
            sb.append("    (box-at ").append(locName(g[0], g[1])).append(")\n");
        }
        sb.append("  ))\n");
        sb.append(")\n");
        return sb.toString();
    }

    public String getDomainPDDL() {
        return "(define (domain sokoban)\n" +
               "  (:requirements :strips :typing)\n" +
               "  (:types location direction)\n" +
               "  (:predicates\n" +
               "    (player-at ?l - location)\n" +
               "    (box-at ?l - location)\n" +
               "    (clear ?l - location)\n" +
               "    (adjacent ?from - location ?to - location ?d - direction)\n" +
               "    (can-push ?from - location ?bloc - location ?to - location ?d - direction)\n" +
               "  )\n" +
               "  (:action move\n" +
               "    :parameters (?from - location ?to - location ?d - direction)\n" +
               "    :precondition (and\n" +
               "      (player-at ?from)\n" +
               "      (adjacent ?from ?to ?d)\n" +
               "      (clear ?to))\n" +
               "    :effect (and\n" +
               "      (player-at ?to)\n" +
               "      (not (player-at ?from)))\n" +
               "  )\n" +
               "  (:action push\n" +
               "    :parameters (?from - location ?bloc - location ?to - location ?d - direction)\n" +
               "    :precondition (and\n" +
               "      (player-at ?from)\n" +
               "      (box-at ?bloc)\n" +
               "      (clear ?to)\n" +
               "      (can-push ?from ?bloc ?to ?d))\n" +
               "    :effect (and\n" +
               "      (player-at ?bloc)\n" +
               "      (not (player-at ?from))\n" +
               "      (box-at ?to)\n" +
               "      (not (box-at ?bloc))\n" +
               "      (clear ?bloc)\n" +
               "      (not (clear ?to)))\n" +
               "  )\n" +
               ")\n";
    }

    public String solve() {
        try {
            Thread.currentThread().setContextClassLoader(
                SokobanPDDLSolver.class.getClassLoader());

            File domainFile = File.createTempFile("sokoban-domain-", ".pddl");
            domainFile.deleteOnExit();
            Files.write(domainFile.toPath(), getDomainPDDL().getBytes(StandardCharsets.UTF_8));

            File problemFile = File.createTempFile("sokoban-problem-", ".pddl");
            problemFile.deleteOnExit();
            Files.write(problemFile.toPath(), generateProblemPDDL().getBytes(StandardCharsets.UTF_8));

            Parser parser = new Parser();
            DefaultParsedProblem parsedProblem = parser.parse(domainFile, problemFile);
            DefaultProblem problem = new DefaultProblem(parsedProblem);
            problem.instantiate();

            System.err.println("[SokobanSolver] Map: " + width + "x" + height
                + ", boxes: " + boxCount
                + ", ground actions: " + problem.getActions().size()
                + ", fluents: " + problem.getFluents().size());

            double[] weights = {1.8, 3.0, 1.2};
            int[] timeouts = {8, 8, 10};

            for (int i = 0; i < weights.length; i++) {
                HSP planner = new HSP();
                planner.setLogLevel(LogLevel.OFF);
                planner.setHeuristic(StateHeuristic.Name.FAST_FORWARD);
                planner.setHeuristicWeight(weights[i]);
                planner.setTimeout(timeouts[i]);

                Plan plan = planner.solve(problem);
                if (plan != null && !plan.isEmpty()) {
                    String moves = extractMovesFromPlan(plan, problem);
                    if (moves != null && !moves.isEmpty()) {
                        System.err.println("[SokobanSolver] Found solution ("
                            + moves.length() + " moves, weight=" + weights[i] + "): " + moves);
                        return moves;
                    }
                }
            }

            System.err.println("[SokobanSolver] No plan returned.");
            return null;

        } catch (Exception e) {
            System.err.println("[SokobanSolver] Error: " + e.getMessage());
            e.printStackTrace(System.err);
            return null;
        }
    }

    private String extractMovesFromPlan(Plan plan, DefaultProblem problem) {
        StringBuilder moves = new StringBuilder();
        List<String> constants = problem.getConstantSymbols();

        for (Action action : plan.actions()) {
            int[] params = action.getInstantiations();
            int dirParamIdx = "move".equals(action.getName()) ? 2 : 3;

            if (dirParamIdx < params.length) {
                String constName = constants.get(params[dirParamIdx]);
                switch (constName) {
                    case "dir-up":    moves.append('U'); break;
                    case "dir-right": moves.append('R'); break;
                    case "dir-down":  moves.append('D'); break;
                    case "dir-left":  moves.append('L'); break;
                }
            }
        }
        return moves.toString();
    }
}