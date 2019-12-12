import model.*;

public class MyStrategy {
  static final double leftBorder = 1;
  static final double rightBorder = 39;
  static final double middlePoint = 20;
  boolean initial = true;
  boolean isRightPos = false;

  static Tile getTile(Game game, double x, double y) {
    return game.getLevel().getTiles()[(int) (x)][(int) (y)];
  }


  static double distanceSqr(Vec2Double a, Vec2Double b) {
    return (a.getX() - b.getX()) * (a.getX() - b.getX()) + (a.getY() - b.getY()) * (a.getY() - b.getY());
  }

  static Unit getNearestEnemy(Unit unit, Game game) {
    Unit nearestEnemy = null;
    for (Unit other : game.getUnits()) {
      if (other.getPlayerId() != unit.getPlayerId()) {
        if (nearestEnemy == null || distanceSqr(unit.getPosition(),
                other.getPosition()) < distanceSqr(unit.getPosition(), nearestEnemy.getPosition())) {
          nearestEnemy = other;
        }
      }
    }
    return nearestEnemy;
  }

  static boolean isOnFireLine(Vec2Double unit, Vec2Double enemyUnit, Game game, int lvl) {
    lvl++;
    double distanceSqr = distanceSqr(unit, enemyUnit);
    if (distanceSqr > 800) {
      return false;
    }
    if ((distanceSqr < 4 && (Math.abs(unit.getX() - enemyUnit.getX()) <=2))) {
      lvl--;
      return true;
    }
    double x = (unit.getX() + enemyUnit.getX()) / 2;
    double y = (unit.getY() + enemyUnit.getY()) / 2;

    if (Tile.WALL != getTile(game, x, y)) {
      Vec2Double vec2Double = new Vec2Double(x ,y);
      return true && isOnFireLine(unit, vec2Double, game, lvl) && isOnFireLine(vec2Double, enemyUnit, game, lvl);
    };
    lvl--;
    return false;
  }

//  static boolean isOnFireLine(Vec2Double unit, Vec2Double enemyUnit, Game game, double distanceToNearestEnemy) {
//    distanceToNearestEnemy =
//    for (i) {
//
//    }
//  }


  static LootBox getNearestWeapon(Unit unit, Game game) {
    LootBox nearestWeapon = null;
    for (LootBox lootBox : game.getLootBoxes()) {
      if (lootBox.getItem() instanceof Item.Weapon) {
        if (nearestWeapon == null || distanceSqr(unit.getPosition(),
                lootBox.getPosition()) < distanceSqr(unit.getPosition(), nearestWeapon.getPosition())) {
          nearestWeapon = lootBox;
        }
      }
    }
    return nearestWeapon;
  }

  static LootBox getNearestHealthPack(Unit unit, Game game, boolean isRightPos) {
    LootBox hp = null;
    for (LootBox lootBox : game.getLootBoxes()) {
      if (lootBox.getItem() instanceof Item.HealthPack) {
        System.out.println("isRightPos = "+isRightPos+" unit ="+unit.getPosition().getX()+" lootBox="+lootBox.getPosition().getX());
        if (((isRightPos && (unit.getPosition().getX() - lootBox.getPosition().getX() < 0) )
                || (!isRightPos && (unit.getPosition().getX() - lootBox.getPosition().getX() > 0)))) {
          System.out.println("         = "+isRightPos+" unit ="+unit.getPosition().getX()+" lootBox="+lootBox.getPosition().getX());
          if (hp == null ||
                  distanceSqr(unit.getPosition(), lootBox.getPosition()) < distanceSqr(unit.getPosition(), hp.getPosition())) {
            hp = lootBox;
          }
        }
      }
    }
    return hp;
  }

  public UnitAction getAction(Unit unit, Game game, Debug debug) {
    if (initial) {
      isRightPos = unit.getPosition().getX() > 20;
    }
    Unit nearestEnemy = getNearestEnemy(unit, game);

    double distanceToNearestEnemy  = distanceSqr(unit.getPosition(), nearestEnemy.getPosition());
    double xDistanceToNearestEnemy  = Math.abs(unit.getPosition().getX() - nearestEnemy.getPosition().getX());
    double yDistanceToNearestEnemy  = unit.getPosition().getY() - nearestEnemy.getPosition().getY();

    LootBox nearestWeapon = getNearestWeapon(unit, game);

    Vec2Double targetPos = unit.getPosition();
    if (unit.getWeapon() == null && nearestWeapon != null) {
      targetPos = nearestWeapon.getPosition();

    } else if (unit.getHealth() < 60){
      LootBox healthPack = getNearestHealthPack(unit, game, isRightPos);
      if (healthPack != null) {
        targetPos = healthPack.getPosition();
      }
    } else if (nearestEnemy != null){
      targetPos = nearestEnemy.getPosition();
    }
    debug.draw(new CustomData.Log("Target pos: " + targetPos));
    Vec2Double aim = new Vec2Double(0, 0);
    if (nearestEnemy != null) {
      aim = new Vec2Double(nearestEnemy.getPosition().getX() - unit.getPosition().getX(),
          nearestEnemy.getPosition().getY() - unit.getPosition().getY());
    }
    //jump
    boolean jump = targetPos.getY() > unit.getPosition().getY();
    if (targetPos.getX() > unit.getPosition().getX() && game.getLevel()
        .getTiles()[(int) (unit.getPosition().getX() + 1)][(int) (unit.getPosition().getY())] == Tile.WALL) {
      jump = true;
    }
    if (targetPos.getX() < unit.getPosition().getX() && game.getLevel()
        .getTiles()[(int) (unit.getPosition().getX() - 1)][(int) (unit.getPosition().getY())] == Tile.WALL) {
      jump = true;
    }
    if ((yDistanceToNearestEnemy < 1) && (xDistanceToNearestEnemy < 7)) {
      jump = true;
    }

    UnitAction action = new UnitAction();
    double velocity = targetPos.getX() - unit.getPosition().getX();
    if (velocity < 0) {
      velocity = -20;
    } else {
      velocity = 20;
    }

    action.setVelocity(velocity);
    action.setJump(jump);
    action.setJumpDown(!jump);
    action.setAim(aim);
    int lvl = -1;
    boolean r = isOnFireLine(unit.getPosition(), nearestEnemy.getPosition(), game, lvl);
//    System.out.println(r);
    action.setShoot(r);
    action.setReload(false);
    action.setSwapWeapon(false);
    action.setPlantMine(false);
    initial = false;
    return action;
  }
}