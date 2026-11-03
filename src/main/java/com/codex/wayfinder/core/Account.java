package com.codex.wayfinder.core;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * What this account has, as last read from the game: levels, finished quests, the game
 * variables the travel tables check, and the items carried, worn and banked. It is a snapshot
 * built on the client thread and then only read, so routes can be planned on another thread.
 */
public final class Account
{
	public static final Account UNKNOWN = builder().build();

	private final Map<String, Integer> levels;
	private final Set<String> quests;
	private final boolean questsKnown;
	private final Map<Integer, Integer> varbits;
	private final Map<Integer, Integer> varps;
	private final Map<Integer, Integer> carried;
	private final Map<Integer, Integer> owned;
	private final Set<Integer> worn;
	private final boolean bankKnown;
	private final boolean known;
	private final Set<String> grownTrees;

	private Account(Builder b)
	{
		levels = Collections.unmodifiableMap(new HashMap<>(b.levels));
		quests = Collections.unmodifiableSet(new HashSet<>(b.quests));
		questsKnown = b.questsKnown;
		varbits = Collections.unmodifiableMap(new HashMap<>(b.varbits));
		varps = Collections.unmodifiableMap(new HashMap<>(b.varps));
		carried = Collections.unmodifiableMap(new HashMap<>(b.carried));
		Map<Integer, Integer> all = new HashMap<>(b.banked);
		b.carried.forEach((id, q) -> all.merge(id, q, Integer::sum));
		owned = Collections.unmodifiableMap(all);
		worn = Collections.unmodifiableSet(new HashSet<>(b.worn));
		bankKnown = b.bankKnown;
		known = b.known;
		grownTrees = Collections.unmodifiableSet(new HashSet<>(b.grownTrees));
	}

	public static Builder builder()
	{
		return new Builder();
	}

	/** Whether this was read from a logged-in account at all. */
	public boolean isKnown()
	{
		return known;
	}

	/** The real (unboosted) level, 1 when never read. */
	public int level(String skill)
	{
		return levels.getOrDefault(skill.toLowerCase(), 1);
	}

	public boolean questDone(String quest)
	{
		return quests.contains(quest);
	}

	public boolean isQuestsKnown()
	{
		return questsKnown;
	}

	public int varbit(int id)
	{
		return varbits.getOrDefault(id, 0);
	}

	public int varp(int id)
	{
		return varps.getOrDefault(id, 0);
	}

	/** In the pack or worn now. */
	public int carried(int id)
	{
		return carried.getOrDefault(id, 0);
	}

	/** Carried, worn or in the bank. */
	public int owned(int id)
	{
		return owned.getOrDefault(id, 0);
	}

	public boolean wears(int id)
	{
		return worn.contains(id);
	}

	public boolean isBankKnown()
	{
		return bankKnown;
	}

	/** A planted spirit tree that is fully grown, by its patch name. */
	public boolean treeGrown(String patch)
	{
		return grownTrees.contains(patch);
	}

	/** Whether everything a route depends on matches another snapshot. */
	public boolean sameAs(Account o)
	{
		return o != null && known == o.known && questsKnown == o.questsKnown && bankKnown == o.bankKnown
			&& levels.equals(o.levels) && quests.equals(o.quests) && varbits.equals(o.varbits) && varps.equals(o.varps)
			&& carried.equals(o.carried) && owned.equals(o.owned) && worn.equals(o.worn) && grownTrees.equals(o.grownTrees);
	}

	public Map<Integer, Integer> getCarried()
	{
		return carried;
	}

	public static final class Builder
	{
		private final Map<String, Integer> levels = new HashMap<>();
		private final Set<String> quests = new HashSet<>();
		private boolean questsKnown;
		private final Map<Integer, Integer> varbits = new HashMap<>();
		private final Map<Integer, Integer> varps = new HashMap<>();
		private final Map<Integer, Integer> carried = new HashMap<>();
		private final Map<Integer, Integer> banked = new HashMap<>();
		private final Set<Integer> worn = new HashSet<>();
		private boolean bankKnown;
		private boolean known;
		private final Set<String> grownTrees = new HashSet<>();

		public Builder known(boolean known)
		{
			this.known = known;
			return this;
		}

		public Builder level(String skill, int level)
		{
			levels.put(skill.toLowerCase(), level);
			return this;
		}

		public Builder quest(String name)
		{
			quests.add(name);
			questsKnown = true;
			return this;
		}

		public Builder questsKnown(boolean known)
		{
			questsKnown = known;
			return this;
		}

		public Builder varbit(int id, int value)
		{
			varbits.put(id, value);
			return this;
		}

		public Builder varp(int id, int value)
		{
			varps.put(id, value);
			return this;
		}

		public Builder carry(int id, int qty)
		{
			if (id > 0 && qty > 0)
			{
				carried.merge(id, qty, Integer::sum);
			}
			return this;
		}

		/** A worn item: carried, and able to stand in for runes when it is a staff. */
		public Builder wear(int id)
		{
			if (id > 0)
			{
				worn.add(id);
				carry(id, 1);
			}
			return this;
		}

		public Builder bank(int id, int qty)
		{
			if (id > 0 && qty > 0)
			{
				banked.merge(id, qty, Integer::sum);
			}
			bankKnown = true;
			return this;
		}

		public Builder bankKnown(boolean known)
		{
			bankKnown = known;
			return this;
		}

		public Builder treeGrown(String patch)
		{
			grownTrees.add(patch);
			return this;
		}

		public Account build()
		{
			return new Account(this);
		}
	}
}
