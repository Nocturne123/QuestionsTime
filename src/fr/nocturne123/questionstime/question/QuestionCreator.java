package fr.nocturne123.questionstime.question;

import fr.nocturne123.questionstime.QuestionsTime;
import fr.nocturne123.questionstime.handler.ConfigHandler;
import fr.nocturne123.questionstime.question.Question.Types;
import fr.nocturne123.questionstime.question.component.Malus;
import fr.nocturne123.questionstime.question.component.Prize;
import fr.nocturne123.questionstime.util.TextUtils;
import org.apache.commons.lang3.StringUtils;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.item.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class QuestionCreator {

	private String question, answer, previousResponse;
	private Types questionType;
	private UUID creator;
	private int currentStep, registerStep;
	private boolean halfConfirm, confirm;
	private List<String> propositions;
	private boolean announcePrize, announceMalus;
	private int moneyPrize, moneyMalus;
	private List<ItemStack> itemsPrize;
	private int timer, timeBetweenAnswer;

	public QuestionCreator(UUID uuid) {
		this.creator = uuid;
		this.currentStep = 0;
		this.propositions = new ArrayList<>();
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
				this.currentStep++;
			} else if(previousResponse.equals("proposition"))
				this.questionType = Types.MULTI;
			else
				QuestionsTime.getInstance().getLogger().error("Error when creating a question > the \"previousResponse\" isn't simple or proposition {\""+this.previousResponse+"\"");
			break;
		case 2:
			//nothing
			break;
		case 3:
			this.answer = previousResponse;
			break;
		case 4:
			if(previousResponse.equals("no")) {
				if(!QuestionsTime.getInstance().getEconomy().isPresent())
					this.currentStep += 6;
				else
					this.currentStep += 3;
			}
			break;
		case 5:
			announcePrize = previousResponse.equals("yes");
			if(!QuestionsTime.getInstance().getEconomy().isPresent())
				this.currentStep++;
			break;
		case 6:
			if(Integer.valueOf(previousResponse) <= 0)
				this.moneyPrize = 0;
			else
				this.moneyPrize = Integer.valueOf(previousResponse);
			break;
		case 7:
			if(!QuestionsTime.getInstance().getEconomy().isPresent())
				this.currentStep += 3;
			break;
		case 8:
			if(this.previousResponse.equals("no"))
				this.currentStep += 2;
			break;
		case 9:
			announceMalus = previousResponse.equals("yes");
			if(!QuestionsTime.getInstance().getEconomy().isPresent())
				this.currentStep++;
			break;
		case 10:
			if(Integer.valueOf(previousResponse) <= 0)
				this.moneyMalus = 0;
			else
				this.moneyMalus = Integer.valueOf(previousResponse);
			break;
		case 11:
			if(previousResponse.equals("no"))
				this.currentStep++;
			break;
		case 12:
			String timeHour = StringUtils.substringBefore(previousResponse, "h");
			String timeMinute = StringUtils.substringBetween(previousResponse, "h", "m");
			String timeSecond = StringUtils.substringBetween(previousResponse, "m", "s");
			timer = (Integer.valueOf(timeHour) * 3600) + (Integer.valueOf(timeMinute) * 60) + Integer.valueOf(timeSecond);
			break;
		case 13:
			 if(previousResponse.equals("no"))
				this.currentStep++;
			break;
		case 14:
			String timeHourAnswer = StringUtils.substringBefore(previousResponse, "h");
			String timeMinuteAnswer = StringUtils.substringBetween(previousResponse, "h", "m");
			String timeSecondAnswer = StringUtils.substringBetween(previousResponse, "m", "s");
			timeBetweenAnswer = (Integer.valueOf(timeHourAnswer) * 3600) + (Integer.valueOf(timeMinuteAnswer) * 60) + Integer.valueOf(timeSecondAnswer);
			break;
		case 15:
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
		this.halfConfirm = false;
	}

	public void finish(boolean ask) {
/*		if(this.itemsPrize.isEmpty())
			this.itemsPrize.add(ItemStack.of(ItemTypes.NONE, 1));*/
		ItemStack[] items = new ItemStack[this.itemsPrize.size()];
		items = this.itemsPrize.toArray(items);
		Prize prize = new Prize(this.moneyPrize, this.announcePrize, items);
		Malus malus = new Malus(this.moneyMalus, this.announceMalus);
		if(this.questionType == Types.MULTI) {
			QuestionMulti multiQ = QuestionMulti.builder().setAnswer(Byte.valueOf(answer))
			.setQuestion(question).setPrize(prize).setMalus(malus).setTimer(timer)
			.addPropositions(this.propositions).setTimeBetweenAnswer(this.timeBetweenAnswer).build();
			if(ask)
				QuestionsTime.getInstance().addQuestion(multiQ);
			ConfigHandler.serializeQuestion(multiQ);
		} else {
			Question q = Question.builder().setQuestion(this.question).setAnswer(this.answer)
					.setPrize(prize).setMalus(malus).setTimer(timer).setTimeBetweenAnswer(this.timeBetweenAnswer).build();
			if(ask)
				QuestionsTime.getInstance().addQuestion(q);
			ConfigHandler.serializeQuestion(q);
		}
	}

	public void setStop() {
		this.registerStep = this.currentStep;
		this.currentStep = -1;
		this.halfConfirm = false;
		this.confirm = false;
	}

	public void resume() {
		this.currentStep = this.registerStep-1;
		this.registerStep = 0;
		this.halfConfirm = false;
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

	public void setHalfConfirm() {
		this.halfConfirm = true;
		this.confirm = false;
	}

	public boolean isHalfConfirm() {
		return halfConfirm;
	}

	public List<String> getPropositions() {
		return propositions;
	}

	public void addItemPrize(ItemStack is) {
		for (ItemStack itemStack : this.itemsPrize) {
			if (itemStack.equalTo(is)) {
				itemStack.setQuantity(itemStack.getQuantity() + is.getQuantity());
				return;
			}
		}
		this.itemsPrize.add(is);
	}

}
