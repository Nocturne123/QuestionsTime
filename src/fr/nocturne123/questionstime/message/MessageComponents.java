package fr.nocturne123.questionstime.message;

import fr.nocturne123.questionstime.message.component.*;

public class MessageComponents {

    public static final ComponentPlayer PLAYER_NAME = new ComponentPlayer("name");
    public static final ComponentDefault<String> QUESTION = new ComponentDefault<String>("question");
    public static final ComponentDefault<Byte> POSITION = new ComponentDefault<Byte>("position");
    public static final ComponentDefault<String> PROPOSITION = new ComponentDefault<String>("proposition");
    public static final ComponentTimer TIMER = new ComponentTimer("timer");
    public static final ComponentDefault<Integer> MONEY = new ComponentDefault<Integer>("money");
    public static final ComponentCurrency CURRENCY = new ComponentCurrency("currency");
    public static final ComponentDefault<Integer> QUANTITY = new ComponentDefault<Integer>("quantity");
    public static final ComponentModID MOD_ID = new ComponentModID("modid");
    public static final ComponentItem ITEM = new ComponentItem("item");
    public static final ComponentMetadata METADATA = new ComponentMetadata("metadata");
    public static final ComponentDefault<String> ANSWER = new ComponentDefault<String>("answer");

}
