package com.codex.wayfinder;

import com.codex.wayfinder.arena.ArenaBoss;
import com.codex.wayfinder.arena.ArenaHunt;
import com.codex.wayfinder.arena.ArenaPins;
import com.codex.wayfinder.arena.ArenaWords;
import com.codex.wayfinder.core.Account;
import com.codex.wayfinder.core.Advisor;
import com.codex.wayfinder.core.Atlas;
import com.codex.wayfinder.core.Format;
import com.codex.wayfinder.core.Kind;
import com.codex.wayfinder.core.Memory;
import com.codex.wayfinder.core.Need;
import com.codex.wayfinder.core.Overview;
import com.codex.wayfinder.core.Place;
import com.codex.wayfinder.core.Pt;
import com.codex.wayfinder.core.Route;
import com.codex.wayfinder.core.Router;
import com.codex.wayfinder.core.Shared;
import com.codex.wayfinder.core.Step;
import com.codex.wayfinder.core.Tracker;
import com.codex.wayfinder.core.Trail;
import com.codex.wayfinder.core.Words;
import com.codex.wayfinder.notes.FieldNotes;
import com.codex.wayfinder.overlay.BoardingMark;
import com.codex.wayfinder.overlay.GroundTrail;
import com.codex.wayfinder.overlay.MapTrail;
import com.codex.wayfinder.overlay.MinimapTrail;
import com.codex.wayfinder.overlay.StepPlaque;
import com.codex.wayfinder.overlay.TrailPaint;
import com.codex.wayfinder.overlay.TrailShown;
import com.codex.wayfinder.ui.Actions;
import com.codex.wayfinder.ui.Sigil;
import com.codex.wayfinder.ui.Tab;
import com.codex.wayfinder.ui.View;
import com.codex.wayfinder.ui.WayfinderPanel;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.inject.Provides;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import javax.inject.Inject;
import javax.swing.SwingUtilities;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.EnumComposition;
import net.runelite.api.EnumID;
import net.runelite.api.GameState;
import net.runelite.api.Item;
import net.runelite.api.ItemContainer;
import net.runelite.api.MenuAction;
import net.runelite.api.NPC;
import net.runelite.api.Player;
import net.runelite.api.Point;
import net.runelite.api.Quest;
import net.runelite.api.QuestState;
import net.runelite.api.Skill;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.ActorDeath;
import net.runelite.api.events.ChatMessage;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.ItemContainerChanged;
import net.runelite.api.events.MenuOpened;
import net.runelite.api.events.MenuOptionClicked;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.gameval.InventoryID;
import net.runelite.api.gameval.ItemID;
import net.runelite.api.gameval.VarbitID;
import net.runelite.api.widgets.Widget;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.chat.ChatColorType;
import net.runelite.client.chat.ChatMessageBuilder;
import net.runelite.client.chat.ChatMessageManager;
import net.runelite.client.chat.QueuedMessage;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.EventBus;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.events.PluginMessage;
import net.runelite.client.events.RuneScapeProfileChanged;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.client.ui.overlay.worldmap.WorldMapOverlay;
import net.runelite.client.ui.overlay.worldmap.WorldMapPoint;
import net.runelite.client.ui.overlay.worldmap.WorldMapPointManager;
import net.runelite.client.util.Text;

/**
 * Codex Wayfinder: one atlas of every way to travel that this account has unlocked, and the
 * quickest way from where you stand to anywhere, timed by your own trips. It plans and shows; it
 * never clicks, never changes menus and sends nothing off the computer. Other Codex apps read
 * what it shares through the RuneLite profile, or ask it for a route with a plugin message.
 */
@PluginDescriptor(
	name = "Codex Wayfinder",
	description = "Your own travel atlas: the quickest way anywhere with the teleports, rings, trees and ships you have unlocked, timed by your own trips",
	tags = {"codex", "travel", "teleport", "route", "fairy ring", "spirit tree", "atlas", "map", "path"},
	internalName = "codex-wayfinder"
)
public class CodexWayfinderPlugin extends Plugin
{
	private static final String MEMORY_KEY = "memory";
	private static final String TAB_KEY = "tab";
	/** Game ticks between reads of levels, items and unlocks. */
	private static final int READ_TICKS = 5;
	/** Game ticks between reads of quest states, which runs a client script per quest. */
	private static final int QUEST_TICKS = 100;
	/** Game ticks between plans while a route is followed. */
	private static final int REPLAN_TICKS = 4;
	/** Arriving within this many tiles of the place ends the route. */
	private static final int ARRIVED = 3;
	private static final int RESULTS = 8;
	private static final int UNLOCKS = 6;
	/** Game ticks between refreshes of the followers' "seen" times while nothing else changes. */
	private static final int ARENA_TICKS = 100;
	/** Walking this many tiles from where the followers' routes were planned plans them again. */
	private static final int ARENA_REPLAN_TILES = 24;
	private static final int[] POUCH_TYPES = {VarbitID.RUNE_POUCH_TYPE_1, VarbitID.RUNE_POUCH_TYPE_2, VarbitID.RUNE_POUCH_TYPE_3,
		VarbitID.RUNE_POUCH_TYPE_4, VarbitID.RUNE_POUCH_TYPE_5, VarbitID.RUNE_POUCH_TYPE_6};
	private static final int[] POUCH_AMOUNTS = {VarbitID.RUNE_POUCH_QUANTITY_1, VarbitID.RUNE_POUCH_QUANTITY_2,
		VarbitID.RUNE_POUCH_QUANTITY_3, VarbitID.RUNE_POUCH_QUANTITY_4, VarbitID.RUNE_POUCH_QUANTITY_5, VarbitID.RUNE_POUCH_QUANTITY_6};
	private static final Set<Integer> POUCHES = new HashSet<>(java.util.Arrays.asList(ItemID.BH_RUNE_POUCH,
		ItemID.BH_RUNE_POUCH_TROUVER, ItemID.DIVINE_RUNE_POUCH, ItemID.DIVINE_RUNE_POUCH_TROUVER));

