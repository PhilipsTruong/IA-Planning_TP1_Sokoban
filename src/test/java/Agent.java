import java.util.Scanner;
import sokoban.SokobanPDDLSolver;

public class Agent {
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);

        // Parse the level from the first turn input
        SokobanPDDLSolver solver = new SokobanPDDLSolver();
        solver.parseLevelInput(scanner);

        // Solve the level using PDDL planning
        String solution = solver.solve();

        if (solution == null || solution.isEmpty()) {
            System.err.println("[Agent] No solution found!");
            return;
        }

        for (char c : solution.toCharArray()) {
            System.out.println(c);
        }
    }
}
