package fr.nocturne123.questionstime.question;

import java.util.ArrayList;
import java.util.UUID;

import org.apache.commons.lang3.StringUtils;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.item.ItemTypes;
import org.spongepowered.api.item.inventory.ItemStack;

import fr.nocturne123.questionstime.Malus;
import fr.nocturne123.questionstime.Prize;
import fr.nocturne123.questionstime.QuestionsTime;
import fr.nocturne123.questionstime.handler.ConfigHandler;
import fr.nocturne123.questionstime.question.Question.Types;
import fr.nocturne123.questionstime.util.TextUtils;

public class QuestionCreator {

	private String question, answer, previousResponse;
	private Types questionType;
	private UUID creator;
	private int currentStep, registerStep;
	private boolean confirm, halfconfirm;
	private String[] propositions;
	private boolean announcePrize, announceMalus;
	private int moneyPrize, moneyMalus;
	private ArrayList<ItemStack> itemsPrize;
	private int timer;
	
	public QuestionCreator(UUID uuid) {
		this.creator = uuid;
		this.currentStep = 0;
		this.propositions = new String[4];
		this.itemsPrize = new ArrayList<>();
	}
	
	public UUID getCreator() {
		return creator;
	}
	
	public int getCurrentStep() {
		return currentStep;
	}
	
	public void nextStep() {
		switch(currentStep) {
		case -1:
			if(previousResponse.equals("yes"))
				this.stop();
			else
				this.resume();
			break;
		case 0:
			this.question = previousResponse;
			break;
		case 1:
			if(previousResponse.equals("simple")) {
				this.questionType = Types.SIMPLE;
				this.currentStep += 4;
			} else if(previousResponse.equals("proposition"))
				this.questionType = Types.MULTI;
			else
				QuestionsTime.getInstance().getLogger().error("Error when creating a question > the \"previousResponse\" isn't simple or proposition {\""+this.previousResponse+"\"");
			break;
		case 2:
			this.propositions[0] = previousResponse;
			break;
		case 3:
			this.propositions[1] = previousResponse;
			break;
		case 4:
			this.propositions[2] = previousResponse;
			break;
		case 5:
			this.propositions[3] = previousResponse;
			break;
		case 6:
			this.answer = previousResponse;
			break;
		case 7:
			if(previousResponse.equals("no"))
				this.currentStep += 3;
			break;
		case 8:
			announcePrize = previousResponse.equals("yes") ? true : false;
			if(!QuestionsTime.getInstance().getEconomy().isPresent())
				this.currentStep++;
			break;
		case 9:
			if(Integer.valueOf(previousResponse) <= 0)
				this.moneyPrize = 0;
			else
				this.moneyPrize = Integer.valueOf(previousResponse);
			break;
		case 11:
			if(this.previousResponse.equals("no"))
				this.currentStep += 2;
			break;
		case 12:
			announceMalus = previousResponse.equals("yes") ? true : false;
			if(!QuestionsTime.getInstance().getEconomy().isPresent())
				this.currentStep++;
			break;
		case 13:
			if(Integer.valueOf(previousResponse) <= 0)
				this.moneyMalus = 0;
			else
				this.moneyMalus = Integer.valueOf(previousResponse);
			break;
		case 14:
			if(previousResponse.equals("no"))
				this.currentStep++;
			break;
		case 15:
			String timeHour = StringUtils.substringBefore(previousResponse, "h");
			String timeMinute = StringUtils.substringBetween(previousResponse, "h", "m");
			String timeSecond = StringUtils.substringBetween(previousResponse, "m", "s");
			timer = (Integer.valueOf(timeHour) * 3600) + (Integer.valueOf(timeMinute) * 60) + Integer.valueOf(timeSecond);
			break;
		case 16:
			if(previousResponse.equals("start"))
				this.finish(true);
			else if(previousResponse.equals("save"))
				this.finish(false);
			Sponge.getServer().getPlayer(creator).get().sendMessage(TextUtils.creatorNormalWithPrefix("OK, you leaving the Question Creator"));
			QuestionsTime.getInstance().removeCreator(this.creator);
			break;
		}
		this.currentStep++;
		this.confirm = false;
		this.halfconfirm = false;
	}
	
	public void finish(boolean ask) {
		if(this.itemsPrize.isEmpty())
			this.itemsPrize.add(ItemStack.of(ItemTypes.NONE, 1));
		ItemStack[] items = new ItemStack[this.itemsPrize.size()];
		items = this.itemsPrize.toArray(items);
		Prize prize = new Prize(this.moneyPrize, this.announcePrize, items);
		Malus malus = new Malus(this.moneyMalus, this.announceMalus);
		if(this.questionType == Types.MULTI) {
			QuestionMulti multiQ = new QuestionMulti(question, prize, propositions, Byte.valueOf(answer), malus, timer);
			if(ask)
				QuestionsTime.getInstance().addQuestion(multiQ);
			ConfigHandler.serializeQuestion(multiQ);
		} else {
			Question q = new Question(question, prize, answer, malus, timer);
			if(ask)
				QuestionsTime.getInstance().addQuestion(q);
			ConfigHandler.serializeQuestion(q);
		}
	}
	
	public void setStop() {
		this.registerStep = this.currentStep;
		this.currentStep = -1;
		this.halfconfirm = false;
		this.confirm = false;
	}
	
	public void resume() {
		this.currentStep = this.registerStep-1;
		this.registerStep = 0;
		this.halfconfirm = false;
		this.confirm = false;
	}
	
	public void stop() {
		this.currentStep = -42;
		Sponge.getServer().getPlayer(creator).get().sendMessage(TextUtils.creatorNormalWithPrefix("OK, you leaving the Question Creator"));
		QuestionsTime.getInstance().removeCreator(this.creator);
	}
	
	public boolean isConfirm() {
		return this.confirm;
	}
	
	public void setConfirm() {
		this.confirm = true;
	}
	
	public void setPreviousResponse(String previousResponse) {
		this.previousResponse = previousResponse;
	}
	
	public Types getQuestionType() {
		return questionType;
	}
	
	public void setHalfconfirm() {
		this.halfconfirm = true;
		this.confirm = false;
	}
	
	public boolean isHalfconfirm() {
		return halfconfirm;
	}
	
	public void addItemPrize(ItemStack is) {
		for(int i = 0; i < this.itemsPrize.size(); i++) {
			ItemStack itemStack = this.itemsPrize.get(i);
			if(itemStack.equalTo(is)) {
				itemStack.setQuantity(itemStack.getQuantity() + is.getQuantity());
				return;
			}
		}
		this.itemsPrize.add(is);
	}
	
}
