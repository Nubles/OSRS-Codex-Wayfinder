package com.codex.wayfinder.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * What one way of travelling needs: skill levels, finished quests, items, and game variables
 * that record unlocks (a diary done, a landing site built, a spellbook chosen). Parsed from
 * the cells of the travel tables, then checked against an {@link Account}.
 */
public final class Need
{
	public static final Need NOTHING = new Need(new String[0], new int[0], new String[0], Collections.emptyList(),
		Collections.emptyList(), Collections.emptyList());

	private final String[] skills;
	private final int[] levels;
	private final String[] quests;
	/** Any one of these groups; each group needs all of its stacks. */
	private final List<List<Stack>> items;
	private final List<Var> varbits;
	private final List<Var> varps;

	private Need(String[] skills, int[] levels, String[] quests, List<List<Stack>> items, List<Var> varbits, List<Var> varps)
	{
		this.skills = skills;
		this.levels = levels;
		this.quests = quests;
		this.items = items;
		this.varbits = varbits;
		this.varps = varps;
	}

	/** One item requirement: any of these ids, this many; for a rune, a worn staff will do. */
	public static final class Stack
	{
		final String name;
		final int[] ids;
		final int[] standIns;
		final int quantity;
		final boolean unlock;

		Stack(String name, int[] ids, int[] standIns, int quantity, boolean unlock)
		{
			this.name = name;
			this.ids = ids;
			this.standIns = standIns;
			this.quantity = quantity;
			this.unlock = unlock;
		}

		public String getName()
		{
			return name;
		}

		public int getQuantity()
		{
			return quantity;
		}

		public int[] getIds()
		{
			return ids.clone();
		}

		boolean isCoins()
		{
			return "COINS".equals(name);
		}
	}

	/** A test on one game variable: equal, above, below, or a bit set. Cooldowns are not tested. */
	public static final class Var
	{
		final int id;
		final char op;
		final int value;

		Var(int id, char op, int value)
		{
			this.id = id;
			this.op = op;
			this.value = value;
		}

		boolean test(int actual)
		{
			switch (op)
			{
				case '=':
					return actual == value;
				case '>':
					return actual > value;
				case '<':
					return actual < value;
				case '&':
					return (actual & value) != 0;
				default:
					return true;
			}
		}

		public int getId()
		{
			return id;
		}
	}

	/** Builds a need from the table's cells; any cell may be null or empty. */
	public static Need parse(String skillCell, String itemCell, String questCell, String varbitCell, String varpCell, Items names)
	{
		List<String> skillNames = new ArrayList<>();
		List<Integer> skillLevels = new ArrayList<>();
		for (String part : split(skillCell, ";"))
		{
			String[] lv = part.trim().split("\\s+", 2);
			if (lv.length == 2)
			{
				try
				{
					skillLevels.add(Integer.parseInt(lv[0]));
					skillNames.add(lv[1].trim());
				}
				catch (NumberFormatException e)
				{
					// Not a "level Skill" pair.
				}
			}
		}
		List<String> questList = new ArrayList<>();
		for (String q : split(questCell, ";"))
		{
			if (!q.trim().isEmpty())
			{
				questList.add(q.trim());
			}
		}
		int[] lv = new int[skillLevels.size()];
		for (int i = 0; i < lv.length; i++)
		{
			lv[i] = skillLevels.get(i);
		}
		return new Need(skillNames.toArray(new String[0]), lv, questList.toArray(new String[0]),
			parseItems(itemCell, names), parseVars(varbitCell), parseVars(varpCell));
	}

