package fr.nocturne123.questionstime.message.component;

import org.spongepowered.api.service.economy.EconomyService;
import org.spongepowered.api.text.Text;

public class ComponentCurrency extends MessageComponent<EconomyService> {

    public ComponentCurrency(String name) {
        super(name);
    }

    @Override
    public Text process(EconomyService economyService) {
        return economyService.getDefaultCurrency().getDisplayName();
    }
}
