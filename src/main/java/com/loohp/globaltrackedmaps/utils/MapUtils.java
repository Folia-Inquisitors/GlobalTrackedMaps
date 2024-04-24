package com.loohp.globaltrackedmaps.utils;

import com.google.common.collect.Collections2;
import com.loohp.globaltrackedmaps.GlobalTrackedMaps;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.MapMeta;
import org.bukkit.map.MapCanvas;
import org.bukkit.map.MapCursor;
import org.bukkit.map.MapCursorCollection;
import org.bukkit.map.MapRenderer;
import org.bukkit.map.MapView;
import org.bukkit.plugin.Plugin;

public class MapUtils {
  private static Class<?> craftMapRendererClass;
  
  private static Class<?> craftMapViewClass;
  
  private static Field craftMapViewWorldMapField;
  
  private static Class<?> nmsWorldMapClass;
  
  private static Field nmsWorldMapHumansField;
  
  private static Class<?> nmsEntityHumanClass;
  
  private static Method nmsEntityHumanGetBukkitEntityMethod;
  
  static {
    try {
      craftMapRendererClass = NMSUtils.getNMSClass("org.bukkit.craftbukkit.%s.map.CraftMapRenderer", new String[0]);
      craftMapViewClass = NMSUtils.getNMSClass("org.bukkit.craftbukkit.%s.map.CraftMapView", new String[0]);
      craftMapViewWorldMapField = craftMapViewClass.getDeclaredField("worldMap");
      nmsWorldMapClass = NMSUtils.getNMSClass("net.minecraft.server.%s.WorldMap", new String[] { "net.minecraft.world.level.saveddata.maps.WorldMap" });
      nmsWorldMapHumansField = NMSUtils.<Field>reflectiveLookup(Field.class, () -> nmsWorldMapClass.getDeclaredField("humans"), (NMSUtils.ReflectionLookupSupplier<Field>[])new NMSUtils.ReflectionLookupSupplier[] { () -> nmsWorldMapClass.getDeclaredField("o") });
      nmsEntityHumanClass = NMSUtils.getNMSClass("net.minecraft.server.%s.EntityHuman", new String[] { "net.minecraft.world.entity.player.EntityHuman" });
      nmsEntityHumanGetBukkitEntityMethod = nmsEntityHumanClass.getMethod("getBukkitEntity", new Class[0]);
    } catch (ReflectiveOperationException e) {
      e.printStackTrace();
    } 
  }
  
  public static Collection<Player> getTrackedPlayers(MapView mapView) {
    craftMapViewWorldMapField.setAccessible(true);
    nmsWorldMapHumansField.setAccessible(true);
    try {
      Object nmsWorldMap = craftMapViewWorldMapField.get(mapView);
      Map<?, ?> nmsEntityHumanMap = (Map<?, ?>)nmsWorldMapHumansField.get(nmsWorldMap);
      return Collections2.transform(nmsEntityHumanMap.keySet(), nmsEntityHuman -> {
            try {
              return (Player)nmsEntityHumanGetBukkitEntityMethod.invoke(nmsEntityHuman, new Object[0]);
            } catch (IllegalAccessException|java.lang.reflect.InvocationTargetException e) {
              throw new RuntimeException(e);
            } 
          });
    } catch (IllegalAccessException e) {
      e.printStackTrace();
      return Collections.emptySet();
    } 
  }
  
  public static MapView getItemMapView(ItemStack itemStack) {
    if (itemStack == null || itemStack.getType().equals(Material.AIR) || !itemStack.hasItemMeta())
      return null; 
    ItemMeta meta = itemStack.getItemMeta();
    if (!(meta instanceof MapMeta))
      return null; 
    return ((MapMeta)meta).getMapView();
  }
  
