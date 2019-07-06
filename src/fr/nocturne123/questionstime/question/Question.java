package fr.nocturne123.questionstime.question;

import com.google.common.base.Strings;
import com.google.common.reflect.TypeToken;
import fr.nocturne123.questionstime.QuestionsTime;
import fr.nocturne123.questionstime.question.component.Malus;
import fr.nocturne123.questionstime.question.component.Prize;
import ninja.leaping.configurate.ConfigurationNode;
import ninja.leaping.configurate.objectmapping.ObjectMappingException;

import java.util.Optional;

public class Question {

	private String question;
	private String answer;
	private Prize prize;
	private Malus malus;
	private int timer;
	private int timeBetweenAnswer;

	protected Question(QuestionBuilder builder) {
	    this.question = builder.question;
	    this.answer = builder.answer;
	    this.timer = builder.timer;
	    this.prize = builder.prize;
	    this.malus =builder.malus;
	    this.timeBetweenAnswer = builder.timeBetweenAnswer;
    }

    public static QuestionBuilder builder() {
	    return new QuestionBuilder();
    }

	public Optional<Prize> getPrize() {
		return Optional.ofNullable(this.prize);
	}

	public static Types getType(ConfigurationNode questionnode) {
		if(questionnode.getNode("question").getValue() != null && questionnode.getNode("answer").getValue() != null) {
			if(questionnode.getNode("proposition").getValue() != null)
				return Types.MULTI;
			return Types.SIMPLE;
		}
		return Types.ERROR;
	}

	public String getAnswer() {
		return answer;
	}

	public String getQuestion() {
		return this.question;
	}

	public Types getType() {
		return Types.SIMPLE;
	}

	public Optional<Malus> getMalus() {
		return Optional.ofNullable(malus);
	}

	public int getTimer() {
		return this.timer;
	}

	public boolean isTimed() {
		return timer > 0;
	}

	public boolean isTimeBetweenAnswer() {
		return this.timeBetweenAnswer > 0;
	}

	public int getTimeBetweenAnswer() {
		return timeBetweenAnswer;
	}

	public enum Types {
		SIMPLE, MULTI, ERROR
	}

	public static class QuestionBuilder<T extends QuestionBuilder<T>> {

        protected String question;
		protected String answer;
		protected Prize prize;
		protected Malus malus;
		protected int timer;
		protected int timeBetweenAnswer;

        public T setQuestion(String question) {
            this.question = question;
            return (T) this;
        }

        public T setAnswer(String answer) {
            this.answer = answer;
            return (T) this;
        }

        public T setMalus(Malus malus) {
            this.malus = malus;
            return (T) this;
        }

        public T setMalus(ConfigurationNode malusNode) {
			try {
				this.malus = malusNode.getValue(TypeToken.of(Malus.class));
			} catch (ObjectMappingException e) {
				QuestionsTime.getInstance().getLogger().error("Error when reading the malus node");
				e.printStackTrace();
			}
            return (T) this;
        }

        public T setPrize(Prize prize) {
            this.prize = prize;
            return (T) this;
        }

        public T setPrize(ConfigurationNode prizeNode) {
			try {
				this.prize = prizeNode.getValue(TypeToken.of(Prize.class));
			} catch (ObjectMappingException e) {
				QuestionsTime.getInstance().getLogger().error("Error when reading the prize node");
				e.printStackTrace();
			}
			return (T) this;
        }

        public T setTimeBetweenAnswer(int timeBetweenAnswer) {
            this.timeBetweenAnswer = timeBetweenAnswer;
            return (T) this;
        }

        public T setTimer(int timer) {
            this.timer = timer;
            return (T) this;
        }

        public Question build() {
            if(Strings.isNullOrEmpty(this.question))
                throw new NullPointerException("The question is null or empty");
            if(Strings.isNullOrEmpty(this.answer))
                throw  new NullPointerException("The answer is null");
            return new Question(this);
        }

    }


}
