package ftb.utils.ranks;

import com.google.gson.*;
import com.mojang.authlib.GameProfile;
import ftb.lib.FTBLib;
import ftb.lib.api.permissions.*;
import latmod.lib.*;
import latmod.lib.json.UUIDTypeAdapterLM;
import net.minecraft.util.EnumChatFormatting;

import java.io.File;
import java.util.*;

public class Ranks implements IPermissionHandler
{
	private static Ranks instance;
	
	public static Ranks instance()
	{
		if(instance == null) instance = new Ranks();
		return instance;
	}
	
	public static final Rank PLAYER = new Rank("Player");
	public static final Rank ADMIN = new Rank("Admin");
	
	public final File fileRanks, filePlayers;
	public final Map<String, Rank> ranks = new LinkedHashMap<>();
	public final Map<UUID, Rank> playerMap = new HashMap<>();
	public Rank defaultRank;
	
	private Ranks()
	{
		fileRanks = new File(FTBLib.folderLocal, "ftbu/ranks.json");
		filePlayers = new File(FTBLib.folderLocal, "ftbu/player_ranks.json");
		ADMIN.color = EnumChatFormatting.DARK_GREEN;
		PLAYER.color = EnumChatFormatting.WHITE;
		ADMIN.parent = PLAYER;
	}
	
	public void reload()
	{
		ranks.clear();
		playerMap.clear();
		defaultRank = null;
		
		try
		{
			JsonElement e = LMJsonUtils.fromJson(fileRanks);
			
			if(e.isJsonObject())
			{
				JsonObject o = e.getAsJsonObject();
				
				for(Map.Entry<String, JsonElement> entry : o.get("ranks").getAsJsonObject().entrySet())
				{
					ranks.put(entry.getKey(), new Rank(entry.getKey()));
				}
				
				for(Map.Entry<String, JsonElement> entry : o.get("ranks").getAsJsonObject().entrySet())
				{
					ranks.get(entry.getKey()).setJson(entry.getValue().getAsJsonObject());
				}
				
				defaultRank = ranks.get(o.get("default_rank").getAsString());
			}
			else
			{
				JsonObject o = new JsonObject();
				o.add("default_rank", new JsonPrimitive(PLAYER.getID()));
				JsonObject o1 = new JsonObject();
				o1.add(PLAYER.getID(), PLAYER.getJson());
				o1.add(ADMIN.getID(), ADMIN.getJson());
				o.add("ranks", o1);
				LMJsonUtils.toJson(fileRanks, o);
			}
		}
		catch(Exception ex)
		{
			ex.printStackTrace();
			defaultRank = null;
		}
		
		try
		{
			JsonElement e = LMJsonUtils.fromJson(LMFileUtils.newFile(filePlayers));
			
			if(e.isJsonObject())
			{
				for(Map.Entry<String, JsonElement> entry : e.getAsJsonObject().entrySet())
				{
					UUID id = UUIDTypeAdapterLM.getUUID(entry.getKey());
					if(id != null)
					{
						String s = entry.getValue().getAsString();
						
						if(ranks.containsKey(s))
						{
							playerMap.put(id, ranks.get(s));
						}
					}
				}
			}
			else
			{
				JsonObject o = new JsonObject();
				o.add(new UUID(0L, 0L).toString(), new JsonPrimitive("ExampleRank"));
				LMJsonUtils.toJson(filePlayers, o);
			}
		}
		catch(Exception ex)
		{
			ex.printStackTrace();
			defaultRank = null;
		}
		
		saveRanks();
	}
	
	public void saveRanks()
	{
		if(defaultRank != null)
		{
			JsonObject o = new JsonObject();
			o.add("default_rank", new JsonPrimitive(defaultRank.getID()));
			JsonObject o1 = new JsonObject();
			
			for(Rank r : ranks.values())
			{
				o1.add(r.getID(), r.getJson());
			}
			
			o.add("ranks", o1);
			LMJsonUtils.toJson(fileRanks, o);
			
			o = new JsonObject();
			for(Map.Entry<UUID, Rank> entry : playerMap.entrySet())
			{
				o.add(UUIDTypeAdapterLM.getString(entry.getKey()), new JsonPrimitive(entry.getValue().getID()));
			}
			LMJsonUtils.toJson(filePlayers, o);
		}
	}
	
