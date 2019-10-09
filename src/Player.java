import java.util.*;

// 1. S'il y a un radar de disponible et qu'il y a moins de 5 minerais découverts, prendre un radar et le mettre au prochain endroit
// 2. S'il n'y a pas de radar de disponible et qu'il n'y a plus de minerai découvert, miner au hasard
// 3. Choisir le minerai le plus proche de sa position et aller le miner
// 3. S'il y a un piège de disponible, et qu'il reste 2 minerais dans la veine, prendre un piège (1 fois sur 2).
// Si toutes les positions de radars ont été prises, le robot peut prendre un radar (1 fois sur 2).
// Bien mettre à jour la liste des minerais disponibles avec les informations des radars (notamment quand des robots ennemis minent)
// S'il n'y a plus de minerai sur la case ou si après de miner le robot de transporte rien, il va miner le filon le plus proche.
// Se souvenir où les pièges ont été posés

// Structures :
// 1 liste ordonnées de position de radar
// 1 liste de cases à miner non triées
// 1 tableau de Case représentant le plateau
// 1 classe Robot avec son id, sa position actuelle, sa destination, ce qu'il transporte.
// 1 classe Case avec s'il y a un trou dessus, le nombre de minerais disponibles, s'il y a un piège allié ou un radar allié.

// TODO: Action robot can take a radar if he mines and a radar is up
// TODO: Mining robots take a trap if there are mining in the vein and no ather ally robot are going to mine this vein
// TODO: DIG and not MOVE to gain some movements
// TODO: Don't dig if it's not an ally who has dig -> avoid enemy trap (add weight to manhattan distance to heuristic).
// TODO: Dig instead wait when no ore is available
// TODO: action robot go for radar only when there is 5 or less ore available
class Player {

    enum Entity_Type {
        ALLY_ROBOT, ENEMY_ROBOT, RADAR, TRAP
    }
    enum Item_Type {
        NONE, RADAR, TRAP, ORE
    }

    private static int width, height;

    private static int radarCooldown;
    private static int trapCooldown;
    private static Robot[] robots = new Robot[5];

    private static ArrayList<Case> oreRemaining = new ArrayList<>();
    private static ArrayList<Case> robotsDestination = new ArrayList<>();

    private static Case[][] board;

    private static LinkedList<Case> radarPositions = new LinkedList<>();

    private static boolean firstTurn = true;

    private static boolean radarRequested = false;

    public static void main(String[] args) {
        Scanner in = new Scanner(System.in);
        width = in.nextInt();
        height = in.nextInt(); // size of the map

        fillRadarPositions();

        board = new Case[height][width];
        for (int i = 0 ; i < height ; i++) {
            for (int j = 0; j < width ; j++) {
                board[i][j] = new Case();
                board[i][j].x = j;
                board[i][j].y = i;
            }
        }

        // game loop
        while (true) {
            int myScore = in.nextInt(); // Amount of ore delivered
            int opponentScore = in.nextInt();
            updateBoard(in);
            updateEntities(in);
            robotTurn();
        }
    }

    private static void updateBoard(Scanner in) {
        oreRemaining = new ArrayList<>();
        for (int i = 0; i < height; i++) {
            for (int j = 0; j < width; j++) {
                String ore = in.next();
                board[i][j].ore = "?".equals(ore) ? -1 : Integer.parseInt(ore); // amount of ore or "?" if unknown
                board[i][j].hole = in.nextInt() == 1; // 1 if cell has a hole
                for (int k = 0 ; k < board[i][j].ore ; k++) {
                    oreRemaining.add(new Case(board[i][j].x, board[i][j].y));
                }
            }
        }
    }

    private static void updateEntities(Scanner in) {
        int entityCount = in.nextInt(); // number of entities visible to you
        radarCooldown = in.nextInt(); // turns left until a new radar can be requested
        trapCooldown = in.nextInt(); // turns left until a new trap can be requested
        for (int i = 0 ; i < entityCount ; i++) {
            int id = in.nextInt(); // unique id of the entity
            int type = in.nextInt(); // 0 for your robot, 1 for other robot, 2 for radar, 3 for trap
            int x = in.nextInt();
            int y = in.nextInt(); // position of the entity
            int item = in.nextInt(); // if this entity is a robot, the item it is carrying (-1 for NONE, 2 for RADAR, 3 for TRAP, 4 for ORE)
            if (firstTurn && type == 0) {
                robots[id % 5] = new Robot(id, x, y);
            }
            if (type == 0) {
                robots[id % 5].item = convertItemType(item);
                robots[id % 5].lastX = robots[id % 5].x;
                robots[id % 5].lastY = robots[id % 5].y;
                robots[id % 5].x = x;
                robots[id % 5].y = y;
                oreRemaining.remove(new Case(robots[id % 5].directionX, robots[id % 5].directionY));
            }
        }
        firstTurn = false;
    }