	@Inject
	private Client client;

	@Inject
	private ClientThread clientThread;

	@Inject
	private ClientToolbar clientToolbar;

	@Inject
	private OverlayManager overlayManager;

	@Inject
	private ConfigManager configManager;

	@Inject
	private ChatMessageManager chatMessageManager;

	@Inject
	private EventBus eventBus;

	@Inject
	private Gson gson;

	@Inject
	private CodexWayfinderConfig config;

	@Inject
	private WorldMapOverlay worldMapOverlay;

	@Inject
	private WorldMapPointManager worldMapPointManager;

	private ExecutorService worker;
	private FieldNotes notes;
	private WayfinderPanel panel;
	private NavigationButton navButton;
	private StepPlaque plaque;
	private BoardingMark mark;
	private GroundTrail groundTrail;
	private MinimapTrail minimapTrail;
	private MapTrail mapTrail;
	/** The route the trail was laid out from, so it is laid out once per plan. */
	private Route trailRoute;
	private Trail trail;
	/** The world map pin on the place, and the place it was put on. */
	private WorldMapPoint pin;
	private Place pinned;
	private final Tracker tracker = new Tracker();
	private ArenaPins arenaPins;
	/** Mage Arena II is in progress on this account. */
	private volatile boolean arenaOn;
	/** The followers as shown, with their routes; null while no hunt is shown. */
	private volatile List<ArenaHunt.Line> arenaLines;
	private boolean arenaDirty = true;
	private int arenaPlannedFrom = Pt.NONE;
	/** The last value of the follower rotation timer, or -1 before the first read. */
	private int arenaTimer = -1;
	private final AtomicInteger arenaGeneration = new AtomicInteger();
	/** Scene indexes of followers seen fighting this player, so only their deaths count as kills. */
	private final Set<Integer> arenaFought = new HashSet<>();

	private volatile Atlas atlas;
	private volatile Router router;
	private volatile Advisor advisor;
	private volatile Account account = Account.UNKNOWN;
	private volatile Memory memory = new Memory();
	private volatile Overview overview;
	private volatile boolean loggedIn;

	private volatile Place destination;
	private volatile Route route;
	private volatile Route bankRoute;
	private volatile boolean planning;
	private volatile String query = "";
	private volatile List<Place> results = Collections.emptyList();
	private volatile List<Advisor.Unlock> unlocks = Collections.emptyList();
	private volatile String note;
	private final AtomicInteger planGeneration = new AtomicInteger();
	private final AtomicInteger rankGeneration = new AtomicInteger();

	private int here = Pt.NONE;
	private int lastPlannedFrom = Pt.NONE;
	private int ticks;
	private boolean questsDue = true;
	private boolean accountDirty = true;
	private Set<String> finishedQuests = new HashSet<>();
	private boolean questsRead;
	private String profileKey;
	private long routeStartMs;
	private int routeStartTile = Pt.NONE;
	private int routePlannedSeconds;
	private volatile String lastShared;

