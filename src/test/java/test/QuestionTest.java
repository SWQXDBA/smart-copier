package test;


import io.github.swqxdba.smartcopier.bean.AbstractSmartCopierBasedProvider;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import io.github.swqxdba.smartcopier.SmartCopier;
import io.github.swqxdba.smartcopier.bean.BeanConvertProvider;
import io.github.swqxdba.smartcopier.bean.BeanConverter;
import test.model.Bank;
import test.model.Question;
import test.model.QuestionDto;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

public class QuestionTest {

    private SmartCopier SmartCopier = new SmartCopier();

    //测试集合内属性复制
    //测试不兼容属性复制
    //测试lombok链式setter
    @Test
    void doTest() {

        SmartCopier.setBeanConvertProvider(new AbstractSmartCopierBasedProvider(SmartCopier){

            @Override
            public boolean shouldConvert(@NotNull Class<?> from, @NotNull Class<?> to) {
                return from.getName().toLowerCase().contains("bank");
            }
        });
        Question question = new Question();
        question.setId(123L);
        question.setAnswerAnalysis("answerAnalysis");
        List<Bank> banks = new ArrayList<>();
        banks.add(new Bank("bankName"));
        question.setBanks(banks);
        QuestionDto questionDto = new QuestionDto();
        SmartCopier.copy(question, questionDto);

        Assertions.assertEquals(question.getId(),questionDto.getId());
        Assertions.assertEquals(question.getAnswerAnalysis(),questionDto.getAnswerAnalysis());
        Assertions.assertEquals(question.getBanks().size(),questionDto.getBanks().size());
        Assertions.assertEquals(question.getBanks().get(0).getName(),questionDto.getBanks().get(0).getName());
    }
}
