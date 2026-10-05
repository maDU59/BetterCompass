package fr.madu59.bettercompass.config.configScreen;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;

public class ModMenuApiImpl implements ModMenuApi {
    @Override
    public ConfigScreenFactory<BetterCompassConfigScreen> getModConfigScreenFactory() {
        return BetterCompassConfigScreen::new;
    }
}
