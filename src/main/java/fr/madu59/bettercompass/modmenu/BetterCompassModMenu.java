package fr.madu59.bettercompass.modmenu;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;

public class BetterCompassModMenu implements ModMenuApi {
    @Override
    public ConfigScreenFactory<BetterCompassConfigScreen> getModConfigScreenFactory() {
        return BetterCompassConfigScreen::new;
    }
}