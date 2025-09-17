package com.maDU59_.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import com.maDU59_.BetterCompass;

import net.fabricmc.loader.api.FabricLoader;

import java.io.*;
import java.lang.reflect.Type;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Collectors;

public class SettingsManager {

    public static List<Option> ALL_OPTIONS = new ArrayList<>();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_PATH = FabricLoader.getInstance().getConfigDir().resolve(BetterCompass.MOD_ID + ".json");
    public static List<Object> ENABLING_OPTION_VALUES = List.of(true, false);
    public static List<Object> COLOR_OPTION_VALUES = List.of("Red", "Green", "Blue", "Yellow", "Cyan", "Magenta", "Purple", "White", "Grey", "Black");
    public static List<Object> POSITION_VALUES = List.of("Aligned", "Above", "Under", "Disabled");

    public static Option SHOW_COMPASS_HUD = loadOptionWithDefaults(
        "SHOW_COMPASS_HUD",
        "Show the compass HUD",
        "Toggle the visibility of the compass HUD",
        "Always",
        "Always",
        List.of("Always", "Compass in inventory", "Compass in hand", "Never")
    );

    public static Option COMPASS_STYLE = loadOptionWithDefaults(
        "COMPASS_STYLE",
        "Compass' style",
        "Change the style of the compass",
        "No shadows",
        "No shadows",
        List.of("Shadows", "No shadows")
    );

    public static Option COMPASS_POSITION = loadOptionWithDefaults(
        "COMPASS_POSITION",
        "Compass' position",
        "Change the position of the compass",
        "Top",
        "Top",
        List.of("Top", "Bottom")
    );

    public static Option CARDINALS_DIRECTION_POSITION = loadOptionWithDefaults(
        "CARDINALS_DIRECTION_POSITION",
        "Position of the cardinal directions on the compass",
        "Choose the position of the cardinal directions on the compass",
        "Aligned",
        "Aligned",
        POSITION_VALUES
    );

    public static Option CARDINALS_DIRECTION_COLOR = loadOptionWithDefaults(
        "CARDINALS_DIRECTION_COLOR",
        "Color of the cardinal directions on the compass",
        "Choose the color of the cardinal directions on the compass",
        "Red",
        "Red",
        COLOR_OPTION_VALUES
    );

    public static Option LAST_DEATH_DIRECTION_POSITION = loadOptionWithDefaults(
        "LAST_DEATH_DIRECTION_POSITION",
        "Position of the last death's direction on the compass",
        "Choose the position of the last death's direction on the compass",
        "Under",
        "Under",
        POSITION_VALUES
    );

    public static Option LAST_DEATH_DIRECTION_COLOR = loadOptionWithDefaults(
        "LAST_DEATH_DIRECTION_COLOR",
        "Color of the last death's direction on the compass",
        "Choose the color of the last death's direction on the compass",
        "Red",
        "Red",
        COLOR_OPTION_VALUES
    );

    public static Option NETHER_PORTAL_DIRECTION_POSITION = loadOptionWithDefaults(
        "NETHER_PORTAL_DIRECTION_POSITION",
        "Position of the nether portal's direction on the compass",
        "Choose the position of the nether portal's direction on the compass",
        "Under",
        "Under",
        POSITION_VALUES
    );

    public static Option NETHER_PORTAL_DIRECTION_COLOR = loadOptionWithDefaults(
        "NETHER_PORTAL_DIRECTION_COLOR",
        "Color of the nether portal's direction on the compass",
        "Choose the color of the nether portal's direction on the compass",
        "Red",
        "Red",
        COLOR_OPTION_VALUES
    );

    public static List<String> getAllOptionsId(){
        List<String> list = new ArrayList<>();
        for (Option option : ALL_OPTIONS){
            list.add(option.getId());
            }
        return list;
    }