	static List<List<Stack>> parseItems(String cell, Items names)
	{
		if (cell == null || cell.trim().isEmpty())
		{
			return Collections.emptyList();
		}
		List<List<Stack>> groups = new ArrayList<>();
		// The tables never mix "or" and "and" in one cell, so "or" splits first.
		for (String alt : cell.trim().split("\\|\\|?"))
		{
			List<Stack> group = new ArrayList<>();
			for (String token : alt.split("&&?"))
			{
				String t = token.trim();
				if (t.isEmpty())
				{
					continue;
				}
				String[] kv = t.split("=", 2);
				String name = kv[0].trim();
				int qty = 1;
				if (kv.length == 2)
				{
					try
					{
						qty = Integer.parseInt(kv[1].trim());
					}
					catch (NumberFormatException e)
					{
						qty = 1;
					}
				}
				int[] ids;
				int[] standIns = new int[0];
				boolean unlock = false;
				if (name.matches("\\d+"))
				{
					ids = new int[]{Integer.parseInt(name)};
				}
				else
				{
					int[] known = names.ids(name);
					ids = known == null ? new int[0] : known;
					standIns = names.standIns(name);
					unlock = name.startsWith("UNLOCK_");
				}
				group.add(new Stack(name, ids, standIns, qty, unlock));
			}
			if (!group.isEmpty())
			{
				groups.add(group);
			}
		}
		return groups;
	}

	static List<Var> parseVars(String cell)
	{
		List<Var> out = new ArrayList<>();
		for (String part : split(cell, ";"))
		{
			String t = part.trim();
			int at = -1;
			for (int i = 0; i < t.length(); i++)
			{
				char c = t.charAt(i);
				if (c == '=' || c == '>' || c == '<' || c == '&' || c == '@')
				{
					at = i;
					break;
				}
			}
			if (at <= 0 || at == t.length() - 1)
			{
				continue;
			}
			try
			{
				out.add(new Var(Integer.parseInt(t.substring(0, at)), t.charAt(at), Integer.parseInt(t.substring(at + 1))));
			}
			catch (NumberFormatException e)
			{
				// Not a variable test.
			}
		}
		return out;
	}

	private static String[] split(String cell, String sep)
	{
		if (cell == null || cell.trim().isEmpty())
		{
			return new String[0];
		}
		return cell.split(sep);
	}

	/** Both needs at once, for a ring dialled from one place to another. */
	public Need and(Need other)
	{
		if (other == NOTHING || other == this)
		{
			return this;
		}
		if (this == NOTHING)
		{
			return other;
		}
		String[] s = concat(skills, other.skills);
		int[] l = new int[levels.length + other.levels.length];
		System.arraycopy(levels, 0, l, 0, levels.length);
		System.arraycopy(other.levels, 0, l, levels.length, other.levels.length);
		List<List<Stack>> it;
		if (items.isEmpty())
		{
			it = other.items;
		}
		else if (other.items.isEmpty())
		{
			it = items;
		}
		else
		{
			it = new ArrayList<>();
			for (List<Stack> a : items)
			{
				for (List<Stack> b : other.items)
				{
					List<Stack> both = new ArrayList<>(a);
					both.addAll(b);
					it.add(both);
				}
			}
		}
		List<Var> vb = new ArrayList<>(varbits);
		vb.addAll(other.varbits);
		List<Var> vp = new ArrayList<>(varps);
		vp.addAll(other.varps);
		return new Need(s, l, concat(quests, other.quests), it, vb, vp);
	}

	private static String[] concat(String[] a, String[] b)
	{
		if (b.length == 0)
		{
			return a;
		}
		if (a.length == 0)
		{
			return b;
		}
		String[] out = new String[a.length + b.length];
		System.arraycopy(a, 0, out, 0, a.length);
		System.arraycopy(b, 0, out, a.length, b.length);
		return out;
	}

	public List<Var> getVarbits()
	{
		return varbits;
	}

	public List<Var> getVarps()
	{
		return varps;
	}

	public String[] getQuests()
	{
		return quests.clone();
	}

	public boolean hasItems()
	{
		return !items.isEmpty();
	}

	/** Where the items for this need must be. */
	public enum Reach
	{
		/** In the pack or worn, ready to go now. */
		CARRIED,
		/** Anywhere the account keeps them, the bank included. */
		OWNED
	}

	/** Whether everything but the items is met: levels, quests and unlocks. */
	public boolean unlocked(Account a)
	{
		return missingUnlocks(a, 1).isEmpty();
	}