    private static void robotTurn() {
        radarRequested = false;
        for (int i = 0 ; i < 5 ; i++) {
            if (robots[i].x != -1 && robots[i].y != -1) {
                if (robots[i].item == Item_Type.RADAR) {
                    printMove(robots[i]);
                // Vient de miner du minerai
                } else if (robots[i].hasDoneAction() && robots[i].item == Item_Type.ORE) {
                    System.err.println(robots[i]);
                    goBackToHeadquarters(i);
                    printMove(robots[i]);
                    // A essayé de miner mais n'a rien trouvé
                } else if (!robots[i].isInHeadquarters() && robots[i].hasDoneAction() && robots[i].item == Item_Type.NONE) {
                    goToNextOre(i);
                    // Est dans les quartiers généraux
                } else if (robots[i].isInHeadquarters()) {
                    // S'il faut poser un nouveau radar
                    if (!radarRequested && radarCooldown <= 0 && !radarPositions.isEmpty() && oreRemaining.size() <= 5) {
                        System.out.println("REQUEST RADAR");
                        Case caseRadar = radarPositions.remove();
                        robots[i].directionX = caseRadar.x;
                        robots[i].directionY = caseRadar.y;
                        radarRequested = true;
                        // S'il n'y a plus de radars dispos
                    } else if (!radarRequested && radarCooldown <= 0 && radarPositions.isEmpty() && Math.random() < 0.5) {
                        System.out.println("REQUEST RADAR");
                        radarRequested = true;
                    } else if (oreRemaining.isEmpty()) {
                        System.out.println("WAIT");
                    } else {
                        goToNextOre(i);
                    }
                    // Cas par défaut : doit continuer l'action courante
                } else {
                    printMove(robots[i]);
                }
            } else {
                System.out.println("WAIT");
            }
        }
    }

    private static void printMove(Robot robot) {
        String action = robot.directionX == 0 ? "MOVE" : "DIG";
        System.out.println(action + " " + robot.directionX + " " + robot.directionY);
    }

    private static void goBackToHeadquarters(int i) {
        robots[i % 5].directionX = 0;
    }

    private static void fillRadarPositions() {
        radarPositions.add(new Case(5, 5));
        radarPositions.add(new Case(10, 9));
        radarPositions.add(new Case(14, 5));
        radarPositions.add(new Case(19, 9));
        radarPositions.add(new Case(23, 5));
        radarPositions.add(new Case(28, 9));
        radarPositions.add(new Case(5, 13));
        radarPositions.add(new Case(15, 13));
        radarPositions.add(new Case(24, 13));
    }

    private static void goToNextOre(int i) {
        int index = getNearestOre(i % 5);
        if (index == -1) {
            System.out.println("WAIT");
        } else {
            Case caseToGo = oreRemaining.remove(index);
            robots[i % 5].directionX = caseToGo.x;
            robots[i % 5].directionY = caseToGo.y;
            System.out.println("DIG " + robots[i % 5].directionX + " " + robots[i % 5].directionY);
        }
    }

    private static int getNearestOre(int i) {
        if (oreRemaining.isEmpty()) return -1;
        int index = 0;
        int manhattanDistance = getManhattanDistance(robots[i].x, oreRemaining.get(0).x, robots[i].y, oreRemaining.get(0).y);
        for (int j = 1; j < oreRemaining.size(); j++) {
            int manhattanDistanceTemp = getManhattanDistance(robots[i].x, oreRemaining.get(j).x, robots[i].y, oreRemaining.get(j).y);
            if (manhattanDistanceTemp < manhattanDistance) {
                manhattanDistance = manhattanDistanceTemp;
                index = j;
            }
        }
        return index;
    }

    private static int getManhattanDistance(int x1, int x2, int y1, int y2) {
        return Math.abs(x2-x1) + Math.abs(y2-y1);
    }

    private static Entity_Type convertEntityType(int type) {
        switch (type) {
            case 0: return Entity_Type.ALLY_ROBOT;
            case 1: return Entity_Type.ENEMY_ROBOT;
            case 2: return Entity_Type.RADAR;
            default: return Entity_Type.TRAP;
        }
    }

    private static Item_Type convertItemType(int type) {
        switch (type) {
            case 2: return Item_Type.RADAR;
            case 3: return Item_Type.TRAP;
            case 4: return Item_Type.ORE;
            default:return Item_Type.NONE;
        }
    }

    public static class Case {
        int ore;
        boolean hole;
        boolean trapped;
        boolean radar;
        int idRobot;
        int x;
        int y;

        Case() {
        }

        Case(int x, int y) {
            this.x = x;
            this.y = y;
        }

        @Override
        public String toString() {
            return "Case{" +
                    "ore='" + ore + '\'' +
                    ", hole=" + hole +
                    ", idRobot=" + idRobot +
                    ", x=" + x +
                    ", y=" + y +
                    '}';
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Case aCase = (Case) o;
            return x == aCase.x &&
                    y == aCase.y;
        }

        @Override
        public int hashCode() {
            return Objects.hash(x, y);
        }
    }

    static class Robot extends Entity {
        int directionX;
        int directionY;
        int lastX;
        int lastY;
        Item_Type item;

        Robot(int id, int x, int y) {
            super(id, x, y);
            this.lastX = x;
            this.lastY = y;
            this.directionX = x;
            this.directionY = y;
        }

        boolean hasDoneAction() {
            return super.x != -1 && super.y != -1 && x == lastX && y == lastY;
        }

        @Override
        public String toString() {
            return "Robot{" +
                    "directionX=" + directionX +
                    ", directionY=" + directionY +
                    ", lastX=" + lastX +
                    ", lastY=" + lastY +
                    ", item=" + item +
                    ", id=" + id +
                    ", type=" + type +
                    ", x=" + x +
                    ", y=" + y +
                    '}';
        }
    }

    static class Entity {
        int id;
        Entity_Type type;
        int x;
        int y;

        Entity() {}

        Entity(int id, int x, int y) {
            this.id = id;
            this.x = x;
            this.y = y;
        }

        boolean isInHeadquarters() {
            return x == 0;
        }

        boolean isInNextRadarPosition() {
            if (radarPositions.isEmpty()) return false;
            return x == radarPositions.getFirst().x && y == radarPositions.getFirst().y;
        }

        @Override
        public String toString() {
            return "Entity{" +
                    "id=" + id +
                    ", type=" + type +
                    ", x=" + x +
                    ", y=" + y +
                    '}';
        }
    }
}