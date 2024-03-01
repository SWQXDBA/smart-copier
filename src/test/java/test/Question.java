package test;


import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;
import lombok.experimental.FieldNameConstants;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * 试题 实体类。
 * 可以作为试题库中的试题，或者作为答卷中的试题使用(此时可以记录部分题型的答案)
 *
 * @author ysc
 * @since 2023-12-12
 */
@Accessors(chain = true)
@Data
@EqualsAndHashCode(callSuper = false)
@FieldNameConstants
public class Question extends BaseEntity {

    /**
     * 试题题干
     */

  //  private String questionTitle;
//
//    /**
//     * 试题内容(保留字段 暂时没用)
//     */
//    private String questionContent;
//
//    /**
//     * 答案解析
//     */
    private String answerAnalysis;
//
//
 //   private Long questionBankId;
//
//    /**
//     * 源题id 外键 当作为考卷题目时连接题库中的原题 此时question_bank_id为0
//     */
//    private Long sourceQuestionId;
//
//    /**
//     * 答卷id 外键 用作答卷考题时使用
//     */
//    private Long answerSheetId;
//
//    /**
//     * 是否已经回答(作为答卷中题目时使用) 用于计算正确率
//     */
//    private Boolean answered;
//
//
////    private QuestionTypeEnum questionType;
//
//
//    /**
//     * 题目实际分值 在生成试卷时设置
//     */
//    private Float actualScore;
//
//
//
//    /**
//     * 图片链接
//     */
////    private List<String> imageUrls;
//
////    /**
////     * 填空题答案 详情字段
////     */
////    private List<QuestionRightBlankDto> rightBlanks = new ArrayList<>();
////
////
////    /**
////     * 可选项 试题选项 详情字段
////     */
////    private List<QuestionOptionDto> options = new ArrayList<>();
////
////
////    /**
////     * 填空题考生的回答 详情字段
////     */
////    private List<QuestionAnswerBlankDto> answerBlanks = new ArrayList<>();
//
//
//    /**
//     * 判断题回答
//     */
//    private Boolean yesOrNoAnswer;
//
//    /**
//     * 判断题 正确答案
//     */
//    private Boolean yesOrNoRightAnswer;
//
//    /**
//     * 判断题 正确字符串
//     */
//    private String yesOrNoYesStr = "正确";
//
//    /**
//     * 判断题 错误字符串
//     */
//    private String yesOrNoNoStr = "错误";


}
