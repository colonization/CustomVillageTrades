package com.alfalfascout.CustomVillageTrades;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

import org.bukkit.Color;
import org.bukkit.DyeColor;
import org.bukkit.FireworkEffect;
import org.bukkit.Material;
import org.bukkit.block.banner.Pattern;
import org.bukkit.block.banner.PatternType;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.EntityType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BannerMeta;
import org.bukkit.inventory.meta.BookMeta;
import org.bukkit.inventory.meta.BookMeta.Generation;
import org.bukkit.inventory.meta.FireworkEffectMeta;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.material.SpawnEgg;
import org.bukkit.potion.PotionData;
import org.bukkit.potion.PotionType;

public class MetaHelper {
    private static CustomVillageTrades plugin;
    static Random rand = new Random();
    
    public MetaHelper(CustomVillageTrades instance) {
        plugin = instance;
    }
    
    public ItemStack getItemName(FileConfiguration f, ItemStack item,
                                  String path) {
        ItemMeta meta = item.getItemMeta();
        
        if (f.isString(path + ".name")) {
            meta.setDisplayName(f.getString(path + ".name"));
            item.setItemMeta(meta);
        }
        else {
            plugin.getLogger().warning(path + ".name should be a string.");
            meta.setDisplayName("a string");
            item.setItemMeta(meta);
        }
        return item;
    }
    