	public void generateExampleFiles()
	{
		List<RankConfig> sortedRankConfigs = new ArrayList<>();
		sortedRankConfigs.addAll(ForgePermissionRegistry.getRegistredConfig());
		Collections.sort(sortedRankConfigs);
		
		List<ForgePermissionContainer> sortedPermissions = new ArrayList<>();
		sortedPermissions.addAll(ForgePermissionRegistry.getRegistredPermissions());
		Collections.sort(sortedPermissions);
		
		try
		{
			List<String> list = new ArrayList<>();
			
			list.add("Modifying this file won't do anything, it just shows all available permission IDs. See ranks_example.json");
			list.add("");
			
			list.add("-- Permissions --");
			list.add("");
			
			for(ForgePermissionContainer p : sortedPermissions)
			{
				list.add(p.getID());
				
				if(p.info != null)
				{
					for(String s : p.info)
						list.add("  " + s);
				}
				
				list.add("");
			}
			
			list.add("-- Config --");
			list.add("");
			
			for(RankConfig p : sortedRankConfigs)
			{
				list.add(p.getID());
				
				if(p.configData.info != null)
				{
					for(String s : p.configData.info)
						list.add("  " + s);
				}
				
				if(!PrimitiveType.isNull(p.configData.type))
				{
					list.add("  Type: " + p.configData.type);
				}
				
				if(p.configData.min() != Double.NEGATIVE_INFINITY)
				{
					if(p.configData.type == PrimitiveType.DOUBLE || p.configData.type == PrimitiveType.FLOAT)
						list.add("  Min: " + p.configData.min());
					else list.add("  Min: " + (long) p.configData.min());
				}
				
				if(p.configData.max() != Double.POSITIVE_INFINITY)
				{
					if(p.configData.type == PrimitiveType.DOUBLE || p.configData.type == PrimitiveType.FLOAT)
						list.add("  Max: " + p.configData.max());
					else list.add("  Max: " + (long) p.configData.max());
				}
				
				list.add("");
			}
			
			LMFileUtils.save(new File(FTBLib.folderLocal, "ftbu/all_permissions.txt"), list);
		}
		catch(Exception ex)
		{
			ex.printStackTrace();
		}
		
		try
		{
			JsonObject o = new JsonObject();
			
			o.add("default_rank", new JsonPrimitive("Player"));
			
			JsonObject o1 = new JsonObject();
			
			Rank rankPlayer = new Rank(PLAYER.getID());
			rankPlayer.setJson(PLAYER.getJson());
			
			for(RankConfig p : sortedRankConfigs)
			{
				rankPlayer.config.put(p, p.getDefaultPlayerValue());
			}
			
			rankPlayer.permissions.clear();
			
			for(ForgePermissionContainer c : sortedPermissions)
			{
				rankPlayer.permissions.put(c.getID(), c.playerValue);
			}
			
			o1.add(rankPlayer.getID(), rankPlayer.getJson());
			
			Rank rankAdmin = new Rank(ADMIN.getID());
			rankAdmin.parent = rankPlayer;
			rankAdmin.setJson(ADMIN.getJson());
			
			for(RankConfig p : sortedRankConfigs)
			{
				if(!p.getDefaultPlayerValue().toString().equals(p.getDefaultOPValue().toString()))
					rankAdmin.config.put(p, p.getDefaultOPValue());
			}
			
			rankAdmin.permissions.put("*", true);
			o1.add(rankAdmin.getID(), rankAdmin.getJson());
			
			o.add("ranks", o1);
			
			LMJsonUtils.toJson(new File(FTBLib.folderLocal, "ftbu/ranks_example.json"), o);
		}
		catch(Exception ex)
		{
			ex.printStackTrace();
		}
	}
	
	public Rank getRank(String s)
	{
		return ranks.get(s);
	}
	
	public Rank getRankOf(GameProfile profile)
	{
		if(defaultRank != null)
		{
			Rank r = playerMap.get(profile.getId());
			return (r == null) ? defaultRank : r;
		}
		
		return FTBLib.isOP(profile) ? ADMIN : PLAYER;
	}
	
	public void setRank(UUID player, Rank rank)
	{
		if(defaultRank != null)
		{
			playerMap.put(player, rank);
		}
	}
	
	public Boolean handlePermission(String permission, GameProfile profile)
	{
		return getRankOf(profile).handlePermission(permission);
	}
	
	public JsonElement handleRankConfig(RankConfig config, GameProfile profile)
	{
		return getRankOf(profile).handleRankConfig(config);
	}
}