package fr.nocturne123.questionstime.question;

import java.util.Optional;

import ninja.leaping.configurate.commented.CommentedConfigurationNode;

public class QuestionMulti extends Question {

	private String[] propositions;
	private byte answer;
	
	public QuestionMulti(String question, Optional<CommentedConfigurationNode> prizeNode, String[] propositions, byte answer) {
		super(question, prizeNode, String.valueOf(answer));
		this.propositions = propositions;
		this.answer = answer;
	}
	
	public String[] getPropositions() {
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

}
