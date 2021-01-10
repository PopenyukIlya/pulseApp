package pulse.service.test;

import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pulse.controller.dto.*;
import pulse.controller.dto.test.ListAnswer;
import pulse.domain.User;
import pulse.domain.quiz.*;
import pulse.domain.repos.AnswerRepo;
import pulse.domain.repos.QuestionRepo;
import pulse.domain.repos.QuizProgressRepo;
import pulse.domain.repos.QuizRepo;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class TestService {

    @Autowired
    private QuizRepo quizRepo;

    @Autowired
    private ModelMapper modelMapper;

    @Autowired
    private AnswerRepo answerRepo;

    @Autowired
    private QuestionRepo questionRepo;

    @Autowired
    private QuizProgressRepo quizProgressRepo;

    public TestQuestion start(Long id, User user) {
        Quiz quiz = quizRepo.findById(id).get();
        QuizProgress quizProgress = new QuizProgress();
        quizProgress.setQuiz(quiz);
        quizProgress.setUser(user);
        quizProgress.setStatus(QuizStatus.STARTED);
        quizProgress = quizProgressRepo.save(quizProgress);
        TestQuestion testQuestion = null;
        List<Question> questions = quiz.getQuestions();
        for (Question question : questions) {
            if (question.getComplexity() <= 2) {
                testQuestion = modelMapper.map(question, TestQuestion.class);
            }
        }
        testQuestion.setTestStatus("started");
        quizProgress = quizProgressRepo.save(quizProgress);
        testQuestion.setProgress(quizProgress.getId());
        return testQuestion;
    }

    public TestQuestion check(User user) {
        List<QuizProgress> quizProgresses = quizProgressRepo.findByUser(user);
        TestQuestion testQuestion = null;
        Quiz quiz = null;
        for (QuizProgress quizProgress : quizProgresses) {
            if (quizProgress.getStatus().equals(QuizStatus.STARTED)) {
                quiz = quizProgress.getQuiz();
                List<Question> questions = quiz.getQuestions();
                for (Question question : questions) {
                    if (question.getComplexity() < 3) {
                        testQuestion = modelMapper.map(question, TestQuestion.class);
                        testQuestion.setProgress(quizProgress.getId());
                    }
                }
            }
        }
        return testQuestion;
    }

    public TestQuestion getQuestion(UserAnswerDto testAnswer) {//переделать
        // пришедшего ответа
        //тут добавлять в пасд
        TestQuestion testQuestion = new TestQuestion();
        QuizProgress quizProgress = quizProgressRepo.findById(testAnswer.getProgress()).get();
        Quiz  quiz = quizProgress.getQuiz();
        String testStatus="";

        List<ListAnswer> answers=testAnswer.getAnswers();
        List<Answer> userAnswers=new ArrayList<>();
        for (ListAnswer userAnswer:answers){
            Answer answer = answerRepo.findById(userAnswer.getId()).get();//тут чекаем трушность

            if (answer.isCorrect()) {//переделать логику верности ответа смотреть сколько правильных ответов
                testStatus="true";
            } else {
                testStatus="false";
            }
            userAnswers.add(answer);
        }
        quizProgress.getAnswers().addAll(userAnswers);

            Question passedQuestion=questionRepo.findById(testAnswer.getId()).get();
            quizProgress.getPassedQuestions().add(passedQuestion);
            quizProgressRepo.save(quizProgress);

            List<Question> passed = quizProgress.getPassedQuestions();
            List<Question> questions = quiz.getQuestions();
            for (Question question : questions) {
                if (question.getComplexity() <= 3 && !passed.contains(question)) {
                    //тут логика
                    // нахождение сложности по пульсу
                    testQuestion = modelMapper.map(question, TestQuestion.class);
                }
            }

        if (testQuestion.getId()==null) {
            testStatus=testStatus+" passed";
            quizProgress.setStatus(QuizStatus.ENDED);
        }
        testQuestion.setProgress(quizProgress.getId());
        testQuestion.setTestStatus(testStatus);
        quizProgressRepo.save(quizProgress);
        return testQuestion;
    }

    public TestResultDto result(Long id) {
        QuizProgress quizProgress = quizProgressRepo.findById(id).get();
        //взять вопросы
        Quiz quiz = quizProgress.getQuiz();
        AllQuizDto allQuizDto = modelMapper.map(quiz, AllQuizDto.class);
        List<AnswerDto> userAnswers = quizProgress.getAnswers()
                .stream().map(answer -> modelMapper.map(answer, AnswerDto.class)).collect(Collectors.toList());
        TestResultDto testResultDto = new TestResultDto();
        testResultDto.setAllQuizDto(allQuizDto);
        testResultDto.setUserAnswers(userAnswers);
        return testResultDto;
    }
}