  public static void addCursor(MapView map, MapCursorCollection collection, MapCursor.Type type, double x, double z, double yaw) {
    byte b2;
    int i = 1 << map.getScale().getValue();
    float f = (float)(x - map.getCenterX()) / i;
    float f1 = (float)(z - map.getCenterZ()) / i;
    byte b0 = (byte)(int)((f * 2.0F) + 0.5D);
    byte b1 = (byte)(int)((f1 * 2.0F) + 0.5D);
    if (f >= -63.0F && f1 >= -63.0F && f <= 63.0F && f1 <= 63.0F) {
      yaw += (yaw < 0.0D) ? -8.0D : 8.0D;
      b2 = (byte)(int)(yaw * 16.0D / 360.0D);
      if (map.getWorld().getEnvironment() == World.Environment.NETHER) {
        int j = (int)(map.getWorld().getTime() / 10L);
        b2 = (byte)(j * j * 34187121 + j * 121 >> 15 & 0xF);
      } 
    } else {
      if (type != MapCursor.Type.WHITE_POINTER)
        return; 
      if (Math.abs(f) < 320.0F && Math.abs(f1) < 320.0F) {
        type = MapCursor.Type.WHITE_CIRCLE;
      } else {
        type = MapCursor.Type.SMALL_WHITE_CIRCLE;
      } 
      b2 = 0;
      if (f <= -63.0F)
        b0 = Byte.MIN_VALUE; 
      if (f1 <= -63.0F)
        b1 = Byte.MIN_VALUE; 
      if (f >= 63.0F)
        b0 = Byte.MAX_VALUE; 
      if (f1 >= 63.0F)
        b1 = Byte.MAX_VALUE; 
    } 
    if (b2 < 0)
      b2 = (byte)(b2 + 16); 
    collection.addCursor(new MapCursor(b0, b1, b2, type, true));
  }
  
  public static MapRenderer getCraftBaseRenderer(MapView mapView) {
    for (MapRenderer mapRenderer : mapView.getRenderers()) {
      if (craftMapRendererClass.isInstance(mapRenderer))
        return mapRenderer; 
    } 
    return null;
  }
  
  public static void removeTrackedRenderers(MapView mapView) {
    for (MapRenderer mapRenderer : mapView.getRenderers()) {
      if (mapRenderer instanceof GlobalTrackerRenderer)
        mapView.removeRenderer(mapRenderer); 
    } 
  }
  
  public static GlobalTrackerRenderer createTrackedRenderer(MapView mapView) {
    MapRenderer craftRenderer = getCraftBaseRenderer(mapView);
    GlobalTrackerRenderer renderer = new GlobalTrackerRenderer(craftRenderer);
    Scheduler.runTask((Plugin)GlobalTrackedMaps.plugin, () -> {
          removeTrackedRenderers(mapView);
          mapView.removeRenderer(craftRenderer);
          mapView.addRenderer(renderer);
        });
    return renderer;
  }
  
  public static class GlobalTrackerRenderer extends MapRenderer {
    private final MapRenderer craftBaseRenderer;
    
    private Map<Player, Location> locations;
    
    private GlobalTrackerRenderer(MapRenderer craftBaseRenderer) {
      super(true);
      this.craftBaseRenderer = craftBaseRenderer;
    }
    
    public void setLocations(Map<Player, Location> locations) {
      this.locations = locations;
    }
    
    public MapRenderer getCraftBaseRenderer() {
      return this.craftBaseRenderer;
    }
    
