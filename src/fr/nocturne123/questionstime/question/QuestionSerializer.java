package fr.nocturne123.questionstime.question;

import com.google.common.reflect.TypeToken;
import fr.nocturne123.questionstime.QuestionsTime;
import fr.nocturne123.questionstime.question.Question.Types;
import fr.nocturne123.questionstime.question.component.Malus;
import fr.nocturne123.questionstime.question.component.Prize;
import ninja.leaping.configurate.ConfigurationNode;
import ninja.leaping.configurate.objectmapping.ObjectMappingException;
import ninja.leaping.configurate.objectmapping.serialize.TypeSerializer;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;

import java.util.List;
import java.util.Optional;

public class QuestionSerializer implements TypeSerializer<Question>{

	private Logger logger = QuestionsTime.getInstance().getLogger();

	@Override
	public Question deserialize(TypeToken<?> type, ConfigurationNode node) throws ObjectMappingException {
		Types questioneType = Question.getType(node);
		if(questioneType == Types.ERROR)
			throw new ObjectMappingException("The question "+node.getKey()+" contain one or many errors. " +
					"Check if he contain the sections \"question\" and \"answer\" at least. ");

		String question = node.getNode("question").getString();
		String answer = node.getNode("answer").getString();
		int timer = node.getNode("timer").getInt();
		int timeBetweenAnswer = node.getNode("time-between-answer").getInt();
		int weight = node.getNode("weight").getInt(10);
		ConfigurationNode prize = node.getNode("prize");
		ConfigurationNode malus = node.getNode("malus");

		if(questioneType == Types.SIMPLE)
			return Question.builder().setQuestion(question).setPrize(prize).setAnswer(answer)
					.setMalus(malus).setTimer(timer).setTimeBetweenAnswer(timeBetweenAnswer)
					.setWeight(weight).build();
		else if(questioneType == Types.MULTI) {
			if(!StringUtils.isNumeric(answer) || Integer.valueOf(answer) > Byte.MAX_VALUE || Integer.valueOf(answer) < 0) {
				logger.error("The question \""+node.getKey()+"\" answer need to be a number between 0 and 127");
				return null;
			}
			ConfigurationNode propositions = node.getNode("proposition");
			if(!(propositions.getValue() instanceof List) || propositions.getChildrenList().size() <= 1) {
				logger.error("The question \""+node.getKey()+"\" answer need to have at least 2 propositions");
				return null;
			}
			byte answerNumber = Byte.parseByte(answer);
            QuestionMulti.QuestionMultiBuilder questionMultiBuilder = QuestionMulti.builder()
                    .setQuestion(question).setPrize(prize).setMalus(malus).setAnswer(answerNumber)
                    .setTimer(timer).setTimeBetweenAnswer(timeBetweenAnswer).setWeight(weight);
            propositions.getChildrenList().forEach(proposition -> questionMultiBuilder.addProposition(proposition.getString()));
			return questionMultiBuilder.build();
		}
		logger.error("This never should happen, but she happened, so... just report the error with the more information you have");
		return null;
	}

	@Override
	public void serialize(TypeToken<?> type, Question question, ConfigurationNode node) {
		node.getNode("question").setValue(question.getQuestion());
		node.getNode("answer").setValue(question.getAnswer());
		node.getNode("timer").setValue(question.getTimer());
		node.getNode("time-between-answer").setValue(question.getTimeBetweenAnswer());
		node.getNode("weight").setValue(question.getWeight());
		if(question instanceof QuestionMulti)
			node.getNode("proposition").setValue(((QuestionMulti) question).getPropositions());

		ConfigurationNode prizeNode = node.getNode("prize");
		Optional<Prize> prizeOptional = question.getPrize();
        prizeOptional.ifPresent(prize -> {
			try {
				prizeNode.setValue(TypeToken.of(Prize.class), prize);
			} catch (ObjectMappingException e) {
				e.printStackTrace();
			}
		});
		ConfigurationNode malusNode = node.getNode("malus");
		Optional<Malus> malusOptional = question.getMalus();
		malusOptional.ifPresent(malus -> {
			try {
				malusNode.setValue(TypeToken.of(Malus.class), malus);
			} catch (ObjectMappingException e) {
				e.printStackTrace();
			}
        });
	}

}
