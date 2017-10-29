package fr.nocturne123.questionstime;

import java.util.Optional;

import ninja.leaping.configurate.commented.CommentedConfigurationNode;

public class Malus {

	private boolean announce;
	private int money;
	
	public Malus(Optional<CommentedConfigurationNode> malusNode) {
		if(malusNode.isPresent()) {
			CommentedConfigurationNode malus = malusNode.get();
			this.announce = malus.getNode("announce") != null ? malus.getNode("announce").getBoolean() : true;
			this.money = malus.getNode("money") != null ? malus.getNode("money").getInt() : 0;
		} else {
			this.announce = true;
			this.money = 0;
		}
	}
	
	public int getMoney() {
		return money;
	}
	
	public boolean isAnnounce() {
		return announce;
	}
	
}
