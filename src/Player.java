import java.util.*;
import java.io.*;
import java.math.*;

/**
 * Deliver more ore to hq (left side of the map) than your opponent. Use radars to find ore but beware of traps!
 **/
class Player {

    enum Entity_Type {
        ALLY_ROBOT, ENEMY_ROBOT, RADAR, TRAP
    }
    enum Item_Type {
        NONE, RADAR, TRAP, ORE
    }

    public static void main(String[] args) {
        Scanner in = new Scanner(System.in);
        int width = in.nextInt();
        int height = in.nextInt(); // size of the map

        Case[][] board = new Case[height][width];

        // game loop
        while (true) {
            int myScore = in.nextInt(); // Amount of ore delivered
            int opponentScore = in.nextInt();
            for (int i = 0; i < height; i++) {
                for (int j = 0; j < width; j++) {
                    board[i][j].ore = in.next(); // amount of ore or "?" if unknown
                    board[i][j].hole = in.nextInt() == 1; // 1 if cell has a hole
                }
            }
            int entityCount = in.nextInt(); // number of entities visible to you
            int radarCooldown = in.nextInt(); // turns left until a new radar can be requested
            int trapCooldown = in.nextInt(); // turns left until a new trap can be requested
            for (int i = 0 ; i < entityCount ; i++) {
                int id = in.nextInt(); // unique id of the entity
                int type = in.nextInt(); // 0 for your robot, 1 for other robot, 2 for radar, 3 for trap
                int x = in.nextInt();
                int y = in.nextInt(); // position of the entity
                int item = in.nextInt(); // if this entity is a robot, the item it is carrying (-1 for NONE, 2 for RADAR, 3 for TRAP, 4 for ORE)
                board[y][x].entities.add(new Entity(id, ))
            }
            putRadar();
            for (int i = 1 ; i < 5 ; i++) {
                System.out.println("WAIT"); // WAIT|MOVE x y|DIG x y|REQUEST item
            }
        }
    }

    private static void putRadar() {

    }

    private static Entity_Type convertEntityType(int type) {
        switch (type) {
            case 0: return Entity_Type.ALLY_ROBOT;
            case 1: return Entity_Type.ENEMY_ROBOT;
            case 2: return Entity_Type.RADAR;
            case 3: return Entity_Type.TRAP;
        }
    }

    public static class Case {
        String ore;
        boolean hole;
        List<Entity> entities;
    }

    public static class Entity {
        int id;
        Entity_Type type;
        Item_Type item;

        public Entity(int id, Entity_Type type, Item_Type item) {
            this.id = id;
            this.type = type;
            this.item = item;
        }
    }

}