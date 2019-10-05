import java.util.*;

// TODO: If robot dig and find nothing, he returns to headquarters
// TODO: Sometimes, robots goes to the same ore vein
// TODO: Modify TreeSet Collection to care about ore number in same case
// TODO: Robots can start and try digging ore.
// TODO: Improve radar robot path. -> list of radar position.
// TODO: Radar robot can mine after his job.
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
    private static int nextRadarX = 3;
    private static int nextRadarY = 3;

    private static boolean initializationTurn = true;

    private static int radarCooldown;
    private static int trapCooldown;
    private static Entity[] robots = new Entity[10];

    private static List<Case> oreRemaining = new ArrayList<>();

    private static Case[][] board;

    public static void main(String[] args) {
        Scanner in = new Scanner(System.in);
        width = in.nextInt();
        height = in.nextInt(); // size of the map

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
                    board[i][j].ore = in.next(); // amount of ore or "?" if unknown
                    board[i][j].hole = in.nextInt() == 1; // 1 if cell has a hole
                    if (isradarJustSet && !board[i][j].ore.equals("?") && Integer.parseInt(board[i][j].ore) > 0) {
                        for (int k = 0 ; k <  Integer.parseInt(board[i][j].ore) ; k++) {
                            oreRemaining.add(board[i][j]);
                        }
                    }
                    if (!board[i][j].ore.equals("?") && Integer.parseInt(board[i][j].ore) == 0) {
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
            putRadar();
            for (int i = 0; i < 5; i++) {
                System.err.println(robots[i]);
            }
            int firstAllyMiningRobot = robots[0].type == Entity_Type.ALLY_ROBOT ? 1 : 6;
            for (int i = firstAllyMiningRobot ; i < firstAllyMiningRobot + 4 ; i++) {
                System.err.println(oreRemaining.size());
                if (!oreRemaining.isEmpty() && robots[i].x == 0) {
                    chooseOreToGo(i);
                    move(i);
                } else if (robots[i].x != robots[i].directionX || robots[i].y != robots[i].directionY) {
                    move(i);
                } else if (robots[i].x == robots[i].directionX && robots[i].y == robots[i].directionY) {
                    System.out.println("DIG " + robots[i].x + " " + robots[i].y);
                    robots[i].directionX = 0;
                } else {
                    System.out.println("WAIT"); // WAIT|MOVE x y|DIG x y|REQUEST item
                }
            }
            initializationTurn = false;
        }
    }

    private static void chooseOreToGo(int i) {
        Case caseToGo = oreRemaining.remove(0);
        System.err.println(caseToGo);
        robots[i].directionX = caseToGo.x;
        robots[i].directionY = caseToGo.y;
    }

    private static void move(int i) {
        System.out.println("MOVE " + robots[i].directionX + " " + robots[i].directionY);
    }

    private static void putRadar() {
        int position = robots[0].type == Entity_Type.ALLY_ROBOT ? 0 : 5;
        if (robots[position].isInHeadquarters() && radarCooldown == 0 && robots[position].item != Item_Type.RADAR){
            System.out.println("REQUEST RADAR");
            isRadarSet = false;
        } else if (!isRadarSet && robots[position].isInHeadquarters() && radarCooldown > 0 && robots[position].item != Item_Type.RADAR) {
            System.out.println("WAIT");
        } else if (!isRadarSet && !robots[position].isInNextRadarPosition()) {
            System.out.println("MOVE " + nextRadarX + " " + nextRadarY);
        } else if (robots[position].isInNextRadarPosition()) {
            System.out.println("DIG " + robots[position].x + " " + robots[position].y);
            calculateNextRadarPosition();
            isRadarSet = true;
            isradarJustSet = true;
        } else {
            System.out.println("MOVE " + " " + 0 + " " + nextRadarY);
        }
    }

    private static void calculateNextRadarPosition() {
        if (nextRadarX + 8 < width) {
            nextRadarX+=8;
        } else if (nextRadarY + 8 < height) {
            nextRadarY+=8;
            nextRadarX=4;
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
        String ore;
        boolean hole;
        int idRobot;
        int x;
        int y;

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
            return x == nextRadarX && y == nextRadarY;
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

    static class CaseComparator implements Comparator<Case> {

        @Override
        public int compare(Case o1, Case o2) {
            if (o1.x == o2.x && o1.y == o2.y) return 0;
            return o1.x + o1.y > o2.x + o2.y ? 1 : -1;
        }
    }
}