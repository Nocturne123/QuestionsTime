package fr.nocturne123.questionstime.question;

import com.google.common.base.Strings;
import fr.nocturne123.questionstime.QuestionException;

import java.util.ArrayList;
import java.util.List;

public class QuestionMulti extends Question {

	private List<String> propositions;
	private byte answer;

	private QuestionMulti(QuestionMultiBuilder builder) {
		super(builder);
		this.propositions = builder.propositions;
		this.answer = builder.answer;
	}

	/*public QuestionMulti(String question, Optional<ConfigurationNode> prizeNode, String[] propositions, byte answer,
			Optional<ConfigurationNode> malusNode, int timer) {
		super(question, prizeNode, String.valueOf(answer), malusNode, timer);
		this.propositions = propositions;
		this.answer = answer;
	}
	
	public QuestionMulti(String question, Prize prize, String[] propositions, byte answer, Malus malus, int timer) {
		super(question, prize, String.valueOf(answer), malus, timer);
		this.propositions = propositions;
		this.answer = answer;
	}*/
	
	public List<String> getPropositions() {
		return propositions;
	}
	
	@Override
	public String getAnswer() {
		return String.valueOf(answer);
	}
	
	@Override
	public Types getType() {
		return Types.MULTI;
	}

	public static QuestionMultiBuilder builder() {
		return new QuestionMultiBuilder();
	}

	public static class QuestionMultiBuilder extends QuestionBuilder<QuestionMultiBuilder> {

		private List<String> propositions = new ArrayList<>();
		private byte answer;

		public QuestionMultiBuilder addProposition(String proposition) {
			if(Strings.isNullOrEmpty(proposition))
				throw new NullPointerException("The proposition is null or empty");
			this.propositions.add(proposition);
			return this;
		}

		public QuestionMultiBuilder addPropositions(List<String> propositions) {
			if(propositions == null || propositions.isEmpty())
				throw new IllegalArgumentException("The propositions are null or empty");
			this.propositions.addAll(propositions);
			return this;
		}

		public QuestionMultiBuilder setAnswer(byte answer) {
			this.answer = answer;
			return this;
		}

		@Override
		public QuestionMultiBuilder setAnswer(String answer) {
			throw new IllegalArgumentException("This method has not te be used for a question with propositions");
		}

		@Override
		public QuestionMulti build() {
			if(Strings.isNullOrEmpty(this.question))
				throw new NullPointerException("The question is null or empty");
			if(propositions.size() <= 1)
				throw new QuestionException("The question need at least 2 propositions");
			if(this.answer < 0)
				throw new QuestionException("The answer need to be a number egal or superior of 0");
			return new QuestionMulti(this);
		}
	}

}
