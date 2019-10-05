import java.util.*;
import java.util.stream.Stream;

// TODO: Action robot can take a radar if he mines and a radar is up
// TODO: Mining robots take a trap if there are mining in the vein and no ather ally robot are going to mine this vein
// TODO: DIG and not MOVE to gain some movements
// TODO: Don't dig if it's not an ally who has dig -> avoid enemy trap (add weight to manhattan distance to heuristic).
class Player {

    enum Entity_Type {
        ALLY_ROBOT, ENEMY_ROBOT, RADAR, TRAP
    }
    enum Item_Type {
        NONE, RADAR, TRAP, ORE
    }

    private static int width, height;

    private static boolean isRadarSet = false;
    private static boolean isradarJustSet = false;

    private static boolean initializationTurn = true;

    private static int radarCooldown;
    private static int trapCooldown;
    private static Entity[] robots = new Entity[10];

    private static List<Case> oreRemaining = new ArrayList<>();

    private static Case[][] board;

    private static LinkedList<Case> radarPositions = new LinkedList<>();

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
            for (int i = 0; i < height; i++) {
                for (int j = 0; j < width; j++) {
                    String ore = in.next();
                    board[i][j].ore = "?".equals(ore) ? -1 : Integer.parseInt(ore); // amount of ore or "?" if unknown
                    board[i][j].hole = in.nextInt() == 1; // 1 if cell has a hole
                    if (isradarJustSet && board[i][j].ore > 0) {
                        for (int k = 0 ; k <  board[i][j].ore ; k++) {
                            oreRemaining.add(board[i][j]);
                        }
                    }
                    if (board[i][j].ore == 0) {
                        while (oreRemaining.contains(board[i][j]))
                            oreRemaining.remove(board[i][j]);
                    }
                }
            }
            isradarJustSet = false;
            int entityCount = in.nextInt(); // number of entities visible to you
            radarCooldown = in.nextInt(); // turns left until a new radar can be requested
            trapCooldown = in.nextInt(); // turns left until a new trap can be requested
            for (int i = 0 ; i < entityCount ; i++) {
                int id = in.nextInt(); // unique id of the entity
                int type = in.nextInt(); // 0 for your robot, 1 for other robot, 2 for radar, 3 for trap
                int x = in.nextInt();
                int y = in.nextInt(); // position of the entity
                int item = in.nextInt(); // if this entity is a robot, the item it is carrying (-1 for NONE, 2 for RADAR, 3 for TRAP, 4 for ORE)
                if (initializationTurn) {
                    robots[id] = new Entity(id, x, y);
                    robots[id].type = convertEntityType(type);
                    if (convertEntityType(type) == Entity_Type.ALLY_ROBOT) {
                        robots[id].directionX = x;
                        robots[id].directionY = y;
                    }
                }
                if (id < 10 && (robots[id].type == Entity_Type.ALLY_ROBOT || robots[id].type == Entity_Type.ENEMY_ROBOT)) {
                    robots[id].item = convertItemType(item);
                    robots[id].x = x;
                    robots[id].y = y;
                }
            }
            int firstAllyMiningRobot;
            int lastAllyMiningRobot;
            if (!radarPositions.isEmpty()) {
                putRadar();
                firstAllyMiningRobot = robots[0].type == Entity_Type.ALLY_ROBOT ? 1 : 6;
                lastAllyMiningRobot = firstAllyMiningRobot + 4;
            } else {
                firstAllyMiningRobot = robots[0].type == Entity_Type.ALLY_ROBOT ? 0 : 5;
                lastAllyMiningRobot = firstAllyMiningRobot + 5;
            }
            for (int i = firstAllyMiningRobot ; i < lastAllyMiningRobot ; i++) {
                mineOre(i);
            }
            initializationTurn = false;
        }
    }

    private static void mineOre(int i) {
        if (!oreRemaining.isEmpty() && robots[i].x == 0) {
            chooseOreToGo(i);
            move(i);
        } else if (robots[i].x != robots[i].directionX || robots[i].y != robots[i].directionY) {
            move(i);
        } else if (robots[i].x == robots[i].directionX && robots[i].y == robots[i].directionY) {
            if (board[robots[i].y][robots[i].x].ore == 0) {
                if (oreRemaining.isEmpty())
                    System.out.println("WAIT");
                else {
                    chooseOreToGo(i);
                    move(i);
                }
            } else {
                System.out.println("DIG " + robots[i].x + " " + robots[i].y);
                robots[i].directionX = 0;
            }
        } else {
            System.out.println("WAIT"); // WAIT|MOVE x y|DIG x y|REQUEST item
        }
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

    private static void chooseOreToGo(int i) {
        int index = getNearestOre(i);
        Case caseToGo = oreRemaining.remove(index);
        robots[i].directionX = caseToGo.x;
        robots[i].directionY = caseToGo.y;
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

    private static void move(int i) {
        System.out.println("MOVE " + robots[i].directionX + " " + robots[i].directionY);
    }

    private static void putRadar() {
        if (radarPositions.isEmpty()) {
            System.out.println("WAIT");
            return;
        }
        int position = robots[0].type == Entity_Type.ALLY_ROBOT ? 0 : 5;
        if (robots[position].isInHeadquarters() && radarCooldown == 0 && robots[position].item != Item_Type.RADAR){
            System.out.println("REQUEST RADAR");
            isRadarSet = false;
        } else if (!isRadarSet && robots[position].isInHeadquarters() && radarCooldown > 0 && robots[position].item != Item_Type.RADAR) {
            System.out.println("WAIT");
        } else if (!isRadarSet && !robots[position].isInNextRadarPosition()) {
            System.out.println("MOVE " + radarPositions.getFirst().x + " " + radarPositions.getFirst().y);
        } else if (robots[position].isInNextRadarPosition()) {
            System.out.println("DIG " + robots[position].x + " " + robots[position].y);
            radarPositions.removeFirst();
            isRadarSet = true;
            isradarJustSet = true;
        } else {
            System.out.println("MOVE " + " " + 0 + " " + radarPositions.getFirst().y);
        }
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

    public static class Entity {
        int id;
        Entity_Type type;
        Item_Type item;
        int x;
        int y;
        int directionX;
        int directionY;

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
                    ", item=" + item +
                    ", x=" + x +
                    ", y=" + y +
                    '}';
        }
    }
}