    public static boolean setOptionValue(String optionId, Object value){
        for (Option option : ALL_OPTIONS){
            System.out.println(optionId + ": " + option.getId() + ", " + option.getId().equalsIgnoreCase(optionId));
            System.out.println(value + ": " + option.getPossibleValues() + ", " + option.getPossibleValues().contains(value));
            if(option.getId().equalsIgnoreCase(optionId)){
                int index = option.getPossibleValues().stream().map(Object::toString).collect(Collectors.toList()).indexOf((String) value);
                if (option.getPossibleValues().contains(value)){
                    option.setValue(value);
                    return true;
                }
                else if(index != -1){
                    option.setValue(option.getPossibleValues().get(index));
                    return true;
                }
            }
        }
        return false;
    }

    public static List<String> getOptionPossibleValues(String optionId){
        for (Option option : ALL_OPTIONS){
            if (option.getId().equalsIgnoreCase(optionId)){
                return option.getPossibleValues().stream().map(Object::toString).collect(Collectors.toList());
            }
        }
        return null;
    }

    public static int getRGBColorFromSetting(String colorName) {
        int[] colors = getColorFromSetting(colorName);
        return colors[2] + colors[1] * 256 + colors[0] * 256 * 256 + 255 * 256 * 256 *256;
    }

    public static float[] convertColorToFloat(int[] colors){
        float red = colors[0]/(float)255.0;
        float green = colors[1]/(float)255.0;
        float blue = colors[2]/(float)255.0;
        return new float[] {red, green, blue};
    }

    public static float convertAlphaToFloat(int alpha){
        float alphaFloat = alpha/(float)255.0;
        return alphaFloat;
    }

    public static int[] getColorFromSetting(String colorName) {
        int red = 0, green = 0, blue = 0;
        switch (colorName) {
            case "Red":
                red = 255;
                break;
            case "Green":
                green = 255;
                break;
            case "Blue":
                blue = 255;
                break;
            case "Yellow":
                red = 255;
                green = 255;
                break;
            case "Cyan":
                green = 255;
                blue = 255;
                break;
            case "Magenta":
                red = 255;
                blue = 255;
                break;
            case "Purple":
                red = 128;
                green = 0;
                blue = 128;
                break;
            case "White":
                red = 255;
                green = 255;
                blue = 255;
                break;
            case "Grey":
                red = 128;
                green = 128;
                blue = 128;
                break;
            case "Black":
                red = 0;
                green = 0;
                blue = 0;
                break;
            default:
                red = 255; // Default to red if unknown
        }

        return new int[] {red, green, blue};
    }

    public static void saveSettings(List<Option> options) {
        Map<String, Option> map = toMap(options);
        try {
            Files.createDirectories(CONFIG_PATH.getParent());
            try (Writer writer = Files.newBufferedWriter(CONFIG_PATH)) {
                GSON.toJson(map, writer);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static Map<String, Option> toMap(List<Option> options) {
        Map<String, Option> map = new LinkedHashMap<>();
        for (Option option : options) {
            map.put(option.getId(), option);
        }
        return map;
    }

    private static Option loadOption(String key) {
        try (Reader reader = Files.newBufferedReader(CONFIG_PATH)) {
            Type type = new TypeToken<Map<String, Option>>() {}.getType();
            Map<String, Option> map = GSON.fromJson(reader, type);
            return map.get(key);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    private static Option loadOptionWithDefaults(String id, String name, String description, Object value, Object defaultValue, List<Object> possibleValues) {
        Option loadedOption = loadOption(id);
        System.out.println("Loaded option for " + id + ": " + (loadedOption == null ? "null" : loadedOption.getValueAsString()));
        if (loadedOption == null) {
            return new Option(
                    id,
                    name,
                    description,
                    value,
                    defaultValue,
                    possibleValues
            );
        } else {
            loadedOption.setPossibleValues(possibleValues);
            SettingsManager.ALL_OPTIONS.add(loadedOption);
            return loadedOption;
        }
    }
    
}