    public void render(MapView map, MapCanvas canvas, Player player) {
      // Byte code:
      //   0: aload_0
      //   1: getfield craftBaseRenderer : Lorg/bukkit/map/MapRenderer;
      //   4: aload_1
      //   5: aload_2
      //   6: aload_3
      //   7: invokevirtual render : (Lorg/bukkit/map/MapView;Lorg/bukkit/map/MapCanvas;Lorg/bukkit/entity/Player;)V
      //   10: aload_1
      //   11: invokeinterface getWorld : ()Lorg/bukkit/World;
      //   16: astore #4
      //   18: aload #4
      //   20: ifnonnull -> 24
      //   23: return
      //   24: aload_2
      //   25: invokeinterface getCursors : ()Lorg/bukkit/map/MapCursorCollection;
      //   30: astore #5
      //   32: iconst_0
      //   33: istore #6
      //   35: iload #6
      //   37: aload #5
      //   39: invokevirtual size : ()I
      //   42: if_icmpge -> 110
      //   45: aload #5
      //   47: iload #6
      //   49: invokevirtual getCursor : (I)Lorg/bukkit/map/MapCursor;
      //   52: astore #7
      //   54: aload #7
      //   56: invokevirtual getType : ()Lorg/bukkit/map/MapCursor$Type;
      //   59: astore #8
      //   61: getstatic com/loohp/globaltrackedmaps/utils/MapUtils$1.$SwitchMap$org$bukkit$map$MapCursor$Type : [I
      //   64: aload #8
      //   66: invokevirtual ordinal : ()I
      //   69: iaload
      //   70: tableswitch default -> 104, 1 -> 96, 2 -> 96, 3 -> 96
      //   96: aload #5
      //   98: aload #7
      //   100: invokevirtual removeCursor : (Lorg/bukkit/map/MapCursor;)Z
      //   103: pop
      //   104: iinc #6, 1
      //   107: goto -> 35
      //   110: aload_0
      //   111: getfield locations : Ljava/util/Map;
      //   114: ifnull -> 264
      //   117: aload_0
      //   118: getfield locations : Ljava/util/Map;
      //   121: invokeinterface entrySet : ()Ljava/util/Set;
      //   126: invokeinterface iterator : ()Ljava/util/Iterator;
      //   131: astore #6
      //   133: aload #6
      //   135: invokeinterface hasNext : ()Z
      //   140: ifeq -> 264
      //   143: aload #6
      //   145: invokeinterface next : ()Ljava/lang/Object;
      //   150: checkcast java/util/Map$Entry
      //   153: astore #7
      //   155: aload #7
      //   157: invokeinterface getKey : ()Ljava/lang/Object;
      //   162: checkcast org/bukkit/entity/Player
      //   165: astore #8
      //   167: aload #7
      //   169: invokeinterface getValue : ()Ljava/lang/Object;
      //   174: checkcast org/bukkit/Location
      //   177: astore #9
      //   179: aload_3
      //   180: aload #8
      //   182: invokeinterface canSee : (Lorg/bukkit/entity/Player;)Z
      //   187: ifeq -> 261
      //   190: aload #8
      //   192: invokeinterface getGameMode : ()Lorg/bukkit/GameMode;
      //   197: getstatic org/bukkit/GameMode.SPECTATOR : Lorg/bukkit/GameMode;
      //   200: invokevirtual equals : (Ljava/lang/Object;)Z
      //   203: ifne -> 261
      //   206: aload #4
      //   208: aload #9
      //   210: invokevirtual getWorld : ()Lorg/bukkit/World;
      //   213: invokevirtual equals : (Ljava/lang/Object;)Z
      //   216: ifeq -> 261
      //   219: aload #8
      //   221: aload_3
      //   222: invokevirtual equals : (Ljava/lang/Object;)Z
      //   225: ifne -> 236
      //   228: aload #8
      //   230: invokestatic isWearingMobHead : (Lorg/bukkit/entity/LivingEntity;)Z
      //   233: ifne -> 261
      //   236: aload_1
      //   237: aload #5
      //   239: getstatic org/bukkit/map/MapCursor$Type.WHITE_POINTER : Lorg/bukkit/map/MapCursor$Type;
      //   242: aload #9
      //   244: invokevirtual getX : ()D
      //   247: aload #9
      //   249: invokevirtual getZ : ()D
      //   252: aload #9
      //   254: invokevirtual getYaw : ()F
      //   257: f2d
      //   258: invokestatic addCursor : (Lorg/bukkit/map/MapView;Lorg/bukkit/map/MapCursorCollection;Lorg/bukkit/map/MapCursor$Type;DDD)V
      //   261: goto -> 133
      //   264: aload_2
      //   265: aload #5
      //   267: invokeinterface setCursors : (Lorg/bukkit/map/MapCursorCollection;)V
      //   272: return
      // Line number table:
      //   Java source line number -> byte code offset
      //   #184	-> 0
      //   #185	-> 10
      //   #186	-> 18
      //   #187	-> 23
      //   #189	-> 24
      //   #190	-> 32
      //   #191	-> 45
      //   #192	-> 54
      //   #193	-> 61
      //   #197	-> 96
      //   #190	-> 104
      //   #201	-> 110
      //   #202	-> 117
      //   #203	-> 155
      //   #204	-> 167
      //   #205	-> 179
      //   #206	-> 236
      //   #208	-> 261
      //   #210	-> 264
      //   #211	-> 272
      // Local variable table:
      //   start	length	slot	name	descriptor
      //   54	50	7	cursor	Lorg/bukkit/map/MapCursor;
      //   61	43	8	type	Lorg/bukkit/map/MapCursor$Type;
      //   35	75	6	i	I
      //   167	94	8	p	Lorg/bukkit/entity/Player;
      //   179	82	9	location	Lorg/bukkit/Location;
      //   155	106	7	entry	Ljava/util/Map$Entry;
      //   0	273	0	this	Lcom/loohp/globaltrackedmaps/utils/MapUtils$GlobalTrackerRenderer;
      //   0	273	1	map	Lorg/bukkit/map/MapView;
      //   0	273	2	canvas	Lorg/bukkit/map/MapCanvas;
      //   0	273	3	player	Lorg/bukkit/entity/Player;
      //   18	255	4	world	Lorg/bukkit/World;
      //   32	241	5	collection	Lorg/bukkit/map/MapCursorCollection;
      // Local variable type table:
      //   start	length	slot	name	signature
      //   155	106	7	entry	Ljava/util/Map$Entry<Lorg/bukkit/entity/Player;Lorg/bukkit/Location;>;
    }
  }
}
