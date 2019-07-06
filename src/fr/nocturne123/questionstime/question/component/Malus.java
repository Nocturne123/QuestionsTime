package fr.nocturne123.questionstime.question.component;

public class Malus {

	private boolean announce;
	private int money;

	public Malus(int money, boolean announce) {
		this.money = money > 0 ? money : 0;
		this.announce = announce;
	}
	
	public int getMoney() {
		return money;
	}
	
	public boolean isAnnounce() {
		return announce;
	}
	
}
