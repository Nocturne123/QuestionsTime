package fr.nocturne123.questionstime;

import java.util.Optional;

import ninja.leaping.configurate.commented.CommentedConfigurationNode;

public class QuestionSimple extends Question{

	public QuestionSimple(String question, Optional<CommentedConfigurationNode> prizeNode, String answer) {
		super(question, prizeNode, answer);
	}

	
}