	public boolean met(Account a, Reach reach)
	{
		return unlocked(a) && itemsMet(a, reach, true);
	}

	/**
	 * Whether the items are there. Coins can be left out of the test, since nearly every account
	 * can bring the fare and a route should not vanish over a few hundred coins in the bank.
	 */
	public boolean itemsMet(Account a, Reach reach, boolean countCoins)
	{
		if (items.isEmpty())
		{
			return true;
		}
		for (List<Stack> group : items)
		{
			boolean all = true;
			for (Stack s : group)
			{
				if (!has(a, s, reach, countCoins))
				{
					all = false;
					break;
				}
			}
			if (all)
			{
				return true;
			}
		}
		return false;
	}

	private static boolean has(Account a, Stack s, Need.Reach reach, boolean countCoins)
	{
		if (s.unlock)
		{
			return false;
		}
		if (s.isCoins() && !countCoins)
		{
			return true;
		}
		for (int id : s.standIns)
		{
			if (a.wears(id))
			{
				return true;
			}
		}
		int n = 0;
		for (int id : s.ids)
		{
			n += reach == Reach.CARRIED ? a.carried(id) : a.owned(id);
		}
		if (n >= s.quantity)
		{
			return true;
		}
		// A bank never seen might hold it: only items carried or worn are ever certain.
		return reach == Reach.OWNED && !a.isBankKnown() && s.ids.length > 0;
	}

	/** The unmet levels, quests and unlocks, in words, at most {@code limit} of them. */
	public List<String> missingUnlocks(Account a, int limit)
	{
		List<String> out = new ArrayList<>();
		for (int i = 0; i < skills.length && out.size() < limit; i++)
		{
			if (a.level(skills[i]) < levels[i])
			{
				out.add(levels[i] + " " + skills[i]);
			}
		}
		for (String q : quests)
		{
			if (out.size() >= limit)
			{
				break;
			}
			// Before quests were ever read, assume they are done rather than hide every route.
			if (a.isQuestsKnown() && !a.questDone(q))
			{
				out.add(q);
			}
		}
		for (Var v : varbits)
		{
			if (out.size() >= limit)
			{
				break;
			}
			if (!v.test(a.varbit(v.id)))
			{
				out.add("an unlock");
			}
		}
		for (Var v : varps)
		{
			if (out.size() >= limit)
			{
				break;
			}
			if (!v.test(a.varp(v.id)))
			{
				out.add("an unlock");
			}
		}
		return out;
	}

	/** Levels short by skill, for ranking what is nearly within reach; 0 when none are short. */
	public int levelsShort(Account a)
	{
		int n = 0;
		for (int i = 0; i < skills.length; i++)
		{
			n += Math.max(0, levels[i] - a.level(skills[i]));
		}
		return n;
	}

	/** The first item group, in words, such as "Ardougne cloak" or "3 air runes, 1 law rune". */
	public String itemWords(Items names)
	{
		if (items.isEmpty())
		{
			return "";
		}
		StringBuilder sb = new StringBuilder();
		for (Stack s : items.get(0))
		{
			if (s.unlock)
			{
				continue;
			}
			if (sb.length() > 0)
			{
				sb.append(", ");
			}
			String n = s.name.matches("\\d+") ? "item" : s.name.toLowerCase(Locale.ROOT).replace('_', ' ');
			if (s.isCoins())
			{
				sb.append(Format.number(s.quantity)).append(" coins");
			}
			else if (s.quantity > 1)
			{
				sb.append(s.quantity).append(' ').append(n).append('s');
			}
			else
			{
				sb.append(n);
			}
		}
		return sb.toString();
	}

	/** Coins this costs each time, or 0. */
	public int fare()
	{
		if (items.isEmpty())
		{
			return 0;
		}
		for (Stack s : items.get(0))
		{
			if (s.isCoins())
			{
				return s.quantity;
			}
		}
		return 0;
	}

	public List<List<Stack>> getItems()
	{
		return items;
	}
}
