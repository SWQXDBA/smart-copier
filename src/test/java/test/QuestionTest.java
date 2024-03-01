package test;

import org.junit.jupiter.api.Test;
import org.swqxdba.smartconvert.SmartCopier;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

public class QuestionTest {

    @Test
    void doTest() {
        SmartCopier.setDebugMode(true);
        SmartCopier.setDebugOutPutDir(".");
        Question question = new Question();
        question.setAnswerAnalysis("answerAnalysis");
        QuestionDto questionDto = new QuestionDto();
        SmartCopier.copy(question, questionDto);
        System.out.println();
    }
}