	@Provides
	CodexWayfinderConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(CodexWayfinderConfig.class);
	}

	@Override
	protected void startUp()
	{
		worker = Executors.newSingleThreadExecutor(r ->
		{
			Thread t = new Thread(r, "codex-wayfinder");
			t.setDaemon(true);
			t.setPriority(Thread.NORM_PRIORITY - 1);
			return t;
		});
		notes = new FieldNotes(this::getPluginDirectory, gson);
		if (config.fieldNotes())
		{
			notes.open(getClass().getPackage().getImplementationVersion());
		}
		panel = new WayfinderPanel(new PanelActions());
		panel.show(rememberedTab());
		navButton = NavigationButton.builder()
			.tooltip("Codex Wayfinder")
			.icon(Sigil.icon(16))
			.priority(6)
			.panel(panel)
			.build();
		clientToolbar.addNavigation(navButton);
		plaque = new StepPlaque();
		mark = new BoardingMark(client);
		groundTrail = new GroundTrail(client);
		minimapTrail = new MinimapTrail(client);
		mapTrail = new MapTrail(client, worldMapOverlay);
		arenaPins = new ArenaPins(worldMapPointManager);
		overlayManager.add(plaque);
		overlayManager.add(mark);
		overlayManager.add(groundTrail);
		overlayManager.add(minimapTrail);
		overlayManager.add(mapTrail);
		worker.execute(() ->
		{
			Atlas a = Atlas.load();
			atlas = a;
			router = new Router(a);
			advisor = new Advisor(router);
			clientThread.invokeLater(() ->
			{
				accountDirty = true;
				questsDue = true;
			});
			push();
		});
		clientThread.invokeLater(() ->
		{
			loggedIn = client.getGameState() == GameState.LOGGED_IN;
			if (loggedIn)
			{
				loadMemory();
			}
		});
		push();
	}

	@Override
	protected void shutDown()
	{
		saveMemory();
		clientToolbar.removeNavigation(navButton);
		overlayManager.remove(plaque);
		overlayManager.remove(mark);
		overlayManager.remove(groundTrail);
		overlayManager.remove(minimapTrail);
		overlayManager.remove(mapTrail);
		unpin();
		arenaPins.clear();
		arenaLines = null;
		arenaOn = false;
		arenaTimer = -1;
		trail = null;
		trailRoute = null;
		worker.shutdownNow();
		notes.shutdown();
		atlas = null;
		router = null;
		advisor = null;
		destination = null;
		route = null;
	}

	// ----- Game events -----------------------------------------------------------------------

	@Subscribe
	public void onGameStateChanged(GameStateChanged e)
	{
		if (e.getGameState() == GameState.LOGGED_IN)
		{
			if (!loggedIn)
			{
				loggedIn = true;
				questsDue = true;
				accountDirty = true;
				loadMemory();
			}
		}
		else if (e.getGameState() == GameState.LOGIN_SCREEN || e.getGameState() == GameState.HOPPING)
		{
			if (e.getGameState() == GameState.LOGIN_SCREEN)
			{
				saveMemory();
				loggedIn = false;
				here = Pt.NONE;
				arenaOn = false;
				arenaTimer = -1;
				showArena();
				push();
			}
		}
	}

	@Subscribe
	public void onRuneScapeProfileChanged(RuneScapeProfileChanged e)
	{
		saveMemory();
		profileKey = null;
		if (client.getGameState() == GameState.LOGGED_IN)
		{
			loadMemory();
			questsDue = true;
			accountDirty = true;
		}
	}

	@Subscribe
	public void onItemContainerChanged(ItemContainerChanged e)
	{
		if (e.getContainerId() == InventoryID.BANK)
		{
			Map<Integer, Integer> bank = new HashMap<>();
			for (Item i : e.getItemContainer().getItems())
			{
				if (i.getId() > 0 && i.getQuantity() > 0)
				{
					bank.merge(i.getId(), i.getQuantity(), Integer::sum);
				}
			}
			memory.bank = bank;
			memory.bankTime = System.currentTimeMillis();
			accountDirty = true;
		}
		else if (e.getContainerId() == InventoryID.INV || e.getContainerId() == InventoryID.WORN)
		{
			accountDirty = true;
			if (e.getContainerId() == InventoryID.INV && arenaActive())
			{
				for (Item i : e.getItemContainer().getItems())
				{
					ArenaBoss b = ArenaBoss.ofDrop(i.getId());
					if (b != null)
					{
						arenaProof(b, ArenaHunt.Proof.CARRIED);
					}
				}
			}
		}
	}

	@Subscribe
	public void onChatMessage(ChatMessage e)
	{
		if (e.getType() != ChatMessageType.GAMEMESSAGE && e.getType() != ChatMessageType.SPAM)
		{
			return;
		}
		String msg = Text.removeTags(e.getMessage());
		if (msg.contains("Quest complete") || msg.startsWith("Congratulations, you've completed a quest")
			|| msg.contains("you've completed the") || msg.contains("Your new task is"))
		{
			questsDue = true;
		}
		if (notes.isOpen() && destination != null)
		{
			JsonObject o = new JsonObject();
			o.addProperty("text", msg);
			o.addProperty("at", Pt.toString(here));
			notes.note("chat", o);
		}
	}

	@Subscribe
	public void onActorDeath(ActorDeath e)
	{
		if (!arenaActive())
		{
			return;
		}
		if (e.getActor() instanceof NPC)
		{
			NPC npc = (NPC) e.getActor();
			ArenaBoss b = ArenaBoss.ofNpc(npc.getId());
			if (b != null && arenaFought.remove(npc.getIndex()))
			{
				arenaProof(b, ArenaHunt.Proof.KILLED);
			}
		}
		else if (e.getActor() == client.getLocalPlayer() && memory.arena.died())
		{
			arenaFought.clear();
			arenaNote("died", null);
			arenaChanged();
		}
	}

	@Subscribe
	public void onMenuOptionClicked(MenuOptionClicked e)
	{
		tracker.clicked(Text.removeTags(e.getMenuOption()), Text.removeTags(e.getMenuTarget()), System.currentTimeMillis());
	}

	@Subscribe
	public void onMenuOpened(MenuOpened e)
	{
		if (!config.worldMapOption() || atlas == null)
		{
			return;
		}
		Widget map = client.getWidget(InterfaceID.Worldmap.MAP_CONTAINER);
		if (map == null || map.isHidden())
		{
			return;
		}
		Point mouse = client.getMouseCanvasPosition();
		Rectangle bounds = map.getBounds();
		if (mouse == null || !bounds.contains(mouse.getX(), mouse.getY()))
		{
			return;
		}
		if (destination != null)
		{
			client.getMenu().createMenuEntry(-1)
				.setOption("Wayfinder")
				.setTarget("<col=d6b26a>Clear route</col>")
				.setType(MenuAction.RUNELITE)
				.onClick(entry -> clearRoute());
		}
		int tile = mapTile(mouse, bounds);
		if (tile == Pt.NONE)
		{
			return;
		}
		client.getMenu().createMenuEntry(-1)
			.setOption("Wayfinder")
			.setTarget("<col=d6b26a>Plan a route here</col>")
			.setType(MenuAction.RUNELITE)
			.onClick(entry -> markedSpot(tile));
	}

	/** The surface tile under a point of the world map, the inverse of RuneLite's own world map drawing. */
	private int mapTile(Point mouse, Rectangle rect)
	{
		float ppt = client.getWorldMap().getWorldMapZoom();
		Point pos = client.getWorldMap().getWorldMapPosition();
		if (ppt <= 0 || pos == null)
		{
			return Pt.NONE;
		}
		int widthInTiles = (int) Math.ceil(rect.getWidth() / ppt);
		int heightInTiles = (int) Math.ceil(rect.getHeight() / ppt);
		double centre = ppt - Math.ceil(ppt / 2);
		int x = (int) Math.floor((mouse.getX() - rect.x - centre) / ppt) - widthInTiles / 2 + pos.getX();
		int yTileOffset = (int) Math.floor((rect.height - (mouse.getY() - rect.y) + centre) / ppt);
		int y = yTileOffset + (pos.getY() - heightInTiles / 2) - 1;
		return x > 0 && y > 0 ? Pt.of(x, y, 0) : Pt.NONE;
	}

	private void markedSpot(int tile)
	{
		Atlas a = atlas;
		Place near = a == null ? null : Words.near(a.getPlaces(), tile, 12);
		String name = near != null ? "Near " + near.getName() : "Marked spot";
		go(new Place(name, tile, Place.Group.CUSTOM));
		SwingUtilities.invokeLater(() ->
		{
			panel.show(Tab.GO);
		});
	}

	@Subscribe
	public void onConfigChanged(ConfigChanged e)
	{
		if (!CodexWayfinderConfig.GROUP.equals(e.getGroup()))
		{
			return;
		}
		if ("fieldNotes".equals(e.getKey()))
		{
			if (config.fieldNotes())
			{
				notes.open(getClass().getPackage().getImplementationVersion());
			}
			else
			{
				notes.close();
			}
			return;
		}
		if (e.getKey().startsWith("arena"))
		{
			clientThread.invokeLater(this::arenaChanged);
			return;
		}
		if (destination != null)
		{
			replan(true);
		}
		if (!config.showPlaque())
		{
			plaque.show(null);
		}
		if (!config.markBoarding())
		{
			mark.show(Pt.NONE, config.markColour());
		}
	}

	@Subscribe
	public void onPluginMessage(PluginMessage e)
	{
		if (!Shared.GROUP.equals(e.getNamespace()))
		{
			return;
		}
		Map<String, Object> data = e.getData();
		int tile = tileOf(data);
		if (tile == Pt.NONE)
		{
			return;
		}
		String name = data.get("name") instanceof String ? (String) data.get("name") : "Marked spot";
		if ("show".equals(e.getName()))
		{
			go(new Place(name, tile, Place.Group.CUSTOM));
		}
		else if ("route".equals(e.getName()))
		{
			Object id = data.get("id");
			int from = here;
			Router r = router;
			Account a = account;
			if (r == null || from == Pt.NONE)
			{
				return;
			}
			Router.Options o = options();
			worker.execute(() ->
			{
				Route found = r.route(from, tile, a, o);
				Map<String, Object> out = new LinkedHashMap<>();
				out.put("id", id);
				out.put("name", name);
				out.put("found", found.isFound());
				out.put("seconds", found.getSeconds());
				List<String> steps = new ArrayList<>();
				for (int i = 0; i < found.getSteps().size(); i++)
				{
					steps.add(Words.title(found.getSteps().get(i), r.getAtlas().getPlaces(), Words.goal(found.getSteps(), i, name)));
				}
				out.put("steps", steps);
				clientThread.invokeLater(() -> eventBus.post(new PluginMessage(Shared.GROUP, "routed", out)));
			});
		}
	}

	private static int tileOf(Map<String, Object> data)
	{
		if (data == null)
		{
			return Pt.NONE;
		}
		Object wp = data.get("target");
		if (wp instanceof WorldPoint)
		{
			WorldPoint p = (WorldPoint) wp;
			return Pt.of(p.getX(), p.getY(), p.getPlane());
		}
		Object x = data.get("x");
		Object y = data.get("y");
		Object z = data.get("plane");
		if (x instanceof Number && y instanceof Number)
		{
			return Pt.of(((Number) x).intValue(), ((Number) y).intValue(), z instanceof Number ? ((Number) z).intValue() : 0);
		}
		return Pt.NONE;
	}

	@Subscribe
	public void onGameTick(GameTick e)
	{
		if (atlas == null || client.getGameState() != GameState.LOGGED_IN)
		{
			return;
		}
		ticks++;
		int before = here;
		here = position();
		long now = System.currentTimeMillis();

		Tracker.Lap lap = tracker.moved(atlas, before, here, now);
		if (lap != null)
		{
			Router r = router;
			double estimate = r == null ? 0 : r.seconds(lap.getTransport(), null);
			boolean kept = memory.paces.add(lap.getTransport().key(), lap.getSeconds(), estimate, now);
			if (notes.isOpen())
			{
				JsonObject o = new JsonObject();
				o.addProperty("trip", lap.getTransport().key());
				o.addProperty("seconds", lap.getSeconds());
				o.addProperty("estimate", estimate);
				o.addProperty("kept", kept);
				o.addProperty("from", Pt.toString(before));
				o.addProperty("to", Pt.toString(here));
				notes.note("lap", o);
			}
			if (kept)
			{
				saveMemorySoon();
			}
		}

		if (accountDirty || ticks % READ_TICKS == 0)
		{
			readAccount();
		}
		watchArena(now);

		Place d = destination;
		if (d != null && here != Pt.NONE)
		{
			if (Pt.distance(here, d.getTile()) <= ARRIVED)
			{
				arrived(d, now);
			}
			else if (ticks % REPLAN_TICKS == 0 && Pt.distance(here, lastPlannedFrom) > 2)
			{
				replan(false);
			}
		}
		updateOverlays();
	}

	private int position()
	{
		Player p = client.getLocalPlayer();
		if (p == null)
		{
			return Pt.NONE;
		}
		WorldPoint wp = WorldPoint.fromLocalInstance(client, p.getLocalLocation());
		return wp == null ? Pt.NONE : Pt.of(wp.getX(), wp.getY(), wp.getPlane());
	}

	private void arrived(Place d, long now)
	{
		int took = routeStartMs == 0 ? 0 : (int) ((now - routeStartMs) / 1000);
		Route r = route;
		if (routeStartTile != Pt.NONE)
		{
			memory.journey(routeStartTile, d.getTile(), d.getName(), now);
		}
		// A route that ended with a walk tells how this player walks against a straight run.
		if (r != null && r.isFound() && !r.getSteps().isEmpty())
		{
			Step last = r.getSteps().get(r.getSteps().size() - 1);
			if (last.getType() == Step.Type.WALK && r.getSteps().size() == 1 && took > 0)
			{
				memory.paces.addWalk(took, last.getTiles() * com.codex.wayfinder.core.Paces.RUN_SECONDS_PER_TILE);
			}
		}
		if (config.arrivalMessage() && took > 0)
		{
			String text = new ChatMessageBuilder()
				.append(ChatColorType.HIGHLIGHT).append("Wayfinder: ")
				.append(ChatColorType.NORMAL).append("at " + d.getName() + " in " + Format.duration(took)
					+ (routePlannedSeconds > 0 ? " (planned " + Format.duration(routePlannedSeconds) + ")" : "") + ".")
				.build();
			chatMessageManager.queue(QueuedMessage.builder().type(ChatMessageType.GAMEMESSAGE).runeLiteFormattedMessage(text).build());
		}
		if (notes.isOpen())
		{
			JsonObject o = new JsonObject();
			o.addProperty("place", d.getName());
			o.addProperty("seconds", took);
			o.addProperty("planned", routePlannedSeconds);
			notes.note("arrived", o);
		}
		destination = null;
		route = null;
		bankRoute = null;
		note = "Arrived at " + d.getName() + (took > 0 ? " in " + Format.duration(took) : "") + ".";
		saveMemory();
		rank();
		push();
	}

	// ----- Reading the account ---------------------------------------------------------------

	private void readAccount()
	{
		Atlas a = atlas;
		if (a == null)
		{
			return;
		}
		accountDirty = false;
		Account.Builder b = Account.builder().known(true);
		for (Skill s : Skill.values())
		{
			b.level(s.getName(), client.getRealSkillLevel(s));
		}
		if (questsDue || (ticks % QUEST_TICKS == 0))
		{
			questsDue = false;
			Set<String> done = new HashSet<>();
			Map<String, Quest> byName = new HashMap<>();
			for (Quest q : Quest.values())
			{
				byName.put(q.getName(), q);
			}
			for (String name : a.quests())
			{
				Quest q = byName.get(name);
				if (q != null && q.getState(client) == QuestState.FINISHED)
				{
					done.add(name);
				}
			}
			finishedQuests = done;
			questsRead = true;
			QuestState ma2 = Quest.MAGE_ARENA_II.getState(client);
			boolean on = ma2 == QuestState.IN_PROGRESS;
			if (ma2 == QuestState.FINISHED && !memory.arena.seen.isEmpty())
			{
				// The miniquest is done: its spots mean nothing now.
				memory.arena = new ArenaHunt();
				saveMemorySoon();
			}
			if (on != arenaOn)
			{
				arenaOn = on;
				arenaDirty = true;
			}
		}
		for (String q : finishedQuests)
		{
			b.quest(q);
		}
		b.questsKnown(questsRead);
		if (arenaActive())
		{
			readArenaVars();
		}
		for (int id : a.varbitIds())
		{
			b.varbit(id, client.getVarbitValue(id));
		}
		for (int id : a.varpIds())
		{
			b.varp(id, client.getVarpValue(id));
		}
		boolean pouch = false;
		ItemContainer inv = client.getItemContainer(InventoryID.INV);
		if (inv != null)
		{
			for (Item i : inv.getItems())
			{
				b.carry(i.getId(), i.getQuantity());
				pouch |= POUCHES.contains(i.getId());
			}
		}
		ItemContainer worn = client.getItemContainer(InventoryID.WORN);
		if (worn != null)
		{
			for (Item i : worn.getItems())
			{
				if (i.getId() > 0)
				{
					b.wear(i.getId());
					if (i.getQuantity() > 1)
					{
						b.carry(i.getId(), i.getQuantity() - 1);
					}
				}
			}
		}
		if (pouch)
		{
			EnumComposition runes = client.getEnum(EnumID.RUNEPOUCH_RUNE);
			for (int i = 0; i < POUCH_TYPES.length; i++)
			{
				int type = client.getVarbitValue(POUCH_TYPES[i]);
				int amount = client.getVarbitValue(POUCH_AMOUNTS[i]);
				if (type > 0 && amount > 0 && runes != null)
				{
					b.carry(runes.getIntValue(type), amount);
				}
			}
		}
		Memory m = memory;
		for (Map.Entry<Integer, Integer> e : m.bank.entrySet())
		{
			b.bank(e.getKey(), e.getValue());
		}
		b.bankKnown(m.bankTime > 0);
		for (Map.Entry<String, String> e : Atlas.treePatches().entrySet())
		{
			String[] rv = e.getValue().split(" ");
			String seen = configManager.getRSProfileConfiguration("timetracking", rv[0] + "." + rv[1]);
			if (seen != null)
			{
				try
				{
					if (Integer.parseInt(seen.split(":")[0]) == 20)
					{
						b.treeGrown(e.getKey());
					}
				}
				catch (NumberFormatException ex)
				{
					// An odd Time Tracking record: leave the tree out.
				}
			}
		}
		Account next = b.build();
		if (!account.sameAs(next))
		{
			account = next;
			worker.execute(this::accountChanged);
		}
	}

	/** On the worker: everything that depends on the account. */
	private void accountChanged()
	{
		Atlas a = atlas;
		Router r = router;
		Account acc = account;
		if (a == null || r == null)
		{
			return;
		}
		overview = Overview.build(a, acc);
		Shared shared = Shared.build(a, r, acc, memory.paces, System.currentTimeMillis());
		shared.savedAt = 0;
		String json = gson.toJson(shared);
		if (!json.equals(lastShared))
		{
			lastShared = json;
			shared.savedAt = System.currentTimeMillis();
			configManager.setRSProfileConfiguration(Shared.GROUP, Shared.KEY, gson.toJson(shared));
		}
		if (notes.isOpen())
		{
			JsonObject o = new JsonObject();
			o.addProperty("teleportsReady", overview.count(overview.getTeleports(), Overview.State.READY));
			o.addProperty("teleportsBanked", overview.count(overview.getTeleports(), Overview.State.BANKED));
			o.addProperty("networksOpen", overview.count(overview.getNetworks(), Overview.State.READY));
			o.add("networks", notes.safe(shared.networks));
			notes.noteChanged("atlas", o);
		}
		push();
		if (destination != null)
		{
			clientThread.invokeLater(() -> replan(true));
		}
		if (unlocks.isEmpty())
		{
			rank();
		}
	}

	// ----- Planning --------------------------------------------------------------------------

	private Router.Options options()
	{
		Router.Options o = new Router.Options();
		o.reach = config.planWithBank() ? Need.Reach.OWNED : Need.Reach.CARRIED;
		o.avoidWilderness = config.avoidWilderness();
		o.paces = memory.paces;
		EnumSet<Kind> off = EnumSet.noneOf(Kind.class);
		if (!config.useCharters())
		{
			off.add(Kind.CHARTER);
		}
		if (!config.useHomeTeleport())
		{
			off.add(Kind.HOME);
		}
		if (!config.useGrouping())
		{
			off.add(Kind.MINIGAME);
		}
		o.off = off;
		return o;
	}

	private void go(Place p)
	{
		destination = p;
		route = null;
		bankRoute = null;
		note = null;
		query = "";
		results = Collections.emptyList();
		memory.visit(p);
		clientThread.invokeLater(() ->
		{
			routeStartMs = System.currentTimeMillis();
			routeStartTile = here;
			routePlannedSeconds = 0;
			replan(true);
		});
		push();
	}

	/** On the client thread: plans from where the player stands, on the worker. */
	private void replan(boolean fresh)
	{
		Place d = destination;
		Router r = router;
		if (d == null || r == null)
		{
			return;
		}
		int from = here;
		if (from == Pt.NONE)
		{
			note = "Log in to plan a route.";
			push();
			return;
		}
		lastPlannedFrom = from;
		Account a = account;
		Router.Options o = options();
		int gen = planGeneration.incrementAndGet();
		planning = true;
		if (fresh)
		{
			push();
		}
		worker.execute(() ->
		{
			if (gen != planGeneration.get())
			{
				return;
			}
			long t = System.currentTimeMillis();
			Route found = r.route(from, d.getTile(), a, o);
			Route alt = null;
			if (found.isFound() && o.reach == Need.Reach.CARRIED && a.isBankKnown())
			{
				Router.Options withBank = o.copy();
				withBank.reach = Need.Reach.OWNED;
				Route b = r.route(from, d.getTile(), a, withBank);
				if (b.isFound() && b.getSeconds() + 10 < found.getSeconds())
				{
					alt = b;
				}
			}
			if (gen != planGeneration.get() || destination != d)
			{
				return;
			}
			route = found;
			bankRoute = alt;
			planning = false;
			if (routePlannedSeconds == 0 && found.isFound())
			{
				routePlannedSeconds = found.getSeconds();
			}
			if (notes.isOpen())
			{
				JsonObject n = new JsonObject();
				n.addProperty("to", d.getName());
				n.addProperty("from", Pt.toString(from));
				n.addProperty("found", found.isFound());
				n.addProperty("seconds", found.getSeconds());
				n.addProperty("ms", System.currentTimeMillis() - t);
				n.addProperty("tiles", found.getExpanded());
				List<String> steps = new ArrayList<>();
				for (Step s : found.getSteps())
				{
					steps.add(s.getTransport() == null ? "walk " + s.getTiles() : s.getTransport().key());
				}
				n.add("steps", notes.safe(steps));
				notes.noteChanged("route", n);
			}
			push();
		});
	}

	private void rank()
	{
		Advisor adv = advisor;
		if (adv == null || !loggedIn)
		{
			return;
		}
		int gen = rankGeneration.incrementAndGet();
		Account a = account;
		Router.Options o = options();
		List<com.codex.wayfinder.core.Trip> trips = new ArrayList<>(memory.tripsByUse());
		if (trips.size() > 8)
		{
			trips = new ArrayList<>(trips.subList(0, 8));
		}
		List<com.codex.wayfinder.core.Trip> use = trips;
		unlocks = null;
		push();
		worker.execute(() ->
		{
			if (gen != rankGeneration.get())
			{
				return;
			}
			List<Advisor.Unlock> ranked = adv.rank(a, o, use, UNLOCKS);
			if (gen == rankGeneration.get())
			{
				unlocks = ranked;
				push();
			}
		});
	}

	// ----- Overlays --------------------------------------------------------------------------

	private void updateOverlays()
	{
		Route r = route;
		Place d = destination;
		Atlas a = atlas;
		if (d == null || r == null || !r.isFound() || a == null || r.getSteps().isEmpty())
		{
			plaque.show(null);
			mark.show(Pt.NONE, config.markColour());
			showTrail(null);
			// Keep the pin while the first plan is made; drop it once the route is gone.
			pin(d);
			return;
		}
		if (r != trailRoute)
		{
			trailRoute = r;
			trail = Trail.of(r);
		}
		Trail t = trail;
		Trail.Ahead ahead = t == null ? null : t.ahead(here);
		Step now = ahead == null ? r.getSteps().get(0) : ahead.getLeg().getStep();
		int at = ahead == null ? 0 : ahead.getLeg().getIndex();
		String title = Words.title(now, a.getPlaces(), Words.goal(r.getSteps(), at, d.getName()));
		String detail = Words.detail(now, a.getItems());
		if (config.showPlaque())
		{
			int total = Math.max(routePlannedSeconds, r.getSeconds());
			plaque.show(new StepPlaque.Shown("TO " + d.getName().toUpperCase(), title, detail,
				Words.eta(r, memory.paces), total - r.getSeconds(), total));
		}
		else
		{
			plaque.show(null);
		}
		int board = Pt.NONE;
		Step next = ahead == null ? null : ahead.getNext();
		if (now.getType() == Step.Type.TRIP && now.getTransport().getOrigin() != Pt.NONE)
		{
			board = now.getTransport().getOrigin();
		}
		else if (now.getType() == Step.Type.WALK && next != null && next.getTransport() != null
			&& next.getTransport().getOrigin() != Pt.NONE)
		{
			board = next.getTransport().getOrigin();
		}
		mark.show(config.markBoarding() ? board : Pt.NONE, config.markColour());

		String callout = next == null ? null : Words.title(next, a.getPlaces(), null);
		if (now.getType() == Step.Type.TRIP)
		{
			callout = Words.title(now, a.getPlaces(), null);
		}
		showTrail(t == null ? null : new TrailShown(t, ahead, here, d.getName(), callout, config.pathColour(), config.markColour(),
			config.pathOnGround(), config.pathOnMinimap(), config.nextStopArrow(), config.pathOnWorldMap()));
		pin(d);
		if (pin != null)
		{
			pin.setTooltip("Wayfinder: " + d.getName() + ", " + Words.eta(r, memory.paces));
		}
	}

	private void showTrail(TrailShown s)
	{
		groundTrail.show(s);
		minimapTrail.show(s);
		mapTrail.show(s);
	}

	/** Puts the Wayfinder pin on the place on the world map, or takes it off when there is none. */
	private void pin(Place d)
	{
		if (d == null || !config.pathOnWorldMap())
		{
			unpin();
			return;
		}
		if (d == pinned && pin != null)
		{
			return;
		}
		unpin();
		int tile = d.getTile();
		WorldPoint wp = new WorldPoint(Pt.x(tile), Pt.y(tile), Pt.plane(tile));
		WorldMapPoint p = new WorldMapPoint(wp, TrailPaint.pin());
		p.setImagePoint(new Point(13, 33));
		p.setSnapToEdge(true);
		p.setJumpOnClick(true);
		p.setTarget(wp);
		p.setName(d.getName());
		p.setTooltip("Wayfinder: " + d.getName());
		worldMapPointManager.add(p);
		pin = p;
		pinned = d;
	}

	private void unpin()
	{
		if (pin != null)
		{
			worldMapPointManager.remove(pin);
		}
		pin = null;
		pinned = null;
	}

	// ----- Mage Arena II ---------------------------------------------------------------------

	private boolean arenaActive()
	{
		return arenaOn && config.arenaFollowers();
	}

	/** On the client thread, every tick: Wilderness time, followers in sight, and refreshing what is shown. */
	private void watchArena(long now)
	{
		if (!arenaActive())
		{
			if (arenaLines != null)
			{
				showArena();
			}
			return;
		}
		ArenaHunt hunt = memory.arena;
		if (Pt.wildernessLevel(here) > 0)
		{
			hunt.wildTick();
		}
		Player me = client.getLocalPlayer();
		for (NPC npc : client.getTopLevelWorldView().npcs())
		{
			ArenaBoss b = npc == null ? null : ArenaBoss.ofNpc(npc.getId());
			// Each player has followers of their own, and another player's may stand in sight: only
			// one fighting this player (it attacks as it rises) is taken to be this player's.
			if (b == null || me == null || (npc.getInteracting() != me && me.getInteracting() != npc))
			{
				continue;
			}
			arenaFought.add(npc.getIndex());
			WorldPoint wp = npc.getWorldLocation();
			int tile = Pt.of(wp.getX(), wp.getY(), wp.getPlane());
			if (hunt.saw(b, tile, now))
			{
				JsonObject o = new JsonObject();
				o.addProperty("npc", npc.getId());
				o.addProperty("at", Pt.toString(tile));
				arenaNote("seen " + b.getTitle(), o);
				saveMemorySoon();
				arenaDirty = true;
			}
		}
		boolean walked = arenaPlannedFrom == Pt.NONE ? here != Pt.NONE : Pt.distance(here, arenaPlannedFrom) > ARENA_REPLAN_TILES;
		if (arenaDirty || ticks % ARENA_TICKS == 0 || (walked && ticks % REPLAN_TICKS == 0))
		{
			showArena();
		}
	}

	/** Reads the hand-in varbits and the rotation timer. Both meanings are Quest Helper's, not the wiki's. */
	private void readArenaVars()
	{
		for (ArenaBoss b : ArenaBoss.values())
		{
			if (client.getVarbitValue(b.getHandedInVarbit()) == 1)
			{
				arenaProof(b, ArenaHunt.Proof.HANDED);
			}
		}
		int timer = client.getVarbitValue(VarbitID.MA2_TIMER_REMAINING);
		if (arenaTimer >= 0 && timer > arenaTimer && memory.arena.rotated())
		{
			JsonObject o = new JsonObject();
			o.addProperty("from", arenaTimer);
			o.addProperty("to", timer);
			arenaNote("rotated", o);
			arenaChanged();
		}
		arenaTimer = timer;
	}

	private void arenaProof(ArenaBoss b, ArenaHunt.Proof p)
	{
		if (memory.arena.prove(b, p))
		{
			arenaNote(p.name().toLowerCase() + " " + b.getTitle(), null);
			arenaChanged();
		}
	}

	private void arenaChanged()
	{
		arenaDirty = true;
		saveMemorySoon();
	}

	private void arenaNote(String what, JsonObject o)
	{
		if (notes.isOpen())
		{
			JsonObject n = o == null ? new JsonObject() : o;
			n.addProperty("what", what);
			notes.note("arena", n);
		}
	}

	/**
	 * On the client thread: puts the followers on the panel and the map at once, then plans the
	 * quickest way to each one still to find on the worker and shows them again with it.
	 */
	private void showArena()
	{
		arenaDirty = false;
		if (!arenaActive() || !loggedIn)
		{
			arenaGeneration.incrementAndGet();
			arenaFought.clear();
			arenaLines = null;
			arenaPins.show(null);
			push();
			return;
		}
		List<ArenaHunt.Line> lines = memory.arena.lines(System.currentTimeMillis());
		// Keep the last routes while new ones are planned, so the panel does not flicker.
		List<ArenaHunt.Line> old = arenaLines;
		if (old != null)
		{
			for (ArenaHunt.Line l : lines)
			{
				for (ArenaHunt.Line o : old)
				{
					if (o.boss == l.boss && o.tile == l.tile)
					{
						l.routeSeconds = o.routeSeconds;
						l.routeVia = o.routeVia;
						l.routeEta = o.routeEta;
					}
				}
			}
		}
		arenaLines = lines;
		arenaPins.show(config.arenaOnWorldMap() ? lines : null);
		push();
		Router r = router;
		Atlas a = atlas;
		int from = here;
		if (r == null || a == null || from == Pt.NONE)
		{
			return;
		}
		arenaPlannedFrom = from;
		Account acc = account;
		Router.Options o = options();
		int gen = arenaGeneration.incrementAndGet();
		worker.execute(() ->
		{
			for (ArenaHunt.Line l : lines)
			{
				if (gen != arenaGeneration.get())
				{
					return;
				}
				if (!l.isSeen() || l.isDone())
				{
					continue;
				}
				Route found = r.route(from, l.tile, acc, o);
				l.routeSeconds = found.isFound() ? found.getSeconds() : -1;
				l.routeVia = ArenaWords.via(found, a.getPlaces());
				l.routeEta = found.isFound() ? Words.eta(found, o.paces) : null;
			}
			if (gen != arenaGeneration.get())
			{
				return;
			}
			push();
			clientThread.invokeLater(() ->
			{
				if (gen == arenaGeneration.get())
				{
					arenaPins.show(config.arenaOnWorldMap() ? lines : null);
				}
			});
		});
	}

	// ----- Memory ----------------------------------------------------------------------------

	private void loadMemory()
	{
		String json = configManager.getRSProfileConfiguration(CodexWayfinderConfig.GROUP, MEMORY_KEY);
		Memory m = null;
		if (json != null)
		{
			try
			{
				m = gson.fromJson(json, Memory.class);
			}
			catch (RuntimeException e)
			{
				// A damaged save: start fresh rather than fail.
			}
		}
		memory = (m == null ? new Memory() : m).mend();
		profileKey = configManager.getRSProfileKey();
		accountDirty = true;
		arenaDirty = true;
		arenaTimer = -1;
		rank();
		push();
	}

	private void saveMemory()
	{
		if (profileKey == null || !profileKey.equals(configManager.getRSProfileKey()))
		{
			return;
		}
		configManager.setRSProfileConfiguration(CodexWayfinderConfig.GROUP, MEMORY_KEY, gson.toJson(memory));
	}

	private void saveMemorySoon()
	{
		clientThread.invokeLater(this::saveMemory);
	}

	private Tab rememberedTab()
	{
		String t = configManager.getConfiguration(CodexWayfinderConfig.GROUP, TAB_KEY);
		try
		{
			return t == null ? Tab.GO : Tab.valueOf(t);
		}
		catch (IllegalArgumentException e)
		{
			return Tab.GO;
		}
	}

	// ----- The panel -------------------------------------------------------------------------

	private void push()
	{
		Atlas a = atlas;
		View v = new View(loggedIn, a != null, loggedIn ? overview : null, destination, route, bankRoute, planning, query,
			results, memory.recentPlaces(), unlocks, memory.tripsByUse(), memory.paces, note,
			a == null ? null : a.getPlaces(), a == null ? null : a.getItems(), arenaLines);
		SwingUtilities.invokeLater(() -> panel.update(v));
	}

	/** Drops the route: from the panel's button or the world map's menu. Overlays clear on the next tick. */
	private void clearRoute()
	{
		destination = null;
		route = null;
		bankRoute = null;
		note = null;
		planGeneration.incrementAndGet();
		push();
	}

	private List<Place> find(String q)
	{
		Atlas a = atlas;
		if (a == null || q.trim().isEmpty())
		{
			return Collections.emptyList();
		}
		List<Place> found = new ArrayList<>();
		Map<Place, Integer> score = new HashMap<>();
		for (Place p : a.getPlaces())
		{
			int s = p.match(q);
			if (s > 0)
			{
				found.add(p);
				// A landing reads before a bank of the same name.
				score.put(p, s * 10 + (p.getGroup() == Place.Group.LANDING ? 5 : 0));
			}
		}
		int me = here;
		found.sort(Comparator.comparingInt((Place p) -> -score.get(p))
			.thenComparingInt(p -> me == Pt.NONE ? 0 : Math.min(9999, Pt.flatDistance(me, p.getTile())))
			.thenComparing(Place::getName));
		return found.size() > RESULTS ? new ArrayList<>(found.subList(0, RESULTS)) : found;
	}

	private final class PanelActions implements Actions
	{
		@Override
		public void tab(Tab tab)
		{
			panel.show(tab);
			configManager.setConfiguration(CodexWayfinderConfig.GROUP, TAB_KEY, tab.name());
			if (tab == Tab.UNLOCKS && unlocks != null && unlocks.isEmpty())
			{
				clientThread.invokeLater(CodexWayfinderPlugin.this::rank);
			}
		}

		@Override
		public void search(String q)
		{
			query = q;
			results = find(q);
			push();
		}

		@Override
		public void go(Place place)
		{
			CodexWayfinderPlugin.this.go(place);
		}

		@Override
		public void clear()
		{
			clearRoute();
		}

		@Override
		public void replan()
		{
			clientThread.invokeLater(() -> CodexWayfinderPlugin.this.replan(true));
		}

		@Override
		public void rank()
		{
			clientThread.invokeLater(CodexWayfinderPlugin.this::rank);
		}
	}
}
