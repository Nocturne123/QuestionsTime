package fr.nocturne123.questionstime.message.component;

import org.spongepowered.api.text.Text;

import java.util.concurrent.TimeUnit;

public class ComponentTimer extends MessageComponent<Integer> {

    public ComponentTimer(String name) {
        super(name);
    }

    @Override
    public Text process(Integer second) {
        long hours = TimeUnit.SECONDS.toHours(second);
        second -= (int) TimeUnit.HOURS.toSeconds(hours);
        long minutes = TimeUnit.SECONDS.toMinutes(second);
        second -= (int) TimeUnit.MINUTES.toSeconds(minutes);
        long seconds = TimeUnit.SECONDS.toSeconds(second);
        StringBuilder strBuilder = new StringBuilder();
        if(hours == 1)
            strBuilder.append(hours).append("h");
        else if(hours > 1)
            strBuilder.append(hours).append("hs");
        if(minutes == 1)
            strBuilder.append(minutes).append("min");
        else if(minutes > 1)
            strBuilder.append(minutes).append("mins");
        if(seconds == 1)
            strBuilder.append(seconds).append("sec");
        else if(seconds > 1)
            strBuilder.append(seconds).append("secs");
        return Text.of(strBuilder.toString());
    }
}