    public ItemStack getItemLore(FileConfiguration f, ItemStack item,
                                  String path) {
        ItemMeta meta = item.getItemMeta();
        if (f.isString(path + ".lore")) {
            List<String> lore = Arrays.asList(
                    f.getString(path + ".lore").split(","));
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        else {
            plugin.getLogger().warning(path + ".lore should be a string.");
            meta.setLore(Arrays.asList("a string"));
            item.setItemMeta(meta);
        }
        return item;
    }
    
    public ItemStack handleSpawnEgg(FileConfiguration f, ItemStack item,
                                    String path) {
        EntityType spawnEggType = EntityType.PIG;
        if (f.contains(path + ".spawns")) {
            try {
                spawnEggType = EntityType.valueOf(
                        f.getString(path + ".spawns").toUpperCase());
            }
            catch (IllegalArgumentException e) {
                e.printStackTrace();
                plugin.getLogger().warning(f.getString(path + ".spawns") +
                        " is not a valid entity type for spawn eggs.");
                spawnEggType = EntityType.PIG;
            }
        }
    
        SpawnEgg spawnEgg = new SpawnEgg(spawnEggType);
        item = spawnEgg.toItemStack(1);
        return item;
    }
    
    public ItemStack handlePotion(FileConfiguration f, ItemStack item,
                                  String path) {
        PotionMeta meta = (PotionMeta) item.getItemMeta();
        PotionType potionType = PotionType.WATER;
        boolean potionExtended = false;
        boolean potionUpgraded = false;
    
        if (f.contains(path + ".potion.type")) {
            try {
                potionType = PotionType.valueOf(
                        f.getString(path + ".potion.type").toUpperCase());
            }
            catch (IllegalArgumentException e) {
                e.printStackTrace();
                plugin.getLogger().warning(f.getString(path + ".potion.type") +
                        " is not a valid potion type.");
            }
        }
    
        if (f.contains(path + ".potion.extended") &&
                potionType.isExtendable()) {
            try {
                potionExtended = f.getBoolean(path + ".potion.extended");
            }
            catch (Exception e) {
                e.printStackTrace();
                plugin.getLogger().warning(
                        "'extended' should be true or false.");
            }
        }
    
        if (f.contains(path + ".potion.upgraded") &&
                potionType.isUpgradeable()) {
            try {
                potionUpgraded = f.getBoolean(path + ".potion.upgraded");
            }
            catch (Exception e) {
                e.printStackTrace();
                plugin.getLogger().warning(
                        "'upgraded' should be true or false.");
            }
        }
    
        if ((potionExtended && potionUpgraded)) {
            plugin.getLogger().warning(
                    "Potion cannot be both extended and upgraded.");
            potionExtended = false;
            potionUpgraded = false;
        }
    
        PotionData potionData =
                new PotionData(potionType, potionExtended, potionUpgraded);
        meta.setBasePotionData(potionData);
        item.setItemMeta(meta);
        return item;
    }
    
    public ItemStack handleBanner(FileConfiguration f, ItemStack item, 
            String path) {
        BannerMeta meta = (BannerMeta) item.getItemMeta();
        if (f.contains(path + ".banner") && f.isString(path + ".banner")) {
            File bannerFile = new File(plugin.getDataFolder(),
                    f.getString(path + ".banner"));
            try {
                bannerFile.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
            FileConfiguration b = 
                    YamlConfiguration.loadConfiguration(bannerFile);
            b.setDefaults(plugin.defaultBanner);

            // base color
            if (!b.contains("base.color", true)) {
                b.set("base.color", "white");
                try {
                    b.save(bannerFile);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            
            DyeColor base = DyeColor.valueOf(
                    b.getString("base.color").toUpperCase());
            if (base == null) {
                plugin.getLogger().info("Invalid base color.");
                base = DyeColor.WHITE;
            }
            meta.setBaseColor(base);
            item.setDurability(getDurabilityByDyeColor(base));
            
            // patterns
            int patternNum = 1;
            String pPath = "pattern" + Integer.toString(patternNum);
            List<Pattern> patterns = new ArrayList<Pattern>();
            
            while (b.contains(pPath) && patternNum <= 6) {
                DyeColor pColor = DyeColor.WHITE;
                PatternType pType = PatternType.BORDER;
                
                if (b.contains(pPath + ".color") &&
                        b.isString(pPath + ".color")) {
                    pColor = DyeColor.valueOf(
                            b.getString(pPath + ".color").toUpperCase());
                    
                    if (pColor == null) {
                        plugin.getLogger().info("Invalid pattern color.");
                        pColor = DyeColor.WHITE;
                    }
                }
                
                if (b.contains(pPath + ".type") &&
                        b.isString(pPath + ".type")) {
                    pType = PatternType.valueOf(
                            b.getString(pPath + ".type").toUpperCase());
                    
                    if (pType == null) {
                        plugin.getLogger().info("Invalid pattern type.");
                        pType = PatternType.BORDER;
                    }
                }
                
                patterns.add(new Pattern(pColor, pType));
                
                patternNum++;
                pPath = "pattern" + Integer.toString(patternNum);
            }
            
            meta.setPatterns(patterns);
        }
        
        item.setItemMeta(meta);
        return item;
    }
    
    public ItemStack handleSkull(FileConfiguration f, ItemStack item,
            String path) {
        if (f.contains(path + ".owner") && f.isString(path + ".owner")) {
            SkullMeta meta = (SkullMeta) item.getItemMeta();
            
            meta.setOwner(f.getString(path + ".owner"));
            item.setItemMeta(meta);
        }
        return item;
    }
    
    public ItemStack handleBook(FileConfiguration f, ItemStack item,
            String path) {
        BookMeta meta = (BookMeta) item.getItemMeta();
        if (f.contains(path + ".book") && f.isString(path + ".book")) {
            File bookFile = new File(plugin.getDataFolder(),
                    f.getString(path + ".book"));
            try {
                bookFile.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
            FileConfiguration b = 
                    YamlConfiguration.loadConfiguration(bookFile);
            b.setDefaults(plugin.defaultBook);
            
            if (b.contains("author", true) && b.isString("author")) {
                meta.setAuthor(b.getString("author"));
            }
            
            if (b.contains("title", true) && b.isString("title")) {
                meta.setTitle(b.getString("title"));
            }
            
            if (b.contains("status", true) && b.isString("status")) {
                meta.setGeneration(Generation.valueOf(b.getString("status")));
            }
            
            if (b.contains("pages", true)) {
                List<String> pages = new ArrayList<String>();
                int pageNum = 1;
                String pagePath = "pages.page" + Integer.toString(pageNum);
                
                while (b.contains(pagePath)) {
                    pages.add(b.getString(pagePath));
                    
                    pageNum++;
                    pagePath = "pages.page" + Integer.toString(pageNum);
                }
                
                meta.setPages(pages);
            }
            
            item.setItemMeta(meta);
        }
        
        return item;
    }
    
    public ItemStack handleRocket(FileConfiguration f, ItemStack item,
            String path) {
        FireworkMeta meta = (FireworkMeta) item.getItemMeta();
        if (f.contains(path + ".firework") && f.isString(path + ".firework")) {
            File fireworkFile = new File(plugin.getDataFolder(),
                    f.getString(path + ".firework"));
            try {
                fireworkFile.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
            FileConfiguration b = 
                    YamlConfiguration.loadConfiguration(fireworkFile);
            
            int power = 1;
            List<FireworkEffect> effects = new ArrayList<FireworkEffect>();
            
            if (b.contains("power") && b.isInt("power") &&
                    b.getInt("power") > -1) {
                power = b.getInt("power");
            }
            
            if (b.contains("effects")) {
                int effectNum = 1;
                String effectPath = "effects.effect" + 
                        Integer.toString(effectNum);
                while (b.contains(effectPath)) {
                    effects.add(buildFirework(
                            b.getConfigurationSection(effectPath)));
                    effectNum++;
                    effectPath = "effects.effect" + 
                            Integer.toString(effectNum);
                }
            }
            
            meta.addEffects(effects);
            meta.setPower(power);
            
            item.setItemMeta(meta);
        }
        
        return item;
    }
    
    public ItemStack handleFireworkStar(FileConfiguration f, ItemStack item,
            String path) {
        FireworkEffectMeta meta = (FireworkEffectMeta) item.getItemMeta();
        if (f.contains(path + ".firework") && f.isString(path + ".firework")) {
            File fireworkFile = new File(plugin.getDataFolder(),
                    f.getString(path + ".firework"));
            try {
                fireworkFile.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
            FileConfiguration b = 
                    YamlConfiguration.loadConfiguration(fireworkFile);
            //b.setDefaults(plugin.defaultFireworkStar);
            
            meta.setEffect(buildFirework(b.getRoot()));
            
            item.setItemMeta(meta);
        }
        return item;
    }
    
    public FireworkEffect buildFirework(ConfigurationSection f) {
        FireworkEffect.Type type = FireworkEffect.Type.BALL;
        List<Color> colors = new ArrayList<Color>();
        List<Color> fades = new ArrayList<Color>();
        boolean flicker = false;
        boolean trail = false;
        
        if (f.contains("type") && f.isString("type")) {
            type = FireworkEffect.Type.valueOf(
                    f.getString("type").toUpperCase());
            
            if (type == null) {
                plugin.getLogger().warning("No such firework type as " + 
                        f.getString("type"));
                type = FireworkEffect.Type.BALL;
            }
        }
        
        if (f.contains("flicker") && f.isBoolean("flicker")) {
            flicker = f.getBoolean("flicker");
        }
        
        if (f.contains("trail") && f.isBoolean("trail")) {
            trail = f.getBoolean("trail");
        }
        
        if (f.contains("colors") && f.isList("colors")) {
            for (String color : f.getStringList("colors")) {
                if (DyeColor.valueOf(color.toUpperCase()) != null) {
                    colors.add(DyeColor.valueOf(
                            color.toUpperCase()).getFireworkColor());
                }
            }
        }
        if (colors.isEmpty()) {
            colors.add(Color.WHITE);
            plugin.getLogger().info("empty color list");
        }
        
        if (f.contains("fades") && f.isList("fades")) {
            for (String fade : f.getStringList("fades")) {
                if (DyeColor.valueOf(fade.toUpperCase()) != null) {
                    fades.add(DyeColor.valueOf(
                            fade.toUpperCase()).getColor());
                }
            }
        }
        
        FireworkEffect.Builder builder = FireworkEffect.builder().with(type);
        builder.flicker(flicker);
        builder.trail(trail);
        builder.withColor(colors);
        if (!fades.isEmpty()) {
            builder.withFade(fades);
        }
        
        return builder.build();
    }
    
    public ItemStack handleEnchantment(FileConfiguration f, ItemStack item,
            String path) {
        // get user-specified enchantments
        if (f.contains(path + ".enchantment.enchant1")) {
            String enchantPath = path + ".enchantment.enchant1";
            int enchantNum = 1;
        
            while (f.contains(enchantPath)) {
                int specLevel = 1;
                Enchantment specType = Enchantment.DURABILITY;
            
                if (f.contains(enchantPath + ".type")) {
                    String typeString = f.getString(
                            enchantPath + ".type").toUpperCase();
                
                    if (typeString.equals("random")) {
                        specType = EnchantHelper.getRandomEnchantment(
                                plugin, item);
                    }
                    else {
                        specType = Enchantment.getByName(typeString);
                    }
                
                    if (specType == null) {
                        plugin.getLogger().warning("No valid type: " + 
                                typeString);
                        specType = Enchantment.DURABILITY;
                    }
                }
            
                if (f.contains(enchantPath + ".level")) {
                    String levelString = f.getString(
                            enchantPath + ".level");
                
                    if (levelString.equals("random")) {
                        specLevel = rand.nextInt(
                                specType.getMaxLevel()) + 1;
                    }
                    else {
                        specLevel = f.getInt(
                                enchantPath + ".level");
                    }
                }
            
                LeveledEnchantment specEnchant = new LeveledEnchantment(
                        plugin, specType.hashCode(), specLevel);
            
                if (specEnchant.canEnchantItem(item) ||
                        item.getType().equals(Material.ENCHANTED_BOOK)) {
                    EnchantHelper.applyEnchantment(
                            plugin, item, specEnchant);
                }
                else {
                    plugin.getLogger().warning("The enchantment " +
                            specEnchant.toString() +
                            " can't be applied to " + item.toString());
                }
            
                enchantNum++;
                enchantPath = (path + ".enchantment.enchant" +
                        Integer.toString(enchantNum));
            }
        }
    
        // OR generate random enchantments
        else {
            int level = 1;
            boolean allowTreasure = false;
    
            if (f.contains(path + ".enchantment.level")) {
                try {
                    level = f.getInt(path + ".enchantment.level");
                    if (level < 0) {
                        plugin.getLogger().warning(
                                "Enchantment level must be greater than zero.");
                        level = 1;
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    plugin.getLogger().warning("The value in " + path +
                            ".enchantment.level should be an integer.");
                }
            }
    
    
            if (f.contains(path + ".enchantment.allow_treasure")) {
                try {
                    allowTreasure = f.getBoolean(path
                            + ".enchantment.allow_treasure");
                } catch (Exception e) {
                    e.printStackTrace();
                    plugin.getLogger().warning("The value in " + path +
                            ".enchantment.allow_treasure should be " +
                            "true or false.");
                }
            }
    
            if (item.getType().equals(Material.ENCHANTED_BOOK) ||
                    item.getType().equals(Material.BOOK)) {
                item = EnchantHelper.randomlyEnchantBook(plugin, item,
                        allowTreasure);
            } else {
                item = EnchantHelper.randomlyEnchant(plugin,
                        item, level, allowTreasure);
            }
        }
        return item;
    }
    
    public void makeBannerFile(ItemStack bannerItem) {
        if (!bannerItem.getType().equals(Material.BANNER)) {
            plugin.getLogger().info("That's not a banner.");
            return;
        }
        BannerMeta bannerMeta = (BannerMeta) bannerItem.getItemMeta();
        
        Integer bannerNo = new Integer(1);
        String bannerName = "banner" + bannerNo.toString() + ".yml";
        File bannerFile = new File(plugin.getDataFolder(), bannerName);
        try {
            while (!bannerFile.createNewFile()) {
                bannerNo++;
                bannerName = "banner" + bannerNo.toString() + ".yml";
                bannerFile = new File(plugin.getDataFolder(), bannerName);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        
        FileConfiguration bannerConf = new YamlConfiguration();
        
        DyeColor baseColor = bannerMeta.getBaseColor();
        if (baseColor == null) {
            baseColor = getColorByDurability(bannerItem.getDurability());
        }
        bannerConf.set("base.color", baseColor.name());
        
        int pNum = 1;
        String pPath = "pattern" + Integer.toString(pNum);
        for (Pattern pattern : bannerMeta.getPatterns()) {
            bannerConf.set(pPath + ".color", pattern.getColor().toString());
            bannerConf.set(pPath + ".type", pattern.getPattern().toString());
            pNum++;
            pPath = "pattern" + Integer.toString(pNum);
        }
        
        try {
            bannerConf.save(bannerFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    public void makeBookFile(ItemStack bookItem) {
        if (!(bookItem.getType().equals(Material.WRITTEN_BOOK) ||
                bookItem.getType().equals(Material.BOOK_AND_QUILL))) {
            plugin.getLogger().info("That's not a book.");
            return;
        }
        BookMeta bookMeta = (BookMeta) bookItem.getItemMeta();
        
        Integer bookNo = new Integer(1);
        String bookName = "book" + bookNo.toString() + ".yml";
        File bookFile = new File(plugin.getDataFolder(), bookName);
        try {
            while (!bookFile.createNewFile()) {
                bookNo++;
                bookName = "book" + bookNo.toString() + ".yml";
                bookFile = new File(plugin.getDataFolder(), bookName);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        
        FileConfiguration bookConf = new YamlConfiguration();
        
        if (bookMeta.hasAuthor()) {
            bookConf.set("author", bookMeta.getAuthor());
        }
        
        if (bookMeta.hasTitle()) {
            bookConf.set("title", bookMeta.getTitle());
        }
        
        if (bookMeta.getGeneration() != null) {
            bookConf.set("status", bookMeta.getGeneration().name());
        }
        
        if (bookMeta.hasPages()) {
            bookConf.createSection("pages");
            String page = "";
            String path = "";
            for (int i = 1; i <= bookMeta.getPageCount(); i++) {
                page = bookMeta.getPage(i);
                path = "pages.page" + Integer.toString(i);
                bookConf.set(path, page);
            }
        }
        
        try {
            bookConf.save(bookFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    public DyeColor getColorByDurability(short durability) {
        switch (durability) {
        case 0:
            return DyeColor.BLACK;
        case 1:
            return DyeColor.RED;
        case 2:
            return DyeColor.GREEN;
        case 3:
            return DyeColor.BROWN;
        case 4:
            return DyeColor.BLUE;
        case 5:
            return DyeColor.PURPLE;
        case 6:
            return DyeColor.CYAN;
        case 7:
            return DyeColor.SILVER;
        case 8:
            return DyeColor.GRAY;
        case 9:
            return DyeColor.PINK;
        case 10:
            return DyeColor.LIME;
        case 11:
            return DyeColor.YELLOW;
        case 12:
            return DyeColor.LIGHT_BLUE;
        case 13:
            return DyeColor.MAGENTA;
        case 14:
            return DyeColor.ORANGE;
        case 15:
            return DyeColor.WHITE;
        default:
            return DyeColor.WHITE;
        }
    }
    
    public short getDurabilityByDyeColor(DyeColor color) {
        switch (color) {
        case BLACK:
            return 0;
        case RED:
            return 1;
        case GREEN:
            return 2;
        case BROWN:
            return 3;
        case BLUE:
            return 4;
        case PURPLE:
            return 5;
        case CYAN:
            return 6;
        case SILVER:
            return 7;
        case GRAY:
            return 8;
        case PINK:
            return 9;
        case LIME:
            return 10;
        case YELLOW:
            return 11;
        case LIGHT_BLUE:
            return 12;
        case MAGENTA:
            return 13;
        case ORANGE:
            return 14;
        case WHITE:
            return 15;
        default:
            return 15;
        }
    }
}
