package fr.nocturne123.questionstime.question;

import java.util.ArrayList;
import java.util.Optional;

import org.apache.commons.lang3.StringUtils;
import org.spongepowered.api.data.DataQuery;
import org.spongepowered.api.item.ItemTypes;
import org.spongepowered.api.item.inventory.ItemStack;

import com.google.common.reflect.TypeToken;

import fr.nocturne123.questionstime.Malus;
import fr.nocturne123.questionstime.Prize;
import fr.nocturne123.questionstime.QuestionsTime;
import fr.nocturne123.questionstime.question.Question.Types;
import ninja.leaping.configurate.ConfigurationNode;
import ninja.leaping.configurate.objectmapping.ObjectMappingException;
import ninja.leaping.configurate.objectmapping.serialize.TypeSerializer;

public class QuestionSerializer implements TypeSerializer<Question>{

	@Override
	public Question deserialize(TypeToken<?> type, ConfigurationNode node) throws ObjectMappingException {
		Types qType = Question.getType(node);
		if(qType == Types.ERROR) {
			QuestionsTime.getInstance().getLogger().error("The question "+node.getKey()+" contain one or many errors. "
					+ "Check if he contain the sections \"question\" and \"answer\" at least. ");
			return null;
		}
		
		String question = node.getNode("question").getString();
		String answer = node.getNode("answer").getString();
		int timer = node.getNode("timer").getInt();
		ConfigurationNode prize = node.getNode("prize");
		ConfigurationNode malus = node.getNode("malus");
		
		if(qType == Types.SIMPLE) 
			return new Question(question, prize == null ? Optional.empty() : Optional.of(prize), answer,
					malus == null ? Optional.empty() : Optional.of(malus), timer);
		else if(qType == Types.MULTI) {
			if(StringUtils.isNumeric(answer) && Integer.valueOf(answer) <= 4 && Integer.valueOf(answer) > 0) {
				byte answerNumber = Byte.valueOf(answer);
				String propositionOne = node.getNode("proposition1").getString();
				ConfigurationNode propositionTwo = node.getNode("proposition2");
				ConfigurationNode propositionThree = node.getNode("proposition3");
				ConfigurationNode propositionFour = node.getNode("proposition4");
				return new QuestionMulti(question, prize == null ? Optional.empty() : Optional.of(prize),
						new String[] {propositionOne, propositionTwo == null ? "" : propositionTwo.getString(),
								 propositionThree == null ? "" : propositionThree.getString(),
								 propositionFour == null ? "" : propositionFour.getString()}, answerNumber, Optional.of(malus), timer);
			}
		}
		QuestionsTime.getInstance().getLogger().error("This never should happen, but she happened, so... just report the error with the more information you have");
		return null;
	}

	@Override
	public void serialize(TypeToken<?> type, Question q, ConfigurationNode node) throws ObjectMappingException {
		node.getNode("question").setValue(q.getQuestion());
		node.getNode("answer").setValue(q.getAnswer());
		node.getNode("timer").setValue(q.getTimer());
		if(q instanceof QuestionMulti) {
			QuestionMulti qMulti = (QuestionMulti) q;
			node.getNode("proposition1").setValue(qMulti.getPropositions()[0]);
			node.getNode("proposition2").setValue(qMulti.getPropositions()[1]);
			node.getNode("proposition3").setValue(qMulti.getPropositions()[2]);
			node.getNode("proposition4").setValue(qMulti.getPropositions()[3]);
		}
		ConfigurationNode prizeNode = node.getNode("prize");
		Prize prize = q.getPrize();
		prizeNode.getNode("announce").setValue(prize.isAnnounce());
		prizeNode.getNode("money").setValue(prize.getMoney());
		if(prize.getItems().length > 0 && prize.getItems()[0].getItem() != ItemTypes.NONE) {
			ArrayList<String> isList = new ArrayList<>();
			for(int i = 0; i < prize.getItems().length; i++) {
				ItemStack is = prize.getItems()[i];
				String isSer = is.getItem().getName();
				isSer += ";"+is.toContainer().getValues(true).get(DataQuery.of("UnsafeDamage"));
				isSer += ";"+is.getQuantity();
				isList.add(isSer);
			}
			prizeNode.getNode("items").setValue(isList);
		}
		ConfigurationNode malusNode = node.getNode("malus");
		Malus malus = q.getMalus();
		malusNode.getNode("announce").setValue(malus.isAnnounce());
		malusNode.getNode("money").setValue(malus.getMoney());
	}

}
