import java.util.*;

class Player {

    enum Entity_Type {
        ALLY_ROBOT, ENEMY_ROBOT, RADAR, TRAP
    }
    enum Item_Type {
        NONE, RADAR, TRAP, ORE
    }

    private static int width, height;

    private static boolean isRadarSet = false;
    private static int nextRadarX = 3;
    private static int nextRadarY = 3;

    private static boolean initializationTurn = true;

    private static int radarCooldown;
    private static int trapCooldown;
    private static Entity[] robots = new Entity[5];

    public static void main(String[] args) {
        Scanner in = new Scanner(System.in);
        width = in.nextInt();
        height = in.nextInt(); // size of the map

        Case[][] board = new Case[height][width];

        // game loop
        while (true) {
            int myScore = in.nextInt(); // Amount of ore delivered
            int opponentScore = in.nextInt();
            for (int i = 0; i < height; i++) {
                for (int j = 0; j < width; j++) {
                    board[i][j] = new Case();
                    board[i][j].ore = in.next(); // amount of ore or "?" if unknown
                    board[i][j].hole = in.nextInt() == 1; // 1 if cell has a hole
                }
            }
            int entityCount = in.nextInt(); // number of entities visible to you
            radarCooldown = in.nextInt(); // turns left until a new radar can be requested
            trapCooldown = in.nextInt(); // turns left until a new trap can be requested
            for (int i = 0 ; i < entityCount ; i++) {
                int id = in.nextInt(); // unique id of the entity
                int type = in.nextInt(); // 0 for your robot, 1 for other robot, 2 for radar, 3 for trap
                int x = in.nextInt();
                int y = in.nextInt(); // position of the entity
                int item = in.nextInt(); // if this entity is a robot, the item it is carrying (-1 for NONE, 2 for RADAR, 3 for TRAP, 4 for ORE)
                if (initializationTurn && id < 5) {
                    robots[id] = new Entity(id, x, y);
                }
                if (id < 5) {
                    robots[id].item = convertItemType(item);
                    robots[id].type = convertEntityType(type);
                    robots[id].x = x;
                    robots[id].y = y;
                }
            }
            putRadar();
            for (int i = 0; i < 5; i++) {
                System.err.println(robots[i]);
            }
            for (int i = 1 ; i < 5 ; i++) {
                System.out.println("WAIT"); // WAIT|MOVE x y|DIG x y|REQUEST item
            }
            initializationTurn = false;
        }
    }

    private static void putRadar() {
        if (robots[0].isInHeadquarters() && radarCooldown == 0 && robots[0].item != Item_Type.RADAR){
            System.out.println("REQUEST RADAR");
            isRadarSet = false;
        } else if (!isRadarSet && robots[0].isInHeadquarters() && radarCooldown > 0 && robots[0].item != Item_Type.RADAR) {
            System.out.println("WAIT");
        } else if (!isRadarSet && !robots[0].isInNextRadarPosition()) {
            System.out.println("MOVE " + nextRadarX + " " + nextRadarY);
        } else if (robots[0].isInNextRadarPosition()) {
            System.out.println("DIG " + robots[0].x + " " + robots[0].y);
            calculateNextRadarPosition();
            isRadarSet = true;
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
    }

    public static class Entity {
        int id;
        Entity_Type type;
        Item_Type item;
        int x;
        int y;

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
}