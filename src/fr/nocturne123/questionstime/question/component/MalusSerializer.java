package fr.nocturne123.questionstime.question.component;

import com.google.common.reflect.TypeToken;
import ninja.leaping.configurate.ConfigurationNode;
import ninja.leaping.configurate.objectmapping.ObjectMappingException;
import ninja.leaping.configurate.objectmapping.serialize.TypeSerializer;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

public class MalusSerializer implements TypeSerializer<Malus> {

    @Nullable
    @Override
    public Malus deserialize(@NonNull TypeToken<?> type, @NonNull ConfigurationNode value) throws ObjectMappingException {
        boolean announce = value.getNode("announce").getBoolean(true);
        int money = value.getNode("money").getInt(0);
        return new Malus(money, announce);
    }

    @Override
    public void serialize(@NonNull TypeToken<?> type, @Nullable Malus malus, @NonNull ConfigurationNode value) throws ObjectMappingException {
        value.getNode("announce").setValue(malus == null || malus.isAnnounce());
        value.getNode("money").setValue(malus != null ? malus.getMoney() : 0);
    }
}
